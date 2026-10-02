package com.wizm.wizmcrates.gui;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class PreviewGui implements InventoryHolder {
    private final Inventory inventory;

    public PreviewGui(Crate crate, WizmCrates plugin) {
        List<Crate.Reward> rewards = crate.rewards();
        int itemCount = rewards.size();
        int rows = 3;
        if (itemCount > 7) rows = Math.min(6, (int) Math.ceil((double) itemCount / 7.0) + 2);
        int size = rows * 9;
        this.inventory = Bukkit.createInventory(this, size, Util.color(crate.guiTitle()));
        ItemStack border = Util.buildItem(plugin.cfg().borderMaterial(), " ", null, false, 0, 1);
        if (plugin.cfg().fillBorder() && plugin.cfg().borderMaterial() != org.bukkit.Material.AIR) {
            for (int i = 0; i < size; i++) {
                int row = i / 9, col = i % 9;
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) inventory.setItem(i, border);
            }
        }
        int slot = 10;
        for (Crate.Reward r : rewards) {
            while (plugin.cfg().fillBorder() && plugin.cfg().borderMaterial() != org.bukkit.Material.AIR && (slot % 9 == 0 || slot % 9 == 8)) slot++;
            if (slot >= size) break;
            inventory.setItem(slot, Util.buildRewardItem(r));
            slot++;
        }
    }

    public static void open(Player p, Crate crate, WizmCrates plugin) { p.openInventory(new PreviewGui(crate, plugin).getInventory()); }
    @Override public Inventory getInventory() { return inventory; }
}