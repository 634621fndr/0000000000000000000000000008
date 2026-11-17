package com.xevis;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * Client command to place holograms in the world.
 * /xeholo add — new hologram here (feet + 2.4), style copied from slot 0.
 * /xeholo set <n> — move slot n here. /xeholo del <n> — remove. /xeholo list.
 * /xeholo through <n> — toggle seeing slot n through walls (off = depth-tested).
 */
public class HoloCommand extends CommandBase {
  @Override
  public String getName() {
    return "xeholo";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/xeholo add | /xeholo set <n> | /xeholo del <n> | /xeholo through <n> | /xeholo list";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return -1;
  }

  @Override
  public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    return true;
  }

  @Override
  public List<String> getAliases() {
    List<String> a = new ArrayList<String>();
    a.add("xholo");
    return a;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
    if (args.length == 0) {
      throw new CommandException("Использование: " + getUsage(sender));
    }
    EntityPlayerSP p = Minecraft.getMinecraft().player;
    if (p == null) {
      throw new CommandException("Только в игре.");
    }
    XevisConfig cfg = XevisConfig.INSTANCE;
    if (cfg.holos == null) {
      cfg.holos = new ArrayList<XevisConfig.Holo>();
    }
    String sub = args[0].toLowerCase();
    if (sub.equals("add")) {
      XevisConfig.Holo h = new XevisConfig.Holo();
      if (!cfg.holos.isEmpty()) {
        copyStyle(cfg.holos.get(0), h);
      }
      h.x = p.posX;
      h.y = p.posY + 2.4;
      h.z = p.posZ;
      cfg.holos.add(h);
      XevisConfig.saveDefault();
      sender.sendMessage(new TextComponentString(
          "Голограма #" + (cfg.holos.size() - 1) + " здесь: " + fmt(h)));
    } else if (sub.equals("set")) {
      int i = parseIdx(args, cfg.holos.size());
      XevisConfig.Holo h = cfg.holos.get(i);
      h.x = p.posX;
      h.y = p.posY + 2.4;
      h.z = p.posZ;
      XevisConfig.saveDefault();
      sender.sendMessage(new TextComponentString("Голограма #" + i + " перенесена: " + fmt(h)));
    } else if (sub.equals("del") || sub.equals("remove") || sub.equals("rm")) {
      int i = parseIdx(args, cfg.holos.size());
      cfg.holos.remove(i);
      XevisConfig.saveDefault();
      sender.sendMessage(new TextComponentString("Голограма #" + i + " удалена."));
    } else if (sub.equals("through") || sub.equals("wall") || sub.equals("walls")) {
      int i = parseIdx(args, cfg.holos.size());
      XevisConfig.Holo h = cfg.holos.get(i);
      h.throughWalls = !h.throughWalls;
      XevisConfig.saveDefault();
      sender.sendMessage(new TextComponentString("Голограма #" + i + ": сквозь стены "
          + (h.throughWalls ? "ВКЛ (видна всегда, перекрывает игроков)"
              : "ВЫКЛ (прячется за блоками и игроками)")));
    } else if (sub.equals("list")) {
      if (cfg.holos.isEmpty()) {
        sender.sendMessage(new TextComponentString("Голограмм нет. /xeholo add — поставить сюда."));
        return;
      }
      for (int i = 0; i < cfg.holos.size(); i++) {
        XevisConfig.Holo h = cfg.holos.get(i);
        sender.sendMessage(new TextComponentString("#" + i + " «" + h.title + "» " + fmt(h)));
      }
    } else {
      throw new CommandException("Использование: " + getUsage(sender));
    }
  }

  private static int parseIdx(String[] args, int size) throws CommandException {
    if (args.length < 2) {
      throw new CommandException("Укажи номер. /xeholo list — список.");
    }
    int i;
    try {
      i = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      throw new CommandException("Номер — число.");
    }
    if (i < 0 || i >= size) {
      throw new CommandException("Нет голограммы #" + i + ".");
    }
    return i;
  }

  private static void copyStyle(XevisConfig.Holo from, XevisConfig.Holo to) {
    to.title = from.title;
    to.titleColor = from.titleColor;
    to.lines = from.lines == null ? null : from.lines.clone();
    to.lineColor = from.lineColor;
    to.lineColors = from.lineColors == null ? null : from.lineColors.clone();
    to.scale = from.scale;
    to.throughWalls = from.throughWalls;
    to.floatAnim = from.floatAnim;
    to.bgDark = from.bgDark;
    to.padSize = from.padSize <= 0.0f ? 1.0f : from.padSize;
  }

  private static String fmt(XevisConfig.Holo h) {
    return (int) h.x + " / " + (int) h.y + " / " + (int) h.z;
  }
}
