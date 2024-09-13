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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.16.7).
- **Luciferin now has specific support
  for [AVX CPU extensions](https://github.com/sblantipodi/firefly_luciferin/wiki/Very-fast-capture#cpu-acceleration-using-avx-simd-extensions).
  **    
  AMD and Intel are investing a lot of resources in improving their CPU extensions. Next-generation CPUs contain various
  optimizations related to `Advanced Vector Extensions (AVX)` which are `Single Instruction, Multiple Data (SIMD)`
  extensions to the x86 instruction set architecture for microprocessors.
  AVX 512 and AVX 256 offer significant performance improvements and resource optimization benefits.
- USB/Serial communication has been redesigned:
  - Improved USB device recognition under Linux.
  - Serial devices may cause an infinite loop due to buggy COM port enumeration. Fixed.
- [Tray icon](https://github.com/sblantipodi/firefly_luciferin/wiki/Tray-icon-shortcuts) has been improved with new
  shortcuts.
- Added a workaround for an existing Windows issue that causes tray menu to stay behind the
  taskbar. [Closes #229](https://github.com/sblantipodi/firefly_luciferin/issues/229).
- Fixed an issue that prevented Firefly Luciferin from detecting Glow Worm Luciferin devices when the computer was
  connected to a VPN.
- Firefly Luciferin infinitely starts itself after PC standby / wake
  up. [Closes #228](https://github.com/sblantipodi/firefly_luciferin/issues/228).
- There are microcontrollers that has built/in LED. This LED can stay on and be annoying, it now follows
  the [device reset](https://github.com/sblantipodi/firefly_luciferin/wiki/Device-reset) behaviour.
- Fixed an issue that prevented Linux version to show the UI
  when [debug level](https://github.com/sblantipodi/firefly_luciferin/wiki/Debug) is set to DEBUG.
- Fixed an issue that prevented Hyprland to show the UI.
- Fixed an issue that caused incorrect color reproduction on non-standard screen resolutions.
- Fixed an issue that prevented the Glow Worm Luciferin Light Firmware from properly turning off the LED strip when
  closing Firefly Luciferin.

### In the previous releases:

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
- Tray icon was unable to load all settings relative to a profile. Fixed.
- Technicalities: Switched to the new ZGC Generational.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.18.1).

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
