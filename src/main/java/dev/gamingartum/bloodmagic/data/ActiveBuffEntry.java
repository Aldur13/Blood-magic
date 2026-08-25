package dev.gamingartum.bloodmagic.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One sacrificed buff. Tick values are world game time ({@code Level#getGameTime()}), which is
 * persisted with the world and never rewinds — unlike {@code MinecraftServer#getTickCount()},
 * which restarts at zero every time the server boots.
 */
public record ActiveBuffEntry(BloodBuff buff, long expiresAtTick, long regenAtTick) {

    /** 12000 ticks = 10 minutes at 20 t/s */
    public static final long REGEN_COOLDOWN_TICKS = 12000L;

    public static final Codec<ActiveBuffEntry> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BloodBuff.CODEC.fieldOf("buff").forGetter(ActiveBuffEntry::buff),
            Codec.LONG.fieldOf("expires_at").forGetter(ActiveBuffEntry::expiresAtTick),
            Codec.LONG.fieldOf("regen_at").forGetter(ActiveBuffEntry::regenAtTick)
        ).apply(instance, ActiveBuffEntry::new)
    );

    public static ActiveBuffEntry create(BloodBuff buff, long currentTick) {
        long expires = currentTick + buff.durationTicks;
        return new ActiveBuffEntry(buff, expires, expires + REGEN_COOLDOWN_TICKS);
    }

    /** End this buff's effect now and start its regen cooldown — used when Blood Ward is spent. */
    public ActiveBuffEntry consumedAt(long currentTick) {
        return new ActiveBuffEntry(buff, currentTick, currentTick + REGEN_COOLDOWN_TICKS);
    }

    public boolean isEffectActive(long currentTick) { return currentTick < expiresAtTick; }
    public boolean isCoolingDown(long currentTick)  { return currentTick >= expiresAtTick && currentTick < regenAtTick; }
    public boolean isExpired(long currentTick)      { return currentTick >= regenAtTick; }

    /** Ticks left in whichever phase this entry is currently in. */
    public long remaining(long currentTick) {
        return Math.max(0L, (isEffectActive(currentTick) ? expiresAtTick : regenAtTick) - currentTick);
    }

    /** Length of the phase this entry is currently in, for progress bars. */
    public long phaseLength(long currentTick) {
        return isEffectActive(currentTick) ? buff.durationTicks : REGEN_COOLDOWN_TICKS;
    }
}
