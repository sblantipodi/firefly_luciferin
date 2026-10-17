# Luciferin

**Bring your games, movies, and shows beyond the screen.**

Luciferin fills your walls with the colors on screen, turning every scene into immersive ambient lighting. Capture your
PC directly or connect consoles and TV sticks through HDMI capture. Add satellite lights to extend the experience around
your room.

[**Firefly Luciferin**](https://github.com/sblantipodi/firefly_luciferin) captures the screen and calculates the
colors. [**Glow Worm Luciferin**](https://github.com/sblantipodi/glow_worm_luciferin), the firmware on your LED
controller, brings them to your LEDs. Built together for **ultra-low latency over Wi-Fi, Ethernet, or USB**.

**Control it from your desktop, phone, or AI assistant.**

<img align="right" width="100" height="100" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/data/img/luciferin_logo.png">

[![Web Installer](https://img.shields.io/website/https/sblantipodi.github.io/glow_worm_luciferin.svg?label=Web%20Installer&down_color=red&down_message=offline&up_color=4bc51d&up_message=online&logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAAIAAAACACAMAAAD04JH5AAAAD1BMVEUAAAD%2F%2F%2F%2F%2F%2F%2F%2F%2FVyL%2F%2F%2F8bK2t7AAAAA3RSTlMAARYXuUDOAAAAhklEQVR42u3WyQ2AMAwEQAz9lxxCCRCuONHse2XPy3JsS98EAAAAAAAAQDpAfLywAgAADAuoJ716dw4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAwgLcCAACQH%2FB3AAAAAAAAAPoDWovrxYdjBwAAmA7QerAKAADANICnD0sBAADIDjgAPijZuQgZskEAAAAASUVORK5CYII%3D)](https://sblantipodi.github.io/glow_worm_luciferin)
[![Wiki](https://img.shields.io/website/https/sblantipodi.github.io/glow_worm_luciferin.svg?label=Wiki&down_color=red&down_message=offline&up_color=4bc51d&up_message=online&logo=read-the-docs
)](https://github.com/sblantipodi/toremove/wiki)
[![GitHub version](https://img.shields.io/github/v/release/sblantipodi/firefly_luciferin.svg)](https://github.com/sblantipodi/firefly_luciferin/releases)
[![DPsoftware](https://img.shields.io/static/v1?label=DP&message=Software&color=orange)](https://www.dpsoftware.org)
[![Discord](https://img.shields.io/discord/747247942074892328.svg?logo=discord&logoColor=white&color=5865F2&label=Discord)](https://discord.gg/aXf9jeN)
[![PayPal](https://img.shields.io/badge/donate-PayPal-blue)](https://www.paypal.com/donate?hosted_button_id=ZEJM8ZLQW5E4A)

If you like **Luciferin**, give it a star, or fork it and contribute!

[![GitHub stars](https://img.shields.io/github/stars/sblantipodi/firefly_luciferin.svg?style=social&label=Star)](https://github.com/sblantipodi/firefly_luciferin/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/sblantipodi/firefly_luciferin.svg?style=social&label=Fork)](https://github.com/sblantipodi/firefly_luciferin/network)
[![Downloads](https://img.shields.io/badge/dynamic/json?color=5865F2&label=downloads&query=%24.installs_total&url=https://dpsoftware.org/luciferin_download.out)](https://github.com/sblantipodi/firefly_luciferin/releases)
[![CI Build](https://github.com/sblantipodi/firefly_luciferin/actions/workflows/build.yml/badge.svg)](https://github.com/sblantipodi/firefly_luciferin/actions)
[![CodeQL Analysis](https://github.com/sblantipodi/firefly_luciferin/actions/workflows/codeql.yml/badge.svg)](https://github.com/sblantipodi/firefly_luciferin/actions/workflows/codeql.yml)

[![Watch Luciferin ambient lighting in action](https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/luciferin_monitor.png)](https://youtu.be/PO37JC7-Nn4?si=nxVYd5b_qlfqo6HP)
Click the image to watch Luciferin in action.

## Two capture modes, one lighting system

| Your setup                    | How it works                                                                                                                                                                                                               |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PC gaming and desktop use** | Run Firefly on the Windows or Linux PC whose screen you want to capture. No capture card required: this is the recommended, most responsive setup for PC gaming.                                                           |
| **TV, console, or TV stick**  | Feed the HDMI source to both your TV and a USB capture card through an HDMI splitter. Firefly processes the captured video on a PC or Raspberry Pi, so you can enjoy ambient lighting without keeping a PC next to the TV. |

Firefly automatically detects whether a desktop is available. The **web interface is always available**, and a
**dedicated desktop interface** is available when a desktop environment is present. On a Raspberry Pi with limited RAM,
an installation without a desktop leaves more memory available for capture and processing.

[![Luciferin connection overview: direct PC screen capture, HDMI capture through a splitter and USB card, and optional satellite lighting](https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/luciferin_schema.jpg)](https://github.com/sblantipodi/toremove/wiki/Quick-start)

## Key features

- **Hardware accelerated processing:** Best in class performance combined with **ultra low CPU/GPU usage**.
- Advanced algorithms for [**smooth colors
  transitions**](https://github.com/sblantipodi/toremove/wiki/Smoothing-color-transitions) and [**color
  correction**](https://github.com/sblantipodi/toremove/wiki/Color-Grading-(Hue-Saturation-and-Lightness-tuning)).
- [**Adaptive
  gamma**](https://github.com/sblantipodi/toremove/wiki/Color-Temperature-and-White-Balance#adaptive-gamma-correction)
  provides significantly improved color accuracy on both **SDR** and **HDR** content.
- [**HDR to SDR tone mapping**](https://github.com/sblantipodi/toremove/wiki/HDR-to-SDR-and-LUT) adapts HDR color and
  brightness to your LEDs.
- [**Remote control**](https://github.com/sblantipodi/toremove/wiki/Remote-Access#luciferin-web-interface): use Firefly
  from phone or desktop, and Glow Worm via its built in web interface.
- [**AI control through MCP**](https://github.com/sblantipodi/toremove/wiki/MCP-and-AI-Agents): ask a compatible AI
  assistant to control Luciferin using natural language.
- **Home automation:** integrate with [**Home
  Assistant**](https://github.com/sblantipodi/toremove/wiki/Home-Automation-configs) and MQTT; neither is required for a
  basic setup.
- [**Multi monitor**](https://github.com/sblantipodi/toremove/wiki/Multi-monitor-support) support with **single or
  multiple instances**.
- [**Surround lighting**](https://github.com/sblantipodi/toremove/wiki/Surround-lighting-with-satellites) with
  satellites.
- [**Web browser firmware installer**](https://sblantipodi.github.io/glow_worm_luciferin/).
- [**Programmable firmware**](https://github.com/sblantipodi/toremove/wiki/Supported-GPIO-and-Baud-Rate), change your
  microcontroller's settings on the fly.
- [**One click updates**](https://github.com/sblantipodi/toremove/wiki/Luciferin-update-management) for both the PC
  software and firmware, keeping your entire setup up to date effortlessly.
- [**Automatic black bar detection**](https://github.com/sblantipodi/toremove/wiki/Aspect-ratio) and aspect ratio
  adjustment for video content.
- **Made from a gamer, for gamers**. No added lag, stutter free.
- Multi platform, [**Windows**](https://github.com/sblantipodi/toremove/wiki/Installers-and-binaries) and [
  **Linux**](https://github.com/sblantipodi/toremove/wiki/Linux-support) ready, supporting **x86 and ARM**
  architectures.
- Prefer a DIY build over a prebuilt board? Grab your soldering iron and use [**Luciferin’s official
  PCB**](https://github.com/sblantipodi/toremove/wiki/Ready-to-print-PCB), without designing your own.
- Have a question? [**Get answered on the community**](https://discord.gg/aXf9jeN).

## Quick start

You can **download the Firefly Luciferin installer for Windows or
Linux [here](https://github.com/sblantipodi/firefly_luciferin/releases)**.  
The Linux version is also available on **[Flathub](https://flathub.org/apps/org.dpsoftware.FireflyLuciferin)**
and **[Snap Store](https://snapcraft.io/fireflyluciferin)**.

**Glow Worm Luciferin firmware can be flashed on your device using the**  
[![Web Installer](https://img.shields.io/website/https/sblantipodi.github.io/glow_worm_luciferin.svg?label=Web%20Installer&down_color=red&down_message=offline&up_color=4bc51d&up_message=online&logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAAIAAAACACAMAAAD04JH5AAAAD1BMVEUAAAD%2F%2F%2F%2F%2F%2F%2F%2F%2FVyL%2F%2F%2F8bK2t7AAAAA3RSTlMAARYXuUDOAAAAhklEQVR42u3WyQ2AMAwEQAz9lxxCCRCuONHse2XPy3JsS98EAAAAAAAAQDpAfLywAgAADAuoJ716dw4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAwgLcCAACQH%2FB3AAAAAAAAAPoDWovrxYdjBwAAmA7QerAKAADANICnD0sBAADIDjgAPijZuQgZskEAAAAASUVORK5CYII%3D)](https://sblantipodi.github.io/glow_worm_luciferin)

Why don't you build your own `Luciferin`?  
Get up and running in minutes with the [**Quick Start
guide**](https://github.com/sblantipodi/toremove/wiki/Quick-start), or dive into the complete [
**Wiki**](https://github.com/sblantipodi/toremove/wiki) for every feature and setting.
## Easy to learn, easy to use

Effortless setup with minimal configuration required, yet highly customizable to meet specific needs.

<p align="center">
  <img src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/settings_1.png">
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/settings_2.png">
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/settings_3.png">
</p>

[<img width="900" src="https://github.com/sblantipodi/firefly_luciferin/blob/luciferin-v3/data/img/test_image_anim_color.gif?raw=true">](https://youtu.be/j7IV9rQr7J8)

#### (click the image above to watch it on YouTube)

## Luciferin Official PCB

<a href="https://github.com/sblantipodi/toremove/wiki/Ready-to-print-PCB"><img width="700" src="https://github.com/sblantipodi/glow_worm_luciferin/blob/master/assets/CAD_schematics/luciferin_official_pcb/img/PCB_front.jpg?raw=true"></a>
<a href="https://github.com/sblantipodi/toremove/wiki/Ready-to-print-PCB"><img width="700" src="https://github.com/sblantipodi/glow_worm_luciferin/blob/master/assets/CAD_schematics/luciferin_official_pcb/img/pre_after.jpg?raw=true"></a>

Prefer a DIY build over a prebuilt board? Grab your soldering iron and use [**Luciferin’s official
PCB**](https://github.com/sblantipodi/toremove/wiki/Ready-to-print-PCB), without designing your own.  
Explore [compatible hardware](https://github.com/sblantipodi/toremove/wiki/Compatible-Hardware), including
**supported prebuilt ready-to-use controllers**.

## Performance and hardware acceleration

Firefly Luciferin uses **hardware acceleration** to deliver responsive lighting with low CPU and GPU usage.  
Luciferin is deeply integrated with your operating system through a series of bindings to native libraries.   
This guarantees maximum efficiency and compatibility.

Depending on your operating system and hardware, supported technologies include  
**DirectX 12, OpenGL, Vulkan, Nvidia CUDA, Intel GPU acceleration, AMD HIP, SIMD (AVX, AVX2 and AVX512)**.

<p align="center">
  <img width="600" alt="Capture and LED controller frame rates" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/data/img/info_screen.png">
</p>

The FPS view shows how fast Firefly captures frames and how fast your LED controller processes them. For consistent
lighting, keep the capture rate within your controller’s capabilities and ensure a strong Wi-Fi signal when streaming
wirelessly.

Firefly Luciferin integrates GStreamer through [GStreamer Java](https://gstreamer.freedesktop.org/bindings/java.html)
bindings, combining native multimedia processing with hardware-accelerated capture for high performance and low resource
usage.

Explore [capture performance and benchmarks](https://github.com/sblantipodi/toremove/wiki/Very-fast-capture).

## Luciferin Web Interfaces

Both Luciferin components expose a **Web Interface**, so you get full control of your lights from any browser, including
your phone.

- **Glow Worm Luciferin** (FULL firmware): control your lights straight from the browser, with no need for the Firefly
  Luciferin desktop client.
- **Firefly Luciferin**: besides the desktop app, it now offers its own web interface for full control from your phone
  or any browser.

<img align="center" width="750" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/web_interface.jpg?raw=true">  
<p></p>
<img align="center" width="750" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/luciferin-v3/data/img/web_interface_ff.jpg?raw=true">

## Home Assistant integration

Luciferin can be easily integrated
in [Home Assistant](https://github.com/sblantipodi/toremove/wiki/Home-Automation-configs).  
<br>
<img align="center" width="800" src="https://github.com/sblantipodi/firefly_luciferin/blob/luciferin-v3/data/img/ha_luciferin.jpg?raw=true">

## Credits

- Davide Perini
- **A note on AI:** Luciferin started years before the AI boom. Its development now *also* leverages locally run AI
  models as a tool, but the architecture, design, review and a good part of the code remain entirely human.
