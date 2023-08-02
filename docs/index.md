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


### In this release:

- ***Breaking changes***: requires `Glow Worm Luciferin` (v5.11.8)
- **Added support for ESP32-C3, ESP32-S2, ESP32-S3.**
  Lolin ESP32-C3, Lolin ESP32-S2 and Lolin ESP32-S3 are now fully compatible with the existing Luciferin Official PCB.
  TinyS2 and TinyS3 are now compatible with the existing Luciferin Module for the Luciferin Official PCB.
  Closes [#46].
- Added **support for DotStar LED strips.** Closes [#42].
- Added the possibility to **configure GPIOs for Relay, Button and LDR**.
- Added the possibility to **switch profiles through MQTT**. Closes [#110].
- Added **BRG, RBG, GBR color order** support.
- Improved power saving mode. Closes [#107].
- Improved existing light effects.
- Improved aspect ratio auto detection, very dark scenes do not trigger an aspect ratio switch as there is no way to
  know which is the correct one.
- IMAX 1.85:1 format now triggers the letterbox aspect ratio.
- There is now a single Web Installer for both stable and beta firmware.
- Reduced firmware footprint.
- Removed the hard limit on the maximum number of LEDs. You can now use as many LEDs as you want as long as your
  microcontroller has enough memory.
- Increased the priority of the capturing threads. This fixed a flickering issue that occurs while using the smoothing
  effect (frame generation) on Hybrid CPUs. Does not affect CPU load.
- UDP broadcast collision fix. Corrects weird behaviours when using two instances of Firefly Luciferin on two or more
  computers on the same network with UDP stream.
- Configuring the Web Interface no longer requires an Internet connection. Closes [#52].
- Fixed a bottleneck that reduced performance when driving many LEDs via USB. ESP32 was able to drive 500LEDs at 5FPS,
  now it can drive the same amount of LEDs at 30FPS.
- Fixed an issue that prevented a profile from changing the current framerate without pausing and restarting the
  capture.
- Arduino Bootstrapper update (v.1.15.2).


### In the previous release:

- ***Breaking changes***: requires `Glow Worm Luciferin` (v5.10.6).
- **Added a smoothing feature that is used to smooth the transitions from one color to another**,
  this is particularly useful to reduce eye strain with contents that produces fast flashing like fast peaced games or
  similar. This setting can be controlled on the fly via MQTT/Home Assistant.
- **Added a latency test**. This test displays colors in rapid succession and helps you check if the latency between the
  image shown on your monitor and the color displayed on the led strip is acceptable to you. Highering the framerate
  helps reducing the latency. You can run this test at 10 different speed. This is also useful when choosing the right
  smoothing level for your preferences.
- **Web Interface now displays an Auto save button**, if auto save is enabled, color and brightness information is
  stored into memory to retain this settings after reboot.
- Added an option within the Web Interface to choose to **turn on the LEDs once the microcontroller boots up**.
- **Device reset** has been improved.
- **Added support for RGB and BGR color order**.
- **RGBW SK6812 performance boost**. +30% maximum framerate and reduced latency.
- **Added support for the QuinLED dig2go pre-build board**.
- Added support for **Hardware Button**.
- Fixed an issue that prevented correct screen capture once changed the desired framerate or aspect ratio.
- Minor UI improvements.
- Web Installer now presents an option to install beta or previous version of the Glow Worm Luciferin firmware.
- Improved log level configurability.
- Removed warnings in the Home Assistant logs.
- Arduino Bootstrapper update (v.1.14).