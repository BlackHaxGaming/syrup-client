package us.syrup.module.modules.render;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.lwjgl.input.Keyboard;

import us.syrup.Syrup;
import us.syrup.event.events.BlockBrightnessRequestEvent;
import us.syrup.event.events.BlockRenderEvent;
import us.syrup.event.events.FluidRenderEvent;
import us.syrup.module.Category;
import us.syrup.module.Module;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class XRay extends Module {

	public XRay() {
		super("XRay", "See only specific blocks", Keyboard.KEY_X, Category.RENDER);
	}

	private List<String> exceptions;

	@Override
	public void setup() {
		moduleSettings.addDefault("blocks", Arrays.asList(Blocks.iron_ore.getLocalizedName().toUpperCase().replace(" ", "_")));

		Function<String, String> consumer = line -> line = line.toUpperCase().replace(" ", "_"); // Replaces every line with an uppercase version that has spaces replaced with underscores
		this.exceptions = moduleSettings.getStringList("blocks", consumer);
	}

	@Override
	public void onEnable() {
		mc.renderGlobal.loadRenderers();
	}

	@Override
	public void onDisable() {
		mc.renderGlobal.loadRenderers();
	}

	@Override
	public void onBlockBrightnessRequest(BlockBrightnessRequestEvent event) {
		if (isInExceptions(event.getBlock()))
			event.setBlockBrightness(15);
	}

	@Override
	public void onBlockRender(BlockRenderEvent event) {
		if (!(isInExceptions(event.getBlock())))
			event.setCancelled(true);
		else
			event.setForceDraw(true);
	}

	@Override
	public void onFluidRender(FluidRenderEvent event) {
		if (!(isInExceptions(event.getBlock())))
			event.setCancelled(true);
		else
			event.setForceDraw(true);
	}

	private boolean isInExceptions(Block block) {
		if (exceptions == null)
			return false;

		String unlocalizedName = Syrup.instance.getEnglishLocale().formatMessage(block.getUnlocalizedName() + ".name", null).toUpperCase().replace(" ", "_");

		return exceptions.contains(unlocalizedName) || exceptions.contains(Integer.toString(Block.getIdFromBlock(block)));
	}

	public List<String> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<String> exceptions) {
		this.exceptions = exceptions;
		moduleSettings.set("blocks", exceptions);

		if (isToggled())
			mc.renderGlobal.loadRenderers();
	}

}
