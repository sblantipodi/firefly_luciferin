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

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.5.6).
- **Added support for RGBW strips.**  
- Misc tab now offers a **slider to fine tune White Balance**.  
- **Web Installer now asks for WiFi credentials**, first configuration is now much easyer.  
- Added 115200 baudrate support, it's now possible to configure baudrate from the Web Interface.  
- Home Assistant integration update, please copy and paste the new configuration.  
- Added Français. (Thanks @Deadrix).
- Various bug fixes, memory leak fixes, and other stability and reliability improvements.
- Arduino Bootstrapper update (v.1.12.9).
- PlatformIO Version Increment update (v0.1.6).

### In the previous release:

- Hotfix: Capture is abruptly interrupted when using Full firmware, UDP stream, no MQTT. Fixed.  
  Firmware upgrade is not required.