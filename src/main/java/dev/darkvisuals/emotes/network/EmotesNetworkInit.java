package dev.darkvisuals.emotes.network;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

 
public class EmotesNetworkInit implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(EmoteC2SPayload.ID, EmoteC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EmoteS2CPayload.ID, EmoteS2CPayload.CODEC);

         
        ServerPlayNetworking.registerGlobalReceiver(EmoteC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            String emoteId = payload.emoteId();

             
            if (emoteId.length() > 32 || !emoteId.matches("[a-z0-9_]*")) return;

            EmoteS2CPayload out = new EmoteS2CPayload(sender.getUuid(), emoteId);
            for (ServerPlayerEntity viewer : PlayerLookup.tracking(sender)) {
                if (viewer != sender && ServerPlayNetworking.canSend(viewer, EmoteS2CPayload.ID)) {
                    ServerPlayNetworking.send(viewer, out);
                }
            }
        });
    }
}