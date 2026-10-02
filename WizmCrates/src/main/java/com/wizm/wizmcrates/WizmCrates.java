package com.wizm.wizmcrates;

import com.wizm.wizmcrates.commands.CrateCommand;
import com.wizm.wizmcrates.config.ConfigManager;
import com.wizm.wizmcrates.crate.CrateManager;
import com.wizm.wizmcrates.crate.ExtraRewardStorage;
import com.wizm.wizmcrates.data.DatabaseManager;
import com.wizm.wizmcrates.data.TimedRewardManager;
import com.wizm.wizmcrates.hooks.PlaceholderHook;
import com.wizm.wizmcrates.hologram.HologramManager;
import com.wizm.wizmcrates.listeners.CrateListener;
import com.wizm.wizmcrates.listeners.GuiListener;
import com.wizm.wizmcrates.listeners.HoverListener;
import com.wizm.wizmcrates.listeners.PlayerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class WizmCrates extends JavaPlugin {
    private static WizmCrates instance;
    private ConfigManager configManager;
    private CrateManager crateManager;
    private DatabaseManager databaseManager;
    private HologramManager hologramManager;
    private ExtraRewardStorage extraRewards;
    private TimedRewardManager timedRewards;

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        extraRewards = new ExtraRewardStorage(this);
        crateManager = new CrateManager(this);
        crateManager.load();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new PlaceholderHook(this).register();
        hologramManager = new HologramManager(this);
        getServer().getScheduler().runTaskLater(this, () -> hologramManager.spawnAll(), 10L);
        timedRewards = new TimedRewardManager(this);
        timedRewards.load();
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new HoverListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        CrateCommand cmd = new CrateCommand(this);
        getCommand("crate").setExecutor(cmd);
        getCommand("crate").setTabCompleter(cmd);
        getLogger().info("WizmCrates abilitato.");
    }

    @Override public void onDisable() {
        if (timedRewards != null) timedRewards.stop();
        if (hologramManager != null) hologramManager.despawnAll();
        if (databaseManager != null) databaseManager.disconnect();
    }

    public static WizmCrates get() { return instance; }
    public ConfigManager cfg() { return configManager; }
    public CrateManager crates() { return crateManager; }
    public DatabaseManager db() { return databaseManager; }
    public HologramManager holo() { return hologramManager; }
    public ExtraRewardStorage extras() { return extraRewards; }
    public TimedRewardManager timers() { return timedRewards; }
}