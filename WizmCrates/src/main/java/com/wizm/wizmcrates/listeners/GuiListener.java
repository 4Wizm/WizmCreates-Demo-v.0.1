package com.wizm.wizmcrates.listeners;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.gui.EditGui;
import com.wizm.wizmcrates.gui.PreviewGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private final WizmCrates plugin;
    public GuiListener(WizmCrates plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof PreviewGui) { e.setCancelled(true); return; }
        if (e.getInventory().getHolder() instanceof EditGui eg) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (e.getClickedInventory() == null) return;
            if (e.getClickedInventory().equals(p.getInventory())) {
                if (e.isShiftClick()) {
                    ItemStack item = e.getCurrentItem();
                    if (item != null && item.getType() != Material.AIR) {
                        plugin.extras().addExtra(eg.crate().id(), item);
                        plugin.crates().load();
                        eg.refresh();
                        p.sendMessage(plugin.cfg().msg("edit-added").replace("{crate}", eg.crate().id()));
                    }
                }
                return;
            }
            if (e.getClickedInventory().equals(e.getInventory())) {
                int slot = e.getSlot();
                if (slot >= 45) return;
                String rid = eg.rewardIdAt(slot);
                if (rid != null) {
                    plugin.extras().removeExtra(eg.crate().id(), rid);
                    plugin.crates().load();
                    eg.refresh();
                    p.sendMessage(plugin.cfg().msg("edit-removed").replace("{crate}", eg.crate().id()));
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PreviewGui || e.getInventory().getHolder() instanceof EditGui) e.setCancelled(true);
    }
}
