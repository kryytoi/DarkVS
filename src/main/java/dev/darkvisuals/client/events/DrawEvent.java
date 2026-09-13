package dev.darkvisuals.client.events;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

@Getter
public class DrawEvent extends Event {
    private final DrawType drawType;
    private final DrawContext drawContext;      
    private final MatrixStack matrixStack;      
    private final float tickDelta;              

    private DrawEvent(DrawType drawType, DrawContext drawContext, MatrixStack matrixStack, float tickDelta) {
        this.drawType = drawType;
        this.drawContext = drawContext;
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
    }

    public static DrawEvent forHud(DrawContext context, float tickDelta) {
        return new DrawEvent(DrawType.HUD, context, context.getMatrices(), tickDelta);
    }

    public static DrawEvent forWorld(MatrixStack stack) {
        return new DrawEvent(DrawType.WORLD, null, stack, 0.0f);
    }

    public enum DrawType {
        HUD,
        WORLD
    }
}
