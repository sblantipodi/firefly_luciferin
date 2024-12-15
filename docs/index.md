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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.17.8).
- **Capture pipeline has been optimized for DX12 on Windows.** Previously, under heavy GPU load, capture framerates would drop significantly, causing occasional LED stuttering. Now, the pipeline has been restructured to better utilize hardware resources, completely eliminating stuttering. This change, along with the previous SIMD extension released with the previous version of Firefly Luciferin, makes Luciferin one of the most optimized and fastest software for bias lighting available at the moment.
- **The `Info` menu now displays the current CPU latency.** Lower values indicate better performance. This value can be influenced by your screen resolution, capture area dimensions, CPU/RAM overclocking, and AVX extensions available on the CPU.
- **Firefly Luciferin is now able to run on a Linux sandbox:**
  - **Added support for Flatpak** with immediate availability on **[Flathub](https://flathub.org/apps/org.dpsoftware.FireflyLuciferin)**. Closes [#207](https://github.com/sblantipodi/firefly_luciferin/issues/207).
  - **Added support for Snap** with immediate availability on **[Snap Store](https://snapcraft.io/fireflyluciferin)**.
- **The response latency during Linux screen capture has been widely reduced.**
- **Added a [Tray icon](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support#luciferin-supports-wayland) and minimize to tray on Linux.** Thanks @sorcererlc for the continued support. Closes [#234](https://github.com/sblantipodi/firefly_luciferin/issues/234).
- **New non intrusive [notification](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support#luciferin-supports-wayland) system on Linux.**
- **Home Assistant: Luciferin entities are now grouped under one devices**, these entites has been renamed:
  - light.glow_worm_luciferin -> light.luciferin_switch
  - sensor.firefly_luciferin_consuming -> sensor.luciferin_firefly_consuming
  - sensor.glow_worm_luciferin_consuming -> sensor.luciferin_glow_worm_consuming
  - sensor.last_update_glow_worm_luciferin -> sensor.luciferin_last_update_glow_worm
  - sensor.firefly_luciferin_producing -> sensor.luciferin_firefly_producing
  - sensor.glow_worm_luciferin_version -> sensor.luciferin_glow_worm_version
  - button.reboot_glow_worm_luciferin -> button.luciferin_reboot_glow_worm
- [Update notifications](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management) are now less intrusive and no longer launch the update popup at computer startup. A simple notification informs about the available update, and you can proceed to install it by right-clicking on the tray icon and selecting 'install updates'.
- Linux Wayland didn't ask you which screen to record when the recording permission expired. Fixed.
- [Power saving](https://github.com/sblantipodi/firefly_luciferin/wiki/Power-saving-features) mode is not interrupted by small changes on the screen like icons changing state in the system tray or incoming notifications.

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.16.7).
- **Luciferin now has specific support for [AVX CPU extensions](https://github.com/sblantipodi/firefly_luciferin/wiki/Very-fast-capture#cpu-acceleration-using-avx-simd-extensions).**  
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
- Java/JavaFX 23, libs update, code refactor to avoid using deprecated methods, CI/CD pipeline improvements.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.18.2).

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
