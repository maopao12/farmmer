package com.smartfarm.framework.security;

import com.smartfarm.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

/**
 * RBAC工具类 — 统一权限判断逻辑
 *
 * <pre>
 * 核心规则：
 *   ADMIN/SUPER_ADMIN → 全局视图，跳过 owner 校验
 *   FARMER → 仅能访问自己的资源
 *
 * 用法：
 *   // 在Service中校验资源归属
 *   RbacUtils.checkOwnership(resourceOwnerId, "地块");
 *
 *   // 在Controller中校验角色
 *   RbacUtils.requireAdmin();
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
public final class RbacUtils {

    private RbacUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 校验当前用户是否为 ADMIN 或 SUPER_ADMIN
     */
    public static boolean isAdmin() {
        String role = UserContext.getCurrentUserRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    /**
     * 校验当前用户是否为 FARMER
     */
    public static boolean isFarmer() {
        return "FARMER".equals(UserContext.getCurrentUserRole());
    }

    /**
     * 要求当前用户必须是 ADMIN，否则抛403
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            log.warn("[RBAC] 非ADMIN用户尝试执行管理操作: userId={}, role={}",
                    UserContext.getCurrentUserId(), UserContext.getCurrentUserRole());
            throw BizException.noPermission();
        }
    }

    /**
     * 校验资源归属。
     * <p>
     * ADMIN → 直接通过
     * FARMER → resourceOwnerId 必须等于 currentUserId
     *
     * @param resourceOwnerId 资源所有者的用户ID
     * @param resourceName    资源名称（用于日志）
     * @throws BizException 非归属且非ADMIN时抛出403
     */
    public static void checkOwnership(Long resourceOwnerId, String resourceName) {
        if (isAdmin()) {
            return; // ADMIN 拥有全局访问权
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (resourceOwnerId == null || !resourceOwnerId.equals(currentUserId)) {
            log.warn("[RBAC-越权] 用户{} 尝试访问不属于自己的{} (owner={})",
                    currentUserId, resourceName, resourceOwnerId);
            throw BizException.noPermission();
        }
    }

    /**
     * 获取当前用户的 owner 过滤ID。
     * <p>
     * ADMIN → 返回 null (表示不过滤，查全部)
     * FARMER → 返回 currentUserId (用于 WHERE owner_id = ?)
     *
     * @return null 表示不需要过滤，非null 则是要过滤的 ownerId
     */
    public static Long getOwnerFilterId() {
        if (isAdmin()) {
            return null; // ADMIN查全部
        }
        return UserContext.getCurrentUserId();
    }

    /**
     * 获取当前登录用户ID，未登录抛异常
     */
    public static Long getCurrentUserId() {
        return UserContext.getCurrentUserId();
    }
}
