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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.18.2).
- The priority of UDP packets in wireless mode has been increased to signal to the router that Luciferin traffic
  requires lower latency than standard packets.
- If the microcontroller is temporarily disconnected from the WiFi network, Firefly Luciferin is now able to reconnect
  much faster without restarting the screen capture.
- Added a 'bottom' capture option
  for [satellites](https://github.com/sblantipodi/firefly_luciferin/wiki/Surround-lighting-with-satellites) when the
  LEDs are configured to use a bottom gap.
- The [save state](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#luciferin-web-interface) has been
  restructured. Auto-save has been disabled to prevent wear on the microcontroller's memory.
  Closes [#249](https://github.com/sblantipodi/firefly_luciferin/issues/249).
- Arch Linux package. Note: AUR package is built from the official sources but it's currently maintained by @Ape.
  Closes [#246](https://github.com/sblantipodi/firefly_luciferin/issues/246). Thanks @Ape for this.
- Libasound2t64 dependency prevents correct installation on some Linux distros.
  Closes [#253](https://github.com/sblantipodi/firefly_luciferin/issues/253).
- Properly handle expired restore token on Wayland.
  Closes [#259](https://github.com/sblantipodi/firefly_luciferin/issues/259). Thanks @Ape for the PR.
- Logging improvements. Closes [#260](https://github.com/sblantipodi/firefly_luciferin/pull/260). Thanks @Ape for the
  PR.
- Proper config path on Linux. Config file and logs has been moved in XDG_CONFIG_HOME (~/.config/FireflyLuciferin). Old
  config files will be automatically moved to the new path.
  Closes [#261](https://github.com/sblantipodi/firefly_luciferin/pull/261).
- The snap version was crashing at startup when there were temporary files created by other instances of Firefly Luciferin on the system. Fixed.

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.17.8).
- **Capture pipeline has been optimized for DX12 on Windows.** Previously, under heavy GPU load, capture framerates would drop significantly, causing occasional LED stuttering. Now, the pipeline has been restructured to better utilize hardware resources, completely eliminating stuttering. This change, along with the previous SIMD extension released with the previous version of Firefly Luciferin, makes Luciferin one of the most optimized and fastest software for bias lighting available at the moment.
- **New effects and improvements to the existing ones.**
- **Firefly Luciferin is now able to run on a Linux sandbox:**
  - **Added support for Flatpak** with immediate availability on **[Flathub](https://flathub.org/apps/org.dpsoftware.FireflyLuciferin)**. Closes [#207](https://github.com/sblantipodi/firefly_luciferin/issues/207).
  - **Added support for Snap** with immediate availability on **[Snap Store](https://snapcraft.io/fireflyluciferin)**.
- **The response latency during Linux screen capture has been widely reduced.**
- **Added a [Tray icon](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support#luciferin-supports-wayland) and minimize to tray on Linux.** Thanks @sorcererlc for the continued support. Closes [#234](https://github.com/sblantipodi/firefly_luciferin/issues/234).
- **New non intrusive [notification](https://github.com/sblantipodi/firefly_luciferin/wiki/Linux-support#luciferin-supports-wayland) system on Linux.**
- **The `Info` menu now displays the current CPU latency.** Lower values indicate better performance. This value can be
  influenced by your screen resolution, capture area dimensions, CPU/RAM overclocking, and AVX extensions available on
  the CPU.
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

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
