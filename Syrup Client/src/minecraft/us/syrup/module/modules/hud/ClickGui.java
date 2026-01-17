package us.syrup.module.modules.hud;

import org.lwjgl.input.Keyboard;

import us.syrup.Syrup;
import us.syrup.module.Category;
import us.syrup.module.Module;

public class ClickGui extends Module {

	public ClickGui() {
		super("ClickGui", "This is the ClickGui", Keyboard.KEY_RSHIFT, Category.HUD, false);
	}

	@Override
	public void onEnable() {
		mc.displayGuiScreen(Syrup.instance.getClickGui());
		super.toggle();
	}

}