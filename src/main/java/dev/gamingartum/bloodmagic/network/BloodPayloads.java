package dev.gamingartum.bloodmagic.network;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import dev.gamingartum.bloodmagic.registry.ModEffects;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

public final class BloodPayloads {

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
            ActivateBloodBuffPayload.TYPE, ActivateBloodBuffPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(ActivateBloodBuffPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            BloodBuff buff = payload.buff();

            // Validate: buff not already active
            if (BloodData.hasActiveBuff(player, buff)) return;

            // Validate: player has enough health to survive the cost (keep at least 1 hp)
            float cost = buff.healthCost();
            if (player.getHealth() <= cost) return;

            // Apply MAX_HEALTH attribute modifier — keeps the heart debt in place
            var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.addTransientModifier(new AttributeModifier(
                    healthModId(buff), -cost, AttributeModifier.Operation.ADD_VALUE
                ));
            }

            // Apply buff-specific effect/modifier
            applyBuffEffect(player, buff);

            // Deal damage equal to the cost (bypasses armor — it's a sacrifice)
            player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), cost);

            // Record the entry
            List<ActiveBuffEntry> entries = new ArrayList<>(BloodData.get(player));
            long currentTick = context.server().getTickCount();
            entries.add(ActiveBuffEntry.create(buff, currentTick));
            BloodData.set(player, entries);

            BloodMagic.LOGGER.debug("Player {} activated buff {} (cost {}hp)", player.getName().getString(), buff, cost);
        });
    }

    /** Called each server tick to expire effects and lift the health debt after cooldown. */
    public static void tick(ServerPlayer player, long tick) {
        List<ActiveBuffEntry> entries = BloodData.get(player);
        if (entries.isEmpty()) return;
        List<ActiveBuffEntry> updated = new ArrayList<>();
        boolean changed = false;

        for (ActiveBuffEntry entry : entries) {
            if (entry.isExpired(tick)) {
                // Full cooldown over — remove the MAX_HEALTH debt
                removeMaxHealthModifier(player, entry.buff());
                changed = true;
                // Don't add to updated list — entry is removed
            } else {
                if (!entry.isEffectActive(tick)) {
                    // Effect just expired — remove buff modifiers (MAX_HEALTH stays until cooldown ends)
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
        // Always apply the display MobEffect so it shows in the vanilla effects HUD
        player.addEffect(ModEffects.instance(buff));

        switch (buff) {
            case CRIMSON_SIGHT -> player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION, buff.durationTicks + 40, 0, false, false
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
            // HEMORRHAGE, BLOOD_WARD, LIFE_DRAIN are handled via events in BloodMagic
            default -> { /* event-based — no attribute modifier */ }
        }
    }

    private static void removeBuffEffect(ServerPlayer player, BloodBuff buff) {
        // Remove the display MobEffect
        player.removeEffect(ModEffects.get(buff));

        switch (buff) {
            case CRIMSON_SIGHT -> player.removeEffect(MobEffects.NIGHT_VISION);
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
        // Clamp health to new (restored) max just in case
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
