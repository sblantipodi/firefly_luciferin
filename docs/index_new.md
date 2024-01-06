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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.13.2).   
- **To make room for the new features Luciferin has dropped support for 1 MB devices like the old ESP8266 Lite or ESP-01.** 
Standard ESP8266 4MB is still obviously supported and it will for a long time. 
**All people running Luciferin on an ESP8266 must manually reinstall and reconfigure the Glow Worm firmware** as the ESP8266 will be "formatted" to allow the use of the new 4MB file system. `Web Installer` is available [here](https://sblantipodi.github.io/glow_worm_luciferin/).
- Screen capture is restarted automatically once an MQTT disconnection/reconnection occurs. Closes [#162](https://github.com/sblantipodi/firefly_luciferin/issues/162). 
- The changelog is now presented in WebView format and allows you to access the Wiki about new features. (This feature is visible from the next update because Firefly Luciferin needs to be updated first).
- There were misalignments in the screen capture zones on newer AMD graphics cards. Fixed.
- Improved HDR support.
- Color temperature adjustments produces a greenish tint on LED strips with a particular color order. Fixed.
- Minor UI fixes.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.16.1).

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.12.9).
- **Hotfix release:** Fixed a regression that doesn't permit to drive LEDs via USB when MQTT is enabled. 
- **Introducing the [Luciferin surround lighting with satellites](https://github.com/sblantipodi/firefly_luciferin/wiki/Surround-lighting-with-satellites). Closes [#97](https://github.com/sblantipodi/firefly_luciferin/issues/97).**
- **[Added Wayland support for Linux.](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support#luciferin-supports-wayland)** Thanks @h7io for the contribution to this feature. Closes [#130](https://github.com/sblantipodi/firefly_luciferin/issues/130).
- **It's now possible to [**disable Glow Worm device auto discovery**](https://github.com/sblantipodi/firefly_luciferin/wiki/Static-IP-and-auto-discovery) in Firefly Luciferin PC software.** This is useful when PC and ESP lives in separate VLANs/Subnets. Closes [#132](https://github.com/sblantipodi/firefly_luciferin/issues/132).
- **Added the possibility to average the color of the screen on all the LEDs**. 
- **Big performance improvements for [Linux](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support) while running on X11.**
- **Added an optimization for [Linux users that is specific for NVIDIA GPUs](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support#nvidia-cuda).** Thanks to @Phoshi for the support on this feature.
- **Ram usage improvements.**
- **UI/UX improvements.** Revamped title bar, one left click on tray icon now open settings. Double left click on tray icon starts/stops screen capture, right left click opens the menu as usual. (Windows only, Linux version has no tray bar).
- Added an option during the installation process to create a desktop shortcut to Firefly Luciferin (Windows only).
- Added an option during the installation process to create a start menu shortcut to Firefly Luciferin (Windows only).
- Potential off-heap memory leak on Linux. Fixed. Thanks @jypma for fixing this issue. Closes [#147](https://github.com/sblantipodi/firefly_luciferin/issues/147).
- Firefly Luciferin caused a brief audio stutter on some systems during startup. Fixed. 
- Fixed sporadic crashes on ESP32-S3 devices.
- Fixed an issue that prevented Glow Worm Luciferin firmware to be [flashed using external tools like esptool](https://github.com/sblantipodi/firefly_luciferin/wiki/How-to-flash-Glow-Worm-Luciferin-firmware-via-esptool).
- Upgrade to Java 21 and JavaFX 21.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.15.3).

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
