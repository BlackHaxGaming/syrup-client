package us.syrup.module.modules.hud;

import org.lwjgl.input.Keyboard;
import us.syrup.Syrup;
import us.syrup.event.events.UpdateEvent;
import us.syrup.module.Category;
import us.syrup.module.Module;

public class AdvancedTabGui extends Module {

	/**
	 * -98 is the CUSTOM mouse wheel click code
	 */
	public AdvancedTabGui() {
		super("AdvancedTabGui", "Interact with the TabGui in an advanced way", Keyboard.KEY_NONE, Category.HUD);
	}

	@Override
	public void onUpdate(UpdateEvent event) {
		if (!(Syrup.instance.getModuleManager().getModule(TabGui.class).isToggled()))
			toggle();
	}

}