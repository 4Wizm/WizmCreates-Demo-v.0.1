package com.wizm.wizmcrates.util;

import com.wizm.wizmcrates.crate.Crate;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Util {
    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private Util() {}

    public static String color(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString());
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static String parsePAPI(Player player, String text) {
        if (text == null) return "";
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) text = PlaceholderAPI.setPlaceholders(player, text);
        return color(text);
    }

    public static List<String> color(List<String> list) {
        List<String> out = new ArrayList<>();
        for (String s : list) out.add(color(s));
        return out;
    }

    public static ItemStack buildItem(Material mat, String name, List<String> lore, boolean glow, int cmd, int amount) {
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) meta.setDisplayName(color(name));
            if (lore != null && !lore.isEmpty()) meta.setLore(color(lore));
            if (cmd > 0) meta.setCustomModelData(cmd);
            if (glow) { meta.addEnchant(Enchantment.UNBREAKING, 1, true); meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack buildRewardItem(Crate.Reward r) {
        ItemStack item;
        if (r.fullItem() != null) {
            item = r.fullItem().clone();
        } else {
            item = buildItem(r.material(), r.name(), r.lore(), false, 0, r.amount());
        }
        item.setAmount(Math.max(1, r.amount()));
        return item;
    }
}