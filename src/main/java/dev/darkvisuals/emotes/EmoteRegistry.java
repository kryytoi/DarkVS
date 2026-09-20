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

    public static final Emote BICEPS = register(new Emote(
            "biceps",
            "emote.darkvisuals.biceps",
            3600L,
            (model, blend, seconds) -> {
                // рука согнута вверх, периодически "сжимается"
                float squeeze = MathHelper.sin(seconds * 7.0f) * 0.5f + 0.5f;

                float targetPitch = -1.15f - squeeze * 0.22f;
                float targetYaw = -0.35f;
                float targetRoll = -0.25f;

                model.rightArm.pitch = MathHelper.lerp(blend, model.rightArm.pitch, targetPitch);
                model.rightArm.yaw = MathHelper.lerp(blend, model.rightArm.yaw, targetYaw);
                model.rightArm.roll = MathHelper.lerp(blend, model.rightArm.roll, targetRoll);

                // вторая рука слегка отведена, голова смотрит на бицепс
                model.leftArm.yaw = MathHelper.lerp(blend, model.leftArm.yaw, 0.15f);
                model.head.pitch = MathHelper.lerp(blend, model.head.pitch, -0.15f);
                model.head.roll = MathHelper.lerp(blend, model.head.roll, 0.22f);
            }
    ));

    public static final Emote SIT = register(new Emote(
            "sit",
            "emote.darkvisuals.sit",
            600000L,
            (model, blend, seconds) -> {
                // ноги согнуты в бёдрах, как на невидимом стуле
                model.rightLeg.pitch = MathHelper.lerp(blend, model.rightLeg.pitch, -1.5f);
                model.leftLeg.pitch = MathHelper.lerp(blend, model.leftLeg.pitch, -1.5f);

                // руки свободно на коленях
                model.rightArm.pitch = MathHelper.lerp(blend, model.rightArm.pitch, -0.6f);
                model.leftArm.pitch = MathHelper.lerp(blend, model.leftArm.pitch, -0.6f);

                model.body.pitch = MathHelper.lerp(blend, model.body.pitch, 0.12f);
            }
    ));

    public static final Emote LIE = register(new Emote(
            "lie",
            "emote.darkvisuals.lie",
            600000L,
            (model, blend, seconds) -> {
                // корпус повёрнут — игрок лежит на спине
                model.body.pitch = MathHelper.lerp(blend, model.body.pitch, -1.45f);

                // голова слегка запрокинута, чтобы не "втыкалась" в землю
                model.head.pitch = MathHelper.lerp(blend, model.head.pitch, 0.55f);

                // руки и ноги вытянуты и чуть расставлены
                model.rightArm.pitch = MathHelper.lerp(blend, model.rightArm.pitch, -0.05f);
                model.rightArm.roll = MathHelper.lerp(blend, model.rightArm.roll, 0.28f);
                model.leftArm.pitch = MathHelper.lerp(blend, model.leftArm.pitch, -0.05f);
                model.leftArm.roll = MathHelper.lerp(blend, model.leftArm.roll, -0.28f);

                model.rightLeg.pitch = MathHelper.lerp(blend, model.rightLeg.pitch, 0.06f);
                model.rightLeg.yaw = MathHelper.lerp(blend, model.rightLeg.yaw, -0.12f);
                model.leftLeg.pitch = MathHelper.lerp(blend, model.leftLeg.pitch, 0.06f);
                model.leftLeg.yaw = MathHelper.lerp(blend, model.leftLeg.yaw, 0.12f);
            }
    ));

    public static final Emote TWERK = register(new Emote(
            "twerk",
            "emote.darkvisuals.twerk",
            4500L,
            (model, blend, seconds) -> {
                // наклон вперёд и быстрое покачивание бёдрами
                float shake = MathHelper.sin(seconds * 9.5f);

                model.body.pitch = MathHelper.lerp(blend, model.body.pitch, 0.55f + shake * 0.28f);
                model.body.roll = MathHelper.lerp(blend, model.body.roll, shake * 0.07f);

                // голова вниз, руки на коленях, ноги чуть согнуты
                model.head.pitch = MathHelper.lerp(blend, model.head.pitch, 0.35f);
                model.rightArm.pitch = MathHelper.lerp(blend, model.rightArm.pitch, -0.5f);
                model.leftArm.pitch = MathHelper.lerp(blend, model.leftArm.pitch, -0.5f);
                model.rightLeg.pitch = MathHelper.lerp(blend, model.rightLeg.pitch, 0.22f);
                model.leftLeg.pitch = MathHelper.lerp(blend, model.leftLeg.pitch, 0.22f);
                model.rightLeg.yaw = MathHelper.lerp(blend, model.rightLeg.yaw, -0.08f);
                model.leftLeg.yaw = MathHelper.lerp(blend, model.leftLeg.yaw, 0.08f);
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