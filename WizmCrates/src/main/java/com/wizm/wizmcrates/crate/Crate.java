package com.wizm.wizmcrates.crate;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class Crate {
    public record Reward(String id, double chance, Material material, int amount, String name, List<String> lore, List<String> commands, ItemStack fullItem) {
        public Reward(String id, double chance, Material material, int amount, String name, List<String> lore, List<String> commands) {
            this(id, chance, material, amount, name, lore, commands, null);
        }
        public boolean hasFullItem() { return fullItem != null; }
    }
    public record GuaranteedReward(int openings, String rewardId) {}
    public record Animation(String name, String particle, int count, int interval, String sound, int soundInterval) {
        public boolean isNone() {
            return particle == null || particle.isEmpty() || particle.equalsIgnoreCase("none") || count <= 0;
        }
    }
    public record KeyItem(String type, Material material, String name, List<String> lore, boolean glow, int customModelData) {
        public boolean isVirtual() { return type != null && (type.equalsIgnoreCase("virtual") || type.equalsIgnoreCase("virtuale")); }
    }

    private final String id, displayName, guiTitle;
    private final Material block;
    private final boolean holoEnabled;
    private final double holoOffsetY;
    private final List<String> holoLines;
    private final Animation animation;
    private final KeyItem key;
    private final List<Reward> rewards;
    private final List<GuaranteedReward> guaranteed;

    public Crate(String id, String displayName, String guiTitle, Material block, boolean holoEnabled, double holoOffsetY,
                 List<String> holoLines, Animation animation, KeyItem key,
                 List<Reward> rewards, List<GuaranteedReward> guaranteed) {
        this.id = id; this.displayName = displayName; this.guiTitle = guiTitle; this.block = block;
        this.holoEnabled = holoEnabled; this.holoOffsetY = holoOffsetY; this.holoLines = holoLines;
        this.animation = animation; this.key = key; this.rewards = rewards; this.guaranteed = guaranteed;
    }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public String guiTitle() { return guiTitle != null && !guiTitle.isEmpty() ? guiTitle : displayName; }
    public Material block() { return block; }
    public boolean holoEnabled() { return holoEnabled; }
    public double holoOffsetY() { return holoOffsetY; }
    public List<String> holoLines() { return holoLines; }
    public Animation animation() { return animation; }
    public KeyItem key() { return key; }
    public List<Reward> rewards() { return rewards; }
    public List<GuaranteedReward> guaranteed() { return guaranteed; }
}