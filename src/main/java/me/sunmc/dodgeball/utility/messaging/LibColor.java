package me.sunmc.dodgeball.utility.messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Utility and helper class to colorize messages with both regular color codes
 * and hex coloring using Adventure API's MiniMessage format.
 * <p>
 * Updated for Paper 1.21.11 - Using Adventure API instead of deprecated ChatColor
 */
public class LibColor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    /**
     * Colorize a message with regular color codes using the '&' symbol,
     * and by colorizing it with hex colors using the {#HEXCODE} format.
     *
     * @param input The message to colorize.
     * @return A Component with proper formatting applied.
     */
    public static @NotNull Component colorize(@NonNull String input) {
        // Convert legacy & codes to section symbols first
        String processed = convertLegacyCodes(input);
        // Convert hex codes in {#RRGGBB} format to MiniMessage format
        processed = convertHexCodes(processed);
        // Deserialize using legacy serializer to get a Component
        return LEGACY_SECTION_SERIALIZER.deserialize(processed);
    }

    /**
     * Colorize a message with regular bukkit color codes using the '&' symbol,
     * and by colorizing it with hex colors. Returns a legacy string format.
     *
     * @param input The message to colorize.
     * @return The colorized message as a legacy string.
     * @deprecated Use {@link #colorize(String)} for Component-based messaging
     */
    @Deprecated
    public static @NotNull String colorMessage(@NonNull String input) {
        Component component = colorize(input);
        return LEGACY_SECTION_SERIALIZER.serialize(component);
    }

    /**
     * Converts legacy & color codes to section symbol (§) codes.
     *
     * @param input The message to convert.
     * @return The converted message.
     */
    @Contract(pure = true)
    private static @NotNull String convertLegacyCodes(@NonNull String input) {
        return input.replace('&', '§');
    }

    /**
     * Converts hex colors in {#RRGGBB} format to legacy section-based hex format.
     *
     * @param input The message to convert.
     * @return The converted message.
     */
    private static @NotNull String convertHexCodes(@NonNull String input) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < input.length()) {
            if (i + 8 <= input.length() && input.charAt(i) == '{' && input.charAt(i + 1) == '#') {
                // Check if it's a valid hex code
                String hexPart = input.substring(i + 2, i + 8);
                if (hexPart.matches("[A-Fa-f0-9]{6}") && i + 8 < input.length() && input.charAt(i + 8) == '}') {
                    // Convert to §x§r§r§g§g§b§b format
                    result.append("§x");
                    for (char c : hexPart.toCharArray()) {
                        result.append('§').append(Character.toLowerCase(c));
                    }
                    i += 9; // Skip the entire {#RRGGBB}
                    continue;
                }
            }
            result.append(input.charAt(i));
            i++;
        }

        return result.toString();
    }

    /**
     * De-color and remove the color from an input string.
     *
     * @param input The message to remove the color from.
     * @return The de-colored version of the message.
     */
    public static String decolor(@NonNull String input) {
        Component component = LEGACY_SECTION_SERIALIZER.deserialize(input);
        return component.toString(); // Gets plain text content
    }

    /**
     * De-color and remove formatting from a Component.
     *
     * @param component The component to strip formatting from.
     * @return Plain text content.
     */
    public static @NotNull String decolor(@NonNull Component component) {
        return LegacyComponentSerializer.builder()
                .character('§')
                .extractUrls()
                .build()
                .serialize(component)
                .replaceAll("§[0-9a-fk-or]", "");
    }
}