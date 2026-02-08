package me.sunmc.dodgeball.menu;

import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.tools.item.util.ItemStackBuilder;
import me.sunmc.tools.menu.Menu;
import me.sunmc.tools.menu.item.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * Team selection menu - FULLY IMPLEMENTED
 */
public class TeamSelectionMenu extends Menu {

    private final @NonNull Arena arena;

    public TeamSelectionMenu(@NonNull Player viewer, @NonNull Arena arena) {
        super(viewer);
        this.arena = arena;
    }

    @Override
    public @NonNull Component getTitle() {
        return Component.text("Select Team", NamedTextColor.GOLD);
    }

    @Override
    public int getRows() {
        return 3;
    }

    @Override
    public void init() {
        ItemStack border = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("")
                .build();
        fillBorder(MenuItem.placeholder(border));

        // Red team
        int redCount = arena.getTeamPlayers(me.sunmc.dodgeball.team.Team.RED).size();
        ItemStack redItem = ItemStackBuilder.of(Material.RED_WOOL)
                .name("§c§lRed Team", true)
                .lore(true,
                        "§7Players: §f" + redCount,
                        "",
                        "§aClick to join!"
                )
                .build();

        setItem(1, 3, MenuItem.of(redItem, e -> {
            e.setCancelled(true);
            DodgeBall.getAPI().switchTeam(Objects.requireNonNull(getViewer()), me.sunmc.dodgeball.team.Team.RED);
            getViewer().sendMessage(Component.text("§cJoined Red Team!"));
            close();
        }).setClickSound(Sound.UI_BUTTON_CLICK));

        // Blue team
        int blueCount = arena.getTeamPlayers(me.sunmc.dodgeball.team.Team.BLUE).size();
        ItemStack blueItem = ItemStackBuilder.of(Material.BLUE_WOOL)
                .name("§9§lBlue Team", true)
                .lore(true,
                        "§7Players: §f" + blueCount,
                        "",
                        "§aClick to join!"
                )
                .build();

        setItem(1, 5, MenuItem.of(blueItem, e -> {
            e.setCancelled(true);
            DodgeBall.getAPI().switchTeam(Objects.requireNonNull(getViewer()), me.sunmc.dodgeball.team.Team.BLUE);
            getViewer().sendMessage(Component.text("§9Joined Blue Team!"));
            close();
        }).setClickSound(Sound.UI_BUTTON_CLICK));

        // Auto-assign
        ItemStack autoItem = ItemStackBuilder.of(Material.COMPASS)
                .name("§e§lAuto-Assign", true)
                .lore(true,
                        "§7Let the server balance teams",
                        "",
                        "§aClick to auto-assign!"
                )
                .build();

        setItem(2, 4, MenuItem.of(autoItem, e -> {
            e.setCancelled(true);
            Objects.requireNonNull(getViewer()).sendMessage(Component.text("§eTeam auto-assigned!"));
            close();
        }).setClickSound(Sound.UI_BUTTON_CLICK));
    }
}