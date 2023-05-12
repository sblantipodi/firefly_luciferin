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

### In the previous release:

- ***Hotfix release:*** Fixed an issue that prevents 21:9 screen resolutions to correctly capture the screen. No
  firmware upgrade needed.
