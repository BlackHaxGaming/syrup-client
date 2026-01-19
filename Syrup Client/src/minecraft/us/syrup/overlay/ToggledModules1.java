package us.syrup.overlay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import us.syrup.Syrup;
import us.syrup.event.EventListener;
import us.syrup.event.events.Render2DEvent;
import us.syrup.module.Category;
import us.syrup.module.Module;
import us.syrup.utils.RenderUtils;
import us.syrup.utils.Strings;
import net.minecraft.client.Minecraft;

/**
 * Purple toggled-modules overlay.
 *
 * Fixes (v2):
 * - ✅ No vertical overlap between active entries (active entries SNAP to slots)
 * - ✅ Fast re-stack (list closes immediately)
 * - ✅ Leaving entry still slides out (left -> right) but SLOWER
 * - Keeps: per-entry stripe, no gaps, box+text slide together
 */
public class ToggledModules1 extends EventListener {

	// ---- layout ----
	private static final int MARGIN_RIGHT = 0;
	private static final int MARGIN_TOP = 4;

	private static final int PAD_X = 6;
	private static final int PAD_Y = 3;

	private static final int STRIPE_W = 2;
	private static final int STRIPE_GAP = 5;

	// ---- colors (purple theme) ----
	private static final int PLATE_COLOR = new Color(60, 34, 98, 125).getRGB();
	private static final int STRIPE_COLOR = new Color(196, 150, 255, 190).getRGB();

	// Text shimmer palette
	private static final int PURPLE_A = new Color(168, 98, 255).getRGB();
	private static final int PURPLE_B = new Color(255, 92, 244).getRGB();
	private static final int PURPLE_C = new Color(196, 150, 255).getRGB();

	// ---- animation ----
	private static final float SLIDE_IN_SPEED = 4.5f;
	private static final float SLIDE_OUT_SPEED = 1.6f;
	private static final float FADE_IN_SPEED = 6.0f;
	private static final float FADE_OUT_SPEED = 2.1f;

	private final Map<Module, EntryAnim> anims = new HashMap<Module, EntryAnim>();
	private long lastFrameNs = System.nanoTime();

	public ToggledModules1() {
		Syrup.instance.getEventManager().registerListener(this);
	}

	@Override
	public void onRender2D(Render2DEvent event) {
		// ---- dt ----
		long now = System.nanoTime();
		float dt = (now - lastFrameNs) / 1_000_000_000.0f;
		lastFrameNs = now;
		if (dt < 0f) dt = 0f;
		if (dt > 0.033f) dt = 0.033f;

		// ---- collect + filter ----
		List<Module> raw = Syrup.instance.getModuleManager().getToggledModules();
		List<Module> modules = new ArrayList<Module>();
		for (int i = 0; i < raw.size(); i++) {
			Module m = raw.get(i);
			if (m == null) continue;
			if (m.getCategory().equals(Category.HIDDEN)) continue;
			if (!m.isShownInModuleArrayList()) continue;
			modules.add(m);
		}

		// ---- sort by display width (desc) ----
		modules.sort((m1, m2) -> {
			int w2 = Strings.getStringWidthCFR(getDisplayString(m2));
			int w1 = Strings.getStringWidthCFR(getDisplayString(m1));
			return w2 - w1;
		});

		// mark all not present first
		for (EntryAnim a : anims.values()) a.present = false;

		// ---- sizes ----
		int fontH = Syrup.instance.getFontRenderer().getFontSize() / 2;
		int lineH = fontH + 2;
		int plateH = lineH + (PAD_Y * 2);

		// ---- assign targets / create anims ----
		// Only active modules reserve slots -> list closes immediately
		int yCursor = MARGIN_TOP;
		for (int i = 0; i < modules.size(); i++) {
			Module m = modules.get(i);

			EntryAnim a = anims.get(m);
			if (a == null) {
				a = new EntryAnim();
				a.anim = 0.0f;
				a.alpha = 0.0f;
				a.y = yCursor;
				anims.put(m, a);
			}

			a.present = true;
			a.targetY = yCursor;
			a.display = getDisplayParts(m);

			// ✅ SNAP actives to slots (no overlap artifacts)
			a.y = a.targetY;

			yCursor += plateH;
		}

		// ---- animate ----
		List<Module> toRemove = new ArrayList<Module>();
		for (Map.Entry<Module, EntryAnim> e : anims.entrySet()) {
			EntryAnim a = e.getValue();

			float target = a.present ? 1.0f : 0.0f;
			float slideSpeed = a.present ? SLIDE_IN_SPEED : SLIDE_OUT_SPEED;
			float fadeSpeed = a.present ? FADE_IN_SPEED : FADE_OUT_SPEED;

			a.anim = approachExp(a.anim, target, slideSpeed, dt);
			a.alpha = approachExp(a.alpha, target, fadeSpeed, dt);

			// Leaving entries keep their last y while sliding out
			if (!a.present && a.alpha < 0.02f && a.anim < 0.02f) {
				toRemove.add(e.getKey());
			}
		}
		for (int i = 0; i < toRemove.size(); i++) anims.remove(toRemove.get(i));

		float t = (System.currentTimeMillis() % 1_000_000L) / 1000.0f;

		// ---- render ----
		Minecraft mc = Minecraft.getMinecraft();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(0, 0, mc.displayWidth, mc.displayHeight);

		// Leaving first, present last (present stays on top -> no visual overlap while restacking)
		List<Map.Entry<Module, EntryAnim>> renderEntries = new ArrayList<Map.Entry<Module, EntryAnim>>(anims.entrySet());
		renderEntries.sort((e1, e2) -> {
			EntryAnim a1 = e1.getValue();
			EntryAnim a2 = e2.getValue();
			if (a1.present != a2.present) return a1.present ? 1 : -1;
			return Float.compare(a1.y, a2.y);
		});

		for (int i = 0; i < renderEntries.size(); i++) {
			Module m = renderEntries.get(i).getKey();
			EntryAnim a = renderEntries.get(i).getValue();
			if (a == null) continue;

			int alpha = (int) (255.0f * clamp01(a.alpha));
			if (alpha <= 3) continue;

			float eased;
			if (a.present) {
				eased = easeOutCubic(a.anim);          // 0..1
			} else {
				eased = easeOutCubic(1.0f - a.anim);   // 0..1 while leaving
			}

			String name = a.display != null ? a.display.name : Strings.capitalizeFirstLetter(m.getName());
			String suffix = a.display != null ? a.display.suffix : buildSuffix(m);

			int nameW = Strings.getStringWidthCFR(name);
			int suffixW = suffix.length() > 0 ? Strings.getStringWidthCFR(suffix) : 0;
			int textW = nameW + suffixW;

			int plateW = PAD_X + STRIPE_W + STRIPE_GAP + textW + PAD_X;

			// slide background + text together
			int targetRight = event.getWidth() - MARGIN_RIGHT + 2;
			float slideDist = plateW + 40.0f;

			int right;
			if (a.present) {
				right = (int) (targetRight + (1.0f - eased) * slideDist); // right->left
			} else {
				right = (int) (targetRight + eased * slideDist);          // left->right
			}
			int left = right - plateW;

			int y = (int) a.y;
			int y1 = y - PAD_Y;
			int y2 = y + lineH + PAD_Y;

			int plateA = (PLATE_COLOR >>> 24) & 0xFF;
			int plateCol = withAlpha(PLATE_COLOR, (int) (plateA * (alpha / 255.0f)));
			RenderUtils.drawRect(left, y1, right, y2, plateCol);

			int stripeX1 = left + PAD_X;
			int stripeX2 = stripeX1 + STRIPE_W;
			int stripeCol = withAlpha(STRIPE_COLOR, (int) (alpha * 0.9f));
			RenderUtils.drawRect(stripeX1, y1 + 1, stripeX2, y2 - 1, stripeCol);

			int textX = stripeX2 + STRIPE_GAP;

			drawGradientShimmerText(name, textX, y, alpha, t);

			if (suffix.length() > 0) {
				int sx = textX + nameW;
				int base = lerpColor(PURPLE_C, PURPLE_A, 0.35f);
				RenderUtils.drawString(suffix, sx, y, withAlpha(base, alpha));
			}
		}

		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	// ---- display helpers ----

	private static String getDisplayString(Module m) {
		DisplayParts p = getDisplayParts(m);
		return p.name + p.suffix;
	}

	private static DisplayParts getDisplayParts(Module m) {
		DisplayParts p = new DisplayParts();
		p.name = Strings.capitalizeFirstLetter(m.getName());
		p.suffix = buildSuffix(m);
		return p;
	}

	private static String buildSuffix(Module m) {
		try {
			Object ac = m.getAntiCheat();
			if (ac == null) return "";
			String s = ac.toString();
			if (s.equalsIgnoreCase("VANILLA")) return "";
			return " - " + s.toUpperCase();
		} catch (Throwable t) {
			return "";
		}
	}

	// ---- shimmer ----

	private static void drawGradientShimmerText(String text, int x, int y, int alpha, float timeSec) {
		int cx = x;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			String s = String.valueOf(ch);

			float phase = (timeSec * 2.2f) + (i * 0.35f);
			float baseT = (float) ((Math.sin(phase) + 1.0) * 0.5);
			int base = lerpColor(PURPLE_A, PURPLE_B, baseT);

			float shimmerPhase = (timeSec * 3.0f) - (i * 0.55f);
			float shimmer = (float) ((Math.sin(shimmerPhase) + 1.0) * 0.5);
			shimmer = (float) Math.pow(shimmer, 3.2);
			int col = lerpColor(base, PURPLE_C, shimmer);

			RenderUtils.drawString(s, cx, y, withAlpha(col, alpha));
			cx += Strings.getStringWidthCFR(s);
		}
	}

	// ---- math ----

	private static float clamp01(float v) {
		if (v < 0f) return 0f;
		if (v > 1f) return 1f;
		return v;
	}

	private static float approachExp(float current, float target, float speed, float dt) {
		float k = 1.0f - (float) Math.exp(-speed * dt);
		return current + (target - current) * clamp01(k);
	}

	private static float easeOutCubic(float t) {
		t = clamp01(t);
		float p = 1.0f - t;
		return 1.0f - (p * p * p);
	}

	private static int withAlpha(int rgb, int a) {
		if (a < 0) a = 0;
		if (a > 255) a = 255;
		return (a << 24) | (rgb & 0x00FFFFFF);
	}

	private static int lerpColor(int c1, int c2, float t) {
		t = clamp01(t);
		int r1 = (c1 >> 16) & 0xFF;
		int g1 = (c1 >> 8) & 0xFF;
		int b1 = (c1) & 0xFF;

		int r2 = (c2 >> 16) & 0xFF;
		int g2 = (c2 >> 8) & 0xFF;
		int b2 = (c2) & 0xFF;

		int r = (int) (r1 + (r2 - r1) * t);
		int g = (int) (g1 + (g2 - g1) * t);
		int b = (int) (b1 + (b2 - b1) * t);

		return (r << 16) | (g << 8) | b;
	}

	// ---- structs ----

	private static class DisplayParts {
		String name;
		String suffix;
	}

	private static class EntryAnim {
		boolean present;
		float anim;
		float alpha;
		float y;
		float targetY;
		DisplayParts display;
	}
}
