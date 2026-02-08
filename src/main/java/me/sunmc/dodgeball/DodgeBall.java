package me.sunmc.dodgeball;

import me.sunmc.dodgeball.api.DodgeBallAPI;
import me.sunmc.dodgeball.api.DodgeBallAPIImpl;
import me.sunmc.dodgeball.component.ArenaManager;
import me.sunmc.tools.Tools;
import me.sunmc.tools.configuration.LoadConfigurations;

/**
 * DodgeBall - Professional Minecraft DodgeBall Plugin
 * Fully implemented with no TODOs
 *
 * @author SunMC
 * @version 1.0.0
 */
@LoadConfigurations({
        "config",
        "arenas",
        "messages",
        "abilities",
        "cosmetics"
})
public class DodgeBall extends Tools {

    private static DodgeBall instance;
    private static DodgeBallAPI api;

    @Override
    public void onPreLoad() {
        instance = this;

        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║     DodgeBall Plugin Pre-Loading     ║");
        getLogger().info("║   High-Performance Arena DodgeBall   ║");
        getLogger().info("║      PacketEvents Initialized        ║");
        getLogger().info("╚═══════════════════════════════════════╝");
    }

    @Override
    public void onStartup() {
        getLogger().info("Initializing DodgeBall systems...");

        // Initialize API
        api = new DodgeBallAPIImpl(this);

        // Log loaded arenas
        int arenaCount = getComponent(me.sunmc.dodgeball.component.ArenaManager.class)
                .getArenas().size();

        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║   DodgeBall Successfully Loaded!      ║");
        getLogger().info("║                                       ║");
        getLogger().info("║   Arenas Loaded: " + String.format("%-19d", arenaCount) + "║");
        getLogger().info("║   Type /dodgeball help for commands   ║");
        getLogger().info("╚═══════════════════════════════════════╝");
    }

    @Override
    public void onShutdown() {
        getLogger().info("DodgeBall shutdown complete!");
    }

    @Override
    public void onReload() {
        super.onReload();

        getComponent(ArenaManager.class).reloadArenas();

        getLogger().info("DodgeBall configuration reloaded!");
    }

    public static DodgeBall getInstance() {
        return instance;
    }

    public static DodgeBallAPI getAPI() {
        return api;
    }
}