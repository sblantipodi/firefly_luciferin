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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.5.8).
- **Capture enhancements.** Bias Light effect is now smoother than before with default settings. The change/transition between the brightness steps is now more linear.
- **Added an option to group LEDs.** The more LEDs you group, the more smooth is the Bias Light effect. Grouping LEDs afflicts precision.
- **Bottom row split adjustment is now possible.** You can now configure the gap size. (Closes #69).
- **Grabber area adjustment.** You can now choose the size of the grab area. (Closes #68).
- **LED corner skip.** The corners now have a horizontal and vertical adjustment feature for the gap. (Closes #67).
- **Color temperature correction no longer affect microcontroller's performance.** Microcontroller's performance has been improved by 40% when using color temperature correction.
- **Added more gamma steps** for a better fine tuning of the gamma.
- Fixed an issue that prevented screen capture when on some resolutions with some LEDs configurations. If you encountered this issue, no more black LEDs when capturing the screen.
- Audio functionality broke when changing the UI language, fixed.
- Fixed language typos.

### In the previous release:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.5.7).
- **Added support for RGBW strips.**
- Misc tab now offers a **slider to fine tune White Balance**.
- **Web Installer now asks for WiFi credentials**, first configuration is now much easyer.
- Added 115200 baudrate support, it's now possible to configure baudrate from the Web Interface.
- Home Assistant integration update, please copy and paste the new configuration.
- Added Français. (Thanks @Deadrix).
- Various bug fixes, memory leak fixes, and other stability and reliability improvements.
- Arduino Bootstrapper update (v.1.12.10).
- PlatformIO Version Increment update (v0.1.6).