package com.travel.constant;

import java.util.Map;

/**
 * 景点热度档位常量类，定义热度档位与基础热度分值的映射关系。
 * <p>
 * 热度档位常量集中维护后，后台配置、热度同步和推荐重排都可以复用同一套基准值。
 */
public final class SpotHeatLevelConstants {

    /**
     * 普通档位。
     */
    public static final int NORMAL = 0;

    /**
     * 推荐档位。
     */
    public static final int RECOMMENDED = 1;

    /**
     * 优先推荐档位。
     */
    public static final int PRIORITY = 2;

    /**
     * 强推荐档位。
     */
    public static final int STRONG_RECOMMENDED = 3;

    private static final Map<Integer, Integer> LEVEL_SCORE_MAP = Map.of(
        NORMAL, 0,
        RECOMMENDED, 200,
        PRIORITY, 500,
        STRONG_RECOMMENDED, 1000
    );

    private SpotHeatLevelConstants() {
    }

    /**
     * 将热度档位转换为对应的基础热度分值。
     *
     * @param heatLevel 热度档位
     * @return 基础热度分值；当档位为空或未定义时返回 0
     */
    public static int toBaseScore(Integer heatLevel) {
        return LEVEL_SCORE_MAP.getOrDefault(heatLevel, 0);
    }
}
