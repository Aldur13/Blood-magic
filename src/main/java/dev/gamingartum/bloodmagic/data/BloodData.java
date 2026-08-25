package dev.gamingartum.bloodmagic.data;

import dev.gamingartum.bloodmagic.BloodMagic;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent per-player buff list. Never synced to clients — this mod is server-side only,
 * so the server is the sole owner of this state and vanilla clients hold no copy of it.
 */
public final class BloodData {

    private BloodData() {
    }

    public static final AttachmentType<List<ActiveBuffEntry>> BLOOD_BUFFS = AttachmentRegistry.create(
        BloodMagic.id("blood_buffs"),
        builder -> builder
            .persistent(ActiveBuffEntry.CODEC.listOf().xmap(ArrayList::new, l -> l))
            .initializer(ArrayList::new)
    );

    /**
     * Read-only view. Uses {@code getAttachedOrElse} rather than {@code getAttachedOrCreate} so
     * that merely reading — which happens every tick for every player — never writes an
     * attachment or marks the player dirty.
     */
    public static List<ActiveBuffEntry> get(Player player) {
        List<ActiveBuffEntry> entries = player.getAttachedOrElse(BLOOD_BUFFS, List.of());
        return Collections.unmodifiableList(entries);
    }

    /** A detached, writable copy — mutate it, then hand it back to {@link #set}. */
    public static List<ActiveBuffEntry> mutableCopy(Player player) {
        return new ArrayList<>(player.getAttachedOrElse(BLOOD_BUFFS, List.of()));
    }

    public static void set(Player player, List<ActiveBuffEntry> entries) {
        player.setAttached(BLOOD_BUFFS, new ArrayList<>(entries));
    }

    public static void clear(Player player) {
        player.setAttached(BLOOD_BUFFS, new ArrayList<>());
    }

    /** The entry for this buff whatever its phase, or null if the player has never taken it. */
    public static ActiveBuffEntry entry(Player player, BloodBuff buff) {
        for (ActiveBuffEntry entry : get(player)) {
            if (entry.buff() == buff) return entry;
        }
        return null;
    }

    /**
     * True only while the buff's effect is running. An entry sitting in its regen cooldown
     * is <em>not</em> active — that distinction is what keeps Hemorrhage, Life Drain and
     * Blood Ward from firing for ten free minutes after they expire.
     */
    public static boolean hasActiveBuff(Player player, BloodBuff buff, long currentTick) {
        ActiveBuffEntry entry = entry(player, buff);
        return entry != null && entry.isEffectActive(currentTick);
    }
}
