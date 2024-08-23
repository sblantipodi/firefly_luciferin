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
  AMD and Intel is investing a lot of resources in improving their CPU extensions. Next-generation CPUs contain various
  optimizations related to `Advanced Vector Extensions` that are `Single Instruction, Multiple Data` extensions to the
  x86 instruction set architecture for microprocessors.
  AVX512 and AVX256 brings big benefits in terms of performance and resource optimization.
- There are microcontrollers that has built/in LED. This LED can stay on and be annoying, it now follows
  the [device reset](https://github.com/sblantipodi/firefly_luciferin/wiki/Device-reset) behaviour.
- Fixed an issue that prevented Linux version to show the UI
  when [debug level](https://github.com/sblantipodi/firefly_luciferin/wiki/Debug) is set to DEBUG.
- Fixed an issue that prevented Hyprland to show the UI.

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
