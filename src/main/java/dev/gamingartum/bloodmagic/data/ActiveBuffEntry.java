package dev.gamingartum.bloodmagic.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ActiveBuffEntry(BloodBuff buff, long expiresAtTick, long regenAtTick) {

    /** 12000 ticks = 10 minutes at 20 t/s */
    public static final long REGEN_COOLDOWN_TICKS = 12000L;

    public static final Codec<ActiveBuffEntry> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.xmap(BloodBuff::valueOf, BloodBuff::name).fieldOf("buff").forGetter(ActiveBuffEntry::buff),
            Codec.LONG.fieldOf("expires_at").forGetter(ActiveBuffEntry::expiresAtTick),
            Codec.LONG.fieldOf("regen_at").forGetter(ActiveBuffEntry::regenAtTick)
        ).apply(instance, ActiveBuffEntry::new)
    );

    public static final StreamCodec<ByteBuf, ActiveBuffEntry> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(BloodBuff::valueOf, BloodBuff::name), ActiveBuffEntry::buff,
        ByteBufCodecs.LONG, ActiveBuffEntry::expiresAtTick,
        ByteBufCodecs.LONG, ActiveBuffEntry::regenAtTick,
        ActiveBuffEntry::new
    );

    public static ActiveBuffEntry create(BloodBuff buff, long currentTick) {
        long expires = currentTick + buff.durationTicks;
        long regen   = expires + REGEN_COOLDOWN_TICKS;
        return new ActiveBuffEntry(buff, expires, regen);
    }

    public boolean isEffectActive(long currentTick)  { return currentTick < expiresAtTick; }
    public boolean isCoolingDown(long currentTick)   { return currentTick >= expiresAtTick && currentTick < regenAtTick; }
    public boolean isExpired(long currentTick)       { return currentTick >= regenAtTick; }
}
