# Java Fast Screen Capture for PC Ambilight
Fast Screen Capture software for my [PC Ambilight](https://github.com/sblantipodi/pc_ambilight).  
_Written in Java for Arduino._

![Java CI with Maven](https://github.com/sblantipodi/JavaFastScreenCapture/workflows/Java%20CI%20with%20Maven/badge.svg)
[![GitHub version](https://img.shields.io/github/v/release/sblantipodi/JavaFastScreenCapture.svg)](https://github.com/sblantipodi/JavaFastScreenCapture/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://GitHub.com/sblantipodi/JavaFastScreenCapture/graphs/commit-activity)
[![DPsoftware](https://img.shields.io/static/v1?label=DP&message=Software&color=orange)](https://www.dpsoftware.org)


If you like **Fast Screen Capture**, give it a star, or fork it and contribute!

[![GitHub stars](https://img.shields.io/github/stars/sblantipodi/JavaFastScreenCapture.svg?style=social&label=Star)](https://github.com/sblantipodi/JavaFastScreenCapture/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/sblantipodi/JavaFastScreenCapture.svg?style=social&label=Fork)](https://github.com/sblantipodi/JavaFastScreenCapture/network)

## Why it's fast?
Fast Screen Capture is written in Java using AWT's Robot class, Robots is the only way to screen capture using Java (without exotic libs).  
With that thing you can almost never get above 5FPS (in 4K) because as you can see in the OpenJDK implementation, `robot.createScreenCapture()` is synchronized and the native calls it uses are pretty slow.  

Fast enough for screenshots but too slow for video. If one Robot can capture at about 5FPS, what about 2 Robots in a producer/consumer environment?  
With 4 threads and an i7 5930K @ 4.2GHz I can capture at 24FPS in 4K.   
If you want, you can increase threads numbers variable and get even higher framerate. 

## To do
LEDMatrix is hardcoded for my monitor/led strip, you can change it with according to your specifications.   
Probably I will make this configurable but this is a task for another day.

## Credits
- Davide Perini