package us.syrup.module.modules.movement;

import org.lwjgl.input.Keyboard;

import us.syrup.event.events.UpdateEvent;
import us.syrup.module.AntiCheat;
import us.syrup.module.Category;
import us.syrup.module.Module;

public class Fly extends Module {

	public Fly() {
		super("Fly", "Reach the outer skies!", Keyboard.KEY_F, Category.MOVEMENT, AntiCheat.VANILLA, AntiCheat.AAC);
	}

	private boolean isFlying;
	private boolean allowFlying;

	@Override
	public void setup() {
		moduleSettings.addDefault("speed", 6.0D, 0.5D, 10.0D, 0.1D);
		moduleSettings.addDefault("TestBoolean", false);
	}

	@Override
	public void onEnable() {
		this.isFlying = mc.thePlayer.capabilities.isFlying;
		this.allowFlying = mc.thePlayer.capabilities.allowFlying;

		mc.thePlayer.capabilities.allowFlying = true;
	}

	@Override
	public void onDisable() {
		mc.thePlayer.capabilities.allowFlying = allowFlying;
		mc.thePlayer.capabilities.isFlying = isFlying;
		mc.timer.timerSpeed = 1.0F;
	}

	@Override
	public void onUpdate(UpdateEvent event) {
		double speed = moduleSettings.getDouble("speed");

		mc.timer.timerSpeed = (float) speed;

		mc.thePlayer.capabilities.isFlying = true;
	}

}