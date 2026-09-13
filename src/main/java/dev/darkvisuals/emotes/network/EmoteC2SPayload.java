package dev.darkvisuals.emotes.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

  
public record EmoteC2SPayload(String emoteId) implements CustomPayload {

    public static final CustomPayload.Id<EmoteC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of("darkvisuals", "emote_play"));

    public static final PacketCodec<RegistryByteBuf, EmoteC2SPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, EmoteC2SPayload::emoteId, EmoteC2SPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}