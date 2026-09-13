package dev.darkvisuals.client.util.math;

import dev.darkvisuals.client.util.Wrapper;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;

@UtilityClass
public class Counter implements Wrapper {

    @Getter private int currentFPS;

    public void updateFPS() {
        int prevFPS = mc.getCurrentFps();
        currentFPS = MathHelper.lerp(0.5f, prevFPS, currentFPS);
    }
}