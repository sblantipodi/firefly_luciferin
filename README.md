# Java Fast Screen Capture for PC Ambilight
Fast Screen Capture software for my [PC Ambilight](https://github.com/sblantipodi/pc_ambilight).  
_Written in Java for Arduino._

[![Java CI with Maven](https://github.com/sblantipodi/JavaFastScreenCapture/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/sblantipodi/JavaFastScreenCapture/actions)
[![GitHub version](https://img.shields.io/github/v/release/sblantipodi/JavaFastScreenCapture.svg)](https://github.com/sblantipodi/JavaFastScreenCapture/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://GitHub.com/sblantipodi/JavaFastScreenCapture/graphs/commit-activity)
[![DPsoftware](https://img.shields.io/static/v1?label=DP&message=Software&color=orange)](https://www.dpsoftware.org)


If you like **Fast Screen Capture**, give it a star, or fork it and contribute!

[![GitHub stars](https://img.shields.io/github/stars/sblantipodi/JavaFastScreenCapture.svg?style=social&label=Star)](https://github.com/sblantipodi/JavaFastScreenCapture/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/sblantipodi/JavaFastScreenCapture.svg?style=social&label=Fork)](https://github.com/sblantipodi/JavaFastScreenCapture/network)

## Why it's fast? What is the achievable framerate?
Fast Screen Capture is written in Java using AWT's Robot class, Robots is the only way to screen capture using Java (without exotic libs).  
With that thing you can almost never get above 5FPS (in 4K) because as you can see in the OpenJDK implementation, `robot.createScreenCapture()` is synchronized and the native calls it uses are pretty slow.  

Fast enough for screenshots but too slow for screen capture. If one Robot can capture at about 5FPS, what about 2 Robots in a `multi threaded producer/consumer` environment?  

## CPU load with 6 threads
With 6 threads and an i7 5930K @ 4.2GHz I can capture at 25FPS in 4K, 12 threads gives me +30FPS.   
If you want, you can increase threads numbers variable and get even higher framerate.  

Note: performance does not increase linearly, find the sweet spot for your taste and your environment.  
`Maximum framerate` is generally achieved by setting thread number at a value greater than your CPU cores, if you  
have a 8 cores CPU, best framerate is achieved with 16 threads.  
  
![CPU LOAD](https://github.com/sblantipodi/JavaFastScreenCapture/blob/master/data/img/smashing_threads.jpg)

If you are using a slow microcontroller, capturing at a very high framerate does not help. If you right click tray icon and then click `FPS`,
you can see the output as shown in the image below. In that output you can see how fast the software is captruing the screen (producing)
and how fast your microcontroller is able to process (consume) this data.  

![Framerate dialog](https://github.com/sblantipodi/JavaFastScreenCapture/blob/master/data/img/framerate_counter.jpg)

Increase `dataRate` accordingly to your microcontroller's serial speed, 115200 is generally more than enough for 30FPS and 100 LEDs. Producers framerate should not exceed the consuming one, all data that is not consumed in time, is lost.

## How To
You can build the software from the source or if you prefer you can download a ready to use binary.  
`FastScreenCapture-vx.x.x-jar-with-dependencies.jar` is the one to get and you can download it from [here](https://github.com/sblantipodi/JavaFastScreenCapture/packages).  
  
This software can run on any Desktop PC using Windows, Linux or macOS. 
To get the full ambilight experience you need a microcontroller connected to the PC (ex. Arduino UNO, ESP8266, ESP32, Teensy, ecc.) running my [PC Ambilight](https://github.com/sblantipodi/pc_ambilight) software.
  
## Configuration
As soon as you start the software it creates a `FastScreenCapture.yaml` file in your documents folder, please configure it and you are ready to go.

```yaml
---
numberOfCPUThreads: 3    // more threads more performance but more CPU usage
gpuHwAcceleration: true  // 
serialPort: "AUTO"       // use "AUTO" to autodetect Serial Port, "COM7" for COM7 
dataRate: 500000         // faster data rate helps when using more LEDs or higher framerate
timeout: 2000            // timeout in serial port detection
screenResX: 3840         // screen resolution width
screenResY: 2160         // screen resolution height
osScaling: 150           // OS scaling feature
ledOffsetX: 30           // X LED offset for led matrix
ledOffsetY: 300          // Y LED offset for led matrix         
gamma: 2.2               // gamma correction for the LED strip
ledMatrix:               // LED Matrix, X,Y position where the LED is positioned
  1:
    x: 2566
    y: 1836
  2:
    x: 2664
    y: 1836
  ...
```

## Hardware Acceleration using Java Native Access (for Windows only) 
Screen capturing is pretty slow and very CPU intensive in Windows systems (Linux is much more efficient here),
for this reason I wrapped the GDI32 C class using Java Native Access to access Windows API.
This API capture and deliver captured frames in GPU memory. It's fast but not enough for my tastes because it adds 
a little bit of lag to the mouse. 

## TODO
Switch hardware accelerated grabber to Windows Native Desktop Duplication API.

## Credits
- Davide Perini
