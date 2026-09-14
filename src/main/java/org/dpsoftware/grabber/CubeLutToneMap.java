package org.dpsoftware.grabber;

import org.dpsoftware.config.InstanceConfigurer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class CubeLutToneMap {

    // TODO
    private static final String CUBE_FILENAME = "HDR2SDR_general.cube";
    private static final String CUBE_URL = "https://raw.githubusercontent.com/sverit/HDR2SDR-LUTs/main/HDR2SDR_general.cube";

    private static final float[] lut;
    private static final int size;

    private static int parsedSize;
    private static float[] parsedLut;

    static {
        float[] tmpLut;
        int tmpSize;
        try {
            File lutFile = getCubeFile();
            if (!lutFile.exists()) {
                downloadCube(CUBE_URL, lutFile);
            }
            parseCube(lutFile);
            tmpLut = parsedLut;
            tmpSize = parsedSize;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        lut = tmpLut;
        size = tmpSize;
    }

    private CubeLutToneMap() {
    }

    public static int[] lookup(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        int n = size - 1;
        float fx = rf * n, fy = gf * n, fz = bf * n;
        int x0 = (int) fx, y0 = (int) fy, z0 = (int) fz;
        if (x0 > n) x0 = n;
        if (y0 > n) y0 = n;
        if (z0 > n) z0 = n;
        int x1 = Math.min(x0 + 1, n);
        int y1 = Math.min(y0 + 1, n);
        int z1 = Math.min(z0 + 1, n);
        float tx = fx - x0, ty = fy - y0, tz = fz - z0;

        float rOut = trilinear(x0, y0, z0, x1, y1, z1, tx, ty, tz, 0);
        float gOut = trilinear(x0, y0, z0, x1, y1, z1, tx, ty, tz, 1);
        float bOut = trilinear(x0, y0, z0, x1, y1, z1, tx, ty, tz, 2);

        return new int[]{
                Math.round(Math.clamp(rOut, 0, 1) * 255),
                Math.round(Math.clamp(gOut, 0, 1) * 255),
                Math.round(Math.clamp(bOut, 0, 1) * 255)
        };
    }

    private static float trilinear(int x0, int y0, int z0, int x1, int y1, int z1,
                                   float tx, float ty, float tz, int ch) {
        int c000 = lutIndex(x0, y0, z0, ch), c100 = lutIndex(x1, y0, z0, ch);
        int c010 = lutIndex(x0, y1, z0, ch), c110 = lutIndex(x1, y1, z0, ch);
        int c001 = lutIndex(x0, y0, z1, ch), c101 = lutIndex(x1, y0, z1, ch);
        int c011 = lutIndex(x0, y1, z1, ch), c111 = lutIndex(x1, y1, z1, ch);

        float c00 = lut[c000] * (1 - tx) + lut[c100] * tx;
        float c10 = lut[c010] * (1 - tx) + lut[c110] * tx;
        float c01 = lut[c001] * (1 - tx) + lut[c101] * tx;
        float c11 = lut[c011] * (1 - tx) + lut[c111] * tx;

        float c0 = c00 * (1 - ty) + c10 * ty;
        float c1 = c01 * (1 - ty) + c11 * ty;

        return c0 * (1 - tz) + c1 * tz;
    }

    private static int lutIndex(int x, int y, int z, int ch) {
        return (z * size * size + y * size + x) * 3 + ch;
    }

    private static File getCubeFile() {
        File dir = new File(InstanceConfigurer.getConfigPath());
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, CUBE_FILENAME);
    }

    private static void downloadCube(String url, File dest) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } finally {
            conn.disconnect();
        }
    }

    private static void parseCube(File file) throws Exception {
        int s = 32;
        int count = s * s * s;
        float[] tmpLut = new float[count * 3];
        int idx = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.startsWith("LUT_3D_SIZE")) {
                    s = Integer.parseInt(line.split("\\s+")[1]);
                    count = s * s * s;
                    tmpLut = new float[count * 3];
                    continue;
                }
                if (line.startsWith("DOMAIN_") || line.startsWith("TITLE")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    tmpLut[idx++] = Float.parseFloat(parts[0]);
                    tmpLut[idx++] = Float.parseFloat(parts[1]);
                    tmpLut[idx++] = Float.parseFloat(parts[2]);
                }
            }
        }
        if (idx != count * 3) throw new Exception("LUT data incomplete: " + idx + " vs " + (count * 3));
        parsedSize = s;
        parsedLut = tmpLut;
    }
}
