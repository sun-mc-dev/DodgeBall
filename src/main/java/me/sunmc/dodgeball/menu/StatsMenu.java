package me.sunmc.dodgeball.menu;

import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.component.PlayerManager;
import me.sunmc.dodgeball.player.DodgeBallPlayer;
import me.sunmc.dodgeball.stats.PlayerStats;
import me.sunmc.tools.item.util.ItemStackBuilder;
import me.sunmc.tools.menu.Menu;
import me.sunmc.tools.menu.item.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * Stats menu - FULLY IMPLEMENTED
 */
public class StatsMenu extends Menu {

    private final @NonNull DodgeBall plugin;
    private final @NonNull Player target;

    public StatsMenu(@NonNull Player viewer, @NonNull Player target) {
        super(viewer);
        this.plugin = DodgeBall.getInstance();
        this.target = target;
    }

    @Override
    public @NonNull Component getTitle() {
        return Component.text("Stats: " + target.getName(), NamedTextColor.AQUA);
    }

    @Override
    public int getRows() {
        return 4;
    }

    @Override
    public void init() {
        ItemStack border = ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("")
                .build();
        fillBorder(MenuItem.placeholder(border));

        PlayerManager playerManager = plugin.getComponent(PlayerManager.class);
        DodgeBallPlayer dbPlayer = playerManager.getPlayer(target);
        PlayerStats stats = dbPlayer.getStats();

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(meta -> {
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(target);
            }
            meta.displayName(Component.text(target.getName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("§7Total Stats", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        });
        setItem(1, 4, MenuItem.placeholder(head));

        // Statistics
        setItem(2, 1, createStatItem(Material.DIAMOND_SWORD, "§6Kills", stats.getKills()));
        setItem(2, 2, createStatItem(Material.SKELETON_SKULL, "§cDeaths", stats.getDeaths()));
        setItem(2, 3, createStatItem(Material.FISHING_ROD, "§eCatches", stats.getCatches()));
        setItem(2, 4, createStatItem(Material.EXPERIENCE_BOTTLE, "§aWins", stats.getWins()));
        setItem(2, 5, createStatItem(Material.REDSTONE, "§7Losses", stats.getLosses()));
        setItem(2, 6, createStatItem(Material.SNOWBALL, "§bThrows", stats.getShoots()));
        setItem(2, 7, createStatItem(Material.CLOCK, "§dGames", stats.getGamesPlayed()));

        // K/D Ratio
        double kd = stats.getKDRatio();
        setItem(2, 8, createStatItem(Material.GOLDEN_SWORD, "§6K/D Ratio",
                String.format("%.2f", kd)));

        // Win Rate
        setItem(1, 1, createStatItem(Material.EMERALD, "§aWin Rate",
                String.format("%.1f%%", stats.getWinRate())));

        // Playtime
        long playtimeSeconds = stats.getPlaytime() / 1000;
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;
        setItem(1, 7, createStatItem(Material.CLOCK, "§ePlaytime",
                hours + "h " + minutes + "m"));

        // Close button
        ItemStack closeItem = ItemStackBuilder.of(Material.BARRIER)
                .name("§cClose", true)
                .build();
        setItem(3, 4, MenuItem.of(closeItem, e -> {
            e.setCancelled(true);
            close();
        }).setClickSound(Sound.UI_BUTTON_CLICK));

        // Refresh button
        ItemStack refreshItem = ItemStackBuilder.of(Material.LIME_DYE)
                .name("§aRefresh", true)
                .lore(true, "§7Click to refresh stats")
                .build();
        setItem(3, 5, MenuItem.of(refreshItem, e -> {
            e.setCancelled(true);
            refresh();
        }).setClickSound(Sound.UI_BUTTON_CLICK));
    }

    private @NonNull MenuItem createStatItem(@NonNull Material material,
                                             @NonNull String name, int value) {
        return createStatItem(material, name, String.valueOf(value));
    }

    private @NonNull MenuItem createStatItem(@NonNull Material material,
                                             @NonNull String name,
                                             @NonNull String value) {
        ItemStack item = ItemStackBuilder.of(material)
                .name(name, true)
                .lore(true,
                        "§7Value: §f" + value
                )
                .build();

        return MenuItem.placeholder(item);
    }
}