package com.wizm.wizmcrates.config;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final WizmCrates plugin;
    private FileConfiguration cfg;

    public ConfigManager(WizmCrates plugin) { this.plugin = plugin; reload(); }
    public void reload() { plugin.reloadConfig(); cfg = plugin.getConfig(); }
    public FileConfiguration get() { return cfg; }

    public String prefix() { return cfg.getString("settings.language-prefix", "&b[WizmCrates] &r"); }
    public long cooldown() { return cfg.getLong("settings.open-cooldown-ms", 1000L); }
    public boolean dropIfFull() { return cfg.getBoolean("settings.drop-key-if-full", true); }
    public String holoBg() { return cfg.getString("settings.hologram.background-color", "#00000080"); }
    public boolean seeThroughWalls() { return cfg.getBoolean("settings.hologram.see-through-walls", true); }
    public int holoUpdateInterval() { return cfg.getInt("settings.hologram.update-interval", 20); }
    public boolean fillBorder() { return cfg.getBoolean("settings.gui.fill-border", true); }
    public Material borderMaterial() {
        Material m = Material.matchMaterial(cfg.getString("settings.gui.border-material", "AIR"));
        return m != null ? m : Material.AIR;
    }
    public boolean hoverEnabled() { return cfg.getBoolean("settings.hover.enabled", true); }
    public double hoverScale() { return cfg.getDouble("settings.hover.scale-multiplier", 1.35); }
    public double hoverRange() { return cfg.getDouble("settings.hover.range", 5.0); }

    public String openAnimMode() {
        String mode = cfg.getString("settings.open-animation.mode", null);
        if (mode != null) return mode.toLowerCase();
        boolean legacy = cfg.getBoolean("settings.open-animation.enabled", true);
        return legacy ? "default" : "none";
    }
    public boolean openAnimEnabled() { return openAnimMode().equals("default"); }
    public int openAnimShrinkTicks() { return cfg.getInt("settings.open-animation.shrink-ticks", 12); }
    public int openAnimShowTicks() { return cfg.getInt("settings.open-animation.show-ticks", 40); }

    public String msg(String key) { return Util.color(prefix() + cfg.getString("messages." + key, "")); }

    public Crate.Animation readAnimation(String name) {
        if (name == null || name.isEmpty()) name = "none";
        String base = "animations." + name + ".";
        return new Crate.Animation(name,
            cfg.getString(base + "particle", "none"),
            cfg.getInt(base + "count", 0),
            cfg.getInt(base + "interval", 4),
            cfg.getString(base + "sound", ""),
            cfg.getInt(base + "sound-interval", 0));
    }

    public boolean mysqlEnabled() { return cfg.getBoolean("mysql.enabled", false); }
    public String mysqlHost() { return cfg.getString("mysql.host", "127.0.0.1"); }
    public int mysqlPort() { return cfg.getInt("mysql.port", 3306); }
    public String mysqlDb() { return cfg.getString("mysql.database", "wizmcrates"); }
    public String mysqlUser() { return cfg.getString("mysql.username", "root"); }
    public String mysqlPass() { return cfg.getString("mysql.password", ""); }
    public String tablePrefix() { return cfg.getString("mysql.table-prefix", "wizm_"); }
    public boolean mysqlSsl() { return cfg.getBoolean("mysql.useSSL", false); }
}
