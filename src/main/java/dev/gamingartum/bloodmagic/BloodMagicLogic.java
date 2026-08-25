package dev.gamingartum.bloodmagic;

import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import dev.gamingartum.bloodmagic.registry.ModEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-only buff activation/expiry logic, shared by every UI entry point
 * (Blood Altar menu, and previously the client keybind screen). No client
 * involvement required — pure server state + vanilla attribute modifiers.
 */
public final class BloodMagicLogic {

    private BloodMagicLogic() {}

    /** Returns true if the buff was activated. */
    public static boolean activateBuff(ServerPlayer player, BloodBuff buff) {
        if (BloodData.hasActiveBuff(player, buff)) return false;

        float cost = buff.healthCost(player.getMaxHealth());
        if (player.getHealth() <= cost) return false;

        var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.addTransientModifier(new AttributeModifier(
                healthModId(buff), -cost, AttributeModifier.Operation.ADD_VALUE
            ));
        }

        applyBuffEffect(player, buff);

        player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), cost);

        List<ActiveBuffEntry> entries = new ArrayList<>(BloodData.get(player));
        long currentTick = player.level().getServer().getTickCount();
        entries.add(ActiveBuffEntry.create(buff, currentTick));
        BloodData.set(player, entries);

        BloodMagic.LOGGER.debug("Player {} activated buff {} (cost {}hp)", player.getName().getString(), buff, cost);
        return true;
    }

    /** Called each server tick to expire effects and lift the health debt after cooldown. */
    public static void tick(ServerPlayer player, long tick) {
        List<ActiveBuffEntry> entries = BloodData.get(player);
        if (entries.isEmpty()) return;
        List<ActiveBuffEntry> updated = new ArrayList<>();
        boolean changed = false;

        for (ActiveBuffEntry entry : entries) {
            if (entry.isExpired(tick)) {
                removeMaxHealthModifier(player, entry.buff());
                changed = true;
            } else {
                if (!entry.isEffectActive(tick)) {
                    removeBuffEffect(player, entry.buff());
                }
                updated.add(entry);
            }
        }

        if (changed) {
            BloodData.set(player, updated);
        }
    }

    // -------------------------------------------------------------------------

    private static void applyBuffEffect(ServerPlayer player, BloodBuff buff) {
        player.addEffect(ModEffects.instance(buff));

        switch (buff) {
            case BLOOD_RUSH -> player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, buff.durationTicks + 40, 0, false, false
            ));
            case COAGULATION -> addAttr(player, Attributes.KNOCKBACK_RESISTANCE,
                buffModId(buff), 0.6, AttributeModifier.Operation.ADD_VALUE);
            case BLOODLUST -> addAttr(player, Attributes.ATTACK_DAMAGE,
                buffModId(buff), 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case SWIFT_BLOOD -> addAttr(player, Attributes.MOVEMENT_SPEED,
                buffModId(buff), 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case IRON_BLOOD -> addAttr(player, Attributes.ARMOR,
                buffModId(buff), 6.0, AttributeModifier.Operation.ADD_VALUE);
            case BERSERKER -> addAttr(player, Attributes.ATTACK_SPEED,
                buffModId(buff), 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case BLOOD_FURY -> addAttr(player, Attributes.ATTACK_DAMAGE,
                buffModId(buff), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            default -> { /* event-based — no attribute modifier */ }
        }
    }

    private static void removeBuffEffect(ServerPlayer player, BloodBuff buff) {
        player.removeEffect(ModEffects.get(buff));

        switch (buff) {
            case BLOOD_RUSH -> player.removeEffect(MobEffects.SPEED);
            case COAGULATION   -> removeAttr(player, Attributes.KNOCKBACK_RESISTANCE, buffModId(buff));
            case BLOODLUST     -> removeAttr(player, Attributes.ATTACK_DAMAGE,        buffModId(buff));
            case SWIFT_BLOOD   -> removeAttr(player, Attributes.MOVEMENT_SPEED,       buffModId(buff));
            case IRON_BLOOD    -> removeAttr(player, Attributes.ARMOR,                buffModId(buff));
            case BERSERKER     -> removeAttr(player, Attributes.ATTACK_SPEED,         buffModId(buff));
            case BLOOD_FURY    -> removeAttr(player, Attributes.ATTACK_DAMAGE,        buffModId(buff));
            default -> { /* event-based — no attribute to remove */ }
        }
    }

    private static void removeMaxHealthModifier(ServerPlayer player, BloodBuff buff) {
        removeAttr(player, Attributes.MAX_HEALTH, healthModId(buff));
        float maxHealth = player.getMaxHealth();
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    // -------------------------------------------------------------------------

    private static void addAttr(ServerPlayer player,
                                net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                Identifier id, double amount, AttributeModifier.Operation op) {
        var instance = player.getAttribute(attr);
        if (instance != null) instance.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void removeAttr(ServerPlayer player,
                                   net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                   Identifier id) {
        var instance = player.getAttribute(attr);
        if (instance != null) instance.removeModifier(id);
    }

    public static Identifier buffModId(BloodBuff buff) {
        return BloodMagic.id("buff/" + buff.name().toLowerCase());
    }

    public static Identifier healthModId(BloodBuff buff) {
        return BloodMagic.id("health_debt/" + buff.name().toLowerCase());
    }
}
