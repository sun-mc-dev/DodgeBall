package me.sunmc.dodgeball.menu;

import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.dodgeball.arena.Arena;
import me.sunmc.dodgeball.arena.ArenaState;
import me.sunmc.dodgeball.component.ArenaManager;
import me.sunmc.tools.item.util.ItemStackBuilder;
import me.sunmc.tools.menu.PaginatedMenu;
import me.sunmc.tools.menu.item.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena list menu - FULLY IMPLEMENTED
 */
public class ArenaListMenu extends PaginatedMenu {

    private final @NonNull DodgeBall plugin;

    public ArenaListMenu(@NonNull Player viewer) {
        super(viewer);
        this.plugin = DodgeBall.getInstance();
        setShowPageInfo(true);
        setNavigationPosition(NavigationPosition.BOTTOM);
    }

    @Override
    public @NonNull Component getTitle() {
        return Component.text("DodgeBall Arenas", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true);
    }

    @Override
    public int getRows() {
        return 6;
    }

    @Override
    public void init() {
        ItemStack border = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("")
                .build();
        fillBorder(MenuItem.placeholder(border));

        ArenaManager manager = plugin.getComponent(ArenaManager.class);
        for (Arena arena : manager.getArenas()) {
            addContentItem(createArenaItem(arena));
        }

        if (getTotalItems() == 0) {
            setEmptyStateItem(createEmptyStateItem());
        }

        super.init();
    }

    private @NonNull MenuItem createArenaItem(@NonNull Arena arena) {
        Material material = switch (arena.getState()) {
            case WAITING -> Material.LIME_WOOL;
            case STARTING -> Material.YELLOW_WOOL;
            case IN_GAME -> Material.RED_WOOL;
            case ENDING -> Material.ORANGE_WOOL;
            case RESETTING -> Material.LIGHT_BLUE_WOOL;
            case DISABLED -> Material.GRAY_WOOL;
        };

        int playerCount = arena.getPlayers().size();
        int maxPlayers = arena.getMaxPlayers();

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + arena.getArenaId());
        lore.add("§7Mode: §f" + arena.getGameMode().getDisplayName());
        lore.add("");
        lore.add("§7Players: §f" + playerCount + "§7/§f" + maxPlayers);
        lore.add("§7Status: " + getStateColor(arena.getState()) + arena.getState());
        lore.add("");

        if (!arena.isSetup()) {
            lore.add("§c§lNOT SET UP");
        } else if (arena.isFull()) {
            lore.add("§c§lFULL");
        } else if (arena.getState() == ArenaState.IN_GAME) {
            lore.add("§e§lIN PROGRESS");
        } else {
            lore.add("§a§lClick to join!");
        }

        ItemStack item = ItemStackBuilder.of(material)
                .name("§6§l" + arena.getDisplayName(), true)
                .lore(true, lore.toArray(new String[0]))
                .build();

        return MenuItem.of(item, event -> {
            event.setCancelled(true);
            Player player = getViewer();

            if (!arena.isSetup()) {
                player.sendMessage(Component.text("§cThis arena is not set up!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (arena.isFull()) {
                player.sendMessage(Component.text("§cArena is full!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
                player.sendMessage(Component.text("§cGame already in progress!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            ArenaManager manager = plugin.getComponent(ArenaManager.class);
            if (manager.addPlayer(player, arena)) {
                close();
                player.sendMessage(Component.text("§aJoined arena: §e" + arena.getDisplayName(),
                        NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(Component.text("§cFailed to join arena!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }).setClickSound(Sound.UI_BUTTON_CLICK);
    }

    private @NonNull MenuItem createEmptyStateItem() {
        ItemStack item = ItemStackBuilder.of(Material.BARRIER)
                .name("§c§lNo Arenas Available", true)
                .lore(true,
                        "§7There are currently no arenas",
                        "§7configured on this server.",
                        "",
                        "§7Contact an administrator!"
                )
                .build();

        return MenuItem.placeholder(item);
    }

    private @NonNull String getStateColor(@NonNull ArenaState state) {
        return switch (state) {
            case WAITING -> "§a";
            case STARTING -> "§e";
            case IN_GAME -> "§c";
            case ENDING -> "§6";
            case RESETTING -> "§b";
            case DISABLED -> "§7";
        };
    }
}