package dev.gamingartum.bloodmagic;

import dev.gamingartum.bloodmagic.buff.BuffBossBars;
import dev.gamingartum.bloodmagic.buff.BuffManager;
import dev.gamingartum.bloodmagic.command.BloodCommand;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blood Magic: sacrifice hearts for temporary combat buffs.
 *
 * <p>Server-side only. There is no client source set, no custom registry entry and no custom
 * packet, so unmodified vanilla clients can join a server running this mod. Everything a
 * player sees is built from vanilla parts: a generic chest menu, boss bars and chat.
 */
public class BloodMagic implements ModInitializer {

    public static final String MOD_ID = "bloodmagic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // Touch the attachment type so it registers before any world loads.
        BloodData.BLOOD_BUFFS.identifier();

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> BloodCommand.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(BuffManager::tick);

        // Attribute modifiers and mob effects are transient, so they must be rebuilt whenever
        // the ServerPlayer entity is: on login, on respawn, and on returning from the End.
        ServerPlayerEvents.JOIN.register(BuffManager::reapply);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            BuffBossBars.remove(oldPlayer);
            BuffManager.reapply(newPlayer);
        });
        ServerPlayerEvents.LEAVE.register(BuffBossBars::remove);

        // A dimension change makes the client drop its boss bars; rebuild them.
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            BuffBossBars.remove(player);
            BuffManager.reapply(player);
        });

        registerCombatHooks();

        LOGGER.info("Blood Magic initialized (server-side only).");
    }

    private void registerCombatHooks() {
        // Hemorrhage: your attacks poison the target for 3 seconds.
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (target instanceof LivingEntity livingTarget
                    && BloodData.hasActiveBuff(serverPlayer, BloodBuff.HEMORRHAGE, level.getGameTime())) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
            return InteractionResult.PASS;
        });

        // Life Drain: your attacks restore 1 health (half a heart).
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (target instanceof LivingEntity
                    && BloodData.hasActiveBuff(serverPlayer, BloodBuff.LIFE_DRAIN, level.getGameTime())) {
                serverPlayer.heal(1.0f);
            }
            return InteractionResult.PASS;
        });

        // Blood Ward: absorbs the next hit you take.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            if (amount <= 0.0f) return true;

            // The void, /kill and similar are not survivable by design; a ward that swallowed
            // them would leave the player stuck rather than protected.
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;

            // Don't burn a ward on damage that was never going to land anyway.
            if (player.isSpectator() || player.getAbilities().invulnerable) return true;

            return !BuffManager.tryConsumeWard(player, BuffManager.gameTime(player));
        });
    }
}
