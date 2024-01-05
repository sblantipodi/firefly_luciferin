<style>
  .footer {
    display: none;
  }
  .body {
    color: #202020;
    background-color: #F5F5F5;
  }
  .px-3 {
    padding-right: 30px !important;
    padding-left: 10px !important;
  }
  .my-5 {
    margin-top: 10px !important;
    margin-bottom: 10px !important;
  }
</style>

# Firefly Luciferin

<img align="right" width="100" height="100" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/data/img/luciferin_logo.png">


[![Web Installer](https://img.shields.io/website/https/sblantipodi.github.io/glow_worm_luciferin.svg?label=Web%20Installer&down_color=red&down_message=offline&up_color=4bc51d&up_message=online&logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAAIAAAACACAMAAAD04JH5AAAAD1BMVEUAAAD%2F%2F%2F%2F%2F%2F%2F%2F%2FVyL%2F%2F%2F8bK2t7AAAAA3RSTlMAARYXuUDOAAAAhklEQVR42u3WyQ2AMAwEQAz9lxxCCRCuONHse2XPy3JsS98EAAAAAAAAQDpAfLywAgAADAuoJ716dw4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAwgLcCAACQH%2FB3AAAAAAAAAPoDWovrxYdjBwAAmA7QerAKAADANICnD0sBAADIDjgAPijZuQgZskEAAAAASUVORK5CYII%3D)](https://sblantipodi.github.io/glow_worm_luciferin)
[![CI Build](https://github.com/sblantipodi/firefly_luciferin/actions/workflows/build.yml/badge.svg)](https://github.com/sblantipodi/firefly_luciferin/actions)
[![CodeQL Analysis](https://github.com/sblantipodi/firefly_luciferin/actions/workflows/codeql.yml/badge.svg)](https://github.com/sblantipodi/firefly_luciferin/actions/workflows/codeql.yml)
[![GitHub version](https://img.shields.io/github/v/release/sblantipodi/firefly_luciferin.svg)](https://github.com/sblantipodi/firefly_luciferin/releases)
[![DPsoftware](https://img.shields.io/static/v1?label=DP&message=Software&color=orange)](https://www.dpsoftware.org)
[![Discord](https://img.shields.io/discord/747247942074892328.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/aXf9jeN)


If you like **Firefly Luciferin**, give it a star, or fork it and contribute!

[![GitHub stars](https://img.shields.io/github/stars/sblantipodi/firefly_luciferin.svg?style=social&label=Star)](https://github.com/sblantipodi/firefly_luciferin/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/sblantipodi/firefly_luciferin.svg?style=social&label=Fork)](https://github.com/sblantipodi/firefly_luciferin/network)
[![PayPal](https://img.shields.io/badge/donate-PayPal-blue)](https://www.paypal.com/donate?hosted_button_id=ZEJM8ZLQW5E4A)

### In this release:

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

### In the previous release:

- Hotfix release: Fixed a regression that doesn't permit to drive LEDs via USB when MQTT is enabled. **This issue
  affects Firefly Luciferin only, there is no need to update the firmware.**.
