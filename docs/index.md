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
- Hotfix: Capture is abruptly interrupted when using Full firmware, UDP stream, no MQTT. Fixed.  
  Firmware upgrade is not required.

### In the previous release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.4.9).
- **Introducing the perfect case for the Official Printed Circuit Board for Luciferin.** Thank you @pbl0m for this wonderful work!
- **Luciferin can now be easily translated in your favourite language.**   
  Do you want to help with the translations?
  Current available languages (more languages coming soon):
  - Spanish (thanks @morganflint)
  - German (thanks @Maaaaarc)
  - Hungarian (thanks @kopidoo)
  - Italian (thanks @sblantipodi)
  - Russian (thanks @whakru, @kreanmire)
- **Added a new effect**: Stereo VU Meter.
- Added the possibility to configure the number of leds via Web Interface.
- First LED offset can now be easily configured via presets which calculate the right value automatically.
- Check for default audio device on Native or Software Audio capture.
- Improved input validation, some values can cause exceptions at runtime.
- Linux version does not support the tray icon, Firefly Luciferin now starts minimized.
- Linux version now shows only one icon on multi monitor setup.
- Fixed a problem that caused DNS IP being interpreted as a subnet mask value.
- Fixed an issue in the Web Interface that prevents to correctly enter MQTT settings.
- Fixed a problem that make ESP8266 to bootloop on some routers when using DHCP.
- Mitigation to LOGBACK-1591 vulnerability.
- Fixed a problem that caused stuttering when using Rainbow Music Mode.
- Fixed an issue that prevented night mode from activating when using Full firmware without MQTT.
- Fixed an issue that prevented a third monitor from working properly when using Full firmware without MQTT.
- Arduino Bootstrapper update (v.1.12.6).