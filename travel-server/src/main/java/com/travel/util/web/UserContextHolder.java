package com.travel.util.web;

/**
 * 用户上下文。
 * <p>
 * 基于 {@link ThreadLocal} 在单次请求线程内保存当前用户或管理员身份信息。
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    /**
     * 写入当前用户 ID。
     *
     * @param userId 当前用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID；若未设置则返回 {@code null}
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 写入当前管理员 ID。
     *
     * @param adminId 当前管理员 ID
     */
    public static void setAdminId(Long adminId) {
        ADMIN_ID.set(adminId);
    }

    /**
     * 获取当前管理员 ID。
     *
     * @return 当前管理员 ID；若未设置则返回 {@code null}
     */
    public static Long getAdminId() {
        return ADMIN_ID.get();
    }

    /**
     * 清理当前线程中的身份上下文。
     */
    public static void clear() {
        USER_ID.remove();
        ADMIN_ID.remove();
    }
}
