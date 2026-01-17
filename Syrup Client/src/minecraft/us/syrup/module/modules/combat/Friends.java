package us.syrup.module.modules.combat;

import us.syrup.Syrup;
import us.syrup.event.events.PacketSentEvent;
import us.syrup.module.Category;
import us.syrup.module.Module;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;

public class Friends extends Module {

	public Friends() {
		super("Friends", "This mod enables friends. When you hit a friend, the event will be cancelled", 0, Category.COMBAT, false);
	}

	@Override
	public void setup() {
		setToggled(true);
	}

	@Override
	public void onPacketSent(PacketSentEvent event) {
		if (!(event.getPacket() instanceof C02PacketUseEntity))
			return;

		C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();

		if (!(packet.getAction().equals(Action.ATTACK)))
			return;

		Entity entity = packet.getEntityFromWorld(mc.theWorld);

		if (!(entity instanceof EntityOtherPlayerMP || entity instanceof EntityPlayerMP))
			return;

		if (Syrup.instance.getFriendsManager().isFriend(entity.getName()))
			event.setCancelled(true);
	}

}