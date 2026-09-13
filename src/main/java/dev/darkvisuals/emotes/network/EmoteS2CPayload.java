package dev.darkvisuals.emotes.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

  
public record EmoteS2CPayload(UUID player, String emoteId) implements CustomPayload {

    public static final CustomPayload.Id<EmoteS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of("darkvisuals", "emote_sync"));

    public static final PacketCodec<RegistryByteBuf, EmoteS2CPayload> CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, EmoteS2CPayload::player,
                    PacketCodecs.STRING, EmoteS2CPayload::emoteId,
                    EmoteS2CPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}