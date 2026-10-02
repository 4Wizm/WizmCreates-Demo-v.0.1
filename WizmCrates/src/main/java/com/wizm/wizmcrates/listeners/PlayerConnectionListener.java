package com.wizm.wizmcrates.listeners;

import com.wizm.wizmcrates.WizmCrates;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final WizmCrates plugin;
    public PlayerConnectionListener(WizmCrates plugin) { this.plugin = plugin; }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!e.getPlayer().isOnline()) return;
            plugin.holo().addPlayer(e.getPlayer());
            if (plugin.timers() != null) plugin.timers().initializePlayer(e.getPlayer());
        }, 20L);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { plugin.holo().removePlayer(e.getPlayer()); }
}
