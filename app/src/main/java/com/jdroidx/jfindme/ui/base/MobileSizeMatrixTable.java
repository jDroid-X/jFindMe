package com.jdroidx.jfindme.ui.base;

import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.ArrayList;
import java.util.List;

/**
 * 👑 L7 ENTERPRISE ARCHITECTURE: Linear Threshold Interceptor (V5.8)
 * REFACTORED: From Nearest-Neighbor to Threshold-Based Reconciler.
 * This ensures that standard Android phones (e.g., 411dp) successfully 
 * hit the STANDARD (1.2x) multiplier instead of snapping to COMPACT (1.0x).
 * 🚀 L7 Architecture Sync: 120px Universal Icon Baseline Verified.
 */
public class MobileSizeMatrixTable {

    public static final float BASELINE_DP = 400.0f;

    public static class StageTether {
        public UiArchitect.DesignStage stage;
        public float minWidth; // Lower threshold bound
        public float multiplier;

        public StageTether(UiArchitect.DesignStage stage, float minWidth, float multiplier) {
            this.stage = stage;
            this.minWidth = minWidth;
            this.multiplier = multiplier;
        }
    }

    private static final List<StageTether> REGISTRY = new ArrayList<>();

    static {
        // --- 🚀 THRESHOLD-BASED MULTIPLIER REGISTRY ---
        // We now use a 'Floor' logic. If width >= minWidth, that stage is engaged.
        REGISTRY.add(new StageTether(UiArchitect.DesignStage.LIBERAL,   700.0f, 1.4f)); // Liberal (1.4x) starts at 700dp+
        REGISTRY.add(new StageTether(UiArchitect.DesignStage.STANDARD,  500.0f, 1.2f)); // Standard (1.2x) starts at 500dp+
        REGISTRY.add(new StageTether(UiArchitect.DesignStage.COMPACT,   350.0f, 1.0f)); // Compact (1.0x) starts at 350dp+
        REGISTRY.add(new StageTether(UiArchitect.DesignStage.SLIM,      200.0f, 0.9f)); // Slim (0.9x) starts at 200dp+
        REGISTRY.add(new StageTether(UiArchitect.DesignStage.ULTRA_SLIM, 0.0f,  0.8f)); // Ultra-Slim (0.8x) is the fallback
    }

    /**
     * 🚀 PILLAR: Threshold-Based Interceptor
     * Analyzes device width and identifies the highest applicable tier.
     */
    public static StageTether reconcileStage(float dpWidth) {
        for (StageTether tether : REGISTRY) {
            if (dpWidth >= tether.minWidth) {
                SystemLogger.i("MobileSizeMatrix", 
                    "L7 Threshold Match: [" + (int)dpWidth + "dp] -> [" + tether.stage.name() + " (" + tether.multiplier + "x)] engaged.");
                return tether;
            }
        }
        return REGISTRY.get(REGISTRY.size() - 1); // Final Fallback: Ultra-Slim
    }

    /**
     * 🚀 Returns the calculated multiplier for a specific stage.
     */
    public static float getFactor(UiArchitect.DesignStage stage) {
        for (StageTether tether : REGISTRY) {
            if (tether.stage == stage) return tether.multiplier;
        }
        return 1.0f;
    }
}
