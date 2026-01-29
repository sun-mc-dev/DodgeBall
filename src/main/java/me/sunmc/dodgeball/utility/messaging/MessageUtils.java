package me.sunmc.dodgeball.utility.messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for message formatting with Adventure API support.
 * Converts legacy color codes to MiniMessage format.
 */
public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern HEX_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}");

    /**
     * Converts legacy color codes to MiniMessage format.
     *
     * @param legacy Legacy formatted string with & codes
     * @return MiniMessage formatted string
     */
    public static @NotNull String legacyToMiniMessage(@NotNull String legacy) {
        // Convert hex colors {#RRGGBB} to <color:#RRGGBB>
        Matcher hexMatcher = HEX_PATTERN.matcher(legacy);
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            legacy = legacy.replace("{#" + hex + "}", "<color:#" + hex + ">");
        }

        // Convert legacy & codes to § for legacy serializer
        legacy = legacy.replace("&", "§");

        // Parse with legacy serializer and convert to MiniMessage
        Component component = LEGACY_SERIALIZER.deserialize(legacy);

        // For now, return the plain text version
        // In the future, we could serialize to MiniMessage format
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Parses a MiniMessage string to a Component.
     *
     * @param miniMessage MiniMessage formatted string
     * @return Parsed Component
     */
    public static @NotNull Component parse(@NotNull String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /**
     * Colorizes a legacy message and returns a Component.
     *
     * @param legacy Legacy formatted string
     * @return Colored Component
     */
    public static @NotNull Component colorMessage(@NotNull String legacy) {
        String miniMessage = legacyToMiniMessage(legacy);
        return parse(miniMessage);
    }

    /**
     * Strips all color codes from a string.
     *
     * @param input String with color codes
     * @return Plain string without colors
     */
    public static @NotNull String stripColor(@NotNull String input) {
        return input.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("&[0-9a-fk-or]", "")
                .replaceAll("<[^>]+>", "");
    }
}