package com.wizm.wizmcrates.listeners;

import com.wizm.wizmcrates.WizmCrates;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

public class HoverListener implements Listener {
    private final WizmCrates plugin;
    public HoverListener(WizmCrates plugin) {
        this.plugin = plugin;
        new BukkitRunnable() { @Override public void run() { tick(); } }.runTaskTimer(plugin, 5L, 5L);
    }
    private void tick() {
        if (!plugin.cfg().hoverEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location blockLoc = null;
            try {
                RayTraceResult r = p.rayTraceBlocks(plugin.cfg().hoverRange());
                if (r != null && r.getHitBlock() != null) {
                    Location l = r.getHitBlock().getLocation();
                    if (plugin.crates().getCrateAt(l) != null) blockLoc = l;
                }
            } catch (Exception ignored) {}
            plugin.holo().setHover(p, blockLoc);
        }
    }
}
