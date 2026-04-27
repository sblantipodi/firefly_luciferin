<style>
.footer {
  display: none;
}
.px-3 {
  padding-right: 30px !important;
  padding-left: 10px !important;
}
.my-5 {
  margin-top: 10px !important;
  margin-bottom: 10px !important;
}
strong {
  font-weight: bold;
}
a {
  font-weight: bold;
  color: #E19A00FF;
}
</style>
<img align="right" width="100" height="100" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/data/img/luciferin_logo.png">

### In this release

- ***Update Requirement:*** This release requires `Glow Worm Luciferin` firmware **v5.24.7**.
- The **[Luciferin Official PCB](https://github.com/sblantipodi/firefly_luciferin/wiki/Ready-to-print-PCB) now supports
  the
  new [Luciferin Module](https://github.com/sblantipodi/firefly_luciferin/wiki/Ready-to-print-PCB#luciferin-module-for-the-official-pcb)
  **, compatible with smaller D1 Mini format boards, allowing hardware upgrades without replacing the PCB or redoing any
  soldering or cabling.
  The [Luciferin Ethernet Module](https://github.com/sblantipodi/firefly_luciferin/wiki/Ready-to-print-PCB#luciferin-ethernet-module-for-the-official-pcb)
  **adds Ethernet connectivity to boards that do not natively support it**.
- **Added support for
  Espressif [ESP32-C6 and ESP32-C5](https://github.com/sblantipodi/firefly_luciferin/wiki/Compatible-Hardware)**,
  enabling Wi-Fi 6 and 5 GHz band support on the C5.
  Closes [#92](https://github.com/sblantipodi/glow_worm_luciferin/issues/92).
- **Added support for SPI-based Ethernet devices**; GPIO pins for custom SPI configurations can now be assigned directly
  from the Web Interface or the Provisioning section in Firefly Luciferin.
- **Added support for
  the [Gledopto](https://github.com/sblantipodi/firefly_luciferin/wiki/Compatible-Hardware#pre-build-boards-support)
  series with Ethernet**.
- **Luciferin is now available via
  the [Windows Package Manager](https://github.com/sblantipodi/firefly_luciferin/wiki/Installers-and-binaries)**.
- The [Web Installer](https://sblantipodi.github.io/glow_worm_luciferin/) has been updated to support newer devices.
  Firmware installation is now significantly easier on devices with a native USB interface (non-UART).
- Wi-Fi and Ethernet can now coexist and operate simultaneously. Firefly Luciferin automatically prioritizes the wired
  Ethernet connection when both are available.
- Driving LEDs via USB is now considered stable on native USB devices (non-UART).
- [USB provisioning via Firefly Luciferin](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#provision-directly-from-firefly-luciferin)
  now supports custom device naming, preventing name collisions when provisioning multiple devices simultaneously.
- Users can
  now [configure the GPIO pin](https://github.com/sblantipodi/firefly_luciferin/wiki/Supported-GPIO-and-Baud-Rate#how-to-change-the-gpio-in-use)
  of the built-in LED on their microcontroller, which provides visual feedback on the device's connection status.
- The Windows installation footprint has also been significantly reduced.
- Enhanced [black bar detection](https://github.com/sblantipodi/firefly_luciferin/wiki/Aspect-ratio) with debounce logic
  to reduce false positives.
- Firmware upgraded to Arduino Core 3 (based on IDF5).
-
Improved [LED placement and alignment](https://github.com/sblantipodi/firefly_luciferin/wiki/Test-image-and-latency-test#custom-led-layout)
on the test canvas, with better spacing and resizing behavior when pressing `Shift+Tab`.
Closes [#406](https://github.com/sblantipodi/firefly_luciferin/issues/406).
- Improved Glow Worm device discovery and UDP communication reliability on complex or multi-network setups, with refined
  interface selection, static IP fallback preservation, explicit stream socket binding to the correct subnet, and
  extended debug logging. Closes [#400](https://github.com/sblantipodi/firefly_luciferin/issues/400).
- Added a reminder for Linux users to add their account to the `dialout` or `uucp` group for USB device access.
  Closes [#371](https://github.com/sblantipodi/firefly_luciferin/issues/371).
- Improved visibility of option buttons on
  the [Web Interface](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#luciferin-web-interface).
  Parts of the UI loading have been offloaded to the server side to reduce rendering overhead during heavy data
  streaming.
- Fixed GPIO0 being unable to drive the LED strip.
- Fixed [baud rate](https://github.com/sblantipodi/firefly_luciferin/wiki/Supported-GPIO-and-Baud-Rate#baud-rate)
  changes not being applied when the device was under load.
- Fixed incorrect Wi-Fi signal strength readings when two or more MQTT devices were in use and one of them was connected
  via Ethernet.
- Fixed duplicate devices appearing in the Devices tab when using multiple devices simultaneously.
- Fixed the [Web Interface](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#luciferin-web-interface)
  not reporting the correct framerate when driving LEDs via USB.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) updated to **v1.19.7**.

Users running a previous version of Luciferin can upgrade using
the [automatic update feature](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management),
available for both the PC software and the firmware.

### In the previous release:

- ***Update requirement***: requires `Glow Worm Luciferin` firmware (v5.23.6)
- **Manual LED Layout Configuration:** In addition to the automatic LED layout setup, you can
  now [manually configure your LED layout](https://github.com/sblantipodi/firefly_luciferin/wiki/Test-image-and-latency-test)
  directly from the test image. Closes [#349](https://github.com/sblantipodi/firefly_luciferin/issues/349)
  and [#343](https://github.com/sblantipodi/firefly_luciferin/issues/343).
  New options include:
    - Drag & drop LED zones to reposition them.
    - Resize LED zones.
    - Disable individual LEDs.
  - Create new custom LED zones and link them
    to [satellites for surround lighting](https://github.com/sblantipodi/firefly_luciferin/wiki/Surround-lighting-with-satellites).
  - Custom LED layouts are now supported
    within [profiles](https://github.com/sblantipodi/firefly_luciferin/wiki/Profiles), allowing Firefly Luciferin to
    automatically switch layouts based on the active app or game.
  - [Watch this feature in action on YouTube](https://youtu.be/j7IV9rQr7J8?si=lby1C7nJFvqjXNiA).
- **Improved Device Provisioning:** You don’t always need
  the [web installer](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#configure-wifi-ethernet-mqtt-on-glow-worm-luciferin-full-firmware-using-the-web-installer)
  to set up your device.
  Firefly Luciferin
  can [provision the firmware directly](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#provision-directly-from-firefly-luciferin),
  letting you configure WiFi and MQTT without touching the browser.
  This feature becomes extremely handy if your device suddenly goes offline. for instance, when your WiFi password
  changes and the device can’t reconnect. With Firefly Luciferin, you can bring it back to life in seconds, no
  reflashing required.
- **New Themes:** Added new visual [themes](https://github.com/sblantipodi/firefly_luciferin/wiki/Themes):
    - Light Silver
    - Light Cyan
    - Dark Amethyst
    - Dark Arctic
    - Dark Bronze Gold
    - Dark Emerald
    - Dark Ruby
- Improved [profile](https://github.com/sblantipodi/firefly_luciferin/wiki/Profiles) handling:
    - The active profile name is now shown in the window title and in the tray icon tooltip.
    - If an event triggers an automatic profile switch while the Settings window is open, the switch is postponed until
      the window is closed.
- Enhanced Log Level Configurability: You can now easily customize the application’s
  default [log level](https://github.com/sblantipodi/firefly_luciferin/wiki/Debug) by setting the `LUCIFERIN_LOG_LEVEL`
  environment variable.
- [LDR readings](https://github.com/sblantipodi/firefly_luciferin/wiki/Eye-care-and-night-mode#automatic-brightness-control-using-ldr)
  was causing occasional LED flickering, fixed.
- Fixed an issue
  preventing [satellites](https://github.com/sblantipodi/firefly_luciferin/wiki/Surround-lighting-with-satellites) from
  being added by IP if they were not reachable on the network.
- Fixed errors occurring
  when [satellites](https://github.com/sblantipodi/firefly_luciferin/wiki/Surround-lighting-with-satellites) were
  configured but turned off.
- Fixed an issue where Firefly Luciferin could not connect to a new output device after changing
  its [IP address](https://github.com/sblantipodi/firefly_luciferin/wiki/Static-IP-and-auto-discovery) without manually
  restarting the application.
- Introduced an AI-powered moderator for Issues and Pull Requests.
- Switched back to JDK25.

As always, users running a previous version of Luciferin can use
the [automatic update feature](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management) for
both the PC software and the firmware.

### Two Versions Ago:

- ***Hotfix release: This issue affects only Firefly Luciferin; no firmware update is required.***
- **Added a setting to adjust screen capture quality.** Balanced is recommended for most users, while higher quality
  offers more precision at the cost of higher resource usage, and lower quality reduces the load on the system. This is
  particularly useful on lower-end hardware or
  in [multi-screen](https://github.com/sblantipodi/firefly_luciferin/wiki/Multi-monitor-support#screen-capture-quality)
  setups with high resolutions.
- **Installation improvements** (Windows only – not required on Linux):
    - Added a checkbox option to launch Firefly Luciferin immediately
      after [installation](https://github.com/sblantipodi/firefly_luciferin/wiki/Installers-and-binaries).
    - Enhanced
      the [auto-update process](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management): no
      manual confirmation is needed in the UI anymore, and the app now starts
    automatically once the update is complete. This will take effect starting with the next update.
- Fixed a Linux-only issue that prevented Firefly Luciferin from running when multiple installation types were present
  on the system, such as .deb/.rpm, Snap, or Flatpak.
- Java/JavaFX 25, libs update, code refactor to avoid using deprecated methods, CI/CD pipeline improvements.

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
