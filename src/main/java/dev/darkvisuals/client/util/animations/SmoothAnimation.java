package dev.darkvisuals.client.util.animations;

 
public class SmoothAnimation {

    private float value;
    private float target;
    private long lastUpdateMs;
      
    private final float speed;

    public SmoothAnimation(float initialValue, float speed) {
        this.value = initialValue;
        this.target = initialValue;
        this.speed = Math.max(0.01f, speed);
        this.lastUpdateMs = System.currentTimeMillis();
    }

    public void setTarget(float target) {
        this.target = target;
    }

      
    public void snapTo(float v) {
        this.value = v;
        this.target = v;
        this.lastUpdateMs = System.currentTimeMillis();
    }

 
    public float update() {
        long now = System.currentTimeMillis();
        float dt = (now - lastUpdateMs) / 1000f;
        lastUpdateMs = now;
         
        if (dt > 0.1f) dt = 0.1f;
        if (dt > 0f) {
            float k = 1f - (float) Math.exp(-speed * dt);
            value += (target - value) * k;
            if (Math.abs(target - value) < 0.0015f) value = target;
        }
        return value;
    }

    public float getValue() {
        return value;
    }

    public float getTarget() {
        return target;
    }

    public boolean finished() {
        return value == target;
    }
}
