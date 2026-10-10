/*
  WebMdnsAdvertiser.java

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

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Publishes the web interface on each bound LAN address, with JmDNS conflict negotiation.
 */
@Slf4j
final class WebMdnsAdvertiser {
    static final String HOST_NAME = "fireflyluciferin";
    static final String SERVICE_TYPE = "_http._tcp.local.";
    private final DnsFactory factory;
    private final Supplier<String> computerName;
    private final Map<JmDNS, Registration> responders = new LinkedHashMap<>();
    private String hostName;
    private ScheduledExecutorService executor;

    /**
     * Creates an advertiser using JmDNS and the local computer name.
     */
    WebMdnsAdvertiser() {
        this(JmDNS::create);
    }
    /**
     * Creates an advertiser with a supplied responder factory and the local computer name.
     *
     * @param factory the factory used to create responders
     */
    WebMdnsAdvertiser(DnsFactory factory) {
        this(factory, WebMdnsAdvertiser::localComputerName);
    }

    /**
     * Creates an advertiser with supplied responder creation and computer name lookup.
     *
     * @param factory      the factory used to create responders
     * @param computerName supplies the computer name when a hostname conflict occurs
     */
    WebMdnsAdvertiser(DnsFactory factory, Supplier<String> computerName) {
        this.factory = factory;
        this.computerName = computerName;
    }

    /**
     * Environment names also work when the OS hostname cannot be resolved through DNS.
     *
     * @return the computer name, or {@code null} when it cannot be determined
     */
    private static String localComputerName() {
        try {
            for (String variable : new String[]{"COMPUTERNAME", "HOSTNAME"}) {
                String name = System.getenv(variable);
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
            InetAddress local = InetAddress.getLocalHost();
            String name = local.getHostName();
            return name.equals(local.getHostAddress()) ? null : name;
        } catch (IOException | SecurityException e) {
            log.debug("Unable to determine computer hostname: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Builds a DNS-safe fallback hostname from the computer name, reserving room for conflict suffixes.
     *
     * @param computerName the computer name, which may be null or fully qualified
     * @return the prefixed host label, or {@link #HOST_NAME} when no usable name is available
     */
    static String mdnsHostName(String computerName) {
        if (computerName == null || computerName.isBlank()) {
            return HOST_NAME;
        }
        // Use the host label of an FQDN, and restrict it to DNS hostname characters.
        String label = computerName.trim().split("\\.", 2)[0].toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-").replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
        // Keep the label below 63 bytes, leaving room for JmDNS conflict suffixes.
        int maxComputerLength = 53 - HOST_NAME.length() - 1;
        if (label.length() > maxComputerLength) {
            label = label.substring(0, maxComputerLength).replaceAll("-+$", "");
        }
        return label.isEmpty() ? HOST_NAME : HOST_NAME + "-" + label;
    }

    /**
     * Closes a responder and logs any failure without interrupting cleanup of other responders.
     *
     * @param dns the responder to close
     */
    private static void close(JmDNS dns) {
        try {
            dns.close();
        } catch (IOException | RuntimeException e) {
            log.warn("Unable to close web mDNS responder: {}", e.getMessage());
        }
    }

    /**
     * Registration and probing run off the web server startup thread.
     * Repeated calls are ignored while the advertiser is running.
     *
     * @param addresses the addresses bound by the HTTP server; only usable LAN IPv4 addresses are advertised
     */
    synchronized void start(Collection<InetAddress> addresses) {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "firefly-web-mdns");
            thread.setDaemon(true);
            return thread;
        });
        ScheduledExecutorService owner = executor;
        addresses.stream().distinct()
                .filter(address -> address instanceof Inet4Address && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress() && !address.isMulticastAddress() && !address.isAnyLocalAddress())
                .forEach(address -> owner.execute(() -> register(address, HOST_NAME, owner)));
        // Host conflicts can also be detected after initial registration.
        owner.scheduleWithFixedDelay(() -> logHostNames(owner), 1, 5, TimeUnit.SECONDS);
    }

    /**
     * Registers the HTTP service on one address and checks its negotiated hostname.
     * Failures are logged and the responder is closed.
     *
     * @param address       the local address to advertise
     * @param requestedName the initial hostname, without the {@code .local} suffix
     * @param owner         the executor for this startup cycle; obsolete tasks are ignored
     */
    private synchronized void register(InetAddress address, String requestedName, ScheduledExecutorService owner) {
        if (executor != owner) {
            return;
        }
        JmDNS dns = null;
        try {
            dns = factory.create(address, requestedName);
            // JmDNS probes host records and automatically negotiates -1, -2, etc. Service instance name collisions are negotiated separately by JmDNS.
            dns.registerService(ServiceInfo.create(SERVICE_TYPE, Constants.FIREFLY_LUCIFERIN,
                    Constants.CONFIG_SERVER_DEFAULT_PORT, 0, 0, Map.of("path", "/")));
            Registration registration = new Registration(address, requestedName, "");
            responders.put(dns, registration);
            refreshHostName(dns, registration, owner);
        } catch (IOException | RuntimeException e) {
            log.warn("Unable to advertise web interface via mDNS on {}: {}", address.getHostAddress(), e.getMessage());
            if (dns != null) {
                close(dns);
            }
        }
    }

    /**
     * Checks responders for hostname changes and conflicts detected after registration.
     *
     * @param owner the executor for this startup cycle; obsolete tasks are ignored
     */
    private synchronized void logHostNames(ScheduledExecutorService owner) {
        if (executor != owner) {
            return;
        }
        for (var entry : List.copyOf(responders.entrySet())) {
            refreshHostName(entry.getKey(), entry.getValue(), owner);
        }
    }

    /**
     * Logs a changed hostname and switches to the computer name after a base-name conflict.
     * Called while holding this advertiser's monitor.
     *
     * @param dns      the responder whose negotiated hostname is checked
     * @param previous the registration and last logged hostname
     * @param owner    the executor used to register a replacement responder if needed
     */
    @SuppressWarnings("all")
    private void refreshHostName(JmDNS dns, Registration previous, ScheduledExecutorService owner) {
        String host = dns.getHostName();
        if (host == null) {
            return;
        }
        // A changed base hostname means JmDNS detected a conflict, including a late one.
        if (HOST_NAME.equals(previous.requestedName()) && !(HOST_NAME + ".local.").equalsIgnoreCase(host)) {
            if (hostName == null) {
                hostName = mdnsHostName(computerName.get());
            }
            if (!HOST_NAME.equals(hostName)) {
                responders.remove(dns);
                close(dns);
                register(previous.address(), hostName, owner);
                return;
            }
        }
        if (!host.equals(previous.hostName())) {
            String urlHost = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
            log.info("Web interface available via mDNS at http://{}:{} (interface {})",
                    urlHost, Constants.CONFIG_SERVER_DEFAULT_PORT, previous.address().getHostAddress());
        }
        responders.put(dns, new Registration(previous.address(), previous.requestedName(), host));
    }

    /**
     * Withdraws records and closes multicast sockets, including on application shutdown.
     * Resets the advertiser so it can be started again.
     */
    synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        responders.keySet().forEach(WebMdnsAdvertiser::close);
        responders.clear();
        hostName = null;
    }

    /**
     * Creates a responder bound to a specific network address.
     */
    @FunctionalInterface
    interface DnsFactory {
        /**
         * Creates a responder with the requested initial hostname.
         *
         * @param address  the local address to bind
         * @param hostName the initial hostname, without the {@code .local} suffix
         * @return the new responder
         * @throws IOException when the multicast socket cannot be created
         */
        JmDNS create(InetAddress address, String hostName) throws IOException;
    }

    /**
     * Tracks the requested name and the last logged negotiated name for one responder.
     *
     * @param address       the bound local address
     * @param requestedName the initial hostname requested for this responder
     * @param hostName      the last logged hostname, or an empty string before the first log
     */
    private record Registration(InetAddress address, String requestedName, String hostName) {
    }
}
