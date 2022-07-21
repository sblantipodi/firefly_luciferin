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

- ***No firmware update required***
- Added HSL control panel. (Closes #64)
  The new HSL tuning control panel contains settings used to adjust the Hue, Saturation, and Lightness of the LED strip. HSL tuning can be used to make slight shifts in hue to individual colors, to desaturate specific colors and to brighten or darken those colors.  
- Change color temperature/brightness requires screen capture restart to take effect. Fixed.

### In the previous release:

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
