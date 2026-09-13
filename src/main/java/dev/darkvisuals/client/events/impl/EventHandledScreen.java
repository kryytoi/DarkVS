package dev.darkvisuals.client.events.impl;

import dev.darkvisuals.client.events.Event;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

@Getter
public class EventHandledScreen extends Event {
	private final DrawContext drawContext;
	private final Slot slotHover;
	private final int backgroundWidth;
	private final int backgroundHeight;
	private final int guiX;
	private final int guiY;

	public EventHandledScreen(DrawContext drawContext, Slot slotHover, int backgroundWidth, int backgroundHeight, int guiX, int guiY) {
		this.drawContext = drawContext;
		this.slotHover = slotHover;
		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
		this.guiX = guiX;
		this.guiY = guiY;
	}
}
