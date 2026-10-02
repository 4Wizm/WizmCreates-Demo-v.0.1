package com.wizm.wizmcrates.commands;

import com.wizm.wizmcrates.WizmCrates;
import com.wizm.wizmcrates.crate.Crate;
import com.wizm.wizmcrates.gui.EditGui;
import com.wizm.wizmcrates.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import java.util.*;

public class CrateCommand implements CommandExecutor, TabCompleter {
    private final WizmCrates plugin;
    public CrateCommand(WizmCrates plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.cfg().msg("usage-help"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload": {
                if (!sender.hasPermission("wizmcrates.command.reload")) { sender.sendMessage(plugin.cfg().msg("no-permission")); return true; }
                plugin.cfg().reload();
                plugin.crates().load();
                plugin.holo().spawnAll();
                plugin.timers().load();
                sender.sendMessage(plugin.cfg().msg("reload-success"));
                break;
            }
            case "key": {
                if (!sender.hasPermission("wizmcrates.command.key")) { sender.sendMessage(plugin.cfg().msg("no-permission")); return true; }
                if (args.length < 4) { sender.sendMessage(Util.color(plugin.cfg().prefix() + "&cUso: /crate key <player|all> <crate> <amount>")); return true; }
                String target = args[1]; String crateId = args[2]; int amount;
                try { amount = Integer.parseInt(args[3]); } catch (Exception ex) { return true; }
                Crate crate = plugin.crates().get(crateId);
                if (crate == null) { sender.sendMessage(plugin.cfg().msg("crate-not-found")); return true; }
                if (target.equalsIgnoreCase("all")) {
                    for (Player p : Bukkit.getOnlinePlayers()) giveKey(p, crate, amount);
                    sender.sendMessage(plugin.cfg().msg("key-given-sender").replace("{amount}", String.valueOf(amount)).replace("{crate}", Util.color(crate.displayName())).replace("{target}", "TUTTI"));
                } else {
                    Player p = Bukkit.getPlayerExact(target);
                    if (p == null) { sender.sendMessage(plugin.cfg().msg("player-offline")); return true; }
                    giveKey(p, crate, amount);
                    sender.sendMessage(plugin.cfg().msg("key-given-sender").replace("{amount}", String.valueOf(amount)).replace("{crate}", Util.color(crate.displayName())).replace("{target}", p.getName()));
                }
                plugin.holo().updateHolograms();
                break;
            }
            case "edit": {
                if (!sender.hasPermission("wizmcrates.command.edit")) { sender.sendMessage(plugin.cfg().msg("no-permission")); return true; }
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) { sender.sendMessage(plugin.cfg().msg("edit-usage")); return true; }
                Crate crate = plugin.crates().get(args[1]);
                if (crate == null) { sender.sendMessage(plugin.cfg().msg("crate-not-found")); return true; }
                EditGui.open(p, crate, plugin);
                break;
            }
            case "setblock": {
                if (!sender.hasPermission("wizmcrates.command.setblock")) { sender.sendMessage(plugin.cfg().msg("no-permission")); return true; }
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) { sender.sendMessage(plugin.cfg().msg("setblock-usage")); return true; }
                Crate crate = plugin.crates().get(args[1]);
                if (crate == null) { sender.sendMessage(plugin.cfg().msg("crate-not-found")); return true; }
                RayTraceResult r = p.rayTraceBlocks(6);
                if (r == null || r.getHitBlock() == null) { sender.sendMessage(plugin.cfg().msg("setblock-looking-error")); return true; }
                Block b = r.getHitBlock();
                b.setType(crate.block());
                plugin.crates().setBlock(b.getLocation(), crate.id());
                plugin.holo().spawnAll();
                sender.sendMessage(plugin.cfg().msg("setblock-success"));
                break;
            }
            case "removeblock": {
                if (!sender.hasPermission("wizmcrates.command.removeblock")) { sender.sendMessage(plugin.cfg().msg("no-permission")); return true; }
                if (!(sender instanceof Player p)) return true;
                RayTraceResult r = p.rayTraceBlocks(6);
                if (r == null || r.getHitBlock() == null) { sender.sendMessage(plugin.cfg().msg("removeblock-looking-error")); return true; }
                Block b = r.getHitBlock();
                if (plugin.crates().removeBlock(b.getLocation())) {
                    b.setType(Material.AIR);
                    plugin.holo().spawnAll();
                    sender.sendMessage(plugin.cfg().msg("removeblock-success"));
                } else sender.sendMessage(plugin.cfg().msg("removeblock-not-a-crate"));
                break;
            }
            default: sender.sendMessage(plugin.cfg().msg("unknown-command"));
        }
        return true;
    }

    private void giveKey(Player p, Crate crate, int amount) {
        if (crate.key().isVirtual()) {
            if (plugin.db().isReady()) {
                plugin.db().addKeys(p.getUniqueId(), crate.id(), amount);
                p.sendMessage(plugin.cfg().msg("key-received-virtual").replace("{amount}", String.valueOf(amount)).replace("{crate}", Util.color(crate.displayName())));
            } else p.sendMessage(plugin.cfg().msg("db-not-connected"));
        } else {
            ItemStack item = Util.buildItem(crate.key().material(), crate.key().name(), crate.key().lore(), crate.key().glow(), crate.key().customModelData(), amount);
            if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), item);
            else p.getInventory().addItem(item);
            p.sendMessage(plugin.cfg().msg("key-received-physical").replace("{amount}", String.valueOf(amount)).replace("{crate}", Util.color(crate.displayName())));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("wizmcrates.command.reload")) out.add("reload");
            if (sender.hasPermission("wizmcrates.command.key")) out.add("key");
            if (sender.hasPermission("wizmcrates.command.setblock")) out.add("setblock");
            if (sender.hasPermission("wizmcrates.command.removeblock")) out.add("removeblock");
            if (sender.hasPermission("wizmcrates.command.edit")) out.add("edit");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("key") && sender.hasPermission("wizmcrates.command.key")) {
                out.add("all");
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if ((args[0].equalsIgnoreCase("setblock") || args[0].equalsIgnoreCase("edit")) &&
                    (sender.hasPermission("wizmcrates.command.setblock") || sender.hasPermission("wizmcrates.command.edit"))) {
                for (Crate c : plugin.crates().getAll()) out.add(c.id());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("key") && sender.hasPermission("wizmcrates.command.key")) {
            for (Crate c : plugin.crates().getAll()) out.add(c.id());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("key") && sender.hasPermission("wizmcrates.command.key")) {
            out.add("1"); out.add("10"); out.add("64");
        }
        return out;
    }
}
