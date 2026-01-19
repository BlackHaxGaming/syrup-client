package us.syrup.gui.clickgui.components;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import us.syrup.gui.clickgui.ClickGui;
import us.syrup.module.AntiCheat;
import us.syrup.module.Module;
import us.syrup.utils.RenderUtils;

/**
 * Animated settings popup for a Module.
 * Fix: ensures texture/blend state is enabled before drawing any text,
 * so the text can't disappear after drawing rectangles.
 */
public class ModuleSettings {

    // Theme
    private static final int SHADOW   = new Color(0, 0, 0, 90).getRGB();
    private static final int BG       = new Color(18, 14, 28, 235).getRGB();
    private static final int CARD     = new Color(14, 10, 22, 215).getRGB();
    private static final int LINE     = new Color(255, 255, 255, 18).getRGB();
    private static final int ACCENT   = new Color(185, 130, 255, 255).getRGB();
    private static final int TEXT     = new Color(245, 245, 250, 255).getRGB();
    private static final int SUBTEXT  = new Color(175, 175, 190, 255).getRGB();
    private static final int HOVER    = new Color(120, 90, 230, 70).getRGB();
    private static final int SELECTED = new Color(185, 130, 255, 90).getRGB();

    private final ClickGui parent;
    private final Module module;

    private int x;
    private int y;

    private final int pad = 8;
    private final int rowH = 14;
    private int width = 170;

    private boolean closed = false;
    private boolean listeningForKey = false;

    // animation
    private float anim = 0f;        // 0..1
    private float animTarget = 1f;  // 1=open, 0=close
    private long lastNanos = System.nanoTime();

    public ModuleSettings(ClickGui parent, Module module, int x, int y) {
        this.parent = parent;
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = Math.max(170, calcBestWidth());
    }

    // --- public api expected by ClickGui ---

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        int list = module.getAllowedAntiCheats().length;
        int h = 0;
        h += pad;
        h += rowH;
        h += 4;
        h += rowH;
        h += 6;
        h += 1;
        h += 6;
        h += rowH;
        h += list * rowH;
        h += pad;
        return h;
    }

    public boolean isClosed() {
        return closed;
    }

    public void closePopup() {
        animTarget = 0f;
        listeningForKey = false;
    }

    /** @return true if consumed */
    public boolean keyTyped(char typedChar, int keyCode) {
        if (closed) return true;

        if (listeningForKey) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                listeningForKey = false;
                return true;
            }

            if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                module.setKey(0);
                listeningForKey = false;
                return true;
            }

            // ignore modifier keys
            if (keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT ||
                    keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL ||
                    keyCode == Keyboard.KEY_LMENU || keyCode == Keyboard.KEY_RMENU) {
                return true;
            }

            module.setKey(keyCode);
            listeningForKey = false;
            return true;
        }

        return false;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (closed) return;

        int h = getHeight();
        int rx = renderX();
        int ry = renderY();

        boolean inside = mouseX >= rx && mouseX <= rx + width && mouseY >= ry && mouseY <= ry + h;
        if (!inside) {
            closePopup();
            return;
        }

        // left click only
        if (mouseButton != 0) return;

        int curY = ry + pad;

        // Title row
        curY += rowH + 4;

        // Key row
        int keyRowY = curY;
        if (mouseY >= keyRowY && mouseY <= keyRowY + rowH) {
            listeningForKey = true;
            return;
        }
        curY += rowH + 6;

        // separator
        curY += 6;

        // AntiCheat label
        curY += rowH;

        AntiCheat[] list = module.getAllowedAntiCheats();
        for (int i = 0; i < list.length; i++) {
            int itemY = curY + i * rowH;
            if (mouseY >= itemY && mouseY <= itemY + rowH) {
                module.setAntiCheat(list[i]);
                return;
            }
        }
    }

    public void draw(int mouseX, int mouseY) {
        // Force sane GUI state (fixes "popup shows but text invisible" issues)
        beginGuiState();
        try {
            tickAnim();

            if (animTarget == 0f && anim <= 0.02f) {
                closed = true;
                return;
            }

            int rx = renderX();
            int ry = renderY();
            int h = getHeight();

            float a = clamp01(anim);

            // shadow + base
            RenderUtils.drawModalRectFromTopLeft(rx + 2, ry + 2, width, h, alpha(SHADOW, a));
            RenderUtils.drawModalRectFromTopLeft(rx, ry, width, h, alpha(BG, a));
            RenderUtils.drawModalRectFromTopLeft(rx, ry, 2, h, alpha(ACCENT, a));
            RenderUtils.drawModalRectFromTopLeft(rx + 3, ry + 3, width - 6, h - 6, alpha(CARD, a));

            int curY = ry + pad;
            int textX = rx + pad;

            // IMPORTANT: some RenderUtils implementations break vanilla font rendering.
            // We draw text through RenderUtils too (same path as the dropdown text), so it always appears.

            // Title
            if (a > 0.06f) drawText(module.getName(), textX, curY, alpha(TEXT, a), true);
            curY += rowH + 4;

            // Key row
            int keyRowY = curY;
            boolean hoverKey = mouseX >= rx && mouseX <= rx + width && mouseY >= keyRowY && mouseY <= keyRowY + rowH;
            if (hoverKey) {
                RenderUtils.drawModalRectFromTopLeft(rx + 3, keyRowY - 1, width - 6, rowH + 2, alpha(HOVER, a));
            }

            String keyName = listeningForKey ? "..." : (module.getKey() == 0 ? "NONE" : Keyboard.getKeyName(module.getKey()));
            if (a > 0.06f) drawText("Key: " + keyName, textX, keyRowY, alpha(SUBTEXT, a), false);
            curY += rowH + 6;

            // separator
            RenderUtils.drawModalRectFromTopLeft(rx + 6, curY, width - 12, 1, alpha(LINE, a));
            curY += 6;

            // AntiCheat label
            if (a > 0.06f) drawText("AntiCheat:", textX, curY, alpha(TEXT, a), true);
            curY += rowH;

            AntiCheat[] list = module.getAllowedAntiCheats();
            for (int i = 0; i < list.length; i++) {
                AntiCheat ac = list[i];
                int itemY = curY + i * rowH;
                boolean hover = mouseX >= rx && mouseX <= rx + width && mouseY >= itemY && mouseY <= itemY + rowH;
                boolean selected = ac.equals(module.getAntiCheat());

                if (selected) {
                    RenderUtils.drawModalRectFromTopLeft(rx + 3, itemY - 1, width - 6, rowH + 2, alpha(SELECTED, a));
                    RenderUtils.drawModalRectFromTopLeft(rx + 3, itemY - 1, 2, rowH + 2, alpha(ACCENT, a));
                } else if (hover) {
                    RenderUtils.drawModalRectFromTopLeft(rx + 3, itemY - 1, width - 6, rowH + 2, alpha(HOVER, a));
                }

                if (a > 0.06f) drawText(capitalizeFirstOnly(ac.name()), textX, itemY, alpha(SUBTEXT, a), false);
            }
        } finally {
            endGuiState();
        }
    }

    // --- internals ---

    private int calcBestWidth() {
        int w = fontWidth(module.getName()) + pad * 2;

        String keyLine = "Key: " + (module.getKey() == 0 ? "NONE" : Keyboard.getKeyName(module.getKey()));
        w = Math.max(w, fontWidth(keyLine) + pad * 2);

        w = Math.max(w, fontWidth("AntiCheat:") + pad * 2);

        for (AntiCheat ac : module.getAllowedAntiCheats()) {
            w = Math.max(w, fontWidth(capitalizeFirstOnly(ac.name())) + pad * 2);
        }

        return w + 18;
    }

    private int fontWidth(String s) {
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(s);
    }

    private void tickAnim() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        if (dt < 0f) dt = 0f;
        if (dt > 0.05f) dt = 0.05f;

        float speed = (animTarget > anim) ? 16f : 18f;
        float t = speed * dt;
        if (t > 1f) t = 1f;
        anim = anim + (animTarget - anim) * t;
    }

    private int renderX() {
        int slide = (int) ((1f - clamp01(anim)) * 10f);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int sw = sr.getScaledWidth();

        int rx = x + slide;
        if (rx + width > sw - 2) rx = (sw - width - 2);
        if (rx < 2) rx = 2;
        return rx;
    }

    private int renderY() {
        int slideY = (int) ((1f - clamp01(anim)) * 4f);
        return y + slideY;
    }

    private int alpha(int argb, float mul) {
        int a = (argb >> 24) & 0xFF;
        a = (int) (a * mul);
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private void ensureTextState() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Hard reset of GL state for 2D GUI.
     * Some RenderUtils / shader / scissor state can break font rendering.
     */
    private void beginGuiState() {
        GlStateManager.pushMatrix();
        // Scissor can accidentally stay enabled from other renders
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void endGuiState() {
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    private void drawText(String s, int x, int y, int color, boolean shadow) {
        // Use the same text pipeline as the ClickGui dropdowns.
        // This avoids cases where vanilla FontRenderer stops drawing due to GL state changes.
        ensureTextState();

        // Many custom font renderers ignore alpha; keep RGB but clamp alpha to avoid "ghost".
        int a = (color >> 24) & 0xFF;
        if (a <= 8) return; // almost invisible anyway

        int rgb = (color & 0x00FFFFFF);
        int solid = (0xFF000000 | rgb);

        if (shadow) {
            RenderUtils.drawString(s, x + 1, y + 1, 0xFF000000);
        }
        RenderUtils.drawString(s, x, y, solid);
    }

    private String capitalizeFirstOnly(String s) {
        if (s == null || s.isEmpty()) return s;
        String lower = s.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
