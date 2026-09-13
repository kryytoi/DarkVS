package dev.darkvisuals.modules.settings.impl;

import dev.darkvisuals.modules.settings.Setting;
import java.awt.*;
import java.util.function.Supplier;

 public class ColorSetting extends Setting<Integer> {
    private Integer cachedValue; 
    private Runnable onAction;  
    private Runnable onSetVisible;  

    public ColorSetting(String name) {
        super(name, Color.RED.getRGB());  
        this.cachedValue = Color.RED.getRGB();
    }

    public ColorSetting(String name, Integer value) {
        super(name, value);
        this.cachedValue = value;
    }

    public ColorSetting set(Integer value) {
        super.setValue(value);  
        this.cachedValue = value;
        if (onAction != null) {
            onAction.run();  
        }
        return this;  
    }

     
    @Override
    public void setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);  
        if (onSetVisible != null) {
            onSetVisible.run();  
        }
    }

     
    public ColorSetting onAction(Runnable action) {
        this.onAction = action;  
        return this;  
    }

     
    public ColorSetting onSetVisible(Runnable action) {
        this.onSetVisible = action;  
        return this;  
    }

    @Override
    public Integer getValue() {
        if (cachedValue == null) {
            cachedValue = super.getValue();
        }
        return cachedValue;
    }

     public void setColor(Color color) {
        set(color.getRGB());
    }

     public Color getColor() {
        return new Color(getValue(), true);
    }
}