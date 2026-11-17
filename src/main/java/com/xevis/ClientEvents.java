package com.xevis;

import com.xevis.label.WorldLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Client-only Forge handlers: vanilla nametag replacement, world labels (nicks + holograms). */
@SideOnly(Side.CLIENT)
public class ClientEvents {
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new ClientEvents());
  }

  /** Suppress vanilla player nametags — styled plates are drawn in RenderWorldLastEvent. */
  @SubscribeEvent
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void onNameplate(RenderLivingEvent.Specials.Pre event) {
    Entity e = event.getEntity();
    if (e instanceof EntityPlayer) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onWorldLast(RenderWorldLastEvent event) {
    Minecraft mc = Minecraft.getMinecraft();
    if (mc.world == null || mc.player == null || mc.getRenderManager() == null) {
      return;
    }
    XevisConfig cfg = XevisConfig.INSTANCE;
    double t = mc.world.getTotalWorldTime() + event.getPartialTicks();
    int i = 0;
    if (cfg.holos != null) {
      for (XevisConfig.Holo h : cfg.holos) {
        if (h == null) {
          continue;
        }
        double bob = Math.sin(t * 0.05 + i * 1.7) * 0.12;
        WorldLabel.drawHolo(mc, h, bob, i);
        i++;
      }
    }
    List<EntityPlayer> players = mc.world.playerEntities;
    for (EntityPlayer p : players) {
      drawPlayerChecked(mc, p);
    }
    // Modded fake players (e.g. Blockbuster actors) are EntityPlayer instances
    // that never join playerEntities: cover them too (vanilla tags for them are
    // already suppressed by the Specials.Pre cancel above).
    try {
      for (Entity e : mc.world.loadedEntityList) {
        if (!(e instanceof EntityPlayer) || players.contains(e)) {
          continue;
        }
        drawPlayerChecked(mc, (EntityPlayer) e);
      }
    } catch (Exception ignored) {
    }
  }

  /** Visibility filters shared by listed and unlisted (fake) players. */
  private static void drawPlayerChecked(Minecraft mc, EntityPlayer p) {
    if (p == null || p.isDead) {
      return;
    }
    boolean self = p == mc.player;
    if (self && mc.gameSettings.thirdPersonView == 0) {
      return;
    }
    if (p.isSpectator()) {
      return;
    }
    if (p.isInvisible() && !self) {
      return;
    }
    if (p.isSneaking() && !self) {
      return;
    }
    if (p.getDistance(mc.player) > 64.0f) {
      return;
    }
    WorldLabel.drawPlayer(mc, p);
  }
}
