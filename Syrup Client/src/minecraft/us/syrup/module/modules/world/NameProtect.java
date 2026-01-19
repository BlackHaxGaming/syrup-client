package us.syrup.module.modules.world;

import java.util.HashMap;
import java.util.Map;

import us.syrup.Syrup;
import us.syrup.command.commands.NamesCommand;
import us.syrup.event.events.MessageReceivedEvent;
import us.syrup.event.events.PlayerSpawnEvent;
import us.syrup.event.events.RenderLivingLabelEvent;
import us.syrup.module.Category;
import us.syrup.module.Module;
import us.syrup.utils.Strings;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class NameProtect extends Module {

	private Map<String, String> names;

	public NameProtect() {
		super("Name Protect", "Hide players names", 0, Category.WORLD);

		this.names = new HashMap<String, String>();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String message = event.getMessage();

		if (!(names.containsKey(mc.thePlayer.getName())))
			getNewName(mc.thePlayer.getName());

		for (String name : names.keySet()) {
			String newName = getNewName(name);
			message = message.replace(name, newName);
		}

		event.setMessage(message);
	}

	@Override
	public void onRenderLivingLabel(RenderLivingLabelEvent event) {
		if (!(event.getEntity() instanceof EntityOtherPlayerMP))
			return;

		if (event.getEntity() == null || event.getEntity().getName() == null)
			return;

		if (((NamesCommand) Syrup.instance.getCommandManager().getCommand(NamesCommand.class)).isInExceptions(event.getEntity().getName()))
			return;

		event.setLabel(getNewName(event.getEntity().getName()));
	}

	@Override
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		getNewName(event.getEntity().getName());
	}

	public String getNewName(String name) {
		String newName = null;
		if (!(names.containsKey(name))) {
			names.put(name, Strings.randomString(10, true, false, false) + (name.equals(mc.thePlayer.getName()) ? " (DU)" : ""));
		}

		newName = names.get(name);

		return newName;
	}

}