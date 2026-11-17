package com.xevis.label;

import java.awt.image.BufferedImage;import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.xevis.XevisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * PNG plates pushed from the editor site: hologram plate/beam/pad art.
 * Textures are created on the main thread (via LinkServer scheduling).
 * Must only be touched from the client main/render thread.
 */
@SideOnly(Side.CLIENT)
public final class PngPlates {
  private PngPlates() {
  }

  /** Site renders plate art at 2x: design px = texture px / SITE_SS. */
  public static final float SITE_SS = 2.0f;

  private static final Map<String, DynamicTexture> TEX = new HashMap<String, DynamicTexture>();
  private static final Map<String, ResourceLocation> LOC = new HashMap<String, ResourceLocation>();
  private static final Map<String, int[]> SIZE = new HashMap<String, int[]>();
  private static File dir;

  /**
   * Transient keys (baked foreign nametags): same textures, but never
   * written to config/*.png and never loaded from disk.
   */
  private static final Set<String> TRANSIENT = new HashSet<String>();

  public static void putTransient(String key, BufferedImage img) {
    TRANSIENT.add(key);
    put(key, img);
  }

  public static void clearTransient() {
    for (String k : new ArrayList<String>(TRANSIENT)) {
      drop(k);
    }
  }

  public static int transientCount() {
    return TRANSIENT.size();
  }

  public static void put(String key, BufferedImage img) {
    DynamicTexture old = TEX.remove(key);
    if (old != null) {
      try {
        old.deleteGlTexture();
      } catch (Exception ignored) {
      }
    }
    LOC.remove(key);
    if (img == null) {
      SIZE.remove(key);
      return;
    }
    DynamicTexture dt;
    try {
      dt = new DynamicTexture(img);
    } catch (Exception e) {
      return;
    }
    TEX.put(key, dt);
    SIZE.put(key, new int[] {img.getWidth(), img.getHeight()});
  }

  public static boolean has(String key) {
    return TEX.containsKey(key);
  }

  /** PNG art survives restarts: stored as config/xevis-<key>.png, loaded on boot. */
  public static void init(File d) {
    dir = d;
    if (dir != null && !dir.exists()) {
      dir.mkdirs();
    }
  }

  public static void drop(String key) {
    put(key, null);
    TRANSIENT.remove(key);
    if (dir != null) {
      try {
        new File(dir, "xevis-" + key + ".png").delete();
      } catch (Exception ignored) {
      }
    }
  }

  public static void saveAll() {
    if (dir == null) {
      return;
    }
    for (Map.Entry<String, int[]> e : SIZE.entrySet()) {
      if (TRANSIENT.contains(e.getKey())) {
        continue; // baked nametags are session-only, never persisted
      }
      DynamicTexture dt = TEX.get(e.getKey());
      if (dt == null) {
        continue;
      }
      try {
        int w = e.getValue()[0];
        int h = e.getValue()[1];
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, w, h, dt.getTextureData(), 0, w);
        ImageIO.write(bi, "png", new File(dir, "xevis-" + e.getKey() + ".png"));
      } catch (Exception ignored) {
      }
    }
  }

  public static void loadAll() {
    if (dir == null) {
      return;
    }
    File[] fs;
    try {
      fs = dir.listFiles((d, n) -> n.startsWith("xevis-") && n.endsWith(".png"));
    } catch (Exception e) {
      return;
    }
    if (fs == null) {
      return;
    }
    for (File f : fs) {
      try {
        String n = f.getName();
        String key = n.substring("xevis-".length(), n.length() - 4);
        BufferedImage img = ImageIO.read(f);
        if (img != null) {
          put(key, img);
        }
      } catch (Exception ignored) {
      }
    }
  }

  public static int[] size(String key) {
    return SIZE.get(key);
  }

  public static void bind(String key) {
    DynamicTexture dt = TEX.get(key);
    if (dt == null) {
      return;
    }
    ResourceLocation rl = LOC.get(key);
    if (rl == null) {
      rl = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("xevis-" + key, dt);
      LOC.put(key, rl);
    }
    Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
    GlStateManager.enableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
  }

  /**
   * Full-texture billboard quad in design px, top-left origin (call inside plate transform).
   * Vanilla 1.12 mapping: canvas row 0 (top) is at v=0 — the same convention as
   * GUI textures and FontRenderer through the shared non-mipmap upload path
   * (top-first rows land at v=0). spriteFlipV=true swaps to v=1 (rare drivers),
   * spriteMirrorU mirrors U. Both pushed live from the site.
   */
  public static void drawKey(String key) {
    int[] s = SIZE.get(key);
    if (s == null) {
      return;
    }
    bind(key);
    boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    GlStateManager.disableCull();
    float w = s[0] / SITE_SS;
    float h = s[1] / SITE_SS;
    boolean flipV = XevisConfig.INSTANCE.spriteFlipV;
    boolean mirrorU = XevisConfig.INSTANCE.spriteMirrorU;
    float uL = mirrorU ? 1.0f : 0.0f;
    float uR = mirrorU ? 0.0f : 1.0f;
    float vT = flipV ? 1.0f : 0.0f;
    float vB = flipV ? 0.0f : 1.0f;
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(-w / 2.0, 0.0, 0.0).tex(uL, vT).endVertex();
    buf.pos(w / 2.0, 0.0, 0.0).tex(uR, vT).endVertex();
    buf.pos(w / 2.0, h, 0.0).tex(uR, vB).endVertex();
    buf.pos(-w / 2.0, h, 0.0).tex(uL, vB).endVertex();
    tess.draw();
    if (cull) {
      GlStateManager.enableCull();
    }
  }

  /** Flat glow ellipse on the ground, world units, no billboard. */
  public static void drawPad(Minecraft mc, double x, double y, double z, float size, boolean throughWalls) {
    if (!TEX.containsKey("pad")) {
      return;
    }
    RenderManager rm = mc.getRenderManager();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x - rm.viewerPosX, y - rm.viewerPosY, z - rm.viewerPosZ);
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    boolean depthOff = false;
    if (throughWalls) {
      GlStateManager.disableDepth();
      depthOff = true;
    }
    boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    GlStateManager.disableCull();
    bind("pad");
    float r = size / 2.0f;
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(-r, 0.0, -r).tex(0.0, 0.0).endVertex();
    buf.pos(r, 0.0, -r).tex(1.0, 0.0).endVertex();
    buf.pos(r, 0.0, r).tex(1.0, 1.0).endVertex();
    buf.pos(-r, 0.0, r).tex(0.0, 1.0).endVertex();
    tess.draw();
    if (depthOff) {
      GlStateManager.enableDepth();
    }
    GlStateManager.depthMask(true);
    if (cull) {
      GlStateManager.enableCull();
    }
    GlStateManager.enableLighting();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.popMatrix();
  }
}
