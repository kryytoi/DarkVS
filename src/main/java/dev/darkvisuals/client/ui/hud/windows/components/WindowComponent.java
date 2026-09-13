package dev.darkvisuals.client.ui.hud.windows.components;

import dev.darkvisuals.client.ui.clickgui.components.Component;
import dev.darkvisuals.client.util.animations.Animation;
import lombok.*;

@Getter @Setter
public abstract class WindowComponent extends Component {
	protected Animation animation;

	public WindowComponent(String name) {
		super(name);
	}
}