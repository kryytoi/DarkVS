package dev.darkvisuals.emotes.network;

import dev.darkvisuals.emotes.EmoteManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

  
public final class EmotesClientNetwork {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EmoteS2CPayload.ID, (payload, context) -> {
            if (payload.emoteId().isEmpty()) {
                EmoteManager.stop(payload.player());
            } else {
                EmoteManager.playRemote(payload.player(), payload.emoteId());
            }
        });

         
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EmoteManager.clearAll());
    }

    private EmotesClientNetwork() {}
}