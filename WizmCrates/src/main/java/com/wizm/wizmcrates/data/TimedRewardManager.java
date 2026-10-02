package com.wizm.wizmcrates.data;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimedRewardManager {
    public record TimedReward(String id, long intervalMillis, List<String> crates, boolean virtual, int amount) {}

    private final WizmCrates plugin;
    private final Map<String, TimedReward> rewards = new LinkedHashMap<>();
    private final File file;
    private YamlConfiguration data;
    private BukkitTask task;

    public TimedRewardManager(WizmCrates plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "timers.yml");
    }

    public void load() {
        stop();
        rewards.clear();
        ConfigurationSection sec = plugin.cfg().get().getConfigurationSection("timed-rewards");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection c = sec.getConfigurationSection(id);
                if (c == null) continue;
                long millis = parseInterval(c.getString("interval", "1h"));
                List<String> crates = c.getStringList("crates");
                boolean virtual = c.getString("key-type", "virtual").equalsIgnoreCase("virtual");
                int amount = c.getInt("amount", 1);
                if (millis > 0 && !crates.isEmpty()) {
                    rewards.put(id, new TimedReward(id, millis, crates, virtual, amount));
                }
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        startTask();
    }

    public static long parseInterval(String s) {
        if (s == null || s.isEmpty()) return 0;
        long total = 0;
        Matcher m = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE).matcher(s);
        while (m.find()) {
            long n = Long.parseLong(m.group(1));
            switch (m.group(2).toLowerCase()) {
                case "s" -> total += n * 1000L;
                case "m" -> total += n * 60_000L;
                case "h" -> total += n * 3_600_000L;
                case "d" -> total += n * 86_400_000L;
            }
        }
        return total;
    }

    public long remaining(UUID u, String rewardId) {
        TimedReward tr = rewards.get(rewardId);
        if (tr == null) return 0L;
        long last = data.getLong(u + "." + rewardId, -1L);
        if (last < 0L) return tr.intervalMillis();
        long next = last + tr.intervalMillis();
        return Math.max(0L, next - System.currentTimeMillis());
    }

    public String formatRemaining(long ms) {
        if (ms <= 0) return "READY";
        long s = ms / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0 || h > 0 || d > 0) sb.append(m).append("m ");
        sb.append(s).append("s");
        return sb.toString().trim();
    }

    public void initializePlayer(Player p) {
        boolean dirty = false;
        String uuid = p.getUniqueId().toString();
        for (TimedReward tr : rewards.values()) {
            String key = uuid + "." + tr.id();
            if (!data.isSet(key)) { data.set(key, System.currentTimeMillis()); dirty = true; }
        }
        if (dirty) save();
    }

    private void save() { try { data.save(file); } catch (Exception ignored) {} }

    private void startTask() {
        task = new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (TimedReward tr : rewards.values()) {
                        long rem = remaining(p.getUniqueId(), tr.id());
                        if (rem <= 0) give(p, tr);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L);
    }

    public void stop() { if (task != null) { task.cancel(); task = null; } }

    private void give(Player p, TimedReward tr) {
        for (String crateId : tr.crates()) {
            Crate c = plugin.crates().get(crateId);
            if (c == null) continue;
            if (tr.virtual()) {
                if (plugin.db().isReady()) plugin.db().addKeys(p.getUniqueId(), c.id(), tr.amount());
            } else {
                ItemStack item = Util.buildItem(c.key().material(), c.key().name(), c.key().lore(),
                    c.key().glow(), c.key().customModelData(), tr.amount());
                if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), item);
                else p.getInventory().addItem(item);
            }
        }
        data.set(p.getUniqueId() + "." + tr.id(), System.currentTimeMillis());
        save();
        p.sendMessage(plugin.cfg().msg("timed-reward-received")
            .replace("{amount}", String.valueOf(tr.amount()))
            .replace("{crate}", String.join(", ", tr.crates())));
        plugin.holo().updateHolograms();
    }
}
