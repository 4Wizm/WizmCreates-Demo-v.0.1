package com.wizm.wizmcrates.listeners;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.gui.PreviewGui;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class CrateListener implements Listener {
    private final WizmCrates plugin;
    private final Map<UUID, Long> lastOpen = new HashMap<>();
    private static final int GUARANTEED_EVERY = 25;

    public CrateListener(WizmCrates plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.crates().getCrateAt(e.getBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (isKey(e.getItemInHand())) e.setCancelled(true);
    }

    private boolean isKey(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return false;
        for (Crate c : plugin.crates().getAll()) {
            if (!c.key().isVirtual() && it.getType() == c.key().material()) return true;
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Location loc = e.getClickedBlock().getLocation();
        String crateId = plugin.crates().getCrateAt(loc);
        if (crateId == null) return;
        e.setCancelled(true);
        Crate crate = plugin.crates().get(crateId);
        if (crate == null) return;
        Player p = e.getPlayer();

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (!p.isSneaking()) PreviewGui.open(p, crate, plugin);
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            long cd = plugin.cfg().cooldown();
            long now = System.currentTimeMillis();
            Long last = lastOpen.get(p.getUniqueId());
            if (last != null && cd > 0 && now - last < cd) {
                p.sendMessage(plugin.cfg().msg("cooldown-wait"));
                return;
            }
            if (p.isSneaking()) bulkOpen(p, crate, loc);
            else open(p, crate, loc);
        }
    }

    private void open(Player p, Crate crate, Location loc) {
        boolean hasKey = false;
        if (crate.key().isVirtual()) {
            if (plugin.db().isReady()) {
                int vk = plugin.db().getKeys(p.getUniqueId(), crate.id());
                if (vk > 0) { plugin.db().addKeys(p.getUniqueId(), crate.id(), -1); hasKey = true; }
                else { p.sendMessage(plugin.cfg().msg("no-key-virtual")); return; }
            } else { p.sendMessage(plugin.cfg().msg("db-not-connected")); return; }
        } else {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == crate.key().material() && hand.hasItemMeta()) {
                hand.setAmount(hand.getAmount() - 1); hasKey = true;
            } else { p.sendMessage(plugin.cfg().msg("no-key-physical")); return; }
        }
        if (!hasKey) return;
        lastOpen.put(p.getUniqueId(), System.currentTimeMillis());
        plugin.holo().updateHolograms();

        Crate.Reward reward = plugin.crates().rollReward(crate);
        if (reward == null) return;
        ItemStack rewardItem = Util.buildRewardItem(reward);

        if (plugin.cfg().openAnimEnabled()) {
            plugin.holo().playOpenAnimation(p, crate, loc, rewardItem, () -> finishOpen(p, crate, reward, rewardItem));
        } else {
            playParticlesOnce(p, crate, loc);
            finishOpen(p, crate, reward, rewardItem);
        }
    }

    private void finishOpen(Player p, Crate crate, Crate.Reward reward, ItemStack rewardItem) {
        if (!p.isOnline()) return;
        if (p.getInventory().firstEmpty() == -1) {
            if (plugin.cfg().dropIfFull()) p.getWorld().dropItemNaturally(p.getLocation(), rewardItem);
            else p.getInventory().addItem(rewardItem);
        } else p.getInventory().addItem(rewardItem);
        for (String cmd : reward.commands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
        String disp = (reward.name() == null || reward.name().isEmpty()) ? reward.material().name() : reward.name();
        p.sendMessage(plugin.cfg().msg("reward-received")
            .replace("{crate}", Util.color(crate.displayName()))
            .replace("{reward}", Util.color(disp))
            .replace("{amount}", String.valueOf(reward.amount())));

        if (plugin.db().isReady()) {
            int opened = plugin.db().incrementOpened(p.getUniqueId(), crate.id());
            if (opened > 0 && opened % GUARANTEED_EVERY == 0) {
                Crate.Reward gr = pickGuaranteed(crate);
                if (gr != null) {
                    giveReward(p, crate, gr, opened);
                }
            }
        }
        plugin.holo().updateHolograms();
    }

    private Crate.Reward pickGuaranteed(Crate crate) {
        List<Crate.Reward> list = crate.rewards();
        if (list.isEmpty()) return null;
        Crate.Reward best = null;
        double minChance = Double.MAX_VALUE;
        for (Crate.Reward r : list) {
            if (r.chance() < minChance) { minChance = r.chance(); best = r; }
        }
        return best;
    }

    private void bulkOpen(Player p, Crate crate, Location loc) {
        int available;
        if (crate.key().isVirtual()) {
            if (!plugin.db().isReady()) { p.sendMessage(plugin.cfg().msg("db-not-connected")); return; }
            available = plugin.db().getKeys(p.getUniqueId(), crate.id());
        } else {
            available = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it != null && it.getType() == crate.key().material() && it.hasItemMeta()) available += it.getAmount();
            }
        }
        if (available <= 0) {
            p.sendMessage(plugin.cfg().msg(crate.key().isVirtual() ? "no-key-virtual" : "no-key-physical"));
            return;
        }
        int freeSlots = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) if (it == null) freeSlots++;
        if (freeSlots <= 0) { p.sendMessage(plugin.cfg().msg("bulk-no-space")); return; }
        int toOpen = Math.min(available, freeSlots);
        if (toOpen <= 0) return;
        if (crate.key().isVirtual()) {
            plugin.db().addKeys(p.getUniqueId(), crate.id(), -toOpen);
        } else {
            int remaining = toOpen;
            for (int i = 0; i < p.getInventory().getSize() && remaining > 0; i++) {
                ItemStack it = p.getInventory().getItem(i);
                if (it != null && it.getType() == crate.key().material() && it.hasItemMeta()) {
                    int take = Math.min(remaining, it.getAmount());
                    it.setAmount(it.getAmount() - take);
                    if (it.getAmount() <= 0) p.getInventory().setItem(i, null);
                    remaining -= take;
                }
            }
        }
        lastOpen.put(p.getUniqueId(), System.currentTimeMillis());
        Map<String, Integer> summary = new LinkedHashMap<>();
        List<String> guaranteedSummary = new ArrayList<>();
        for (int i = 0; i < toOpen; i++) {
            Crate.Reward reward = plugin.crates().rollReward(crate);
            if (reward != null) {
                ItemStack item = Util.buildRewardItem(reward);
                Map<Integer, ItemStack> left = p.getInventory().addItem(item);
                for (ItemStack drop : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
                for (String cmd : reward.commands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
                String key = (reward.name() == null || reward.name().isEmpty()) ? reward.material().name() : Util.color(reward.name());
                summary.merge(key, reward.amount(), Integer::sum);
            }
            if (plugin.db().isReady()) {
                int opened = plugin.db().incrementOpened(p.getUniqueId(), crate.id());
                if (opened > 0 && opened % GUARANTEED_EVERY == 0) {
                    Crate.Reward gr = pickGuaranteed(crate);
                    if (gr != null) {
                        ItemStack gitem = Util.buildRewardItem(gr);
                        Map<Integer, ItemStack> gleft = p.getInventory().addItem(gitem);
                        for (ItemStack drop : gleft.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
                        for (String cmd : gr.commands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
                        String gname = (gr.name() == null || gr.name().isEmpty()) ? gr.material().name() : Util.color(gr.name());
                        guaranteedSummary.add(Util.color(plugin.cfg().msg("guaranteed-reward")
                            .replace("{crate}", Util.color(crate.displayName()))
                            .replace("{reward}", gname)
                            .replace("{openings}", String.valueOf(GUARANTEED_EVERY))));
                    }
                }
            }
        }
        playParticlesOnce(p, crate, loc);
        p.sendMessage(Util.color(plugin.cfg().prefix() + plugin.cfg().get().getString("messages.bulk-summary-header", "&aHai aperto &e{amount}x &acrate &e{crate}&a!")
            .replace("{amount}", String.valueOf(toOpen))
            .replace("{crate}", Util.color(crate.displayName()))));
        String lineTpl = plugin.cfg().get().getString("messages.bulk-summary-line", "  &8- &f{reward} &7x{amount}");
        for (Map.Entry<String, Integer> e : summary.entrySet()) {
            p.sendMessage(Util.color(lineTpl.replace("{reward}", e.getKey()).replace("{amount}", String.valueOf(e.getValue()))));
        }
        for (String g : guaranteedSummary) p.sendMessage(g);
        plugin.holo().updateHolograms();
    }

    private void giveReward(Player p, Crate crate, Crate.Reward r, int opened) {
        ItemStack item = Util.buildRewardItem(r);
        if (p.getInventory().firstEmpty() == -1) {
            if (plugin.cfg().dropIfFull()) p.getWorld().dropItemNaturally(p.getLocation(), item);
            else p.getInventory().addItem(item);
        } else p.getInventory().addItem(item);
        for (String cmd : r.commands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
        String disp = (r.name() == null || r.name().isEmpty()) ? r.material().name() : r.name();
        p.sendMessage(plugin.cfg().msg("guaranteed-reward")
            .replace("{crate}", Util.color(crate.displayName()))
            .replace("{reward}", Util.color(disp))
            .replace("{amount}", String.valueOf(r.amount()))
            .replace("{openings}", String.valueOf(opened)));
    }

    private void playParticlesOnce(Player p, Crate crate, Location loc) {
        if (loc == null || crate.animation().isNone()) return;
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(crate.animation().particle().toUpperCase());
            loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 1.0, 0.5), crate.animation().count() * 6, 0.5, 0.5, 0.5, 0.05);
        } catch (Exception ignored) {}
        if (crate.animation().sound() != null && !crate.animation().sound().isEmpty()) {
            try { p.playSound(loc, crate.animation().sound(), 1f, 1f); } catch (Exception ignored) {}
        }
    }
}