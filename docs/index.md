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

### In the previous release:

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

### Two Versions Ago:

- ***Hotfix release: This issue only affects Firefly Luciferin; there is no need to update the firmware.***
- Luciferin restarted after seemingly random periods of time. Fixed. Closes [#324](https://github.com/sblantipodi/firefly_luciferin/issues/324).
- Reduced latency in screen capture on Linux Wayland.
- Prevented invalid values from being entered during
  the [Single Device Multi Screen](https://github.com/sblantipodi/firefly_luciferin/wiki/Multi-monitor-support) setup
  configuration.
- Fixed an issue that caused incorrect colors when using
  the [smoothing](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions) effect on Linux
  Wayland.
- Fixed an issue where settings could not be opened on Flatpak when an update was available.



[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
