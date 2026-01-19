package us.syrup.gui.clickgui.components;

import java.util.ArrayList;
import java.util.List;

import us.syrup.Syrup;
import us.syrup.gui.clickgui.ClickGui;
import us.syrup.gui.clickgui.components.ModuleButton.UpdateAction;
import us.syrup.module.Category;
import us.syrup.module.Module;
import us.syrup.utils.Strings;
import net.minecraft.client.gui.GuiButton;

public class Dropdown {

	private ClickGui clickGui;

	private Category category;

	private int x;
	private int y;

	private int width;
	private int height;

	private int headerHeight;

	private boolean dragging;
	private boolean extended;

	private int fontSize;

	private List<Module> modules;
	private List<ModuleButton> moduleButtons;

	private int dragOffX;
	private int dragOffY;

	public Dropdown(ClickGui clickGui, Category category, int x, int y, boolean extended) {
		this.clickGui = clickGui;
		this.category = category;

		this.x = x;
		this.y = y;

		this.extended = extended;

		this.modules = Syrup.instance.getModuleManager().getModules(category);
		this.moduleButtons = new ArrayList<>();

		modules.sort((module1, module2) ->
				Strings.getStringWidthCFR(Strings.capitalizeFirstLetter(module2.getName())) -
						Strings.getStringWidthCFR(Strings.capitalizeFirstLetter(module1.getName()))
		);

		this.fontSize = Syrup.instance.getFontRenderer().getFontSize() / 2;

		this.width = Strings.getStringWidthCFR(category.name()) + 5;

		updateHeight();

		this.width = width + 12;

		updateButtons(UpdateAction.REPOPULATE);
	}

	public Category getCategory() { return category; }
	public void setCategory(Category category) { this.category = category; }

	public int getX() { return x; }
	public void setX(int x) {
		this.x = x;
		updateHeight();
		updateButtonPositionsOnly();
	}

	public int getY() { return y; }
	public void setY(int y) {
		this.y = y;
		updateHeight();
		updateButtonPositionsOnly();
	}

	public int getWidth() { return width; }
	public void setWidth(int width) { this.width = width; }

	public int getHeight() { return height; }
	public void setHeight(int height) { this.height = height; }

	public int getHeaderHeight() { return headerHeight; }

	public boolean isDragging() { return dragging; }
	public boolean isExtended() { return extended; }

	public void setExtended(boolean extended) {
		this.extended = extended;
		updateHeight();
		syncButtonsToGui();
	}

	public void toggleExtend() {
		this.extended = !extended;
		updateHeight();
		syncButtonsToGui();
	}

	private void updateHeight() {
		this.height = fontSize * (extended ? (modules.size()) : 0) + 6;
		this.headerHeight = fontSize + 6;
	}

	public List<Module> getModules() { return modules; }
	public List<ModuleButton> getModuleButtons() { return moduleButtons; }
	public void setModules(List<Module> modules) { this.modules = modules; }

	public void handleDrag(int mouseX, int mouseY) {
		if (!dragging) return;

		this.x = mouseX - dragOffX;
		this.y = mouseY - dragOffY;

		updateButtonPositionsOnly();
	}

	public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
			this.dragging = true;
			this.dragOffX = mouseX - this.x;
			this.dragOffY = mouseY - this.y;
			return true;
		} else if (mouseButton == 1 && isHovered(mouseX, mouseY)) {
			if (modules.size() > 0) toggleExtend();
			return true;
		}
		return false;
	}

	public boolean mouseReleased(int mouseX, int mouseY, int state) {
		if (state == 0 && dragging) {
			dragging = false;
			return true;
		}
		return false;
	}

	public boolean mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {

		return mouseButton == 0 && dragging;
	}

	public boolean isHovered(int mouseX, int mouseY) {
		return (mouseX >= x) && (mouseX <= x + width) && (mouseY >= y) && (mouseY <= y + fontSize + 5);
	}

	private void updateButtons(UpdateAction action) {
		if (action.equals(UpdateAction.REPOPULATE)) {
			moduleButtons.clear();

			for (int i = 0; i < modules.size(); i++) {
				Module module = modules.get(i);
				int moduleWidth = Strings.getStringWidthCFR(Strings.capitalizeFirstLetter(module.getName()));
				moduleWidth += 6 + 3;

				if (moduleWidth > this.width) this.width = moduleWidth;

				int[] position = ModuleButton.getPosition(this, i);
				this.moduleButtons.add(new ModuleButton(i, position[0], position[1], moduleWidth, fontSize, module, clickGui));
			}

			syncButtonsToGui();
			return;
		}

		if (action.equals(UpdateAction.UPDATE_POSITION)) {
			updateButtonPositionsOnly();
		}
	}

	private void updateButtonPositionsOnly() {
		for (int i = 0; i < moduleButtons.size(); i++) {
			ModuleButton button = moduleButtons.get(i);
			int[] position = ModuleButton.getPosition(this, i);
			button.xPosition = position[0];
			button.yPosition = position[1];
		}
	}

	private void syncButtonsToGui() {
		List<GuiButton> buttonList = new ArrayList<>(clickGui.getButtonList());
		buttonList.removeAll(moduleButtons);
		if (extended) buttonList.addAll(moduleButtons);
		clickGui.setButtonList(buttonList);
	}
}
