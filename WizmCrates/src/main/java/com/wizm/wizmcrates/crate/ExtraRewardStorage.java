package com.wizm.wizmcrates.crate;

import com.wizm.wizmcrates.WizmCrates;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.util.*;

public class ExtraRewardStorage {
    private final File file;
    private final YamlConfiguration data;

    public ExtraRewardStorage(WizmCrates plugin) {
        this.file = new File(plugin.getDataFolder(), "extra-rewards.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rawList(String crateId) {
        List<?> raw = data.getMapList(crateId.toLowerCase());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : raw) if (o instanceof Map) out.add((Map<String, Object>) o);
        return out;
    }

    public List<Crate.Reward> getExtras(String crateId) {
        List<Crate.Reward> list = new ArrayList<>();
        for (Map<String, Object> m : rawList(crateId)) {
            ItemStack full = null;
            Object rawItem = m.get("item");
            if (rawItem instanceof ItemStack is) {
                full = is;
            } else if (rawItem instanceof Map<?, ?> rawMap) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> im = (Map<String, Object>) rawMap;
                    full = ItemStack.deserialize(im);
                } catch (Exception ignored) {}
            }

            Material mat;
            int amount;
            String name;
            List<String> lore;
            if (full != null) {
                mat = full.getType();
                amount = full.getAmount();
                name = (full.hasItemMeta() && full.getItemMeta().hasDisplayName())
                    ? full.getItemMeta().getDisplayName() : "";
                lore = (full.hasItemMeta() && full.getItemMeta().hasLore())
                    ? new ArrayList<>(full.getItemMeta().getLore()) : new ArrayList<>();
            } else {
                mat = Material.matchMaterial(String.valueOf(m.getOrDefault("material", "STONE")));
                if (mat == null) mat = Material.STONE;
                amount = m.get("amount") instanceof Number n ? n.intValue() : 1;
                name = String.valueOf(m.getOrDefault("name", ""));
                @SuppressWarnings("unchecked")
                List<String> l = m.get("lore") instanceof List<?> ll ? (List<String>) ll : new ArrayList<>();
                lore = l;
            }

            double chance = m.get("chance") instanceof Number n ? n.doubleValue() : 1.0;
            @SuppressWarnings("unchecked")
            List<String> cmds = m.get("commands") instanceof List<?> l ? (List<String>) l : new ArrayList<>();

            list.add(new Crate.Reward(String.valueOf(m.getOrDefault("id", "extra")), chance, mat, amount,
                name, lore, cmds, full));
        }
        return list;
    }

    public void addExtra(String crateId, ItemStack item) {
        List<Map<String, Object>> list = rawList(crateId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "extra_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 9999));
        ItemStack copy = item.clone();
        map.put("item", copy);
        map.put("material", copy.getType().name());
        map.put("amount", copy.getAmount());
        String name = (copy.hasItemMeta() && copy.getItemMeta().hasDisplayName())
            ? copy.getItemMeta().getDisplayName() : "&f" + copy.getType().name();
        map.put("name", name);
        List<String> lore = (copy.hasItemMeta() && copy.getItemMeta().hasLore())
            ? new ArrayList<>(copy.getItemMeta().getLore()) : new ArrayList<>();
        map.put("lore", lore);
        map.put("chance", 5.0);
        map.put("commands", new ArrayList<String>());
        list.add(map);
        data.set(crateId.toLowerCase(), list);
        save();
    }

    public void removeExtra(String crateId, String rewardId) {
        List<Map<String, Object>> list = rawList(crateId);
        list.removeIf(m -> rewardId.equals(String.valueOf(m.get("id"))));
        data.set(crateId.toLowerCase(), list);
        save();
    }

    public void save() { try { data.save(file); } catch (Exception ignored) {} }
}