package net.ming.bilibilichatmcforge.utils;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Compatibility layer for Minecraft 26.1.x permission system.
 *
 * <p>MC 26.1.x uses the new permission system introduced in 1.21.11:
 * {@code CommandSourceStack.permissions().hasPermission(Permission)}
 */
public final class VersionCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Method newPermissionsMethod;
    private static Method newHasPermissionMethod;
    private static Object gamemastersPermission;

    static {
        try {
            Class<?> permLevelClass = Class.forName("net.minecraft.server.permissions.PermissionLevel");
            Class<?> permClass = Class.forName("net.minecraft.server.permissions.Permission");
            Class<?> hasCmdLevelClass = Class.forName("net.minecraft.server.permissions.Permission$HasCommandLevel");
            Class<?> permSetClass = Class.forName("net.minecraft.server.permissions.PermissionSet");

            Object gamemastersLevel = permLevelClass.getField("GAMEMASTERS").get(null);
            gamemastersPermission = hasCmdLevelClass
                    .getConstructor(permLevelClass)
                    .newInstance(gamemastersLevel);

            newPermissionsMethod = CommandSourceStack.class.getMethod("permissions");
            newHasPermissionMethod = permSetClass.getMethod("hasPermission", permClass);

            LOGGER.info("[VersionCompat] Permission system initialized for MC 26.1.x");
        } catch (Exception e) {
            LOGGER.error("[VersionCompat] Failed to initialize permission system", e);
        }
    }

    private VersionCompat() {}

    /**
     * Check if a CommandSourceStack has the specified permission level.
     *
     * @param source the command source
     * @param level  the permission level (0-4, matching op levels)
     * @return true if the source has permission
     */
    public static boolean checkPermission(CommandSourceStack source, int level) {
        try {
            Object permSet = newPermissionsMethod.invoke(source);
            return (boolean) newHasPermissionMethod.invoke(permSet, gamemastersPermission);
        } catch (Exception e) {
            LOGGER.error("[VersionCompat] Failed to check permission", e);
            return false;
        }
    }
}
