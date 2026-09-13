package dev.darkvisuals.emotes;

import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.emotes.network.EmoteC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

 
public final class EmoteManager implements Wrapper {

    private static final long BLEND_MS = 300L;

    public record ActiveEmote(Emote emote, long startMs) {

        public long elapsedMs() {
            return System.currentTimeMillis() - startMs;
        }

        public float seconds() {
            return elapsedMs() / 1000.0f;
        }

        public boolean isExpired() {
            return elapsedMs() >= emote.durationMs();
        }

          
        public float blend() {
            long elapsed = elapsedMs();
            float in = MathHelper.clamp(elapsed / (float) BLEND_MS, 0f, 1f);
            float out = MathHelper.clamp((emote.durationMs() - elapsed) / (float) BLEND_MS, 0f, 1f);
            float t = Math.min(in, out);
             
            return t * t * (3f - 2f * t);
        }
    }

    private static final Map<UUID, ActiveEmote> ACTIVE = new ConcurrentHashMap<>();

      
    public static void playLocal(Emote emote) {
        if (mc.player == null) return;
        ACTIVE.put(mc.player.getUuid(), new ActiveEmote(emote, System.currentTimeMillis()));
        sendToServer(emote.id());
    }

      
    public static void stopLocal() {
        if (mc.player == null) return;
        ACTIVE.remove(mc.player.getUuid());
        sendToServer("");
    }

    private static void sendToServer(String emoteId) {
         
        try {
            if (mc.getNetworkHandler() != null && ClientPlayNetworking.canSend(EmoteC2SPayload.ID)) {
                ClientPlayNetworking.send(new EmoteC2SPayload(emoteId));
            }
        } catch (Throwable ignored) {
             
        }

         
         
        dev.darkvisuals.client.managers.CosmeticsSyncManager.sendEmote(emoteId);
    }

      
    public static void playRemote(UUID player, String emoteId) {
        Emote emote = EmoteRegistry.byId(emoteId);
        if (emote == null) return;
        ACTIVE.put(player, new ActiveEmote(emote, System.currentTimeMillis()));
    }

    public static void stop(UUID player) {
        ACTIVE.remove(player);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

      
    public static ActiveEmote getActive(UUID player) {
        if (player == null) return null;
        ActiveEmote active = ACTIVE.get(player);
        if (active == null) return null;
        if (active.isExpired()) {
            ACTIVE.remove(player, active);
            return null;
        }
        return active;
    }

    public static boolean isPlayingLocal() {
        return mc.player != null && getActive(mc.player.getUuid()) != null;
    }

    private EmoteManager() {}
}