package net.ming.bilibilichatmcforge.utils;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Cross-version compatibility layer for Minecraft 1.21.x.
 *
 * <p>Handles API differences between MC versions at runtime via reflection:
 * <ul>
 *   <li>1.21.9 ~ 1.21.10: {@code CommandSourceStack.hasPermission(int)}</li>
 *   <li>1.21.11+: {@code CommandSourceStack.permissions().hasPermission(Permission)}</li>
 * </ul>
 *
 * <p>Rendering pipeline (renderBackground) is identical across 1.21.9+,
 * so no version-specific rendering code is needed.
 */
public final class VersionCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MC_VERSION = detectMCVersion();
    private static final boolean NEW_PERMISSION_SYSTEM;

    // Cached reflection objects for legacy permission system (1.21.10 and below)
    private static Method legacyHasPermissionMethod;

    // Cached reflection objects for new permission system (1.21.11+)
    private static Method newPermissionsMethod;
    private static Method newHasPermissionMethod;
    private static Object gamemastersPermission;

    static {
        boolean detected;
        try {
            Class.forName("net.minecraft.server.permissions.Permission");
            detected = true;
            initNewPermissionSystem();
        } catch (ClassNotFoundException e) {
            detected = false;
            initLegacyPermissionSystem();
        }
        NEW_PERMISSION_SYSTEM = detected;
        LOGGER.info("[VersionCompat] MC version: {}, permission API: {}",
                MC_VERSION, detected ? "new (1.21.11+)" : "legacy (1.21.10-)");
    }

    private VersionCompat() {}

    private static String detectMCVersion() {
        try {
            Object version = net.minecraft.SharedConstants.getCurrentVersion();
            // WorldVersion API varies across 1.21.x: try getName(), then getReleaseTarget(), then toString()
            for (String methodName : new String[]{"getName", "getReleaseTarget"}) {
                try {
                    java.lang.reflect.Method m = version.getClass().getMethod(methodName);
                    Object result = m.invoke(version);
                    if (result != null) return result.toString();
                } catch (NoSuchMethodException ignored) {}
            }
            return version.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @SuppressWarnings("unchecked")
    private static void initLegacyPermissionSystem() {
        try {
            legacyHasPermissionMethod = CommandSourceStack.class.getMethod("hasPermission", int.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("[VersionCompat] Could not find hasPermission(int) method", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void initNewPermissionSystem() {
        try {
            Class<?> permLevelClass = Class.forName("net.minecraft.server.permissions.PermissionLevel");
            Class<?> permClass = Class.forName("net.minecraft.server.permissions.Permission");
            Class<?> hasCmdLevelClass = Class.forName("net.minecraft.server.permissions.Permission$HasCommandLevel");
            Class<?> permSetClass = Class.forName("net.minecraft.server.permissions.PermissionSet");

            // PermissionLevel.GAMEMASTERS
            Object gamemastersLevel = permLevelClass.getField("GAMEMASTERS").get(null);

            // new Permission.HasCommandLevel(GAMEMASTERS)
            gamemastersPermission = hasCmdLevelClass
                    .getConstructor(permLevelClass)
                    .newInstance(gamemastersLevel);

            // CommandSourceStack.permissions()
            newPermissionsMethod = CommandSourceStack.class.getMethod("permissions");

            // PermissionSet.hasPermission(Permission)
            newHasPermissionMethod = permSetClass.getMethod("hasPermission", permClass);
        } catch (Exception e) {
            LOGGER.error("[VersionCompat] Failed to initialize new permission system", e);
        }
    }

    /**
     * Check if a CommandSourceStack has the specified permission level.
     *
     * <p>Uses reflection to support both legacy (hasPermission(int)) and
     * new (permissions().hasPermission(HasCommandLevel)) APIs.
     *
     * @param source the command source
     * @param level  the permission level (0-4, matching op levels)
     * @return true if the source has permission
     */
    public static boolean checkPermission(CommandSourceStack source, int level) {
        if (NEW_PERMISSION_SYSTEM) {
            try {
                Object permSet = newPermissionsMethod.invoke(source);
                return (boolean) newHasPermissionMethod.invoke(permSet, gamemastersPermission);
            } catch (Exception e) {
                LOGGER.error("[VersionCompat] Failed to check permission via new API", e);
                return false;
            }
        } else {
            try {
                return (boolean) legacyHasPermissionMethod.invoke(source, level);
            } catch (Exception e) {
                LOGGER.error("[VersionCompat] Failed to check permission via legacy API", e);
                return false;
            }
        }
    }

    /**
     * Get the detected Minecraft version string (e.g. "1.21.10", "1.21.11").
     *
     * @return the MC version, or "unknown" if detection failed
     */
    public static String getMCVersion() {
        return MC_VERSION;
    }

    /**
     * Check if the running MC version is at least the specified version.
     *
     * @param minVersion the minimum version (e.g. "1.21.11")
     * @return true if the running version is >= minVersion
     */
    public static boolean isAtLeast(String minVersion) {
        return compareVersions(MC_VERSION, minVersion) >= 0;
    }

    /**
     * Compare two version strings (e.g. "1.21.10" vs "1.21.11").
     *
     * @return negative if v1 < v2, 0 if equal, positive if v1 > v2
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
