package dev.gamingartum.bloodmagic.data;

public enum BloodBuff {

    // heartCost = full hearts; durationTicks at 20t/s; iconColor = 0xRRGGBB
    CRIMSON_SIGHT ("Crimson Sight", "Night vision",                  1, 12000, 0xCC1111),
    COAGULATION   ("Coagulation",   "60% knockback resistance",      2,  9600, 0x770000),
    BLOODLUST     ("Bloodlust",     "+25% melee damage",             2,  9600, 0xFF2222),
    SWIFT_BLOOD   ("Swift Blood",   "+25% movement speed",           2,  7200, 0xFF6611),
    IRON_BLOOD    ("Iron Blood",    "+6 armor points",               3,  6000, 0x885555),
    BERSERKER     ("Berserker",     "+40% attack speed",             3,  4800, 0xFF4400),
    HEMORRHAGE    ("Hemorrhage",    "Poisons enemies for 3s",        3,  4800, 0x990000),
    BLOOD_WARD    ("Blood Ward",    "Absorbs the next hit",          4,  3600, 0x882288),
    LIFE_DRAIN    ("Life Drain",    "Hits restore 1 health",         4,  3600, 0x229933),
    BLOOD_FURY    ("Blood Fury",    "+50% melee damage",             6,  2400, 0xFF8800);

    public final String displayName;
    public final String description;
    public final int heartCost;
    public final int durationTicks;
    public final int iconColor;  // 0xRRGGBB used for MobEffect color + icon tint

    BloodBuff(String displayName, String description, int heartCost, int durationTicks, int iconColor) {
        this.displayName   = displayName;
        this.description   = description;
        this.heartCost     = heartCost;
        this.durationTicks = durationTicks;
        this.iconColor     = iconColor;
    }

    public float healthCost() { return heartCost * 2.0f; }

    public String durationString() {
        int s = durationTicks / 20;
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
