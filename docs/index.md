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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.7.2).
- **Added profiles** for fast switching between modes via tray icon.  
- **Added new themes:**
    - Dark blue theme.
    - Dark purple theme.
    - Light gray theme.
- **Most settings can now be changed and saved on the fly** without the needs of restarting Firefly Luciferin.
- GUI improvements, text now wraps on two or more lines when the phrase is too long, this applies to tooltips too.
- Temp files cleanup to save disk space on startup.
- Fire effect was too slow on light firmware, fixed.
- Improved precision of the capture area when using margin.

### In the previous release:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.6.1).
- **Capture enhancements.** Bias Light effect is now smoother than before. The change/transition between the brightness steps is now more linear.
- **Added an option to group LEDs.** The more LEDs you group, the smoother the Bias Light effect will be. Grouping LEDs afflicts precision.
- **Bottom row split adjustment is now possible.** You can now configure the gap size. (Closes #69).
- **Grabber area adjustment.** You can now choose the size of the grab area. (Closes #68).
- **LED corner skip.** The corners now have a horizontal and vertical adjustment feature for the gap. (Closes #67).
- **Color temperature correction no longer affect microcontroller's performance.** Microcontroller's performance has been improved by 40% when using color temperature correction.
- **Added more gamma steps** for a better fine tuning of the gamma.
- Fixed an issue that prevented screen capture when on some resolutions with some LEDs configurations. If you encountered this issue, no more black LEDs when capturing the screen.
- Audio functionality broke when changing the UI language, fixed.
- Fixed language typos.
