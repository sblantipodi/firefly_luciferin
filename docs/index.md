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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.8.4).  
- Added **automatic brightness control** using a Light Dependent Resistor (LDR).  
- **Luciferin Official PCB has been upgraded** to v1.1 to support the new LDR feature.  
- Added a **brightness limiter feature**.  
- **This upgrade will format your ESP32.** SPIFFS has been deprecated on ESP32 in favour of LittleFS. Please reconfigure WiFi or MQTT if you are using an ESP32 along with the full firmware.  
- Menu is not visible when taskbar is on top. Fixed.  
- Improved MQTT reconnection.  
- Arduino Bootstrapper update (v.1.13.0).
- PlatformIO Version Increment update (v0.1.7).



### In the previous release:

- ***No firmware update required***
- **Added HSL control panel.**
  The new HSL tuning control panel contains settings used to adjust the Hue, Saturation, and Lightness of the LED strip. HSL tuning can be used to make slight shifts in hue to individual colors, to desaturate specific colors and to brighten or darken those colors. Thanks @kopidoo for the help in this release.
- Changing the color temperature / brightness requires restarting the screen capture to take effect. Fixed.
- Improved multi monitor support on Linux.
