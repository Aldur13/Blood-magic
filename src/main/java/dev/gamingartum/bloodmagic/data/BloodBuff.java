package dev.gamingartum.bloodmagic.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum BloodBuff {

    // percentCost = fraction of CURRENT max health sacrificed (0.10 = 10%); durationTicks at 20t/s;
    // iconColor = 0xRRGGBB; icon = altar-menu display item.
    // Costs are computed off current max health (not a flat hearts amount) so this scales correctly
    // alongside lifesteal-style mods that grow/shrink max health over time.
    BLOOD_RUSH    ("Blood Rush",    "Speed I",                       0.10, 12000, 0xCC1111, Items.SUGAR),
    COAGULATION   ("Coagulation",   "60% knockback resistance",      0.20,  9600, 0x770000, Items.SHIELD),
    BLOODLUST     ("Bloodlust",     "+25% melee damage",             0.20,  9600, 0xFF2222, Items.IRON_SWORD),
    SWIFT_BLOOD   ("Swift Blood",   "+25% movement speed",           0.20,  7200, 0xFF6611, Items.FEATHER),
    IRON_BLOOD    ("Iron Blood",    "+6 armor points",               0.30,  6000, 0x885555, Items.IRON_CHESTPLATE),
    BERSERKER     ("Berserker",     "+40% attack speed",             0.30,  4800, 0xFF4400, Items.BLAZE_POWDER),
    HEMORRHAGE    ("Hemorrhage",    "Poisons enemies for 3s",        0.30,  4800, 0x990000, Items.SPIDER_EYE),
    BLOOD_WARD    ("Blood Ward",    "Absorbs the next hit",          0.40,  3600, 0x882288, Items.TOTEM_OF_UNDYING),
    LIFE_DRAIN    ("Life Drain",    "Hits restore 1 health",         0.40,  3600, 0x229933, Items.GOLDEN_APPLE),
    BLOOD_FURY    ("Blood Fury",    "+50% melee damage",             0.60,  2400, 0xFF8800, Items.REDSTONE);

    public final String displayName;
    public final String description;
    public final double percentCost; // fraction of current max health, e.g. 0.40 = 40%
    public final int durationTicks;
    public final int iconColor;  // 0xRRGGBB used for MobEffect color + icon tint
    public final Item icon;      // display item shown in the Blood Altar menu

    BloodBuff(String displayName, String description, double percentCost, int durationTicks, int iconColor, Item icon) {
        this.displayName   = displayName;
        this.description   = description;
        this.percentCost   = percentCost;
        this.durationTicks = durationTicks;
        this.iconColor     = iconColor;
        this.icon          = icon;
    }

    /** Health sacrificed, as a fraction of the player's CURRENT max health at cast time. */
    public float healthCost(float currentMaxHealth) {
        return (float) (currentMaxHealth * percentCost);
    }

    public String percentString() {
        return Math.round(percentCost * 100) + "% max health";
    }

    public String durationString() {
        int s = durationTicks / 20;
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
