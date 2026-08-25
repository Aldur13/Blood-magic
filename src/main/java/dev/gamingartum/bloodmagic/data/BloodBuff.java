package dev.gamingartum.bloodmagic.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Optional;

public enum BloodBuff {

    // heartCost = full hearts; durationTicks at 20t/s
    CRIMSON_SIGHT ("Crimson Sight", "Night vision",             1, 12000, Items.GOLDEN_CARROT,    BossEvent.BossBarColor.PURPLE),
    COAGULATION   ("Coagulation",   "60% knockback resistance", 2,  9600, Items.SHIELD,           BossEvent.BossBarColor.WHITE),
    BLOODLUST     ("Bloodlust",     "+25% melee damage",        2,  9600, Items.IRON_SWORD,       BossEvent.BossBarColor.RED),
    SWIFT_BLOOD   ("Swift Blood",   "+25% movement speed",      2,  7200, Items.SUGAR,            BossEvent.BossBarColor.YELLOW),
    IRON_BLOOD    ("Iron Blood",    "+6 armor points",          3,  6000, Items.IRON_CHESTPLATE,  BossEvent.BossBarColor.WHITE),
    BERSERKER     ("Berserker",     "+40% attack speed",        3,  4800, Items.DIAMOND_SWORD,    BossEvent.BossBarColor.RED),
    HEMORRHAGE    ("Hemorrhage",    "Poisons enemies for 3s",   3,  4800, Items.SPIDER_EYE,       BossEvent.BossBarColor.GREEN),
    BLOOD_WARD    ("Blood Ward",    "Absorbs the next hit",     4,  3600, Items.TOTEM_OF_UNDYING, BossEvent.BossBarColor.PURPLE),
    LIFE_DRAIN    ("Life Drain",    "Hits restore 1 health",    4,  3600, Items.GHAST_TEAR,       BossEvent.BossBarColor.GREEN),
    BLOOD_FURY    ("Blood Fury",    "+50% melee damage",        6,  2400, Items.NETHERITE_SWORD,  BossEvent.BossBarColor.RED);

    public final String displayName;
    public final String description;
    public final int heartCost;
    public final int durationTicks;
    public final Item icon;
    public final BossEvent.BossBarColor barColor;

    BloodBuff(String displayName, String description, int heartCost, int durationTicks,
              Item icon, BossEvent.BossBarColor barColor) {
        this.displayName   = displayName;
        this.description   = description;
        this.heartCost     = heartCost;
        this.durationTicks = durationTicks;
        this.icon          = icon;
        this.barColor      = barColor;
    }

    /** Lowercase form used by commands and by the persisted codec. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public float healthCost() {
        return heartCost * 2.0f;
    }

    public String durationString() {
        int s = durationTicks / 20;
        return (s / 60) + "m " + (s % 60) + "s";
    }

    public static Optional<BloodBuff> byId(String id) {
        for (BloodBuff buff : values()) {
            if (buff.id().equalsIgnoreCase(id)) return Optional.of(buff);
        }
        return Optional.empty();
    }

    /**
     * Fault-tolerant codec. A renamed or removed buff yields a decode error rather than
     * an exception, so a stale save file cannot take the server down on load.
     */
    public static final Codec<BloodBuff> CODEC = Codec.STRING.comapFlatMap(
        s -> byId(s)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown blood buff: " + s)),
        BloodBuff::id
    );
}
