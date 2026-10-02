package com.wizm.wizmcrates.gui;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class EditGui implements InventoryHolder {
    private final WizmCrates plugin;
    private final Crate crate;
    private final Inventory inventory;
    private final Map<Integer, String> slotToRewardId = new HashMap<>();

    public EditGui(WizmCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54, Util.color("&8Edit: &e" + crate.id()));
        refresh();
    }

    public Crate crate() { return crate; }

    public void refresh() {
        inventory.clear();
        slotToRewardId.clear();
        List<Crate.Reward> extras = plugin.extras().getExtras(crate.id());
        int slot = 0;
        for (Crate.Reward r : extras) {
            if (slot >= 45) break;
            ItemStack item = Util.buildRewardItem(r);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(Util.color("&7Chance: &e" + r.chance() + "%"));
                lore.add(Util.color("&cClick per rimuovere"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slotToRewardId.put(slot, r.id());
            slot++;
        }
        inventory.setItem(49, Util.buildItem(Material.PAPER, "&a&lCome aggiungere", List.of(
            Util.color("&7Prendi un item dal tuo inventario"),
            Util.color("&7e fai &eShift + Click &7sull'item"),
            Util.color("&7per aggiungerlo alla crate."),
            "",
            Util.color("&7Click su un item qui sopra"),
            Util.color("&7per rimuoverlo.")
        ), false, 0, 1));
    }

    public String rewardIdAt(int slot) { return slotToRewardId.get(slot); }

    public static void open(Player p, Crate crate, WizmCrates plugin) {
        p.openInventory(new EditGui(plugin, crate).getInventory());
    }

    @Override public Inventory getInventory() { return inventory; }
}