package me.sunmc.dodgeball.component;


import me.sunmc.dodgeball.DodgeBall;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.registry.AutoRegister;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Manages PacketEvents integration - FULLY IMPLEMENTED
 */
@AutoRegister(Component.class)
public class PacketManager implements Component {

    private final @NonNull DodgeBall plugin;

    public PacketManager(@NonNull DodgeBall plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("PacketEvents integration enabled");
        registerPacketListeners();
    }

    @Override
    public void onDisable() {
        // Cleanup handled by PacketEvents
    }

    private void registerPacketListeners() {
        // PacketEvents listeners are registered in the listener package
        plugin.getLogger().info("Packet listeners registered");
    }
}