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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.19.4).
- **Color accuracy has been significantly improved when using HDR contents.** Closes [#268](https://github.com/sblantipodi/firefly_luciferin/issues/268).
- New [color orders](https://github.com/sblantipodi/firefly_luciferin/wiki/RGB-and-RGBW-support#how-to-change-color-order) to support newer LED strips.
- Get command path dynamically for restarting. Closes [#262](https://github.com/sblantipodi/firefly_luciferin/pull/262). Thanks @Ape for the PR.
- Fix "Sources not selected" crash on Wayland. Closes [#264](https://github.com/sblantipodi/firefly_luciferin/pull/264). Thanks @Ape for the PR.
- OnBoard-Led managment for esp8266. Closes [#266](https://github.com/sblantipodi/firefly_luciferin/pull/266).
- When setting a low brightness color on Firefly Luciferin, it’s not possible to increase the brightness in the [web interface](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#luciferin-web-interface). Fixed.
- Fix "Slow rainbow" effect switching to "Solid" automatically.

### In the previous releases:

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

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
