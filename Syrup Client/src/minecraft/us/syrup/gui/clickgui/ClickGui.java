package us.syrup.gui.clickgui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import us.syrup.gui.clickgui.components.Dropdown;
import us.syrup.gui.clickgui.components.ModuleButton;
import us.syrup.gui.clickgui.components.ModuleSettingsParent;
import us.syrup.module.Category;
import us.syrup.module.Module;
import us.syrup.utils.RenderUtils;
import us.syrup.utils.Strings;

/**
 * Purple / premium ClickGUI (v3).
 * - Smoother global fade
 * - Dropdown open/close uses eased scissor clip
 * - Hover anim a bit snappier
 * - Popup: delegated to ModuleSettingsParent (fade/slide/scale). ClickGui clears it reliably.
 */
public class ClickGui extends GuiScreen {

	// Theme
	private static final int DIM_BG       = new Color(0, 0, 0, 78).getRGB();
	private static final int PANEL_BG     = new Color(18, 14, 28, 220).getRGB();
	private static final int CONTENT_BG   = new Color(14, 10, 22, 205).getRGB();
	private static final int SHADOW       = new Color(0, 0, 0, 95).getRGB();

	private static final int HEADER       = new Color(92, 62, 198, 225).getRGB();
	private static final int HEADER_HOVER = new Color(125, 95, 235, 240).getRGB();
	private static final int ACCENT       = new Color(185, 130, 255, 255).getRGB();

	private static final int TEXT         = new Color(245, 245, 250, 255).getRGB();

	private List<Dropdown> dropdowns;

	// popup
	private ModuleSettingsParent popup;

	// anim state
	private final IdentityHashMap<Dropdown, Float> extendAnim = new IdentityHashMap<Dropdown, Float>();
	private final IdentityHashMap<Dropdown, Float> hoverAnim  = new IdentityHashMap<Dropdown, Float>();
	private float guiAlpha = 0f;
	private long lastFrameNanos = System.nanoTime();

	@Override
	public void initGui() {
		guiAlpha = 0f;
		lastFrameNanos = System.nanoTime();

		this.dropdowns = new ArrayList<Dropdown>();

		Dropdown previous = null;
		for (Category c : Category.values()) {
			if (c.equals(Category.HIDDEN)) continue;

			int x = 10 + (previous == null ? 0 : 14 + previous.getX() + previous.getWidth());
			int y = (previous == null ? 12 : previous.getY());

			Dropdown d = new Dropdown(this, c, x, y, false);

			if (x + d.getWidth() > RenderUtils.getScaledResolution().getScaledWidth() && previous != null) {
				d.setX(10);
				d.setY(previous.getY() + previous.getHeight() + 34);
			}

			dropdowns.add(d);
			previous = d;

			extendAnim.put(d, d.isExtended() ? 1f : 0f);
			hoverAnim.put(d, 0f);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		float dt = frameDeltaSeconds();
		guiAlpha = approach(guiAlpha, 1f, 12f, dt);
		float gEase = easeOutCubic(guiAlpha);

		// drag update
		for (int i = dropdowns.size() - 1; i >= 0; i--) {
			dropdowns.get(i).handleDrag(mouseX, mouseY);
		}

		// background dim
		RenderUtils.drawModalRectFromTopLeft(0, 0, this.width, this.height, alpha(DIM_BG, gEase));

		// dropdowns
		for (int i = 0; i < dropdowns.size(); i++) {
			Dropdown d = dropdowns.get(i);

			float targetOpen = d.isExtended() ? 1f : 0f;
			float open = extendAnim.containsKey(d) ? extendAnim.get(d) : targetOpen;
			open = approach(open, targetOpen, 15f, dt);
			extendAnim.put(d, open);
			float oEase = easeOutCubic(open);

			boolean hoverHeader = isIn(mouseX, mouseY, d.getX(), d.getY(), d.getWidth(), d.getHeaderHeight());
			float targetHover = hoverHeader ? 1f : 0f;
			float h = hoverAnim.containsKey(d) ? hoverAnim.get(d) : 0f;
			h = approach(h, targetHover, 22f, dt);
			hoverAnim.put(d, h);

			int x = d.getX();
			int y = d.getY();
			int w = d.getWidth();
			int headerH = d.getHeaderHeight();

			int fontH = Math.max(1, headerH - 6);
			int count = (d.getModules() == null ? 0 : d.getModules().size());
			int fullContentH = (count > 0 ? (fontH * count + 6) : 0);
			int contentH = (int) (fullContentH * oEase);

			// shadow + base
			RenderUtils.drawModalRectFromTopLeft(x + 2, y + 2, w, headerH + contentH, alpha(SHADOW, gEase));
			RenderUtils.drawModalRectFromTopLeft(x, y, w, headerH + contentH, alpha(PANEL_BG, gEase));

			// header
			int headerCol = lerpColor(HEADER, HEADER_HOVER, h);
			RenderUtils.drawModalRectFromTopLeft(x, y, w, headerH, alpha(headerCol, gEase));

			// accent
			RenderUtils.drawModalRectFromTopLeft(x, y, 2, headerH + contentH, alpha(ACCENT, gEase));

			// title
			RenderUtils.drawString(Strings.capitalizeOnlyFirstLetter(d.getCategory().name()), x + 6, y + 2, alpha(TEXT, gEase));

			// content
			if (contentH > 0) {
				RenderUtils.drawModalRectFromTopLeft(x, y + headerH, w, contentH, alpha(CONTENT_BG, gEase));

				beginScissor(x, y + headerH, w, contentH);
				try {
					List<ModuleButton> buttons = d.getModuleButtons();
					if (buttons != null) {
						for (int b = 0; b < buttons.size(); b++) {
							buttons.get(b).drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
						}
					}
				} finally {
					endScissor();
				}

				// top separator
				RenderUtils.drawModalRectFromTopLeft(x + 6, y + headerH, w - 12, 1, alpha(new Color(255, 255, 255, 25).getRGB(), gEase));
			}
		}

		// popup on top (and clear it once fully closed)
		if (popup != null) {
			popup.draw(mouseX, mouseY);
			if (popup.isClosed()) popup = null;
		}
	}

	// --- Popup positioning: always next to dropdown right edge ---

	public void openPopupNextToDropdown(Module module, Dropdown dropdown, int anchorY) {
		ModuleSettingsParent p = new ModuleSettingsParent(this, module, 0, 0);

		int popupW = p.getWidth();
		int popupH = p.getHeight();

		ScaledResolution sr = new ScaledResolution(mc);
		int sw = sr.getScaledWidth();
		int sh = sr.getScaledHeight();

		int gap = 10;

		int x = dropdown.getX() + dropdown.getWidth() + gap;
		int y = anchorY;

		if (x + popupW > sw) {
			x = dropdown.getX() - popupW - gap;
		}

		if (y + popupH > sh) y = sh - popupH - 6;
		if (y < 6) y = 6;
		if (x < 6) x = 6;

		p.setPos(x, y);
		this.popup = p;
	}

	// Backwards compatible overloads
	public void openPopup(Module module, int anchorX, int anchorY, int anchorW, int anchorH) {
		ModuleSettingsParent p = new ModuleSettingsParent(this, module, 0, 0);
		int popupW = p.getWidth();
		int popupH = p.getHeight();

		ScaledResolution sr = new ScaledResolution(mc);
		int sw = sr.getScaledWidth();
		int sh = sr.getScaledHeight();

		int gap = 10;
		int x = anchorX + anchorW + gap;
		int y = anchorY;

		if (x + popupW > sw) x = anchorX - popupW - gap;
		if (y + popupH > sh) y = sh - popupH - 6;
		if (y < 6) y = 6;
		if (x < 6) x = 6;

		p.setPos(x, y);
		this.popup = p;
	}

	public void openPopup(Module module, int mouseX, int mouseY) {
		openPopup(module, mouseX, mouseY, 0, 0);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {

		// popup consumes clicks (outside closes). Still let it animate out.
		if (popup != null) {
			popup.mouseClicked(mouseX, mouseY, mouseButton);
			return;
		}

		// dropdown headers
		Dropdown clicked = null;
		for (int i = dropdowns.size() - 1; i >= 0; i--) {
			Dropdown d = dropdowns.get(i);
			if (d.mouseClicked(mouseX, mouseY, mouseButton)) {
				clicked = d;
				break;
			}
		}
		if (clicked != null) {
			dropdowns.remove(clicked);
			dropdowns.add(clicked);
			return;
		}

		// module buttons
		for (int i = 0; i < this.buttonList.size(); i++) {
			GuiButton btn = this.buttonList.get(i);
			if (!(btn instanceof ModuleButton)) continue;

			ModuleButton mb = (ModuleButton) btn;

			if (mb.mousePressed(this.mc, mouseX, mouseY, mouseButton)) {

				// right click => open next to dropdown
				if (mouseButton == 1) {
					Module m = mb.getModule();
					if (m != null) {
						Dropdown parent = null;
						for (int di = 0; di < dropdowns.size(); di++) {
							Dropdown d = dropdowns.get(di);
							if (d.getModuleButtons().contains(mb)) {
								parent = d;
								break;
							}
						}
						if (parent != null) {
							openPopupNextToDropdown(m, parent, mb.yPosition);
						}
					}
					return;
				}

				// left click => normal flow
				selectedButton = mb;
				this.actionPerformed(mb);
				return;
			}
		}

		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (popup != null) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				popup.closePopup();
				return;
			}
			if (popup.keyTyped(typedChar, keyCode)) return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		for (int i = dropdowns.size() - 1; i >= 0; i--) {
			if (dropdowns.get(i).mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) return;
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		for (int i = dropdowns.size() - 1; i >= 0; i--) {
			if (dropdowns.get(i).mouseReleased(mouseX, mouseY, state)) return;
		}
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		// optional
	}

	public List<GuiButton> getButtonList() {
		return buttonList;
	}

	public void setButtonList(List<GuiButton> buttonList) {
		this.buttonList = buttonList;
	}

	// --- helpers ---

	private float frameDeltaSeconds() {
		long now = System.nanoTime();
		long diff = now - lastFrameNanos;
		lastFrameNanos = now;
		float dt = diff / 1_000_000_000f;
		if (dt < 0f) dt = 0f;
		if (dt > 0.05f) dt = 0.05f;
		return dt;
	}

	private float approach(float current, float target, float speed, float dt) {
		float t = speed * dt;
		if (t > 1f) t = 1f;
		return current + (target - current) * t;
	}

	private float easeOutCubic(float t) {
		float u = 1f - t;
		return 1f - u * u * u;
	}

	private boolean isIn(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}

	private int alpha(int argb, float mul) {
		int a = (argb >> 24) & 0xFF;
		a = (int) (a * mul);
		if (a < 0) a = 0;
		if (a > 255) a = 255;
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	private int lerpColor(int a, int b, float t) {
		if (t < 0f) t = 0f;
		if (t > 1f) t = 1f;
		int aA = (a >> 24) & 0xFF, aR = (a >> 16) & 0xFF, aG = (a >> 8) & 0xFF, aB = a & 0xFF;
		int bA = (b >> 24) & 0xFF, bR = (b >> 16) & 0xFF, bG = (b >> 8) & 0xFF, bB = b & 0xFF;
		int oA = (int) (aA + (bA - aA) * t);
		int oR = (int) (aR + (bR - aR) * t);
		int oG = (int) (aG + (bG - aG) * t);
		int oB = (int) (aB + (bB - aB) * t);
		return (oA << 24) | (oR << 16) | (oG << 8) | oB;
	}

	private void beginScissor(int x, int y, int w, int h) {
		ScaledResolution sr = new ScaledResolution(mc);
		int scale = sr.getScaleFactor();

		int sx = x * scale;
		int sy = (mc.displayHeight - (y + h) * scale);
		int sw = w * scale;
		int sh = h * scale;

		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(sx, sy, sw, sh);
	}

	private void endScissor() {
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}
}
