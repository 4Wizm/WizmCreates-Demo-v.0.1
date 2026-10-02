package com.wizm.wizmcrates.hooks;

import com.wizm.wizmcrates.WizmCrates;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.UUID;

public class PlaceholderHook extends PlaceholderExpansion {
    private final WizmCrates plugin;
    public PlaceholderHook(WizmCrates plugin) { this.plugin = plugin; }
    @Override public @NotNull String getIdentifier() { return "wizmcrates"; }
    @Override public @NotNull String getAuthor() { return "Wizm"; }
    @Override public @NotNull String getVersion() { return "beta-0.0.1"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String p = params.toLowerCase();
        if (p.startsWith("keys_")) {
            if (player == null) return "0";
            return String.valueOf(plugin.db().getKeys(player.getUniqueId(), p.substring(5)));
        }
        if (p.startsWith("opened_")) {
            if (player == null) return "0";
            return String.valueOf(plugin.db().getOpened(player.getUniqueId(), p.substring(7)));
        }
        if (p.startsWith("timer_")) {
            if (player == null) return "READY";
            long rem = plugin.timers().remaining(player.getUniqueId(), p.substring(6));
            return plugin.timers().formatRemaining(rem);
        }
        if (p.startsWith("leaderboard_")) {
            String[] parts = p.split("_");
            if (parts.length >= 4) {
                int page, row;
                try { page = Integer.parseInt(parts[2]); row = Integer.parseInt(parts[3]); } catch (Exception e) { return ""; }
                int index = (page - 1) * 10 + (row - 1);
                Map<UUID, Integer> top = plugin.db().topOpened(parts[1], index + 1);
                int i = 0;
                for (Map.Entry<UUID, Integer> e : top.entrySet()) {
                    if (i == index) {
                        String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                        return (name == null ? "???" : name) + " - " + e.getValue();
                    }
                    i++;
                }
            }
        }
        return null;
    }
}