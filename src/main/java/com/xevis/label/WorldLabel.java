package com.xevis.label;

import com.xevis.XevisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Billboarded world-space label plates: nametags + holograms. */
@SideOnly(Side.CLIENT)
public final class WorldLabel {
  private WorldLabel() {
  }

  public static final class Seg {
    public final String text;
    public final int color;
    public final boolean bold;

    public Seg(String text, int color, boolean bold) {
      this.text = text == null ? "" : text;
      this.color = color;
      this.bold = bold;
    }
  }

  /** Draws a centered multi-segment plate floating at a world position. */
  public static void drawSegPlate(Minecraft mc, double x, double y, double z,
      List<List<Seg>> lines, float scale, int bgArgb, boolean seeThrough, int glowArgb,
      boolean textShadow) {
    if (lines == null || lines.isEmpty()) {
      return;
    }
    FontRenderer fr = mc.fontRenderer;
    RenderManager rm = mc.getRenderManager();
    float lh = fr.FONT_HEIGHT + 1;
    float totalH = lines.size() * lh;
    float maxW = 0.0f;
    for (List<Seg> line : lines) {
      float w = 0.0f;
      for (Seg s : line) {
        w += fr.getStringWidth(s.text);
      }
      if (w > maxW) {
        maxW = w;
      }
    }

    GlStateManager.pushMatrix();
    GlStateManager.translate(x - rm.viewerPosX, y - rm.viewerPosY, z - rm.viewerPosZ);
    GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
    GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
    float s = -0.025f * scale;
    GlStateManager.scale(s, s, s);
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    boolean depthOff = false;
    if (seeThrough) {
      GlStateManager.disableDepth();
      depthOff = true;
    }
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    try {
      if (bgArgb != 0) {
        float pad = 4.0f;
        float x0 = -maxW / 2.0f - pad;
        float x1 = maxW / 2.0f + pad;
        float y0 = -pad;
        float y1 = totalH - 1.0f + pad;
        float a = ((bgArgb >>> 24) & 0xFF) / 255.0f;
        float r = ((bgArgb >>> 16) & 0xFF) / 255.0f;
        float g = ((bgArgb >>> 8) & 0xFF) / 255.0f;
        float b = (bgArgb & 0xFF) / 255.0f;
        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(x0, y1, 0.0).color(r, g, b, a).endVertex();
        buf.pos(x1, y1, 0.0).color(r, g, b, a).endVertex();
        buf.pos(x1, y0, 0.0).color(r, g, b, a).endVertex();
        buf.pos(x0, y0, 0.0).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
      }

      float yy = 0.0f;
      for (List<Seg> line : lines) {
        float w = 0.0f;
        for (Seg sg : line) {
          w += fr.getStringWidth(sg.text);
        }
        float xx = -w / 2.0f;
        for (Seg sg : line) {
          if (glowArgb != 0) {
            fr.drawString(sg.text, xx, yy + 1.2f, glowArgb, false);
          }
          fr.drawString(sg.text, xx, yy, sg.color, textShadow);
          if (sg.bold) {
            fr.drawString(sg.text, xx + 0.6f, yy, sg.color, false);
          }
          xx += fr.getStringWidth(sg.text);
        }
        yy += lh;
      }
    } finally {
      if (depthOff) {
        GlStateManager.enableDepth();
      }
      GlStateManager.depthMask(true);
      GlStateManager.enableLighting();
      GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
      GlStateManager.popMatrix();
    }
  }

  /** Styled player nametag: prefix + nick + clan, optional free second line. No plate. */
  public static void drawPlayer(Minecraft mc, EntityPlayer player) {
    XevisConfig c = XevisConfig.INSTANCE;
    boolean self = player == mc.player;
    String pre;
    String clan;
    String sub2 = null;
    String preC = c.preColor;
    String nickC = c.nickColor;
    String sufC = c.sufColor;
    String subC = c.subColor;
    if (self) {
      pre = c.prefix;
      clan = c.clan;
      if (c.showSub && c.sub != null && !c.sub.isEmpty()) {
        sub2 = c.sub;
      }
    } else {
      // foreign players (Blockbuster actors included): per-name override wins,
      // otherwise the shared otherPre/otherClan defaults
      pre = c.otherPre;
      clan = c.otherClan;
      if (c.players != null) {
        XevisConfig.PlayerTag tag = c.players.get(player.getName());
        if (tag != null) {
          if (tag.prefix != null && !tag.prefix.isEmpty()) {
            pre = tag.prefix;
          }
          if (tag.clan != null && !tag.clan.isEmpty()) {
            clan = tag.clan;
          }
          if (tag.sub != null && !tag.sub.isEmpty()) {
            sub2 = tag.sub;
          }
          if (tag.preColor != null && !tag.preColor.isEmpty()) {
            preC = tag.preColor;
          }
          if (tag.nickColor != null && !tag.nickColor.isEmpty()) {
            nickC = tag.nickColor;
          }
          if (tag.sufColor != null && !tag.sufColor.isEmpty()) {
            sufC = tag.sufColor;
          }
          if (tag.subColor != null && !tag.subColor.isEmpty()) {
            subC = tag.subColor;
          }
        }
      }
    }
    List<Seg> first = new ArrayList<Seg>();
    if (pre != null && !pre.isEmpty()) {
      first.add(new Seg(pre + " ", XevisConfig.argb(preC), false));
    }
    first.add(new Seg(player.getName(), XevisConfig.argb(nickC), true));
    if (clan != null && !clan.isEmpty()) {
      first.add(new Seg(" " + clan, XevisConfig.argb(sufC), false));
    }
    List<List<Seg>> lines = new ArrayList<List<Seg>>();
    lines.add(first);
    if (c.showSub && sub2 != null && !sub2.isEmpty()) {
      List<Seg> second = new ArrayList<Seg>();
      second.add(new Seg(sub2, XevisConfig.argb(subC), false));
      lines.add(second);
    }
    double y = player.posY + player.height + 0.32;
    if (!c.nickPng) {
      drawMcPlate(mc, player, y, lines, c);
      return;
    }
    if (self && PngPlates.has("nick0")) {
      drawNickPlate(mc, player, y, "nick0", c, nativeSize(mc, lines));
      return;
    }
    if (!self) {
      String bk = bakedKey(player.getName(), c);
      if (!PngPlates.has(bk)) {
        bakeNickSprite(player.getName());
      }
      if (PngPlates.has(bk)) {
        drawNickPlate(mc, player, y, bk, c, nativeSize(mc, lines));
        return;
      }
    }
    drawSegPlate(mc, player.posX, y, player.posZ, lines,
        c.labelScale, 0, false, 0, c.labelShadow);
  }

  /** Minecraft-font plate honoring the site's plate/shadow settings. */
  private static void drawMcPlate(Minecraft mc, EntityPlayer player, double y,
      List<List<Seg>> lines, XevisConfig c) {
    boolean light = "light".equals(c.labelBg);
    int bg = light ? 0xFFF4F4F5 : ("dark".equals(c.labelBg) ? 0xAD000000 : 0);
    List<List<Seg>> use = lines;
    if (light) {
      // mirror the site preview: dark ink on the light plate
      use = new ArrayList<List<Seg>>();
      for (int li = 0; li < lines.size(); li++) {
        List<Seg> row = new ArrayList<Seg>();
        for (int si = 0; si < lines.get(li).size(); si++) {
          Seg s = lines.get(li).get(si);
          int col = 0xFF09090B;
          if ((li == 0 && si == 0) || li == 1) {
            col = 0xFF6D28D9;
          }
          row.add(new Seg(s.text, col, s.bold));
        }
        use.add(row);
      }
    }
    drawSegPlate(mc, player.posX, y, player.posZ, use,
        c.labelScale, bg, false, 0, c.labelShadow);
  }

  /** Draws a nick sprite lifted so its bottom edge lands exactly at the anchor. */
  private static void drawNickPlate(Minecraft mc, EntityPlayer player, double y,
      String key, XevisConfig c, float[] nativeWH) {
    float eff = fitScale(mc, c, key, nativeWH);
    int[] s = PngPlates.size(key);
    float dh = (s == null || s[1] <= 0) ? nativeWH[1] : s[1] / PngPlates.SITE_SS;
    drawPngPlate(mc, player.posX, y, player.posZ, eff, key, false, dh);
  }

  /** Native-equivalent design size of the given lines (max width, total height). */
  private static float[] nativeSize(Minecraft mc, List<List<Seg>> lines) {
    float maxW = 1.0f;
    for (List<Seg> line : lines) {
      float w = 0.0f;
      for (Seg s : line) {
        w += mc.fontRenderer.getStringWidth(s.text);
      }
      if (w > maxW) {
        maxW = w;
      }
    }
    float lh = mc.fontRenderer.FONT_HEIGHT + 1;
    return new float[] {maxW, lines.size() * lh};
  }

  /**
   * Scale so a PNG sprite never renders bigger than its native-text equivalent:
   * shrinks loose canvases (padding, big fonts) down to native size, never enlarges.
   */
  private static float fitScale(Minecraft mc, XevisConfig c, String key, float[] nativeWH) {
    int[] s = PngPlates.size(key);
    if (s == null || s[0] <= 0 || s[1] <= 0) {
      return c.labelScale;
    }
    float dw = s[0] / PngPlates.SITE_SS;
    float dh = s[1] / PngPlates.SITE_SS;
    float f = Math.min(nativeWH[0] / dw, nativeWH[1] / dh);
    if (f > 1.0f) {
      f = 1.0f;
    }
    if (f < 0.2f) {
      f = 0.2f;
    }
    return c.labelScale * f;
  }

  /** Hologram: PNG plate (or native text), beam leg down to the ground, glow pad on the ground. */
  public static void drawHolo(Minecraft mc, XevisConfig.Holo h, double bobY, int slot) {
    double y = h.y + (h.floatAnim ? bobY : 0.0);
    String plateKey = "plate" + slot;
    float plateLocalH;
    if (PngPlates.has(plateKey)) {
      int[] s = PngPlates.size(plateKey);
      plateLocalH = (s == null ? 64 : s[1]) / PngPlates.SITE_SS;
      drawPngPlate(mc, h.x, y, h.z, h.scale, plateKey, h.throughWalls, 0.0f);
    } else {
      List<List<Seg>> lines = buildHoloLines(h);
      plateLocalH = lines.size() * (mc.fontRenderer.FONT_HEIGHT + 1);
      drawSegPlate(mc, h.x, y, h.z, lines, h.scale,
          h.bgDark ? 0x8C000000 : 0, h.throughWalls, XevisConfig.argb(h.titleColor), true);
    }
    int topY;
    try {
      // walk down from the holo to the first movement-blocking block (leaves
      // don't count, same as the vanilla helper): feet level is that block + 1.
      // (World.getTopSolidOrLiquidBlock already returns the air above the
      // ground, so the old "+ 1" floated the glow a full block too high.)
      BlockPos base = new BlockPos(h.x, h.y, h.z);
      BlockPos.MutableBlockPos mp =
          new BlockPos.MutableBlockPos(base.getX(), base.getY(), base.getZ());
      while (mp.getY() > 0) {
        IBlockState st = mc.world.getBlockState(mp);
        if (st.getMaterial().blocksMovement() && !(st.getBlock() instanceof BlockLeaves)) {
          break;
        }
        mp.move(EnumFacing.DOWN);
      }
      topY = mp.getY() + 1;
    } catch (Exception e) {
      return;
    }
    double padY = topY + 0.03;
    if (padY > y + 0.3) {
      return; // holo at/below ground level: no leg, no glow
    }
    float s = -0.025f * h.scale;
    float groundLocal = (float) ((padY - y) / s);
    if (PngPlates.has("beam")) {
      drawBeam(mc, h.x, y, h.z, h.scale, plateLocalH, groundLocal, h.throughWalls);
    }
    if (PngPlates.has("pad")) {
      PngPlates.drawPad(mc, h.x, padY, h.z, 1.5f * h.padSize, h.throughWalls);
    }
  }

  /**
   * Bakes a foreign player's nametag into a PNG sprite with the same style as
   * the editor's nick export (prefix + name + clan, MC-like offset shadow).
   * Cached per name+style, session-only (never written to disk).
   */
  private static final int BAKED_MAX = 48;

  private static Font monoReg = null;
  private static Font monoBold = null;
  private static boolean monoTried = false;

  /** Real JetBrains Mono from the jar (matches the site's nick export);
   * falls back to the logical Monospaced font when the TTFs are missing. */
  private static Font monoFace(int style, float size) {
    if (!monoTried) {
      monoTried = true;
      try {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font r = Font.createFont(Font.TRUETYPE_FONT, WorldLabel.class
            .getResourceAsStream("/assets/xevis/fonts/JetBrainsMono-Regular.ttf"));
        Font b = Font.createFont(Font.TRUETYPE_FONT, WorldLabel.class
            .getResourceAsStream("/assets/xevis/fonts/JetBrainsMono-Bold.ttf"));
        ge.registerFont(r);
        ge.registerFont(b);
        monoReg = r;
        monoBold = b;
      } catch (Throwable ignored) {
        monoReg = null;
        monoBold = null;
      }
    }
    Font base = (style == Font.BOLD) ? monoBold : monoReg;
    if (base != null) {
      return base.deriveFont(style, size);
    }
    return new Font(Font.MONOSPACED, style, (int) size);
  }

  private static String bakedKey(String name, XevisConfig c) {
    String tagSig = "";
    if (c.players != null) {
      XevisConfig.PlayerTag tag = c.players.get(name);
      if (tag != null) {
        tagSig = tag.prefix + "|" + tag.clan + "|" + tag.sub + "|"
            + tag.preColor + "|" + tag.nickColor + "|" + tag.sufColor + "|" + tag.subColor + "|";
      }
    }
    String sig = c.otherPre + "|" + c.otherClan + "|" + tagSig + c.preColor + "|"
        + c.nickColor + "|" + c.sufColor + "|" + c.subColor + "|" + (c.labelShadow ? "S" : "s")
        + "|" + (c.labelBold ? "B" : "b");
    return "pn:" + name + ":" + sig.hashCode();
  }

  private static String[] foreignStyle(String name) {
    XevisConfig c = XevisConfig.INSTANCE;
    String pre = c.otherPre == null ? "" : c.otherPre;
    String clan = c.otherClan == null ? "" : c.otherClan;
    String sub = "";
    String preC = c.preColor;
    String nickC = c.nickColor;
    String sufC = c.sufColor;
    String subCc = c.subColor;
    if (c.players != null) {
      XevisConfig.PlayerTag tag = c.players.get(name);
      if (tag != null) {
        if (tag.prefix != null && !tag.prefix.isEmpty()) {
          pre = tag.prefix;
        }
        if (tag.clan != null && !tag.clan.isEmpty()) {
          clan = tag.clan;
        }
        if (tag.sub != null && !tag.sub.isEmpty()) {
          sub = tag.sub;
        }
        if (tag.preColor != null && !tag.preColor.isEmpty()) {
          preC = tag.preColor;
        }
        if (tag.nickColor != null && !tag.nickColor.isEmpty()) {
          nickC = tag.nickColor;
        }
        if (tag.sufColor != null && !tag.sufColor.isEmpty()) {
          sufC = tag.sufColor;
        }
        if (tag.subColor != null && !tag.subColor.isEmpty()) {
          subCc = tag.subColor;
        }
      }
    }
    return new String[] {pre, clan, sub, preC, nickC, sufC, subCc};
  }

  private static void bakeNickSprite(String name) {
    XevisConfig c = XevisConfig.INSTANCE;
    try {
      if (GraphicsEnvironment.isHeadless()) {
        return;
      }
      if (PngPlates.transientCount() > BAKED_MAX) {
        PngPlates.clearTransient();
      }
      String key = bakedKey(name, c);
      if (PngPlates.has(key)) {
        return;
      }
      final int SS = 2;
      int fs = 10 * SS;
      int fsSub = 9 * SS;
      Font plain = monoFace(Font.PLAIN, fs);
      Font boldF = monoFace(c.labelBold ? Font.BOLD : Font.PLAIN, fs);
      Font subF = monoFace(Font.PLAIN, fsSub);
      String[] style = foreignStyle(name);
      String sPre = style[0] == null || style[0].isEmpty() ? "" : style[0] + " ";
      String sName = name == null ? "?" : name;
      String sClan = style[1] == null || style[1].isEmpty() ? "" : " " + style[1];
      String sSub = style[2] == null ? "" : style[2];
      boolean hasSub = c.showSub && !sSub.isEmpty();
      String[] texts = new String[] {sPre, sName, sClan};
      Font[] fonts = new Font[] {plain, boldF, plain};
      Color[] cols = new Color[] {
          new Color(XevisConfig.argb(style[3]), true),
          new Color(XevisConfig.argb(style[4]), true),
          new Color(XevisConfig.argb(style[5]), true)};
      BufferedImage probe = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g0 = probe.createGraphics();
      float lineW = 0.0f;
      int ascent = 1;
      for (int i = 0; i < 3; i++) {
        if (texts[i].isEmpty()) {
          continue;
        }
        g0.setFont(fonts[i]);
        FontMetrics fm = g0.getFontMetrics();
        lineW += fm.stringWidth(texts[i]);
        if (fm.getAscent() > ascent) {
          ascent = fm.getAscent();
        }
      }
      float subW = 0.0f;
      int subAscent = 1;
      if (hasSub) {
        g0.setFont(subF);
        FontMetrics fm = g0.getFontMetrics();
        subW = fm.stringWidth(sSub);
        subAscent = fm.getAscent();
      }
      g0.dispose();
      float maxW = Math.max(lineW, subW);
      if (maxW <= 0.0f) {
        return;
      }
      boolean sh = c.labelShadow;
      int o = sh ? SS : 0;
      int padX = SS + 2;
      int padTop = SS;
      int padBot = SS + 2;
      int lh0 = Math.round(fs * 1.15f);
      int gap = SS;
      int lh1 = Math.round(fsSub * 1.15f);
      int textH = lh0 + (hasSub ? gap + lh1 : 0);
      int cw = (int) Math.ceil(maxW + padX * 2 + o);
      int ch = (int) Math.ceil(padTop + textH + padBot + o);
      BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      Color subCol = new Color(XevisConfig.argb(style[6]), true);
      float x0 = padX + (cw - padX * 2 - o - lineW) / 2.0f;
      float xs0 = padX + (cw - padX * 2 - o - subW) / 2.0f;
      float yb = padTop + ascent;
      float ybSub = padTop + lh0 + gap + subAscent;
      for (int pass = 0; pass < 2; pass++) {
        if (pass == 0 && !sh) {
          continue;
        }
        float dx = pass == 0 ? SS : 0;
        float dy = pass == 0 ? SS : 0;
        float x = x0 + dx;
        float y = yb + dy;
        for (int i = 0; i < 3; i++) {
          if (texts[i].isEmpty()) {
            continue;
          }
          g.setFont(fonts[i]);
          g.setColor(pass == 0 ? new Color(10, 10, 10, 220) : cols[i]);
          g.drawString(texts[i], x, y);
          x += g.getFontMetrics().stringWidth(texts[i]);
        }
        if (hasSub) {
          g.setFont(subF);
          g.setColor(pass == 0 ? new Color(10, 10, 10, 220) : subCol);
          g.drawString(sSub, xs0 + dx, ybSub + dy);
        }
      }
      g.dispose();
      PngPlates.putTransient(key, img);
    } catch (Throwable ignored) {
      // native-text fallback stays in place
    }
  }

  private static List<List<Seg>> buildHoloLines(XevisConfig.Holo h) {
    List<List<Seg>> lines = new ArrayList<List<Seg>>();
    List<Seg> title = new ArrayList<Seg>();
    title.add(new Seg(h.title, XevisConfig.argb(h.titleColor), true));
    lines.add(title);
    if (h.lines != null) {
      for (int i = 0; i < h.lines.length; i++) {
        String lc = h.lineColor;
        if (h.lineColors != null && i < h.lineColors.length && h.lineColors[i] != null
            && !h.lineColors[i].isEmpty()) {
          lc = h.lineColors[i];
        }
        List<Seg> row = new ArrayList<Seg>();
        row.add(new Seg(h.lines[i], XevisConfig.argb(lc), false));
        lines.add(row);
      }
    }
    return lines;
  }

  /** Billboarded textured plate in design px (texture is SITE_SS x supersampled).
   * liftDesign shifts the plate up so its bottom edge lands at the anchor
   * (nicks: pass plate design height; holos: 0 = top edge at anchor). */
  private static void drawPngPlate(Minecraft mc, double x, double y, double z,
      float scale, String key, boolean seeThrough, float liftDesign) {
    RenderManager rm = mc.getRenderManager();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x - rm.viewerPosX,
        y - rm.viewerPosY + liftDesign * 0.025f * scale, z - rm.viewerPosZ);
    GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
    GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
    float s = -0.025f * scale;
    GlStateManager.scale(s, s, -s); // vanilla-exact: proper rotation, no mirror winding
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    boolean depthOff = false;
    if (seeThrough) {
      GlStateManager.disableDepth();
      depthOff = true;
    }
    boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    GlStateManager.disableCull();
    PngPlates.drawKey(key);
    if (cull) {
      GlStateManager.enableCull();
    }
    if (depthOff) {
      GlStateManager.enableDepth();
    }
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.popMatrix();
  }

  /** Vertical beam strip from the plate bottom down to the ground (design px). */
  private static void drawBeam(Minecraft mc, double x, double y, double z, float scale,
      float fromLocal, float toLocal, boolean throughWalls) {
    if (toLocal <= fromLocal + 1.0f) {
      return;
    }
    RenderManager rm = mc.getRenderManager();
    float w = 3.0f;
    GlStateManager.pushMatrix();
    GlStateManager.translate(x - rm.viewerPosX, y - rm.viewerPosY, z - rm.viewerPosZ);
    GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
    GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
    float s = -0.025f * scale;
    GlStateManager.scale(s, s, -s); // vanilla-exact: proper rotation, no mirror winding
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    boolean depthOff = false;
    if (throughWalls) {
      GlStateManager.disableDepth();
      depthOff = true;
    }
    boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    GlStateManager.disableCull();
    PngPlates.bind("beam");
    boolean flipV = XevisConfig.INSTANCE.spriteFlipV;
    boolean mirrorU = XevisConfig.INSTANCE.spriteMirrorU;
    float uL = mirrorU ? 1.0f : 0.0f;
    float uR = mirrorU ? 0.0f : 1.0f;
    // canvas row 0 (top) sits at the plate end: v=0 at fromLocal (v=1 if flipped)
    float vP = flipV ? 1.0f : 0.0f;
    float vG = flipV ? 0.0f : 1.0f;
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(-w / 2.0, fromLocal, 0.0).tex(uL, vP).endVertex();
    buf.pos(w / 2.0, fromLocal, 0.0).tex(uR, vP).endVertex();
    buf.pos(w / 2.0, toLocal, 0.0).tex(uR, vG).endVertex();
    buf.pos(-w / 2.0, toLocal, 0.0).tex(uL, vG).endVertex();
    tess.draw();
    if (cull) {
      GlStateManager.enableCull();
    }
    if (depthOff) {
      GlStateManager.enableDepth();
    }
    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.popMatrix();
  }
}
