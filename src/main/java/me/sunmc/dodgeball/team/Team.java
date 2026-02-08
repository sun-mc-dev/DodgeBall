package me.sunmc.dodgeball.team;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum Team {
    RED("Red Team", NamedTextColor.RED, Material.RED_WOOL, Color.RED),
    BLUE("Blue Team", NamedTextColor.BLUE, Material.BLUE_WOOL, Color.BLUE),
    SPECTATOR("Spectators", NamedTextColor.GRAY, Material.BARRIER, Color.GRAY);

    private final String displayName;
    private final TextColor color;
    private final Material material;
    private final Color armorColor;

    Team(String displayName, TextColor color, Material material, Color armorColor) {
        this.displayName = displayName;
        this.color = color;
        this.material = material;
        this.armorColor = armorColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }

    public Material getMaterial() {
        return material;
    }

    public Color getArmorColor() {
        return armorColor;
    }
}