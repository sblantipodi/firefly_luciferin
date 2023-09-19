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

- Hotfix release: Fixed a regression that doesn't permit to drive LEDs via USB when MQTT is enabled. **This issue
  affects Firefly Luciferin only, there is no need to update the firmware.**

### In the previous release:

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