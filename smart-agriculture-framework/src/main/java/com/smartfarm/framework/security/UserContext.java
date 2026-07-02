package com.smartfarm.framework.security;

/**
 * 当前用户上下文 — 从JWT Token中解析后由拦截器注入
 * <p>
 * 使用 ThreadLocal 确保线程安全
 *
 * @author SmartFarm Team
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    public static void setCurrentUser(LoginUser user) {
        USER_HOLDER.set(user);
    }

    public static LoginUser getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static Long getCurrentUserId() {
        LoginUser user = USER_HOLDER.get();
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return user.id();
    }

    public static String getCurrentUserRole() {
        LoginUser user = USER_HOLDER.get();
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return user.role();
    }

    public static boolean isAdmin() {
        String role = getCurrentUserRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    public static void clear() {
        USER_HOLDER.remove();
    }

    /**
     * JWT中解析出的登录用户信息
     */
    public record LoginUser(Long id, String username, String role, String realName) {
    }
}
