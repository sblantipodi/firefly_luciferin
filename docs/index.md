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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.21.3).
- **The smoothing feature is greatly improved thanks to the [Exponential Moving Average (EMA)](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions#what-is-exponential-moving-average-ema).**  
  EMA is a smoothing technique that gradually adjusts values over time, giving more weight to recent data while still
  considering past values.
  Unlike a simple average, EMA reacts faster to changes while still keeping transitions smooth.  
  By
  combining [Frame Generation](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions#how-does-it-works-linear-interpolation-with-frame-insertion)
  and [EMA](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions#what-is-exponential-moving-average-ema),
  Luciferin delivers an even more natural and immersive ambient lighting effect while keeping system performance
  efficient.
- Home Assistant integration has been updated to make room for this new feature:
  - `select.luciferin_smoothing_level` entity has been removed.
  - `select.luciferin_frame_generation` has been added.
  - `select.luciferin_smoothing_ema` has been added.
- **[Eye care](https://github.com/sblantipodi/firefly_luciferin/wiki/Eye-care-and-night-mode) features has been
  extended:** Closes [#247](https://github.com/sblantipodi/firefly_luciferin/issues/247).
  - `Night light:` Use warmer colors to block blue light. The higher you raise the level, the more blue light will be
    blocked. The 'Auto' setting syncs with the night light setting in your operating system.
  - `Luminosity threshold:` Added the option to customize the threshold value for this feature.
- [Color temperature correction](https://github.com/sblantipodi/firefly_luciferin/wiki/Color-Temperature-and-White-Balance)
  algorithm has been improved, now it does not reduce brightness on faint colors.
- Optimize PIPEWIREXDG pipeline. PR [#276](https://github.com/sblantipodi/firefly_luciferin/pull/276). Thanks @Ape.
- The firmware now smooths out transitions when switching from one color to another in solid mode. (Full firmware only).
- [Test image](https://github.com/sblantipodi/firefly_luciferin/wiki/Color-Grading-(Hue-Saturation-and-Lightness-tuning))
  improvements:
  - You can now access other settings while using Test Image.
    Closes [108](https://github.com/sblantipodi/firefly_luciferin/issues/108).
  - Every change is now reflected in real time by the test image.
  - The test image now allows for 4 levels of saturation (100%, 75%, 50%, 25%). This helps to test colors with various
    levels of saturation.
- You can now open the `Info menu` along with the `Settings menu`. This allows you to check in real time how changing
  settings affects FPS, without having to swap back and forth between the Settings and Info menus.
  Closes [#236](https://github.com/sblantipodi/firefly_luciferin/issues/236).
- **Net code overhead reduction for reduced power usage.**
  Closes [#281](https://github.com/sblantipodi/firefly_luciferin/issues/281).
- Small UI reorganizations and improvements and better handling of the info tooltips.
- Restarting capture due to a framerate change is now much faster.
- Added support for 4.000.000 and 6.000.000 baud rates for newer UART chips. Use with caution.
- Home assistant brightness control not working with Bias light. Fixed.
  Closes [#278](https://github.com/sblantipodi/firefly_luciferin/issues/278).
- Fixed an issue that reverted the baud rate to 115.200 when changing network settings.
- Tray icon [update menu](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management) does not
  work when `Check for updates` is disabled. Fixed.
- Settings cannot be opened when the MQTT server is unreachable. Issue resolved.
- Following a recent change
  in [Home Assistant](https://github.com/sblantipodi/firefly_luciferin/wiki/Home-Automation-configs), the light entity
  is no longer recognized as an RGB entity. Fixed.

### In the previous releases:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.20.2).
- Fixed a regression that caused Firefly Luciferin to crash at startup when using an AMD GPU along with the DDUPL_DX12
  capture method.
- Grab area improvements made on `DDUPL/XIMAGESRC/PIPEWIRESRC` capture method has been backported on legacy capture
  methods `WinAPI/CPU`.
- Added Polish language. Thanks @gmx168. Closes [#258](https://github.com/sblantipodi/firefly_luciferin/pull/258).
- WiFi access does not work if MQTT server is down, fixed.
  Closes [#74](https://github.com/sblantipodi/glow_worm_luciferin/issues/74).
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.18.3).

[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
