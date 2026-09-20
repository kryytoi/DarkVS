package dev.darkvisuals.liteapi;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public record LiteApiPayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<LiteApiPayload> ID =
            new CustomPayload.Id<>(Identifier.of("liteapi", "feature-control"));

    public static final PacketCodec<PacketByteBuf, LiteApiPayload> CODEC = new PacketCodec<>() {
        @Override
        public LiteApiPayload decode(PacketByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new LiteApiPayload(new String(bytes, StandardCharsets.UTF_8));
        }

        @Override
        public void encode(PacketByteBuf buf, LiteApiPayload value) {
            buf.writeBytes(value.json().getBytes(StandardCharsets.UTF_8));
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}