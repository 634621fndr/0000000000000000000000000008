package com.xevis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-visual config. Flat keys so the editor page can POST the same JSON
 * over the localhost link. Unknown/missing keys are ignored (defaults kept).
 */
public class XevisConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  public static XevisConfig INSTANCE = new XevisConfig();
  private static File file;

  public String serverName = "NEXUS MC";
  public String season = "SEASON VI";
  public String ip = "play.nexusmc.net";

  // sidebar player block
  public String rank = "ADMIN";
  public String money = "$142,500";
  public String shards = "312";
  public String level = "47";
  public String xp = "72";
  public String online = "24 / 100";
  public String ping = "18 ms";

  // nametag: self uses prefix/clan/sub, others get otherPre/otherClan
  // unless players[] has a per-name override (Blockbuster actors included)
  public String prefix = "ADMIN";
  public String clan = "WOLF";
  public String otherPre = "[VIP]";
  public String otherClan = "WILD";
  public static class PlayerTag {
    public String prefix = ""; // literal, e.g. "[VIP]" — null/empty = keep default
    public String clan = ""; // literal, e.g. "WILD" — null/empty = keep default
    public String sub = ""; // free second line; empty = none
    public String preColor = ""; // hex — empty = global preColor
    public String nickColor = ""; // hex — empty = global nickColor
    public String sufColor = ""; // hex — empty = global sufColor
    public String subColor = ""; // hex — empty = global subColor
  }
  public Map<String, PlayerTag> players = new HashMap<String, PlayerTag>();
  public String sub = "Легенда сервера";
  public String preColor = "#fbbf24";
  public String nickColor = "#ffffff";
  public String sufColor = "#a3e635";
  public String subColor = "#ffd166";
  public boolean showSub = true;
  public boolean labelBold = true;
  public boolean labelShadow = true;
  public String labelBg = "none"; // dark | light | none (nicks always render without plate)
  public float labelScale = 1.0f;
  public boolean nickPng = true; // false = vanilla-font plates honoring labelBg/labelShadow
  public boolean spriteFlipV = false; // true = swapped V (rare drivers/shaders); vanilla mapping is v0 = canvas top
  public boolean spriteMirrorU = false; // mirror sprites horizontally (empirical fix)

  // accent (header, badges, stripes)
  public String accent = "#ffffff";
  public String accentInk = "#09090b";

  // visibility toggles (mirror editor chips)
  public boolean compass = true;
  public boolean sidebar = true;
  public boolean showEvent = true;
  public boolean showIcons = true;
  public boolean compact = false;
  public boolean showQuest = false;
  public boolean showWar = false;
  public boolean showPunish = false;
  public int sidebarWidth = 280;

  public static class Marker {
    public String name = "СПАВН";
    public double bearing = 0.0;
    public String dist = "12м";
    public String color = "#ffffff";
  }

  public List<Marker> markers = markers();

  public static class EventItem {
    public String text = "Дроп ×2 до 21:00";
    public int secs = 5;
    public String c = "#ffffff";
    public int anim = 3;
  }

  public List<EventItem> events = defaultEvents();

  public static class HoloLine {
    public String text = "";
    public String c = "#e4e4e7";
  }

  public static class Holo {
    public double x = 0.5;
    public double y = 72.0;
    public double z = 0.5;
    public String title = "◆ СПАВН";
    public String titleColor = "#a3e635";
    public String[] lines = {"Добро пожаловать на NEXUS", "Онлайн: 24/100 · Вайп 12.12", "▶ Нажмите, чтобы начать"};
    public String lineColor = "#e4e4e7";
    public String[] lineColors = null; // per-line override, may be null/short -> lineColor
    public float scale = 1.0f;
    public boolean throughWalls = false;
    public boolean floatAnim = true;
    public boolean bgDark = true;
    public float padSize = 1.0f; // glow size multiplier, site slider %
  }

  public List<Holo> holos = holos();

  public static class Quest {
    public String name = "Убей 10 зомби";
    public int cur = 7;
    public int tot = 10;
  }

  public Quest quest = new Quest();

  public static class War {
    public String name1 = "WOLF";
    public String color1 = "#a3e635";
    public String name2 = "BEAR";
    public String color2 = "#f87171";
    public int s1 = 12;
    public int s2 = 9;
    public String info = "До победы: 21 очко · финал через 02:14";
  }

  public War war = new War();

  public static class Punish {
    public String type = "Мут";
    public String term = "3 часа";
    public String by = "Moder_Ann";
    public int leftMin = 150;
    public String reason = "Флуд в чате";
    public String soc = "discord.gg/nexusmc";
  }

  public Punish punish = new Punish();

  private static List<Marker> markers() {
    List<Marker> out = new ArrayList<Marker>();
    Marker a = new Marker();
    Marker b = new Marker();
    b.name = "БАЗА";
    b.bearing = 140.0;
    b.dist = "342м";
    b.color = "#a3e635";
    Marker c = new Marker();
    c.name = "БОСС";
    c.bearing = 250.0;
    c.dist = "87м";
    c.color = "#f87171";
    Marker d = new Marker();
    d.name = "СМЕРТЬ";
    d.bearing = 320.0;
    d.dist = "512м";
    d.color = "#22d3ee";
    out.add(a);
    out.add(b);
    out.add(c);
    out.add(d);
    return out;
  }

  private static List<EventItem> defaultEvents() {
    List<EventItem> out = new ArrayList<EventItem>();
    EventItem a = new EventItem();
    EventItem b = new EventItem();
    b.text = "Босс на арене в 20:00";
    b.c = "#fbbf24";
    b.anim = 1;
    EventItem c = new EventItem();
    c.text = "Скидки 30% в магазине";
    c.c = "#22d3ee";
    c.anim = 2;
    out.add(a);
    out.add(b);
    out.add(c);
    return out;
  }

  private static List<Holo> holos() {
    List<Holo> out = new ArrayList<Holo>();
    Holo a = new Holo();
    Holo b = new Holo();
    b.x = 12.5;
    b.y = 70.0;
    b.z = -8.5;
    b.title = "◆ АРЕНА";
    b.titleColor = "#fbbf24";
    b.lines = new String[]{"Бой через 02:14", "Награда: кейс"};
    out.add(a);
    out.add(b);
    return out;
  }

  /** Parses #rrggbb or #aarrggbb into ARGB int. */
  public static int argb(String hex) {
    if (hex == null || hex.isEmpty()) {
      return 0xFFFFFFFF;
    }
    String h = hex.startsWith("#") ? hex.substring(1) : hex;
    if (h.length() == 3) {
      h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
    }
    try {
      if (h.length() > 6) {
        return (int) Long.parseLong(h, 16);
      }
      return 0xFF000000 | (int) Long.parseLong(h, 16);
    } catch (NumberFormatException e) {
      return 0xFFFFFFFF;
    }
  }

  /** Fill in defaults for anything the incoming JSON omitted. */
  public void normalize() {
    if (serverName == null) serverName = "NEXUS MC";
    if (season == null) season = "";
    if (ip == null) ip = "";
    if (rank == null) rank = "";
    if (money == null) money = "";
    if (shards == null) shards = "";
    if (level == null) level = "";
    if (xp == null) xp = "";
    if (online == null) online = "";
    if (ping == null) ping = "";
    if (prefix == null) prefix = "";
    if (clan == null) clan = "";
    if (otherPre == null) otherPre = "[VIP]";
    if (otherClan == null) otherClan = "WILD";
    if (players == null) players = new HashMap<String, PlayerTag>();
    if (players != null) {
      players.values().removeIf(t -> t == null);
      for (PlayerTag t : players.values()) {
        if (t.prefix == null) t.prefix = "";
        if (t.clan == null) t.clan = "";
        if (t.sub == null) t.sub = "";
        if (t.preColor == null) t.preColor = "";
        if (t.nickColor == null) t.nickColor = "";
        if (t.sufColor == null) t.sufColor = "";
        if (t.subColor == null) t.subColor = "";
      }
    }
    if (sub == null) sub = "";
    if (preColor == null) preColor = "#fbbf24";
    if (nickColor == null) nickColor = "#ffffff";
    if (sufColor == null) sufColor = "#a3e635";
    if (subColor == null) subColor = "#ffd166";
    if (labelBg == null) labelBg = "none";
    if (accent == null) accent = "#ffffff";
    if (accentInk == null) accentInk = "#09090b";
    if (markers == null) markers = markers();
    if (events == null) events = defaultEvents();
    if (holos == null) holos = holos();
    if (holos != null) {
      for (Holo h : holos) {
        if (h != null && h.padSize <= 0.0f) {
          h.padSize = 1.0f;
        }
      }
    }
    if (quest == null) quest = new Quest();
    if (war == null) war = new War();
    if (punish == null) punish = new Punish();
    if (labelScale <= 0.2f || labelScale > 3.0f) labelScale = 1.0f;
  }

  public static void load(File f) {
    file = f;
    try {
      if (f.exists()) {
        Reader r = new FileReader(f);
        try {
          XevisConfig loaded = GSON.fromJson(r, XevisConfig.class);
          if (loaded != null) {
            loaded.normalize();
            INSTANCE = loaded;
            return;
          }
        } finally {
          r.close();
        }
      }
      INSTANCE.normalize();
      save(f);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void save(File f) {
    try {
      File parent = f.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      Writer w = new FileWriter(f);
      try {
        GSON.toJson(INSTANCE, w);
      } finally {
        w.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void saveDefault() {
    if (file != null) {
      save(file);
    }
  }
}
