package dev.gamingartum.bloodmagic.buff;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/** All buff mechanics. Every method here runs on the server thread only. */
public final class BuffManager {

    /** A player must be left with at least this much maximum health after paying. */
    private static final float MIN_REMAINING_MAX_HEALTH = 2.0f;

    private BuffManager() {
    }

    public enum Result {
        OK,
        ALREADY_ACTIVE,
        COOLING_DOWN,
        NOT_ENOUGH_BLOOD,
        BODY_TOO_WEAK
    }

    /** World game time: persisted, monotonic, and identical across all dimensions. */
    public static long gameTime(ServerPlayer player) {
        return player.level().getGameTime();
    }

    // -------------------------------------------------------------------------
    // Activation
    // -------------------------------------------------------------------------

    public static Result activate(ServerPlayer player, BloodBuff buff) {
        long now = gameTime(player);

        ActiveBuffEntry existing = BloodData.entry(player, buff);
        if (existing != null) {
            return existing.isEffectActive(now) ? Result.ALREADY_ACTIVE : Result.COOLING_DOWN;
        }

        float cost = buff.healthCost();

        // You have to have the blood to give it.
        if (player.getHealth() <= cost) return Result.NOT_ENOUGH_BLOOD;

        // ...and enough heart capacity left that the sacrifice does not erase you.
        if (player.getMaxHealth() - cost < MIN_REMAINING_MAX_HEALTH) return Result.BODY_TOO_WEAK;

        // The cost IS the lost heart capacity: MAX_HEALTH drops and current health is clamped
        // down with it. Dealing separate damage on top of that would charge the player twice.
        applyHealthDebt(player, buff);
        applyBuffEffect(player, buff, buff.durationTicks);

        List<ActiveBuffEntry> entries = BloodData.mutableCopy(player);
        entries.add(ActiveBuffEntry.create(buff, now));
        BloodData.set(player, entries);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7f, 0.6f);

        BuffBossBars.update(player, entries, now);

        BloodMagic.LOGGER.debug("{} sacrificed {} hearts for {}",
            player.getName().getString(), buff.heartCost, buff.id());
        return Result.OK;
    }

    // -------------------------------------------------------------------------
    // Ticking
    // -------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        boolean refreshBars = now % 20L == 0L;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player, now, refreshBars);
        }
    }

    private static void tickPlayer(ServerPlayer player, long now, boolean refreshBars) {
        List<ActiveBuffEntry> entries = BloodData.get(player);
        if (entries.isEmpty()) return;

        List<ActiveBuffEntry> kept = new ArrayList<>(entries.size());
        boolean changed = false;

        for (ActiveBuffEntry entry : entries) {
            if (entry.isExpired(now)) {
                // Regen cooldown is over: give the hearts back.
                removeHealthDebt(player, entry.buff());
                changed = true;
            } else {
                if (!entry.isEffectActive(now)) {
                    // Effect is over, but the heart debt stays until the cooldown ends.
                    // Idempotent, so repeating it every cooldown tick costs nothing.
                    removeBuffEffect(player, entry.buff());
                }
                kept.add(entry);
            }
        }

        if (changed) BloodData.set(player, kept);
        if (changed || refreshBars) BuffBossBars.update(player, kept, now);
    }

    // -------------------------------------------------------------------------
    // Re-applying after the ServerPlayer entity is rebuilt (login, respawn, End return)
    // -------------------------------------------------------------------------

    /**
     * Attribute modifiers and mob effects are transient, dying with the entity, while the buff
     * list persists. Without this, a relog would leave a player holding the cooldown but none
     * of the buff they paid for.
     */
    public static void reapply(ServerPlayer player) {
        long now = gameTime(player);
        List<ActiveBuffEntry> entries = BloodData.get(player);

        if (entries.isEmpty()) {
            BuffBossBars.remove(player);
            return;
        }

        List<ActiveBuffEntry> kept = new ArrayList<>(entries.size());
        for (ActiveBuffEntry entry : entries) {
            if (entry.isExpired(now)) continue;

            applyHealthDebt(player, entry.buff());
            if (entry.isEffectActive(now)) {
                applyBuffEffect(player, entry.buff(),
                    (int) Math.min(Integer.MAX_VALUE, entry.remaining(now)));
            }
            kept.add(entry);
        }

        BloodData.set(player, kept);
        BuffBossBars.remove(player);          // drop bars bound to the previous entity
        BuffBossBars.update(player, kept, now);
    }

    /** Drops every buff, heart debt and cooldown. Used by the clear subcommand. */
    public static void clear(ServerPlayer player) {
        for (BloodBuff buff : BloodBuff.values()) {
            removeBuffEffect(player, buff);
            removeHealthDebt(player, buff);
        }
        BloodData.clear(player);
        BuffBossBars.remove(player);
    }

    // -------------------------------------------------------------------------
    // Blood Ward
    // -------------------------------------------------------------------------

    /**
     * Spends an active Blood Ward if the player has one. Returns true when the incoming
     * damage should be cancelled.
     */
    public static boolean tryConsumeWard(ServerPlayer player, long now) {
        List<ActiveBuffEntry> entries = BloodData.mutableCopy(player);

        for (int i = 0; i < entries.size(); i++) {
            ActiveBuffEntry entry = entries.get(i);
            if (entry.buff() != BloodBuff.BLOOD_WARD || !entry.isEffectActive(now)) continue;

            // Send the ward into its regen cooldown instead of deleting the entry: deleting it
            // would strand its MAX_HEALTH debt with nothing left to ever lift it.
            entries.set(i, entry.consumedAt(now));
            BloodData.set(player, entries);
            removeBuffEffect(player, BloodBuff.BLOOD_WARD);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
            BuffBossBars.update(player, entries, now);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Effects and modifiers
    // -------------------------------------------------------------------------

    private static void applyBuffEffect(ServerPlayer player, BloodBuff buff, int remainingTicks) {
        if (remainingTicks <= 0) return;

        switch (buff) {
            // Given the exact remaining duration so it lapses on its own. Nothing in this mod
            // ever strips Night Vision, so a potion drunk later is left alone.
            case CRIMSON_SIGHT -> player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION, remainingTicks, 0, false, false));

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

            // Handled by the attack / damage hooks in BloodMagic.
            case HEMORRHAGE, BLOOD_WARD, LIFE_DRAIN -> { }
        }
    }

    private static void removeBuffEffect(ServerPlayer player, BloodBuff buff) {
        switch (buff) {
            case CRIMSON_SIGHT -> { /* Night Vision expires by itself, never yanked */ }
            case COAGULATION -> removeAttr(player, Attributes.KNOCKBACK_RESISTANCE, buffModId(buff));
            case BLOODLUST   -> removeAttr(player, Attributes.ATTACK_DAMAGE,        buffModId(buff));
            case SWIFT_BLOOD -> removeAttr(player, Attributes.MOVEMENT_SPEED,       buffModId(buff));
            case IRON_BLOOD  -> removeAttr(player, Attributes.ARMOR,                buffModId(buff));
            case BERSERKER   -> removeAttr(player, Attributes.ATTACK_SPEED,         buffModId(buff));
            case BLOOD_FURY  -> removeAttr(player, Attributes.ATTACK_DAMAGE,        buffModId(buff));
            case HEMORRHAGE, BLOOD_WARD, LIFE_DRAIN -> { }
        }
    }

    private static void applyHealthDebt(ServerPlayer player, BloodBuff buff) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        Identifier id = healthModId(buff);
        if (!instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(
                id, -buff.healthCost(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void removeHealthDebt(ServerPlayer player, BloodBuff buff) {
        removeAttr(player, Attributes.MAX_HEALTH, healthModId(buff));
    }

    private static void addAttr(ServerPlayer player, Holder<Attribute> attr,
                                Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    private static void removeAttr(ServerPlayer player, Holder<Attribute> attr, Identifier id) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    public static Identifier buffModId(BloodBuff buff) {
        return BloodMagic.id("buff/" + buff.id());
    }

    public static Identifier healthModId(BloodBuff buff) {
        return BloodMagic.id("health_debt/" + buff.id());
    }
}
