/*
  HttpServerStartRetryTest.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.dpsoftware.network.web;

import com.sun.net.httpserver.HttpServer;
import org.dpsoftware.grabber.WebRtcStreamer;
import org.dpsoftware.network.McpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HttpServerStartRetryTest {

    /**
     * Verify that a failed LAN bind releases the already started loopback server before retrying.
     *
     * @throws Exception when network setup or server mocking fails
     */
    @Test
    void configRetryClosesPartiallyStartedInterfacesBeforeRetrying() throws Exception {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ExecutorService requests = mock(ExecutorService.class);
        HttpServer loopback = mock(HttpServer.class);
        HttpServer lan = mock(HttpServer.class);
        InetAddress lanAddress = InetAddress.getByName("192.0.2.1");
        when(loopback.getAddress()).thenReturn(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080));
        when(loopback.getExecutor()).thenReturn(requests);
        when(lan.getAddress()).thenReturn(new InetSocketAddress(lanAddress, 8080));
        NetworkInterface network = mock(NetworkInterface.class);
        when(network.isUp()).thenReturn(true);
        when(network.getInetAddresses()).thenAnswer(ignored -> Collections.enumeration(List.of(lanAddress)));
        try (var executors = mockStatic(Executors.class);
             var httpServers = mockStatic(HttpServer.class);
             var interfaces = mockStatic(NetworkInterface.class);
             var mdns = mockConstruction(WebMdnsAdvertiser.class);
             var streamers = mockConstruction(WebRtcStreamer.class)) {
            interfaces.when(NetworkInterface::getNetworkInterfaces)
                    .thenAnswer(ignored -> Collections.enumeration(List.of(network)));
            executors.when(() -> Executors.newSingleThreadScheduledExecutor(any(ThreadFactory.class))).thenReturn(scheduler);
            httpServers.when(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)))
                    .thenReturn(loopback)
                    .thenThrow(new BindException("Address already in use"))
                    .thenReturn(loopback, lan);
            ConfigServer server = new ConfigServer();
            try {
                server.start();
                verify(loopback).stop(0);
                verify(requests).shutdown();
                verify(mdns.constructed().getFirst(), never()).start(anyList());

                ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
                verify(scheduler).scheduleWithFixedDelay(retry.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
                retry.getValue().run();
                verify(loopback, times(2)).start();
                verify(lan).start();
                verify(mdns.constructed().getFirst()).start(List.of(InetAddress.getLoopbackAddress(), lanAddress));
                verify(scheduler).shutdown();
            } finally {
                server.stop();
            }
        }
    }

    /**
     * Verify the retry interval, repeated bind failures and cancellation after successful startup.
     *
     * @param configServer true to test the config server, false to test the MCP server
     * @throws Exception when network setup or server mocking fails
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retriesEveryTenSecondsUntilBindingSucceeds(boolean configServer) throws Exception {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        HttpServer endpoint = mock(HttpServer.class);
        when(endpoint.getAddress()).thenReturn(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080));
        try (var executors = mockStatic(Executors.class);
             var httpServers = mockStatic(HttpServer.class);
             var interfaces = mockStatic(NetworkInterface.class);
             var mdns = mockConstruction(WebMdnsAdvertiser.class);
             var streamers = mockConstruction(WebRtcStreamer.class)) {
            interfaces.when(NetworkInterface::getNetworkInterfaces).thenAnswer(ignored -> Collections.emptyEnumeration());
            executors.when(() -> Executors.newSingleThreadScheduledExecutor(any(ThreadFactory.class))).thenReturn(scheduler);
            httpServers.when(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)))
                    .thenThrow(new BindException("Address already in use"))
                    .thenThrow(new BindException("Address already in use"))
                    .thenReturn(endpoint);
            Lifecycle lifecycle = lifecycle(configServer);
            try {
                lifecycle.start.run();
                ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
                verify(scheduler).scheduleWithFixedDelay(retry.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));

                retry.getValue().run();
                verify(endpoint, never()).start();
                verify(scheduler, never()).shutdown();

                retry.getValue().run();
                verify(endpoint).start();
                verify(scheduler).shutdown();

                lifecycle.start.run();
                retry.getValue().run();
                verify(endpoint, times(1)).start();
                httpServers.verify(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)), times(3));
                verify(scheduler, times(1)).scheduleWithFixedDelay(any(Runnable.class), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
            } finally {
                lifecycle.stop.run();
            }
        }
    }

    /**
     * Verify that stopping cancels retries and stale callbacks cannot affect a later startup.
     *
     * @param configServer true to test the config server, false to test the MCP server
     * @throws Exception when network setup or server mocking fails
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stopCancelsRetriesAndStaleCallbackCannotRestartServer(boolean configServer) throws Exception {
        ScheduledExecutorService firstScheduler = mock(ScheduledExecutorService.class);
        ScheduledExecutorService nextScheduler = mock(ScheduledExecutorService.class);
        try (var executors = mockStatic(Executors.class);
             var httpServers = mockStatic(HttpServer.class);
             var interfaces = mockStatic(NetworkInterface.class);
             var mdns = mockConstruction(WebMdnsAdvertiser.class);
             var streamers = mockConstruction(WebRtcStreamer.class)) {
            interfaces.when(NetworkInterface::getNetworkInterfaces).thenAnswer(ignored -> Collections.emptyEnumeration());
            executors.when(() -> Executors.newSingleThreadScheduledExecutor(any(ThreadFactory.class)))
                    .thenReturn(firstScheduler, nextScheduler);
            httpServers.when(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)))
                    .thenThrow(new BindException("Address already in use"));
            Lifecycle lifecycle = lifecycle(configServer);
            try {
                lifecycle.start.run();
                ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
                verify(firstScheduler).scheduleWithFixedDelay(retry.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
                lifecycle.stop.run();
                verify(firstScheduler).shutdown();

                retry.getValue().run();
                httpServers.verify(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)), times(1));

                lifecycle.start.run();
                verify(nextScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
                retry.getValue().run();
                httpServers.verify(() -> HttpServer.create(any(InetSocketAddress.class), eq(0)), times(2));
            } finally {
                lifecycle.stop.run();
            }
            verify(nextScheduler).shutdown();
        }
    }

    /**
     * Create start and stop callbacks for the server under test.
     *
     * @param configServer true to create a config server, false to create an MCP server
     * @return lifecycle callbacks bound to the new server instance
     */
    private Lifecycle lifecycle(boolean configServer) {
        if (configServer) {
            ConfigServer server = new ConfigServer();
            return new Lifecycle(server::start, server::stop);
        }
        McpServer server = new McpServer();
        return new Lifecycle(server::start, server::stop);
    }

    private record Lifecycle(Runnable start, Runnable stop) {
    }
}
