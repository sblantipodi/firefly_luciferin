/*
  CustomGDI32Util.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.grabber;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.dpsoftware.config.Constants;

import java.awt.*;
import java.awt.image.*;

/**
 * GPU Hardware Acceleration using Java Native Access API
 */
public class CustomGDI32Util {

    private static final DirectColorModel SCREENSHOT_COLOR_MODEL = new DirectColorModel(24, 16711680, 65280, 255);
    private static final int[] SCREENSHOT_BAND_MASKS;
    private final HWND target;
    int windowWidth;
    int windowHeight;
    Memory buffer;
    int bufferSize;
    BITMAPINFO bmi;
    DataBuffer dataBuffer;
    BufferedImage image;
    HANDLE hOriginal;

    /**
     * Constructor
     * @param target hwnd
     */
    public CustomGDI32Util(HWND target) {

        RECT rect = new RECT();
        this.target = target;
        if (!User32.INSTANCE.GetWindowRect(target, rect)) {
            throw new Win32Exception(Native.getLastError());
        }
        Rectangle jRectangle = rect.toRectangle();
        windowWidth = jRectangle.width;
        windowHeight = jRectangle.height;
        buffer = new Memory((long) windowWidth * windowHeight * 4);
        bufferSize = windowWidth * windowHeight;
        bmi = new BITMAPINFO();
        bmi.bmiHeader.biWidth = windowWidth;
        bmi.bmiHeader.biHeight = -windowHeight;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = 0;
        dataBuffer = new DataBufferInt(buffer.getIntArray(0L, bufferSize), bufferSize);
        image = null;

    }

    /**
     * Take single picture at high framerate
     * @return screenshot image
     */
    public BufferedImage getScreenshot() {

        if (windowWidth != 0 && windowHeight != 0) {
            HDC  hdcTarget = User32.INSTANCE.GetDC(target);
            if (hdcTarget == null) {
                throw new Win32Exception(Native.getLastError());
            } else {
                HDC hdcTargetMem;
                HBITMAP hBitmap;

                {
                    HANDLE result;
                    {
                        try {
                            hdcTargetMem = GDI32.INSTANCE.CreateCompatibleDC(hdcTarget);
                            if (hdcTargetMem == null) {
                                throw new Win32Exception(Native.getLastError());
                            }

                            hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcTarget, windowWidth, windowHeight);
                            if (hBitmap == null) {
                                throw new Win32Exception(Native.getLastError());
                            }

                            hOriginal = GDI32.INSTANCE.SelectObject(hdcTargetMem, hBitmap);
                            if (hOriginal == null) {
                                throw new Win32Exception(Native.getLastError());
                            }

                            if (!GDI32.INSTANCE.BitBlt(hdcTargetMem, 0, 0, windowWidth, windowHeight, hdcTarget, 0, 0, 13369376)) {
                                throw new Win32Exception(Native.getLastError());
                            }

                            int resultOfDrawing = GDI32.INSTANCE.GetDIBits(hdcTarget, hBitmap, 0, windowHeight, buffer, bmi, 0);
                            if (resultOfDrawing == 0 || resultOfDrawing == 87) {
                                throw new Win32Exception(Native.getLastError());
                            }

                            DataBuffer dataBuffer = new DataBufferInt(buffer.getIntArray(0L, bufferSize), bufferSize);
                            WritableRaster raster = Raster.createPackedRaster(dataBuffer, windowWidth, windowHeight, windowWidth, SCREENSHOT_BAND_MASKS, null);
                            image = new BufferedImage(SCREENSHOT_COLOR_MODEL, raster, false, null);

                        } catch (Win32Exception var23) {
                            throw new IllegalStateException(Constants.WIN32_EXCEPTION);
                        }

                    }

                    if (hOriginal != null) {
                        result = GDI32.INSTANCE.SelectObject(hdcTargetMem, hOriginal);
                        if (result == null || WinGDI.HGDI_ERROR.equals(result)) {
                            throw new IllegalStateException(Constants.SELECT_OBJ_EXCEPTION);
                        }
                    }

                    if (!GDI32.INSTANCE.DeleteObject(hBitmap)) {
                        throw new IllegalStateException(Constants.DELETE_OBJ_EXCEPTION);
                    }

                    if (!GDI32.INSTANCE.DeleteDC(hdcTargetMem)) {
                        throw new IllegalStateException(Constants.DELETE_DC_EXCEPTION);
                    }

                    if (0 == User32.INSTANCE.ReleaseDC(target, hdcTarget)) {
                        throw new IllegalStateException(Constants.DEVICE_CONTEXT_RELEASE_EXCEPTION);
                    }
                }

                return image;

            }
        } else {
            throw new IllegalStateException(Constants.WINDOWS_EXCEPTION);
        }

    }

    static {
        SCREENSHOT_BAND_MASKS = new int[]{SCREENSHOT_COLOR_MODEL.getRedMask(), SCREENSHOT_COLOR_MODEL.getGreenMask(), SCREENSHOT_COLOR_MODEL.getBlueMask()};
    }
}