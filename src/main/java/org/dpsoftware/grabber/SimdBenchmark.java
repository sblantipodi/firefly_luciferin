/*
  SimdBenchmark.java

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
package org.dpsoftware.grabber;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for benchmarking SIMD (Single Instruction Multiple Data) and scalar CPU computations,
 * along with the management of SIMD processing strategies.
 */
@Slf4j
public class SimdBenchmark {

    static final Object SIMD_STRATEGY_BENCH_LOCK = new Object();
    public static long simdStrategyDoubleVectorBenchNanos;
    public static long simdStrategyFullVectorBenchNanos;
    public static long totalBenchmarkedFramesDoubleVector;
    public static long totalBenchmarkedFramesFullVector;
    public static volatile SimdProcessingStrategy selectedSimdStrategy = resolveInitialSimdProcessingStrategy();
    static long startSimdTime;
    static boolean usingSimd;
    static long simdBenchEndTime = -1;

    /**
     * Determines the initial SIMD processing strategy based on an environment variable.
     * If an override is provided via the environment variable defined by {@link Constants#LUCIFERIN_SIMD_STRATEGY_OVERRIDE}, the method attempts to match
     * the override to a known {@link SimdProcessingStrategy} value. If no valid override is found or if the variable is unset, the method returns {@code null}.
     *
     * @return The resolved {@link SimdProcessingStrategy} based on the override, or {@code null} if no valid override is specified or the environment variable is missing.
     */
    private static SimdProcessingStrategy resolveInitialSimdProcessingStrategy() {
        String override = System.getenv(Constants.LUCIFERIN_SIMD_STRATEGY_OVERRIDE);
        if (override != null) {
            if (SimdProcessingStrategy.DOUBLE_VECTOR.name().equals(override)) {
                return SimdProcessingStrategy.DOUBLE_VECTOR;
            }
            if (SimdProcessingStrategy.FULL_VECTOR.name().equals(override)) {
                return SimdProcessingStrategy.FULL_VECTOR;
            }
        }
        return null;
    }

    /**
     * Provides a description of the SIMD strategy selection process based on current system properties and runtime state. If an override for the SIMD strategy is provided via an environment variable,
     * the method describes the override. Otherwise, it returns the current state of the runtime benchmark process, potentially including CPU identification details.
     *
     * @return A string describing how the SIMD strategy is selected, either through an override or runtime benchmarking, optionally including CPU identification information.
     */
    static String describeSimdStrategySelection() {
        String override = System.getenv(Constants.LUCIFERIN_SIMD_STRATEGY_OVERRIDE);
        if (override != null && !override.isBlank() && (override.equals(SimdProcessingStrategy.DOUBLE_VECTOR.name())
                || override.equals(SimdProcessingStrategy.FULL_VECTOR.name()))) {
            return "override via env variable -> " + Constants.LUCIFERIN_SIMD_STRATEGY_OVERRIDE + "=" + override;
        }
        String cpuIdentifier = getCpuIdentifier();
        return cpuIdentifier.isBlank()
                ? "runtime benchmark pending"
                : "runtime benchmark pending on cpu=" + cpuIdentifier;
    }

    private static String getCpuIdentifier() {
        String processorIdentifier = System.getenv("PROCESSOR_IDENTIFIER");
        if (processorIdentifier != null && !processorIdentifier.isBlank()) {
            return processorIdentifier.toLowerCase();
        }
        String osArch = System.getProperty("os.arch");
        return osArch != null ? osArch.toLowerCase() : "";
    }

    /**
     * Bench SIMD vs Scalar CPU computations
     *
     * @param pickNumber LED to analuze (first one=
     */
    static void benchSimd(int pickNumber) {
        if (pickNumber == 0) {
            int simdScalarBenchIterations = (int) (MainSingleton.getInstance().FPS_PRODUCER * Constants.SIMD_SCALAR_BENCH_ITERATIONS);
            if (GrabberSingleton.getInstance().getNanoSimd().size() < simdScalarBenchIterations) {
                if (usingSimd) GrabberSingleton.getInstance().getNanoSimd().add(System.nanoTime() - startSimdTime);
            } else {
                printSimdBenchResult();
            }
            if (GrabberSingleton.getInstance().getNanoScalar().size() < simdScalarBenchIterations) {
                if (!usingSimd) GrabberSingleton.getInstance().getNanoScalar().add(System.nanoTime() - startSimdTime);
            } else {
                printSimdBenchResult();
            }
        }
    }

    /**
     * Print Bench results for SIMD vs Scalar CPU computations
     */
    private static void printSimdBenchResult() {
        long avgSimdTime = 0;
        long avgScalarTime = 0;
        if (!GrabberSingleton.getInstance().getNanoSimd().isEmpty()) {
            avgSimdTime = (long) GrabberSingleton.getInstance().getNanoSimd().stream()
                    .mapToLong(l -> l)
                    .average()
                    .orElse(0.0);
        }
        if (!GrabberSingleton.getInstance().getNanoScalar().isEmpty()) {
            avgScalarTime = (long) GrabberSingleton.getInstance().getNanoScalar().stream()
                    .mapToLong(l -> l)
                    .average()
                    .orElse(0.0);
        }
        List<Long> unifiedList = new ArrayList<>(GrabberSingleton.getInstance().getNanoSimd());
        unifiedList.addAll(GrabberSingleton.getInstance().getNanoScalar());
        long averageTime = (long) unifiedList.stream()
                .mapToLong(l -> l)
                .average()
                .orElse(0.0);
        if (Enums.SimdAvxOption.findByValue(MainSingleton.getInstance().config.getSimdAvx()).getSimdOptionNumeric() != 0) {
            log.trace("AVG TIME FOR ONE FRAME={}ns - AVG SIMD BENCH={}ns - AVG SCALAR BENCH={}ns", averageTime, avgSimdTime, avgScalarTime);
        }
        MainSingleton.getInstance().setCpuLatencyBench((int) averageTime);
        GrabberSingleton.getInstance().getNanoSimd().clear();
        GrabberSingleton.getInstance().getNanoScalar().clear();
    }

    /**
     * Resets the SIMD benchmark state at runtime.
     * Upon capturing the next frame, the system will initiate a new benchmark cycle.
     */
    public static void resetSimdBenchmark() {
        synchronized (SIMD_STRATEGY_BENCH_LOCK) {
            // If there is a system property override (-DLUCIFERIN_SIMD_STRATEGY), we skip the benchmark to respect the user's fixed choice.
            String simdStrategy = System.getenv(Constants.LUCIFERIN_SIMD_STRATEGY_OVERRIDE);
            if (simdStrategy != null && (simdStrategy.equals(SimdProcessingStrategy.DOUBLE_VECTOR.name())
                    || simdStrategy.equals(SimdProcessingStrategy.FULL_VECTOR.name()))) {
                log.debug("Cannot reset benchmark: an env override (-D{}) is currently active.", Constants.LUCIFERIN_SIMD_STRATEGY_OVERRIDE);
                return;
            }
            selectedSimdStrategy = null;
            simdBenchEndTime = -1;
            simdStrategyDoubleVectorBenchNanos = 0;
            simdStrategyFullVectorBenchNanos = 0;
            totalBenchmarkedFramesDoubleVector = 0;
            totalBenchmarkedFramesFullVector = 0;
            log.debug("SIMD benchmark reset at runtime");
        }
    }

    /**
     * Performs operations related to SIMD strategy benchmarking and CPU latency analysis. If benchmarking is active, it calculates and accumulates the elapsed time for doubleVector
     * SIMD and fullVector SIMD strategies for the current frame. Once the benchmarking period ends, it selects a SIMD processing strategy based on the total elapsed times.
     * Additionally, it invokes latency benchmarking when specific debug conditions are met.
     *
     * @param isBenchmarkingActive Indicates whether the benchmarking process is currently active.
     */
    public static void evaluateSimdPerformance(boolean isBenchmarkingActive) {
        if (isBenchmarkingActive) {
            synchronized (SIMD_STRATEGY_BENCH_LOCK) {

                if (System.currentTimeMillis() >= simdBenchEndTime) {
                    int resDoubleVector = Math.toIntExact(simdStrategyDoubleVectorBenchNanos / totalBenchmarkedFramesDoubleVector);
                    int resFullVector = Math.toIntExact(simdStrategyFullVectorBenchNanos / totalBenchmarkedFramesFullVector);
                    selectedSimdStrategy = resFullVector <= resDoubleVector
                            ? SimdProcessingStrategy.FULL_VECTOR
                            : SimdProcessingStrategy.DOUBLE_VECTOR;
                    log.debug("SIMD benchmark completed: selected={}, Double Vector Avg={}ns, Full Vector Avg={}ns",
                            selectedSimdStrategy,
                            resDoubleVector,
                            resFullVector);
                }
            }
        }
        if (log.isDebugEnabled() || MainSingleton.getInstance().isCpuLatencyBenchRunning()) {
            benchSimd(0);
        }
    }

    public enum SimdProcessingStrategy {
        DOUBLE_VECTOR,
        FULL_VECTOR
    }

}
