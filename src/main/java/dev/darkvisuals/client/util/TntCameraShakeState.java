package dev.darkvisuals.client.util;

 public final class TntCameraShakeState {

    private TntCameraShakeState() {}

    private static volatile long startMs = 0L;
    private static volatile long durationMs = 0L;
    private static volatile float magnitudeDeg = 0f;

     public static void trigger(float magnitudeDegrees, long durationMillis) {
        if (magnitudeDegrees <= 0f || durationMillis <= 0L) return;
         
         
        float currentRemaining = getRemainingMagnitude();
        if (magnitudeDegrees >= currentRemaining) {
            startMs = System.currentTimeMillis();
            durationMs = durationMillis;
            magnitudeDeg = magnitudeDegrees;
        }
    }

    private static float getRemainingMagnitude() {
        if (magnitudeDeg <= 0f) return 0f;
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed >= durationMs) return 0f;
        float t = elapsed / (float) durationMs;
        return magnitudeDeg * (1f - t);
    }

     public static float[] getOffset() {
        if (magnitudeDeg <= 0f) return ZERO;
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed >= durationMs) return ZERO;

        float t = elapsed / (float) durationMs;
         
        float envelope = magnitudeDeg * (1f - t) * (1f - t);

        double seconds = elapsed / 1000.0;
        double yaw = Math.sin(seconds * 2 * Math.PI * 11.0) * envelope;
        double pitch = Math.cos(seconds * 2 * Math.PI * 7.5) * envelope * 0.6;

        return new float[]{(float) yaw, (float) pitch};
    }

    private static final float[] ZERO = {0f, 0f};
}
