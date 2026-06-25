package dev.gamingartum.bloodmagic;

import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import dev.gamingartum.bloodmagic.network.BloodPayloads;
import dev.gamingartum.bloodmagic.registry.ModEffects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BloodMagic implements ModInitializer {

    public static final String MOD_ID = "bloodmagic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEffects.initialize();
        BloodPayloads.initialize();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTickCount();
            server.getPlayerList().getPlayers().forEach(p -> BloodPayloads.tick(p, tick));
        });

        // Hemorrhage: attacks apply 3s poison to the target
        AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            if (!world.isClientSide()
                    && target instanceof LivingEntity livingTarget
                    && BloodData.hasActiveBuff(player, BloodBuff.HEMORRHAGE)) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
            return InteractionResult.PASS;
        });

        // Life Drain: attacks heal the attacker 1 hp (0.5 heart)
        AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            if (!world.isClientSide()
                    && target instanceof LivingEntity
                    && BloodData.hasActiveBuff(player, BloodBuff.LIFE_DRAIN)) {
                player.heal(1.0f);
            }
            return InteractionResult.PASS;
        });

        // Blood Ward: absorb the next hit the player takes
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            if (!BloodData.hasActiveBuff(player, BloodBuff.BLOOD_WARD)) return true;

            // Cancel the damage and consume the ward entry
            var entries = BloodData.get(player);
            entries.removeIf(e -> e.buff() == BloodBuff.BLOOD_WARD);
            BloodData.set(player, entries);
            player.removeEffect(ModEffects.get(BloodBuff.BLOOD_WARD));
            return false;
        });

        LOGGER.info("Blood Magic initialized.");
    }
}
