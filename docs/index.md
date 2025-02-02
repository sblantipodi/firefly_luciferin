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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.20.2).
- Fixed a regression that caused Firefly Luciferin to crash at startup when using an AMD GPU along with the DDUPL_DX12
  capture method.
- Grab area improvements made on `DDUPL/XIMAGESRC/PIPEWIRESRC` capture method has been backported on legacy capture
  methods `WinAPI/CPU`.
- Added Polish language. Thanks @gmx168. Closes [#258](https://github.com/sblantipodi/firefly_luciferin/pull/258).
- WiFi access does not work if MQTT server is down, fixed.
  Closes [#74](https://github.com/sblantipodi/glow_worm_luciferin/issues/74).
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.18.3).

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.19.4).
- **Color accuracy has been significantly improved when using HDR contents.** Closes [#268](https://github.com/sblantipodi/firefly_luciferin/issues/268).
- New [color orders](https://github.com/sblantipodi/firefly_luciferin/wiki/RGB-and-RGBW-support#how-to-change-color-order) to support newer LED strips.
- Get command path dynamically for restarting. Closes [#262](https://github.com/sblantipodi/firefly_luciferin/pull/262). Thanks @Ape for the PR.
- Fix "Sources not selected" crash on Wayland. Closes [#264](https://github.com/sblantipodi/firefly_luciferin/pull/264). Thanks @Ape for the PR.
- OnBoard-Led managment for esp8266. Closes [#266](https://github.com/sblantipodi/firefly_luciferin/pull/266).
- When setting a low brightness color on Firefly Luciferin, it’s not possible to increase the brightness in the [web interface](https://github.com/sblantipodi/firefly_luciferin/wiki/Remote-Access#luciferin-web-interface). Fixed.
- Fix "Slow rainbow" effect switching to "Solid" automatically.

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
