package com.travel.config;

/**
 * Centralized Redis key naming rules.
 * <p>
 * Key format:
 * waytrip:{module}:{resource}[:identifier...]
 */
public final class RedisKeyManager {

    private static final String ROOT = "waytrip";
    private static final String RECOMMENDATION = ROOT + ":recommendation";
    private static final String SPOT = ROOT + ":spot";

    private RedisKeyManager() {
    }

    public static String recommendationSimilarity(Long spotId) {
        return RECOMMENDATION + ":similarity:" + spotId;
    }

    public static String recommendationUser(Long userId) {
        return RECOMMENDATION + ":user:" + userId;
    }

    public static String recommendationConfig() {
        return RECOMMENDATION + ":config";
    }

    public static String recommendationConfigAlgorithm() {
        return recommendationConfig() + ":algorithm";
    }

    public static String recommendationConfigHeat() {
        return recommendationConfig() + ":heat";
    }

    public static String recommendationConfigCache() {
        return recommendationConfig() + ":cache";
    }

    public static String recommendationStatus() {
        return RECOMMENDATION + ":status";
    }

    public static String spotHeatView(Long spotId, Long userId) {
        return SPOT + ":heat:view:" + spotId + ":" + userId;
    }
}
