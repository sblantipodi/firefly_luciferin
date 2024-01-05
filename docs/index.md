<style>
  .footer {
    display: none;
  }
  .body {
    color: #202020;
    background-color: #F5F5F5;
  }
  .px-3 {
    padding-right: 30px !important;
    padding-left: 10px !important;
  }
  .my-5 {
    margin-top: 10px !important;
    margin-bottom: 10px !important;
  }
</style>

<img align="right" width="100" height="100" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/data/img/luciferin_logo.png">
### In this release:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.12.9).
- **Introducing the Luciferin surround lighting with satellites.**
- **Added Wayland support for Linux.** Thanks @h7io for the contribution to this feature.
- **It's now possible to **disable Glow Worm device auto discovery** in Firefly Luciferin PC software.** This is useful
  when PC and ESP lives in separate VLANs/Subnets.
- **Added the possibility to average the color of the screen on all the LEDs**.
- **Big performance improvements for Linux while running on X11.**
- **Added an optimization for Linux users that is specific for NVIDIA GPUs.** Thanks to @Phoshi for the support on this
  feature.
- **Ram usage improvements.**
- **UI/UX improvements.** Revamped title bar, one left click on tray icon now open settings. Double left click on tray
  icon starts/stops screen capture, right left click opens the menu as usual. (Windows only, Linux version has no tray
  bar).
- Added an option during the installation process to create a desktop shortcut to Firefly Luciferin (Windows only).
- Added an option during the installation process to create a start menu shortcut to Firefly Luciferin (Windows only).
- Potential off-heap memory leak on Linux. Fixed. Thanks @jypma for fixing this issue.
- Firefly Luciferin caused a brief audio stutter on some systems during startup. Fixed.
- Fixed sporadic crashes on ESP32-S3 devices.
- Fixed an issue that prevented Glow Worm Luciferin firmware to be flashed using external tools like esptool.
- Upgrade to Java 21 and JavaFX 21.
- Arduino Bootstrapper update (v.1.15.3).

### In the previous release:

- Hotfix release: Fixed a regression that doesn't permit to drive LEDs via USB when MQTT is enabled. **This issue
  affects Firefly Luciferin only, there is no need to update the firmware.**.
