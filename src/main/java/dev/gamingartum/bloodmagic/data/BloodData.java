package dev.gamingartum.bloodmagic.data;

import dev.gamingartum.bloodmagic.BloodMagic;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class BloodData {

    public static final AttachmentType<List<ActiveBuffEntry>> BLOOD_BUFFS = AttachmentRegistry.create(
        BloodMagic.id("blood_buffs"),
        builder -> builder
            .persistent(ActiveBuffEntry.CODEC.listOf().xmap(ArrayList::new, l -> l))
            .initializer(ArrayList::new)
            .syncWith(
                ActiveBuffEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
                AttachmentSyncPredicate.targetOnly()
            )
    );

    public static List<ActiveBuffEntry> get(Player player) {
        return player.getAttachedOrCreate(BLOOD_BUFFS);
    }

    public static void set(Player player, List<ActiveBuffEntry> entries) {
        player.setAttached(BLOOD_BUFFS, entries);
    }

    public static boolean hasActiveBuff(Player player, BloodBuff buff) {
        return get(player).stream().anyMatch(e -> e.buff() == buff);
    }
}
