package dev.darkvisuals.emotes;

import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EmoteRegistry {

    private static final Map<String, Emote> EMOTES = new LinkedHashMap<>();

    public static final Emote WAVE = register(new Emote(
            "wave",
            "emote.darkvisuals.wave",
            3200L,
            (model, blend, seconds) -> {
                 
                float wave = MathHelper.sin(seconds * 7.5f) * 0.45f;

                float targetPitch = -(float) Math.PI * 0.92f;
                float targetYaw = 0.0f;
                float targetRoll = -0.30f + wave;

                model.rightArm.pitch = MathHelper.lerp(blend, model.rightArm.pitch, targetPitch);
                model.rightArm.yaw = MathHelper.lerp(blend, model.rightArm.yaw, targetYaw);
                model.rightArm.roll = MathHelper.lerp(blend, model.rightArm.roll, targetRoll);

                 
                model.head.roll = MathHelper.lerp(blend, model.head.roll, wave * 0.12f);
            }
    ));

    private static Emote register(Emote emote) {
        EMOTES.put(emote.id(), emote);
        return emote;
    }

    public static Emote byId(String id) {
        return EMOTES.get(id);
    }

    public static List<Emote> all() {
        return List.copyOf(EMOTES.values());
    }

    public static Map<String, Emote> map() {
        return Collections.unmodifiableMap(EMOTES);
    }

    private EmoteRegistry() {}
}