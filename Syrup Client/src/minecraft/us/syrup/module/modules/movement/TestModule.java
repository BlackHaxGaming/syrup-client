package us.syrup.module.modules.movement;

import us.syrup.event.events.PacketReceivedEvent;
import us.syrup.event.events.UpdateEvent;
import us.syrup.module.AntiCheat;
import us.syrup.module.Category;
import us.syrup.module.Module;

public class TestModule extends Module {

	public TestModule() {
		super("TestModule", "This is a test module...", 0, Category.MOVEMENT, AntiCheat.AAC);
	}

	@Override
	public void setup() {
	}

	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onUpdate(UpdateEvent event) {
	}

	@Override
	public void onPacketReceived(PacketReceivedEvent event) {
	}

}