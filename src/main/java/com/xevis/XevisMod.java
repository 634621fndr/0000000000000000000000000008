package com.xevis;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import com.xevis.label.PngPlates;
import com.xevis.link.LinkServer;

import net.minecraftforge.client.ClientCommandHandler;

import java.io.File;

@Mod(modid = XevisMod.MODID, name = XevisMod.NAME, version = XevisMod.VERSION,
    acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true)
public class XevisMod {
  public static final String MODID = "xevis";
  public static final String NAME = "Xevis Labels";
  public static final String VERSION = "0.2.8";

  public static Logger logger;

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    File cfg = new File(event.getModConfigurationDirectory(), "xevis.json");
    XevisConfig.load(cfg);
    PngPlates.init(event.getModConfigurationDirectory());
    PngPlates.loadAll();
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
      ClientEvents.register();
      ClientCommandHandler.instance.registerCommand(new HoloCommand());
      LinkServer.start();
    }
  }
}
