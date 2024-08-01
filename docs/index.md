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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.15.4)
- [Initial setup](https://github.com/sblantipodi/firefly_luciferin/wiki/Quick-start#install-firefly-luciferin-java-fast-screen-capture-software-on-your-pc)
  has been simplified by automatically using the right settings for Full/Light firmware.
- [Web installer](https://sblantipodi.github.io/glow_worm_luciferin/) improvements on newer ESP devices.
- [Power saving](https://github.com/sblantipodi/firefly_luciferin/wiki/Power-saving-features) feature is now enabled by
  default.
- Fixed an issue that prevented the user to select the preferred capture method.
- Fixed an issue that prevented LilyGO-T-POE-Pro and ESP32-POE-WROVER from connecting to Ethernet correctly.
- It took too long to start the
  device [reset process](https://github.com/sblantipodi/firefly_luciferin/wiki/Device-reset) when MQTT server is not
  available. Fixed.
- Technicalities: Switched to the new ZGC Generational.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.18.1).

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.14.5)
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

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
