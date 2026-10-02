package com.wizm.wizmcrates.hologram;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

public class HologramManager {
    private final WizmCrates plugin;
    private final Map<String, Map<UUID, TextDisplay>> perPlayerHolos = new HashMap<>();
    private final Map<String, ItemDisplay> hoverDisplays = new HashMap<>();
    private final Map<UUID, String> playerHover = new HashMap<>();
    private final Map<UUID, BukkitTask> hoverTasks = new HashMap<>();
    private BukkitTask updateTask;

    public HologramManager(WizmCrates plugin) { this.plugin = plugin; }

    public void spawnAll() {
        despawnAll();
        for (Map.Entry<String, String> entry : plugin.crates().getBlockLocations().entrySet()) {
            String locKey = entry.getKey();
            Location base = keyToLoc(locKey);
            if (base == null) continue;
            Crate c = plugin.crates().get(entry.getValue());
            if (c == null) continue;
            ItemDisplay d = base.getWorld().spawn(base.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, e -> {
                e.setItemStack(new ItemStack(c.block()));
                e.setVisibleByDefault(false);
                e.setBillboard(Display.Billboard.FIXED);
                float s = (float) plugin.cfg().hoverScale();
                e.setTransformation(new Transformation(new Vector3f(0,0,0), new Quaternionf(), new Vector3f(s,s,s), new Quaternionf()));
                e.setPersistent(false);
            });
            hoverDisplays.put(locKey, d);
            if (c.holoEnabled()) {
                Map<UUID, TextDisplay> map = new HashMap<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    TextDisplay td = createHoloFor(base, c, p);
                    if (td != null) map.put(p.getUniqueId(), td);
                }
                perPlayerHolos.put(locKey, map);
            }
        }
        startUpdateTask();
    }

    private TextDisplay createHoloFor(Location base, Crate c, Player owner) {
        Location l = base.clone().add(0.5, c.holoOffsetY(), 0.5);
        try {
            TextDisplay td = base.getWorld().spawn(l, TextDisplay.class, e -> {
                e.setText(buildHoloText(c, owner));
                e.setBillboard(Display.Billboard.CENTER);
                e.setSeeThrough(plugin.cfg().seeThroughWalls());
                e.setShadowed(true);
                e.setBackgroundColor(parseColor(plugin.cfg().holoBg()));
                e.setPersistent(false);
                e.setVisibleByDefault(false);
            });
            owner.showEntity(plugin, td);
            return td;
        } catch (Exception ex) { return null; }
    }

    private String buildHoloText(Crate c, Player p) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = c.holoLines();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(Util.parsePAPI(p, lines.get(i)));
            if (i < lines.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    public void addPlayer(Player p) {
        for (Map.Entry<String, Map<UUID, TextDisplay>> entry : perPlayerHolos.entrySet()) {
            Location base = keyToLoc(entry.getKey());
            if (base == null) continue;
            Crate c = plugin.crates().get(plugin.crates().getBlockLocations().get(entry.getKey()));
            if (c == null || !c.holoEnabled()) continue;
            TextDisplay td = createHoloFor(base, c, p);
            if (td != null) entry.getValue().put(p.getUniqueId(), td);
        }
    }

    public void removePlayer(Player p) {
        for (Map<UUID, TextDisplay> map : perPlayerHolos.values()) {
            TextDisplay td = map.remove(p.getUniqueId());
            if (td != null && !td.isDead()) td.remove();
        }
        BukkitTask t = hoverTasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
        playerHover.remove(p.getUniqueId());
    }

    private void startUpdateTask() {
        if (updateTask != null) updateTask.cancel();
        int interval = plugin.cfg().holoUpdateInterval();
        if (interval <= 0) interval = 20;
        updateTask = new BukkitRunnable() { @Override public void run() { updateHolograms(); } }
            .runTaskTimer(plugin, interval, interval);
    }

    public void updateHolograms() {
        for (Map.Entry<String, Map<UUID, TextDisplay>> entry : perPlayerHolos.entrySet()) {
            Location base = keyToLoc(entry.getKey());
            if (base == null) continue;
            Crate c = plugin.crates().get(plugin.crates().getBlockLocations().get(entry.getKey()));
            if (c == null) continue;
            for (Map.Entry<UUID, TextDisplay> e2 : entry.getValue().entrySet()) {
                TextDisplay td = e2.getValue();
                if (td == null || td.isDead()) continue;
                Player p = Bukkit.getPlayer(e2.getKey());
                if (p == null) continue;
                String txt = buildHoloText(c, p);
                try { if (!txt.equals(td.getText())) td.setText(txt); } catch (Exception ignored) {}
            }
        }
    }

    private Location keyToLoc(String key) {
        String[] p = key.split(";");
        if (p.length < 4) return null;
        try {
            World w = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
        } catch (Exception e) { return null; }
    }

    private String locToKey(Location l) { return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ(); }

    private Color parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int a = 128, r, g, b;
            if (h.length() == 8) {
                a = Integer.parseInt(h.substring(0, 2), 16);
                r = Integer.parseInt(h.substring(2, 4), 16);
                g = Integer.parseInt(h.substring(4, 6), 16);
                b = Integer.parseInt(h.substring(6, 8), 16);
            } else {
                r = Integer.parseInt(h.substring(0, 2), 16);
                g = Integer.parseInt(h.substring(2, 4), 16);
                b = Integer.parseInt(h.substring(4, 6), 16);
            }
            return Color.fromARGB(a, r, g, b);
        } catch (Exception e) { return Color.fromARGB(128, 0, 0, 0); }
    }

    public void despawnAll() {
        if (updateTask != null) updateTask.cancel();
        for (Map<UUID, TextDisplay> map : perPlayerHolos.values())
            for (TextDisplay td : map.values())
                if (td != null && !td.isDead()) td.remove();
        for (ItemDisplay d : hoverDisplays.values())
            if (d != null && !d.isDead()) d.remove();
        for (BukkitTask t : hoverTasks.values()) t.cancel();
        perPlayerHolos.clear(); hoverDisplays.clear(); playerHover.clear(); hoverTasks.clear();
    }

    public void setHover(Player p, Location blockLoc) {
        UUID u = p.getUniqueId();
        String prev = playerHover.get(u);
        String newKey = blockLoc == null ? null : locToKey(blockLoc);
        if (Objects.equals(prev, newKey)) return;
        if (prev != null) {
            ItemDisplay d = hoverDisplays.get(prev);
            if (d != null) p.hideEntity(plugin, d);
            BukkitTask t = hoverTasks.remove(u);
            if (t != null) t.cancel();
        }
        if (newKey == null) { playerHover.remove(u); return; }
        playerHover.put(u, newKey);
        ItemDisplay d = hoverDisplays.get(newKey);
        if (d != null) p.showEntity(plugin, d);
        Crate c = plugin.crates().get(plugin.crates().getBlockLocations().get(newKey));
        if (c != null) startHoverAnimation(p, c, blockLoc);
    }

    private void startHoverAnimation(Player p, Crate c, Location loc) {
        Crate.Animation anim = c.animation();
        if (anim == null || anim.isNone()) return;
        final Particle particle;
        try { particle = Particle.valueOf(anim.particle().toUpperCase()); } catch (Exception e) { return; }
        final int interval = anim.interval() > 0 ? anim.interval() : 4;
        final int count = anim.count();
        final String sound = anim.sound();
        final int soundInt = anim.soundInterval();
        final UUID uuid = p.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); hoverTasks.remove(uuid); return; }
                if (!p.getWorld().equals(loc.getWorld()) || p.getLocation().distanceSquared(loc) > 900) { cancel(); hoverTasks.remove(uuid); return; }
                try { p.spawnParticle(particle, loc.clone().add(0.5, 1.0, 0.5), count, 0.35, 0.35, 0.35, 0.02); } catch (Exception ignored) {}
                tick++;
                if (soundInt > 0 && sound != null && !sound.isEmpty() && tick % soundInt == 0) {
                    try { p.playSound(loc, sound, 0.6f, 1f); } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
        hoverTasks.put(uuid, task);
    }

    public void playOpenAnimation(Player p, Crate crate, Location loc, ItemStack reward, Runnable onComplete) {
        String locKey = locToKey(loc);
        ItemDisplay display = hoverDisplays.get(locKey);
        float startScale = (float) plugin.cfg().hoverScale();
        if (display == null || display.isDead() || !plugin.cfg().openAnimEnabled()) { onComplete.run(); return; }
        float endScale = 0.3f;
        int shrinkTicks = Math.max(2, plugin.cfg().openAnimShrinkTicks());
        int showTicks = Math.max(5, plugin.cfg().openAnimShowTicks());

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { resetDisplay(display, startScale); onComplete.run(); cancel(); return; }
                if (t >= shrinkTicks) {
                    try {
                        if (!crate.animation().isNone()) {
                            Particle particle = Particle.valueOf(crate.animation().particle().toUpperCase());
                            p.spawnParticle(particle, loc.clone().add(0.5, 1.0, 0.5), 60, 0.5, 0.5, 0.5, 0.15);
                        }
                    } catch (Exception ignored) {}
                    if (crate.animation().sound() != null && !crate.animation().sound().isEmpty()) {
                        try { p.playSound(loc, crate.animation().sound(), 1f, 1f); } catch (Exception ignored) {}
                    }
                    ItemDisplay rewardDisplay = loc.getWorld().spawn(loc.clone().add(0.5, 1.6, 0.5), ItemDisplay.class, e -> {
                        e.setItemStack(reward);
                        e.setBillboard(Display.Billboard.CENTER);
                        e.setVisibleByDefault(false);
                        e.setPersistent(false);
                        e.setTransformation(new Transformation(new Vector3f(0,0,0), new Quaternionf(), new Vector3f(0.1f,0.1f,0.1f), new Quaternionf()));
                    });
                    p.showEntity(plugin, rewardDisplay);
                    new BukkitRunnable() {
                        int t2 = 0;
                        @Override public void run() {
                            if (t2 >= showTicks || !p.isOnline() || rewardDisplay.isDead()) {
                                if (!rewardDisplay.isDead()) rewardDisplay.remove();
                                resetDisplay(display, startScale);
                                onComplete.run();
                                cancel(); return;
                            }
                            float progress = t2 / (float) showTicks;
                            float scale = 0.1f + progress * 1.0f;
                            Quaternionf rot = new Quaternionf().rotateY((float) (t2 * 0.18));
                            try {
                                rewardDisplay.setTransformation(new Transformation(new Vector3f(0,0,0), rot, new Vector3f(scale, scale, scale), new Quaternionf()));
                            } catch (Exception ignored) {}
                            t2++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                    cancel(); return;
                }
                float progress = t / (float) shrinkTicks;
                float scale = startScale - (startScale - endScale) * progress;
                try {
                    display.setTransformation(new Transformation(new Vector3f(0,0,0), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
                } catch (Exception ignored) {}
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void resetDisplay(ItemDisplay display, float startScale) {
        try {
            display.setTransformation(new Transformation(new Vector3f(0,0,0), new Quaternionf(), new Vector3f(startScale, startScale, startScale), new Quaternionf()));
        } catch (Exception ignored) {}
    }
}
