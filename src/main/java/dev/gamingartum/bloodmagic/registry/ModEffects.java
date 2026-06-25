package dev.gamingartum.bloodmagic.registry;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.EnumMap;
import java.util.Map;

public final class ModEffects {

    private static final Map<BloodBuff, Holder<MobEffect>> BY_BUFF = new EnumMap<>(BloodBuff.class);

    /** Display-only MobEffect — the actual mechanics come from attribute modifiers + event hooks. */
    private static final class DisplayEffect extends MobEffect {
        DisplayEffect(int color) {
            super(MobEffectCategory.BENEFICIAL, color);
        }
    }

    public static void initialize() {
        for (BloodBuff buff : BloodBuff.values()) {
            Holder<MobEffect> holder = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                BloodMagic.id(buff.name().toLowerCase()),
                new DisplayEffect(buff.iconColor)
            );
            BY_BUFF.put(buff, holder);
        }
    }

    public static Holder<MobEffect> get(BloodBuff buff) {
        return BY_BUFF.get(buff);
    }

    /** Build a display-only MobEffectInstance (no particles, icon visible). */
    public static MobEffectInstance instance(BloodBuff buff) {
        return new MobEffectInstance(
            get(buff),
            buff.durationTicks,
            0,       // amplifier
            false,   // not ambient (beacon-like)
            false,   // no particles
            true     // show icon in HUD
        );
    }
}
