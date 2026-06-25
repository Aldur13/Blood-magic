package dev.gamingartum.bloodmagic.network;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActivateBloodBuffPayload(BloodBuff buff) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivateBloodBuffPayload> TYPE =
        new CustomPacketPayload.Type<>(BloodMagic.id("activate_buff"));

    public static final StreamCodec<ByteBuf, ActivateBloodBuffPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(BloodBuff::valueOf, BloodBuff::name),
        ActivateBloodBuffPayload::buff,
        ActivateBloodBuffPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
