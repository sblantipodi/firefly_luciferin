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

### In this release:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.14.5).  
Connected devices will be [automatically updated](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management) after the Firefly Luciferin update.
- **Added support
  for [Ethernet devices](https://github.com/sblantipodi/firefly_luciferin/wiki/Compatible-Hardware#ethernet-devices).**
  Closes [#44](https://github.com/sblantipodi/glow_worm_luciferin/issues/44).
  - QuinLed-ESP32-Ethernet
  - QuinLed-Dig-Octa Brainboard-32-8L
  - LilyGO-T-ETH-POE
  - LilyGO-T-POE-Pro
  - WT32-ETH01
  - ESP32-ETHERNET-KIT-VE
  - ESP32-POE
  - ESP32-POE-WROVER
  - WESP32
- Improved [aspect ratio auto detection](https://github.com/sblantipodi/firefly_luciferin/wiki/Aspect-ratio) on wide
  screen format display.
- Display scaling setting now supports custom values.
  Closes [#211](https://github.com/sblantipodi/firefly_luciferin/issues/211).
- Added support for non-standard Documents folder paths, ex: `~/OneDrive/Documents`, existing configuration files will
  be automatically moved to your default path.
- Fixed an issue that prevented OTA fimware upload via PlatformIO.
- Improved German translations. Thanks @Maaaaarc for
  the [pull request](https://github.com/sblantipodi/firefly_luciferin/pull/210).
- Improved latency
  with [satellites](https://github.com/sblantipodi/firefly_luciferin/wiki/Surround-lighting-with-satellites).
- Improved latency when turning on/off the strip.
- Improved "smart button debounce" to eliminate unwanted button press due to noise on the board.
- Firefly
  Luciferin [auto update feature](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management) now
  compresses the firmware before sending it to the ESP8266 microcontroller. Fixes some occasional hangup during firmware
  update due to out of memory error.
- ESP32 file system layout has been adjusted to accommodate a larger firmware. This change has no impact if you use the
  automatic update feature with Firefly Luciferin. However, manually updating the firmware through
  the [Web Installer](https://sblantipodi.github.io/glow_worm_luciferin/) will erase your ESP32 device.
- Java/JavaFX 22, libs update, code refactor to avoid using deprecated methods, CI/CD pipeline improvements for faster
  build.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.17.0).

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.13.2).
- **To make room for the new features Luciferin Full firmware has dropped support for 1 MB devices** like the old
  ESP8266 Lite or ESP-01. Standard ESP8266 4MB is still obviously supported and it will for a long time.
  **All people running Luciferin Full firmware on an ESP8266 must manually reinstall and reconfigure the Glow Worm
  firmware** as the ESP8266 will be "formatted" to allow the use of the new 4MB file system. Please use
  the [Web Installer](https://sblantipodi.github.io/glow_worm_luciferin/) for the purpose, you may need to reconfigure,
  WiFi, GPIO, device name and baudrate. If for some reason you can't use
  the [Web Installer](https://sblantipodi.github.io/glow_worm_luciferin/), you can reconfigure your ESP8266 using
  the [alternative method](https://github.com/sblantipodi/firefly_luciferin/wiki/WiFi-and-MQTT-configuration-using-the-Luciferin-Access-Point).
- **Improved HDR support.**
- [Changelog](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management) is now presented in
  WebView format and allows you to access the [Wiki](https://github.com/sblantipodi/firefly_luciferin/wiki) about new
  features.(This feature is visible from the next update because Firefly Luciferin needs to be updated first).
- Screen capture is restarted automatically once an MQTT disconnection/reconnection occurs.
  Closes [#162](https://github.com/sblantipodi/firefly_luciferin/issues/162).
- Luciferin now defaults to the dark theme if your operating system is using a dark theme.
- There were misalignments in the screen capture zones on newer AMD graphics cards. Fixed.
- [Color temperature adjustments](https://github.com/sblantipodi/firefly_luciferin/wiki/Color-Temperature-and-White-Balance)
  produces a greenish tint on LED strips with a particular color order. Fixed.
- Firefly Luciferin icons vanishes after some seconds when using a dual monitor setup on GNOME, fixed.
- Minor UI fixes.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.16.1).

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
