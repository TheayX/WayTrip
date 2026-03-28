package com.travel.constant;

import java.util.Map;

/**
 * 景点热度档位常量。
 */
public final class SpotHeatLevelConstants {

    public static final int NORMAL = 0;
    public static final int RECOMMENDED = 1;
    public static final int PRIORITY = 2;
    public static final int STRONG_RECOMMENDED = 3;

    private static final Map<Integer, Integer> LEVEL_SCORE_MAP = Map.of(
        NORMAL, 0,
        RECOMMENDED, 200,
        PRIORITY, 500,
        STRONG_RECOMMENDED, 1000
    );

    private SpotHeatLevelConstants() {
    }

    public static int toBaseScore(Integer heatLevel) {
        return LEVEL_SCORE_MAP.getOrDefault(heatLevel, 0);
    }
}
