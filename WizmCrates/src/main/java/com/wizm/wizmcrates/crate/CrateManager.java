package com.wizm.wizmcrates.crate;

import com.wizm.wizmcrates.WizmCrates;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

public class CrateManager {
    private final WizmCrates plugin;
    private final Map<String, Crate> crates = new HashMap<>();
    private final Map<String, String> blockLocations = new HashMap<>();

    public CrateManager(WizmCrates plugin) { this.plugin = plugin; }

    public void load() {
        crates.clear();
        FileConfiguration cfg = plugin.cfg().get();
        ConfigurationSection sec = cfg.getConfigurationSection("crates");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(id);
            if (c == null) continue;
            Material blockMat = Material.matchMaterial(c.getString("block", "CHEST"));
            if (blockMat == null) blockMat = Material.CHEST;
            boolean holoEnabled = c.getBoolean("hologram.enabled", true);
            double holoY = c.getDouble("hologram.offset-y", 1.2);
            List<String> holoLines = c.getStringList("hologram.lines");
            Crate.Animation anim = plugin.cfg().readAnimation(c.getString("animation", "none"));
            Material keyMat = Material.matchMaterial(c.getString("key.material", "TRIPWIRE_HOOK"));
            if (keyMat == null) keyMat = Material.TRIPWIRE_HOOK;
            Crate.KeyItem key = new Crate.KeyItem(
                c.getString("key.type", "physical"), keyMat,
                c.getString("key.name", "&6Key"),
                c.getStringList("key.lore"),
                c.getBoolean("key.glow", true),
                c.getInt("key.custom-model-data", 0));
            List<Crate.Reward> rewards = new ArrayList<>();
            for (Map<?, ?> rawMap : c.getMapList("rewards")) {
                @SuppressWarnings("unchecked") Map<String, Object> r = (Map<String, Object>) rawMap;
                double chance = r.get("chance") instanceof Number n ? n.doubleValue() : 0.0;
                Material mat = Material.matchMaterial(String.valueOf(r.getOrDefault("material", "STONE")));
                if (mat == null) mat = Material.STONE;
                int amount = r.get("amount") instanceof Number n ? n.intValue() : 1;
                @SuppressWarnings("unchecked") List<String> lore = r.get("lore") instanceof List<?> l ? (List<String>) l : new ArrayList<>();
                @SuppressWarnings("unchecked") List<String> commands = r.get("commands") instanceof List<?> l ? (List<String>) l : new ArrayList<>();
                rewards.add(new Crate.Reward(String.valueOf(r.get("id")), chance, mat, amount, String.valueOf(r.getOrDefault("name", "")), lore, commands));
            }
            rewards.addAll(plugin.extras().getExtras(id));
            List<Crate.GuaranteedReward> gua = new ArrayList<>();
            for (Map<?, ?> rawMap : c.getMapList("guaranteed-rewards")) {
                @SuppressWarnings("unchecked") Map<String, Object> g = (Map<String, Object>) rawMap;
                gua.add(new Crate.GuaranteedReward(g.get("openings") instanceof Number n ? n.intValue() : 0, String.valueOf(g.getOrDefault("reward-id", ""))));
            }
            crates.put(id.toLowerCase(), new Crate(id, c.getString("display-name", id),
                c.getString("gui-title", c.getString("display-name", id)),
                blockMat, holoEnabled, holoY, holoLines, anim, key, rewards, gua));
        }
        loadBlockLocations();
    }

    private void loadBlockLocations() {
        blockLocations.clear();
        File f = new File(plugin.getDataFolder(), "blocks.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String key : y.getKeys(false)) {
            String crateId = y.getString(key);
            if (crateId != null) blockLocations.put(key, crateId);
        }
    }

    public void saveBlockLocations() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, String> e : blockLocations.entrySet()) y.set(e.getKey(), e.getValue());
        try { y.save(new File(plugin.getDataFolder(), "blocks.yml")); } catch (Exception ignored) {}
    }

    public Crate get(String id) { return id == null ? null : crates.get(id.toLowerCase()); }
    public Collection<Crate> getAll() { return crates.values(); }
    public Map<String, String> getBlockLocations() { return blockLocations; }
    public void setBlock(Location loc, String crateId) { blockLocations.put(locKey(loc), crateId); saveBlockLocations(); }
    public boolean removeBlock(Location loc) { if (blockLocations.remove(locKey(loc)) != null) { saveBlockLocations(); return true; } return false; }
    public String getCrateAt(Location loc) { return blockLocations.get(locKey(loc)); }
    private String locKey(Location l) { return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ(); }

    public Crate.Reward rollReward(Crate crate) {
        double total = 0;
        for (Crate.Reward r : crate.rewards()) total += r.chance();
        if (total <= 0) return null;
        double rnd = Math.random() * total, cum = 0;
        for (Crate.Reward r : crate.rewards()) { cum += r.chance(); if (rnd <= cum) return r; }
        return crate.rewards().get(crate.rewards().size() - 1);
    }
}
