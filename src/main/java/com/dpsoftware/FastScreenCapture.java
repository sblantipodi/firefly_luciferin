/*
  FastScreenCapture.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/

package com.dpsoftware;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.*;


/**
 * Fast Screen Capture for PC Ambilight
 * (https://github.com/sblantipodi/pc_ambilight)
 */
public class FastScreenCapture {

    // Number of CPU cores to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 CORES are enough for 24FPS on an Intel i7 5930K@4.2GHz
    final int NUMBER_OF_CORES = 4;
    int threadPoolNumber;
    int executorNumber;

    // FPS counter
    private static float FPS;

    // Number of LEDs on the strip
    final int LEDS_NUM = 95;

    // Initialize LED Matrix with the X,Y of your LEDs
    LEDMatrix ledMatrix = new LEDMatrix();

    // Arduino/Microcontroller config
    final int DATA_RATE = 500000;
    int TIMEOUT = 2000; // used for Serial connection timeout
    private SerialPort serial;
    private OutputStream output;

    // Screen resolution (with OS scaling - es: 3840x2160 with 150% OS scaling equals to 2560x1440)
    final int X_RES = ledMatrix.getSCREEN_RES_X();
    final int Y_RES = ledMatrix.getSCREEN_RES_Y();
    private Rectangle rect;

    // This queue orders elements FIFO. Producer offers some data, consumer throws data to the Serial port
    BlockingQueue sharedQueue = new LinkedBlockingQueue<Color[]>(100);


    public static void main(String[] args) throws Exception {

        FastScreenCapture fscapture = new FastScreenCapture();

        fscapture.initLEDMatrix();
        fscapture.initSerial();
        fscapture.initOutputStream();
        fscapture.getFPS();
        fscapture.initThreadPool();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(fscapture.threadPoolNumber);

        Robot robot = null;

        // Run producers
        for (int i = 0; i < fscapture.executorNumber; i++) {
            // One AWT Robot instance every three threads seems to be the sweet spot for performance.
            if (i%3 == 0) {
                robot = new Robot();
            }
            Robot finalRobot = robot;
            // No need for completablefuture here, we wrote the queue with a producer and we forget it
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    fscapture.producerTask(finalRobot);
                }
            }, 0, 250, TimeUnit.MILLISECONDS);
        }

        // Run a very fast consumer
        CompletableFuture.supplyAsync(() -> {
            try {
                fscapture.consume();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            return "Something went wrong.";
        }, scheduledExecutorService).thenAcceptAsync(s -> {
            System.out.println(s);
        }).exceptionally(e -> {
            fscapture.clean();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
            return null;
        });

    }

    /**
     * Initialize Serial communication
     */
    private void initSerial() {

        CommPortIdentifier serialPortId = null;
        Enumeration enumComm = CommPortIdentifier.getPortIdentifiers();
        while (enumComm.hasMoreElements() && serialPortId == null) {
            serialPortId = (CommPortIdentifier) enumComm.nextElement();
        }
        System.out.print("Serial Port in use: ");
        System.out.println(serialPortId.getName());
        try {
            serial = (SerialPort) serialPortId.open(this.getClass().getName(), TIMEOUT);
            serial.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (PortInUseException | UnsupportedCommOperationException e) {
            e.printStackTrace();
        }

    }

    /**
     * Initialize how many Threads to use in the ThreadPool and how many Executor to use
     */
    private void initThreadPool() {

        if (NUMBER_OF_CORES > 1) {
            threadPoolNumber = NUMBER_OF_CORES * 2;
            executorNumber = NUMBER_OF_CORES * 3;
        } else {
            threadPoolNumber = NUMBER_OF_CORES;
            executorNumber = NUMBER_OF_CORES;
        }

    }

    /**
     * Initialize LED Matrix based on LEDMatrix config
     */
    private void initLEDMatrix() {

        rect = new Rectangle(new Dimension(X_RES, Y_RES));
        ledMatrix.initLedMatrix();

    }

    /**
     * Initialize OutputStream
     */
    private void initOutputStream() {

        try {
            output = serial.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Screen Capture and analysis
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     * @return array of LEDs containing the avg color to be displayed on the LED strip
     */
    private Color[] getColors(Robot robot) {

        BufferedImage screen = robot.createScreenCapture(rect);

        Color[] leds = new Color[LEDS_NUM];
        Map matrix = ledMatrix.getLedMatrix();

        for (int led = 0; led < LEDS_NUM; led++) {
//            BufferedImage section = screen.getSubimage(led * SECT_WIDTH, 0, SECT_WIDTH, SECT_HEIGHT);
//            Color sectionAvgColor = getAvgColor(section);
//            Color c = new Color(255,0,0);

            LEDMatrix ledPosition = (LEDMatrix) matrix.get(led + 1);
//            System.out.println("---------------------------");
//            System.out.println("---------------------------");
//            System.out.println(ledPosition.getX());
            int colorAtPositionXY = screen.getRGB(ledPosition.getX(), ledPosition.getY());
            Color sectionAvgColor = new Color(colorAtPositionXY);

            leds[led] = sectionAvgColor;
        }
        return leds;

    }

    /**
     * Get the average color from the screen buffer section
     *
     * @param imgSection a little portion of the screen
     * @return the average color
     */
    private Color getAvgColor(BufferedImage imgSection) {

        int width = imgSection.getWidth();
        int height = imgSection.getHeight();
        int r = 0, g = 0, b = 255;
        int loops = 0;
        int SECT_SKIP = 10;
        for (int x = 0; x < width; x += SECT_SKIP) {
            for (int y = 0; y < height; y += SECT_SKIP) {
                int rgb = imgSection.getRGB(x, y);
                Color color = new Color(rgb);
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
                loops++;
            }
        }
        r = r / loops;
        g = g / loops;
        b = b / loops;
        return new Color(r, g, b);

    }

    /**
     * Write Serial Stream to the Serial Output
     *
     * @param leds array of LEDs containing the average color to display on the LED
     */
    private void sendColors(Color[] leds) throws IOException {

        FPS++;
        output.write(0xff);
        for (int i = 0; i < LEDS_NUM; i++) {
            output.write(leds[i].getRed()); //output.write(0);
            output.write(leds[i].getGreen()); //output.write(0);
            output.write(leds[i].getBlue()); //output.write(255);
        }

    }

    /**
     * Write Serial Stream to the Serial Output
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     */
    private void producerTask(Robot robot) {

        sharedQueue.offer(getColors(robot));

    }

    /**
     * Print the average FPS number we are able to capture
     */
    int consume() throws InterruptedException, IOException {

        while (true) {
            Color[] num = (Color[]) sharedQueue.take();
            if (num.length == 95) {
                sendColors(num);
                TimeUnit.MILLISECONDS.sleep(10);
            }
        }

    }

    /**
     * Print the average FPS number we are able to capture
     */
    void getFPS() {

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 5 seconds
        Runnable task1 = () -> {
            System.out.print(" --* FPS= " + (FPS / 5) + " *-- ");
            System.out.println(" | " + new Date() + " | ");
            FPS = 0;
        };
        scheduledExecutorService.scheduleAtFixedRate(task1, 0, 5, TimeUnit.SECONDS);

    }

    /**
     * Clean and Close Serial Output Stream
     */
    private void clean() {
        if(output != null) {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(serial != null) {
            serial.close();
        }
    }

}


