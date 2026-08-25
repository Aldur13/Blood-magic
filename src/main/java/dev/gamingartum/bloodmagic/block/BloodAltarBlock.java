package dev.gamingartum.bloodmagic.block;

import dev.gamingartum.bloodmagic.menu.BloodAltarMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A physical, craftable altar. Right-clicking opens a vanilla container-style
 * menu (like a chest) — this works for any vanilla client, no mod required
 * on their end. All buff logic runs entirely on the server.
 */
public class BloodAltarBlock extends Block {

    public BloodAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new BloodAltarMenu(containerId, inventory),
                Component.literal("Blood Altar")
            ));
        }
        return InteractionResult.SUCCESS;
    }
}
