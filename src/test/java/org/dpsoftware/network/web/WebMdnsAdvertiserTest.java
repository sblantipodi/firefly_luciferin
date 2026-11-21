/*
  WebMdnsAdvertiserTest.java

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

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.junit.jupiter.api.Test;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.NameRegister;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebMdnsAdvertiserTest {
    @Test
    void computerNameIsNotReadWithoutAConflict() throws Exception {
        JmDNS dns = mock(JmDNS.class);
        when(dns.getHostName()).thenReturn("fireflyluciferin.local.");
        @SuppressWarnings("unchecked")
        Supplier<String> computer = mock(Supplier.class);
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser((address, name) -> dns, computer);
        try {
            advertiser.start(List.of(InetAddress.getByName("192.168.1.10")));
            verify(dns, timeout(3000)).registerService(any());
            synchronized (advertiser) {
                assertEquals("http://fireflyluciferin.local:8091/", MainSingleton.getInstance().webInterfaceUrl);
            }
            advertiser.stop();
            assertEquals("http://localhost:8091/", MainSingleton.getInstance().webInterfaceUrl);
            verifyNoInteractions(computer);
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void conflictUsesComputerNameAndAllowsFurtherNumericConflicts() throws Exception {
        InetAddress lan = InetAddress.getByName("192.168.1.10");
        JmDNS base = mock(JmDNS.class);
        JmDNS fallback = mock(JmDNS.class);
        when(base.getHostName()).thenReturn("fireflyluciferin-1.local.");
        when(fallback.getHostName()).thenReturn("fireflyluciferin-test-pc-1.local.");
        WebMdnsAdvertiser.DnsFactory factory = mock(WebMdnsAdvertiser.DnsFactory.class);
        when(factory.create(lan, WebMdnsAdvertiser.HOST_NAME)).thenReturn(base);
        when(factory.create(lan, "fireflyluciferin-test-pc")).thenReturn(fallback);
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser(factory, () -> "Test-PC");
        try {
            advertiser.start(List.of(lan));
            verify(fallback, timeout(3000)).registerService(any());
            synchronized (advertiser) {
                assertEquals("http://fireflyluciferin-test-pc-1.local:8091/", MainSingleton.getInstance().webInterfaceUrl);
            }
            advertiser.stop();
            verify(base).close();
            verify(fallback).close();
            verify(factory).create(lan, WebMdnsAdvertiser.HOST_NAME);
            verify(factory).create(lan, "fireflyluciferin-test-pc");
            verifyNoMoreInteractions(factory);
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void lateConflictAlsoSwitchesToComputerName() throws Exception {
        InetAddress lan = InetAddress.getByName("192.168.1.10");
        AtomicReference<String> negotiatedName = new AtomicReference<>("fireflyluciferin.local.");
        JmDNS base = mock(JmDNS.class);
        JmDNS fallback = mock(JmDNS.class);
        when(base.getHostName()).thenAnswer(invocation -> negotiatedName.get());
        when(fallback.getHostName()).thenReturn("fireflyluciferin-test-pc.local.");
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser(
                (address, name) -> name.equals(WebMdnsAdvertiser.HOST_NAME) ? base : fallback, () -> "Test-PC");
        try {
            advertiser.start(List.of(lan));
            verify(base, timeout(3000)).registerService(any());
            synchronized (advertiser) {
                assertEquals("http://fireflyluciferin.local:8091/", MainSingleton.getInstance().webInterfaceUrl);
            }
            negotiatedName.set("fireflyluciferin-1.local.");
            verify(fallback, timeout(7000)).registerService(any());
            synchronized (advertiser) {
                assertEquals("http://fireflyluciferin-test-pc.local:8091/", MainSingleton.getInstance().webInterfaceUrl);
            }
            advertiser.stop();
            verify(base).close();
            verify(fallback).close();
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void missingComputerNameKeepsJmdnsNumericFallback() throws Exception {
        JmDNS dns = mock(JmDNS.class);
        when(dns.getHostName()).thenReturn("fireflyluciferin-1.local.");
        WebMdnsAdvertiser.DnsFactory factory = mock(WebMdnsAdvertiser.DnsFactory.class);
        InetAddress lan = InetAddress.getByName("192.168.1.10");
        when(factory.create(lan, WebMdnsAdvertiser.HOST_NAME)).thenReturn(dns);
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser(factory, () -> null);
        try {
            advertiser.start(List.of(lan));
            verify(dns, timeout(3000)).registerService(any());
            advertiser.stop();
            verify(factory).create(lan, WebMdnsAdvertiser.HOST_NAME);
            verifyNoMoreInteractions(factory);
            verify(dns).close();
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void computerNamesBecomeValidMdnsHostLabels() {
        assertEquals("fireflyluciferin-desktop-abc", WebMdnsAdvertiser.mdnsHostName("DESKTOP-ABC"));
        assertEquals("fireflyluciferin-linux-pc", WebMdnsAdvertiser.mdnsHostName("linux-pc.example.org"));
        assertEquals("fireflyluciferin-my-pc", WebMdnsAdvertiser.mdnsHostName("  My__PC!  "));
        assertEquals(WebMdnsAdvertiser.HOST_NAME, WebMdnsAdvertiser.mdnsHostName(null));
        assertEquals(WebMdnsAdvertiser.HOST_NAME, WebMdnsAdvertiser.mdnsHostName(" "));
        assertEquals(WebMdnsAdvertiser.HOST_NAME, WebMdnsAdvertiser.mdnsHostName("___"));
    }

    @Test
    void longComputerNamesLeaveRoomForConflictSuffixes() throws Exception {
        String name = WebMdnsAdvertiser.mdnsHostName("pc-" + "a".repeat(100));
        assertTrue(name.length() <= 53);
        assertTrue(name.matches("[a-z0-9]+(?:-[a-z0-9]+)*"));
        assertEquals(name + "-1.local.", NameRegister.Factory.getRegistry().incrementName(
                InetAddress.getByName("192.168.1.10"), name + ".local.", NameRegister.NameType.HOST));
    }

    @Test
    void advertisesOnlyBoundLanAddressesAndClosesOnStop() throws Exception {
        InetAddress lan = InetAddress.getByName("192.168.1.10");
        WebMdnsAdvertiser.DnsFactory factory = mock(WebMdnsAdvertiser.DnsFactory.class);
        JmDNS dns = mock(JmDNS.class);
        when(factory.create(lan, WebMdnsAdvertiser.HOST_NAME)).thenReturn(dns);
        when(dns.getHostName()).thenReturn("fireflyluciferin.local.");
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser(factory, () -> "Test-PC");
        try {
            advertiser.start(List.of(lan, lan, InetAddress.getByName("127.0.0.1"),
                    InetAddress.getByName("::1"), InetAddress.getByName("169.254.1.10")));
            verify(dns, timeout(3000)).registerService(any(ServiceInfo.class));
            advertiser.stop();
            var service = org.mockito.ArgumentCaptor.forClass(ServiceInfo.class);
            verify(dns).registerService(service.capture());
            assertEquals(WebMdnsAdvertiser.SERVICE_TYPE, service.getValue().getType());
            assertEquals(Constants.CONFIG_SERVER_DEFAULT_PORT, service.getValue().getPort());
            assertEquals("/", service.getValue().getPropertyString("path"));
            verify(factory).create(lan, WebMdnsAdvertiser.HOST_NAME);
            verifyNoMoreInteractions(factory);
            verify(dns).close();
            advertiser.stop();
            verify(dns, times(1)).close();
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void registrationFailureClosesResponderAndContinuesWithNextInterface() throws Exception {
        InetAddress first = InetAddress.getByName("192.168.1.10");
        InetAddress second = InetAddress.getByName("192.168.2.10");
        JmDNS broken = mock(JmDNS.class);
        JmDNS healthy = mock(JmDNS.class);
        doThrow(new IOException("Multicast unavailable")).when(broken).registerService(any());
        when(healthy.getHostName()).thenReturn("fireflyluciferin.local.");
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser((address, name) -> address.equals(first) ? broken : healthy);
        try {
            advertiser.start(List.of(first, second));
            verify(healthy, timeout(3000)).registerService(any());
            advertiser.stop();
            verify(broken).close();
            verify(healthy).close();
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void startIsIdempotentAndCanRestartAfterStop() throws Exception {
        InetAddress lan = InetAddress.getByName("192.168.1.10");
        JmDNS first = mock(JmDNS.class);
        JmDNS second = mock(JmDNS.class);
        WebMdnsAdvertiser.DnsFactory factory = mock(WebMdnsAdvertiser.DnsFactory.class);
        when(factory.create(lan, WebMdnsAdvertiser.HOST_NAME)).thenReturn(first, second);
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser(factory, () -> "Test-PC");
        try {
            advertiser.start(List.of(lan));
            advertiser.start(List.of(lan));
            verify(first, timeout(3000)).registerService(any());
            advertiser.stop();
            advertiser.start(List.of(lan));
            verify(second, timeout(3000)).registerService(any());
            advertiser.stop();
            verify(factory, times(2)).create(lan, WebMdnsAdvertiser.HOST_NAME);
            verify(first).close();
            verify(second).close();
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void mdnsFailureDoesNotEscapeStartup() throws Exception {
        CountDownLatch attempted = new CountDownLatch(1);
        WebMdnsAdvertiser advertiser = new WebMdnsAdvertiser((address, name) -> {
            attempted.countDown();
            throw new IOException("UDP 5353 unavailable");
        });
        try {
            assertDoesNotThrow(() -> advertiser.start(List.of(InetAddress.getByName("192.168.1.10"))));
            assertTrue(attempted.await(3, TimeUnit.SECONDS));
        } finally {
            advertiser.stop();
        }
    }

    @Test
    void jmdnsNegotiatesSequentialDashSuffixesForHostnameConflicts() throws Exception {
        // Exercise the actual library naming policy used by its host conflict handling.
        NameRegister registry = NameRegister.Factory.getRegistry();
        InetAddress lan = InetAddress.getByName("192.168.1.10");
        String first = registry.incrementName(lan, "fireflyluciferin.local.", NameRegister.NameType.HOST);
        assertEquals("fireflyluciferin-1.local.", first);
        assertEquals("fireflyluciferin-2.local.", registry.incrementName(lan, first, NameRegister.NameType.HOST));
    }
}
