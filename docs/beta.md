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
</style>


### In this release:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.13.2).  
- **To make room for the new features Luciferin Full firmware has dropped support for 1 MB devices** like the old
  ESP8266 Lite or ESP-01. Standard ESP8266 4MB is still obviously supported and it will for a long time.  
  **All people running Luciferin Full firmware on an ESP8266 must manually reinstall and reconfigure the Glow Worm
  firmware** as the ESP8266 will be "formatted" to allow the use of the new 4MB file system. Please use the Web
  Installer for the purpose, you may need to reconfigure, WiFi, GPIO, device name and baudrate. If for some reason you
  can't use the Web Installer, you can reconfigure your ESP8266 using the alternative method.
- **Improved HDR support.**
- Changelog is now presented in WebView format and allows you to access the Wiki about new features. (This feature is
  visible from the next update because Firefly Luciferin needs to be updated first).
- Screen capture is restarted automatically once an MQTT disconnection/reconnection occurs.
- Luciferin now defaults to the dark theme if your operating system is using a dark theme.
- There were misalignments in the screen capture zones on newer AMD graphics cards. Fixed.
- Color temperature adjustments produces a greenish tint on LED strips with a particular color order, fixed.
- Firefly Luciferin icons vanishes after some seconds when using a dual monitor setup on GNOME, fixed.
- Minor UI fixes.
- Arduino Bootstrapper update (v.1.16.1).

### In the previous release:

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
