/*
  CubeLutToneMap.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.lut;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.InstanceConfigurer;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Cube LUT tone mapper.
 * <p>
 * Loads a 3D lookup table (LUT) in Adobe Cube format and performs trilinear interpolation to map HDR colors to SDR.
 * The LUT filename is read from the application configuration ({@code cubeLut}) at class initialization time
 * and can be changed at runtime via {@link #refresh()}.
 * The LUT is resolved from the following locations, in order:
 * - classpath resources co-located in this package ({@code org.dpsoftware.lut})
 * - configuration path {@code <config>/cube_lut/}
 * If the LUT is not found in either location, or cannot be parsed, the tone mapper degrades gracefully
 * and {@link #lookup(int, int, int)} returns the input color unmodified.
 */
@Slf4j
public final class CubeLutToneMap {

    private static final String CUBE_LUT_RESOURCE = "lut";
    private static final String CUBE_LUT_DIR = "cube_lut";

    /**
     * Parsed LUT data. Remains {@code null} when no LUT could be loaded,
     * in which case {@link #lookup(int, int, int)} returns the input unchanged.
     */
    private static volatile float[] lut;
    private static volatile int size;

    private static int parsedSize;
    private static float[] parsedLut;

    /**
     * Name of the LUT currently loaded. Used to detect config changes and
     * invalidate the cache on {@link #refresh()}.
     */
    private static volatile String loadedLutName = null;

    static {
        loadLutFromConfig();
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private CubeLutToneMap() {
    }

    /**
     * Re-read the LUT filename from {@code MainSingleton.getInstance().config.getCubeLut()}
     * and reload the LUT if the configured name has changed since the last load.
     * <p>
     * This allows the LUT to be switched at runtime (e.g. via a UI setting) without
     * re-initializing the class. If the configured name is unchanged, this method is a
     * no-op. If the new LUT cannot be loaded, the previously loaded LUT is retained.
     * </p>
     */
    public static void refresh() {
        if (isDisabled()) {
            lut = null;
            size = 0;
            loadedLutName = "Disabled";
            return;
        }
        String lutName = resolveLutName();
        if (lutName == null || lutName.equals(loadedLutName)) {
            return;
        }
        loadLut(lutName);
    }

    /**
     * List the filenames of the LUTs available to be loaded, using the same
     * resolution order as {@link #loadLut(String)}:
     * <ol>
     *   <li>classpath resources co-located in this package ({@code org.dpsoftware.lut})</li>
     *   <li>configuration path {@code <config>/cube_lut/}</li>
     * </ol>
     * Results are deduplicated (classpath entries take precedence) and sorted
     * lexicographically.
     *
     * @return sorted list of LUT filenames (e.g. {@code 1000nits_HDR-to-SDR.cube});
     * empty list if no LUT is available in either location
     */
    public static List<String> listAvailableLuts() {
        Set<String> names = new LinkedHashSet<>(listResourceLuts());
        File dir = new File(InstanceConfigurer.getConfigPath(), CUBE_LUT_DIR);
        File[] files = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".cube"));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName());
            }
        }
        List<String> result = new ArrayList<>(names);
        Collections.sort(result);
        result.addFirst(Constants.DISABLED);
        return result;
    }

    /**
     * List LUT filenames found co-located in this package ({@code org.dpsoftware.lut}).
     * The LUT files are packaged in the same package as this class and the package is
     * {@code opens} in the module descriptor, so the classloader can read them in JPMS
     * module mode as well (a non opened package would be invisible to it).
     * <p>
     * The lookup is performed by locating the class (jar or exploded classes directory)
     * and scanning it for {@code .cube} entries in this package, which works both when the
     * jar is on the classpath and when it is resolved as a named module.
     */
    private static List<String> listResourceLuts() {
        List<String> names = new ArrayList<>();
        try {
            URL codeSource = CubeLutToneMap.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSource == null) {
                return names;
            }
            String path = codeSource.getFile();
            if (path.endsWith(".jar")) {
                names.addAll(scanJarForCubeLuts(path));
            } else {
                // Exploded classes directory: the package directory is
                // <codeSource>/<package-as-paths>.
                File pkgDir = new File(new File(path),
                        CubeLutToneMap.class.getPackageName().replace('.', File.separatorChar));
                File[] files = pkgDir.listFiles(x -> x.isFile() && x.getName().endsWith(".cube"));
                if (files != null) {
                    for (File file : files) {
                        names.add(file.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to list cube LUT resources: {}", e.getMessage());
        }
        return names;
    }

    /**
     * Scan the entries of the given jar file for {@code .cube} files co-located in this
     * package ({@code org/dpsoftware/lut}) and return their base names.
     */
    private static List<String> scanJarForCubeLuts(String jarPath) {
        List<String> found = new ArrayList<>();
        String pkgDir = CubeLutToneMap.class.getPackageName().replace('.', '/') + "/";
        try (JarFile jar = new JarFile(new File(jarPath))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.startsWith(pkgDir) && entryName.endsWith(".cube")) {
                    String base = entryName.substring(pkgDir.length());
                    if (!base.contains("/")) {
                        found.add(base);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scan jar {} for cube LUTs: {}", jarPath, e.getMessage());
        }
        return found;
    }

    /**
     * Return {@code true} when the configured LUT is {@code "Disabled"}, meaning
     * the tone mapper must be completely bypassed (no LUT loaded, {@link #lookup}
     * returns the input color unmodified).
     */
    private static boolean isDisabled() {
        Configuration cfg = MainSingleton.getInstance().config;
        return cfg != null && Constants.DISABLED.equals(cfg.getCubeLut());
    }

    private static String resolveLutName() {
        Configuration cfg = MainSingleton.getInstance().config;
        if (cfg == null) {
            return Constants.DEFAULT_CUBE_LUT;
        }
        String lutName = cfg.getCubeLut();
        if (lutName == null || lutName.isBlank() || Constants.DISABLED.equals(lutName)) {
            return null;
        }
        return lutName;
    }

    private static void loadLutFromConfig() {
        if (isDisabled()) {
            lut = null;
            size = 0;
            loadedLutName = Constants.DISABLED;
            return;
        }
        String lutName = resolveLutName();
        loadLut(lutName);
    }

    /**
     * Resolve the LUT from classpath then config path, parse it, and publish the
     * result. On any failure the previous LUT is retained (or {@code null} on first load).
     */
    private static void loadLut(String lutName) {
        if (lutName == null) {
            return;
        }
        try {
            // Resolve the LUT stream: classpath resource first, then the co-located
            // file in the package (JPMS module mode, where the classloader does not
            // resolve resources from the classpath), then the user config location.
            String resourcePath = CUBE_LUT_RESOURCE + "/" + lutName;
            InputStream in = CubeLutToneMap.class.getResourceAsStream(resourcePath);
            if (in == null) {
                File pkgFile = resolveCoLocatedLutFile(lutName);
                if (pkgFile != null && pkgFile.exists()) {
                    in = new FileInputStream(pkgFile);
                }
            }
            if (in == null) {
                File dir = new File(InstanceConfigurer.getConfigPath());
                File localFile = new File(dir, CUBE_LUT_DIR + File.separator + lutName);
                if (localFile.exists()) {
                    in = new FileInputStream(localFile);
                }
            }
            if (in == null) {
                log.warn("Cube LUT '{}' not found on classpath, in package dir, or in config path", lutName);
                return;
            }
            try (InputStream stream = in) {
                parseCube(stream);
            }
            lut = parsedLut;
            size = parsedSize;
            loadedLutName = lutName;
            log.info("Loaded cube LUT '{}' ({}x{}x{})", lutName, size, size, size);
        } catch (Exception e) {
            // LUT unavailable or unreadable; retain the previous LUT (identity if none).
            log.warn("Failed to load cube LUT '{}': {}", lutName, e.getMessage());
        }
    }

    /**
     * Resolve the {@code .cube} file co-located in this package on the filesystem, when the
     * class was loaded from an exploded classes directory (e.g. the deployed
     * {@code lib/app/classes} layout). Returns {@code null} when the class was loaded from a
     * jar, in which case the LUT must be read through the classloader instead.
     */
    private static File resolveCoLocatedLutFile(String lutName) {
        try {
            URL codeSource = CubeLutToneMap.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSource == null || codeSource.getFile().endsWith(".jar")) {
                return null;
            }
            File pkgDir = new File(new File(codeSource.getFile()),
                    CubeLutToneMap.class.getPackageName().replace('.', File.separatorChar));
            return new File(pkgDir, lutName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Look up the tone mapped SDR color for the given HDR RGB values, preserving floating-point precision
     * end to end. Each input channel is normalized to [0, 1], mapped to LUT grid coordinates, and the result
     * is obtained via trilinear interpolation between the 8 nearest LUT entries.
     * If no LUT was loaded (resource and config-path file both missing, or parsing failed), the input color
     * is returned unmodified (still as float, no rounding).
     * This is the primary implementation; {@link #lookup(int, int, int)} delegates here.
     *
     * @param r red   channel in [0, 255] (float precision, values outside range are clamped)
     * @param g green channel in [0, 255]
     * @param b blue  channel in [0, 255]
     * @return tone mapped RGB array in [0, 255] float range; the original input when no LUT is available
     */
    public static float[] lookup(float r, float g, float b) {
        if (lut == null) {
            return new float[]{r, g, b};
        }
        float rf = Math.clamp(r, 0, 255) / 255f;
        float gf = Math.clamp(g, 0, 255) / 255f;
        float bf = Math.clamp(b, 0, 255) / 255f;
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

        return new float[]{Math.clamp(rOut, 0, 1) * 255, Math.clamp(gOut, 0, 1) * 255, Math.clamp(bOut, 0, 1) * 255};
    }

    /**
     * Integer convenience overload of {@link #lookup(float, float, float)}, for callers that only have
     * 8-bit channel values available (e.g. reading raw pixel bytes for the debug screenshot path).
     * Prefer the float overload when the caller already holds float precision, to avoid an unnecessary
     * truncation before the LUT lookup.
     *
     * @param r red   channel in [0, 255]
     * @param g green channel in [0, 255]
     * @param b blue  channel in [0, 255]
     * @return tone mapped RGB array, each value rounded and clamped to [0, 255]
     */
    public static int[] lookup(int r, int g, int b) {
        float[] out = lookup((float) r, (float) g, (float) b);
        return new int[]{Math.round(out[0]), Math.round(out[1]), Math.round(out[2])};
    }

    /**
     * Perform trilinear interpolation of a single color channel.
     *
     * @param x0, y0, z0 lower corner grid coordinates
     * @param x1, y1, z1 upper corner grid coordinates
     * @param tx, ty, tz fractional offsets within the grid cell
     * @param ch  color channel index: 0=red, 1=green, 2=blue
     * @return interpolated value, typically in [0, 1]
     */
    private static float trilinear(int x0, int y0, int z0, int x1, int y1, int z1, float tx, float ty, float tz, int ch) {
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

    /**
     * Compute the flat array index for a given LUT grid coordinate and channel.
     * The LUT is stored in ZYX order (blue, green, red), with R, G, B values
     * laid out contiguously for each grid point.
     *
     * @param x red   grid coordinate
     * @param y green grid coordinate
     * @param z blue  grid coordinate
     * @param ch color channel index: 0=red, 1=green, 2=blue
     * @return index into the flat LUT array
     */
    private static int lutIndex(int x, int y, int z, int ch) {
        return (z * size * size + y * size + x) * 3 + ch;
    }

    /**
     * Parse an Adobe Cube format LUT from the given input stream. Reads the {@code LUT_3D_SIZE} directive to
     * determine the grid dimensions, then reads the R G B float triples. Comments ({@code #}),
     * {@code DOMAIN_MIN/MAX} and {@code TITLE} lines are skipped.
     * Package-private for unit testing.
     *
     * @param in input stream containing the Cube LUT file content
     * @throws Exception if the LUT data is incomplete or cannot be parsed
     */
    static void parseCube(InputStream in) throws Exception {
        int s = 32;
        int count = s * s * s;
        float[] tmpLut = new float[count * 3];
        int idx = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
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
