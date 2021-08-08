<style>
.footer {
  display: none;
}
.body {
  color: #202020;
  background-color: F5F5F5;
}
.px-3 {
    padding-right: 0px !important;
    padding-left: 0px !important;
}
.my-5 {
    margin-top: 0px !important;
    margin-bottom: 0px !important;
}
</style>
- **Breaking changes**: requires `Glow Worm Luciferin` firmware (v5.0.1)
- **Launching Luciferin Official PCBs.** If you don't want to design your own PCB and you have a soldering iron, you might find **Luciferin's official PCB** interesting. If you have an existing setup or want to design your own PCB, you can do it.
- **DMA mode (Direct Memory Access) and UART mode is now supported.** Please use the right GPIO to enable these modes. GPIO2 is now the default pin.  
- **UDP wireless stream has been added to the MQTT stream** and now it's the default option for wireless streaming.
- **Massive performance increase.** Thanks to DMA/UART and UDP stream you can now run 500+LEDs at 60+FPS and 200LEDs at 144+FPS. Frametime has been widely reduced. Wireless stream is now 3 times faster on ESP8266 and 7 times faster on ESP32. ESP8266 continue to be the recommended option due to a more mature ecosystem.
- **Added two power saving features:**
    - Turn off LEDs for long inactivity period
    -  Add a relay to cut power to the LED strip
- **Added support for themes**, "Dark theme" added. 
- Added new light effects:
    - Fire
    - Twinkle
    - Chase rainbow
- Fixed an issue that prevented capture to start and stop by double clicking the tray icon.
- When you change the total number of LEDs in use, the microcontroller gets the update instantly and optimizes performance for the number of LEDs in use, no need to restart the microcontroller.
- This is a major update, it will erase ESP32 devices, please reconfigure them.
- Preparing the support for ESP32-C3 and ESP32-S2.
