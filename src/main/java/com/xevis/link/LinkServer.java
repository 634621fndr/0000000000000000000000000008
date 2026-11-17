package com.xevis.link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xevis.XevisConfig;
import com.xevis.XevisMod;
import com.xevis.label.PngPlates;
import net.minecraft.client.Minecraft;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Localhost HTTP bridge: the editor page POSTs live config JSON here.
 * Plain ServerSocket + minimal HTTP, JDK only, no extra deps.
 * Binds 127.0.0.1 only. Editor polls GET /xevis for the LIVE indicator.
 */
public final class LinkServer {
  public static final int PORT = 25585;
  private static final Gson GSON = new Gson();
  private static volatile boolean started = false;

  private LinkServer() {
  }

  public static void start() {
    if (started) {
      return;
    }
    started = true;
    Thread t = new Thread(LinkServer::loop, "xevis-link");
    t.setDaemon(true);
    t.start();
  }

  private static void loop() {
    try {
      ServerSocket ss = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));
      for (;;) {
        Socket s = null;
        try {
          s = ss.accept();
          handle(s);
        } catch (Exception ignored) {
        } finally {
          if (s != null) {
            try {
              s.close();
            } catch (Exception ignored) {
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handle(Socket s) throws Exception {
    s.setSoTimeout(8000);
    InputStream in = s.getInputStream();
    OutputStream out = s.getOutputStream();
    ByteArrayOutputStream head = new ByteArrayOutputStream();
    int b, p0 = -1, p1 = -1, p2 = -1;
    while ((b = in.read()) != -1) {
      head.write(b);
      if (p2 == '\r' && p1 == '\n' && p0 == '\r' && b == '\n') {
        break;
      }
      p2 = p1;
      p1 = p0;
      p0 = b;
      if (head.size() > 65536) {
        return;
      }
    }
    String[] lines = new String(head.toByteArray(), StandardCharsets.UTF_8).split("\r\n");
    if (lines.length == 0) {
      return;
    }
    String[] req = lines[0].split(" ");
    if (req.length < 2) {
      return;
    }
    String method = req[0];
    String rawPath = req[1];
    String path = rawPath;
    int q = path.indexOf('?');
    if (q >= 0) {
      path = path.substring(0, q);
    }
    int len = 0;
    for (String l : lines) {
      if (l.regionMatches(true, 0, "Content-Length:", 0, 15)) {
        try {
          len = Integer.parseInt(l.substring(15).trim());
        } catch (NumberFormatException ignored) {
        }
      }
    }
    byte[] body = new byte[0];
    if (len > 0 && len <= 1048576) {
      body = new byte[len];
      int off = 0;
      while (off < len) {
        int r = in.read(body, off, len - off);
        if (r < 0) {
          break;
        }
        off += r;
      }
    }
    String cors = "Access-Control-Allow-Origin: *\r\n"
        + "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
        + "Access-Control-Allow-Headers: *\r\n"
        + "Access-Control-Allow-Private-Network: true\r\n";
    if (method.equals("OPTIONS")) {
      String h = "HTTP/1.1 204 No Content\r\n" + cors
          + "Content-Length: 0\r\nConnection: close\r\n\r\n";
      out.write(h.getBytes(StandardCharsets.UTF_8));
      out.flush();
      return;
    }
    String payload;
    int code = 200;
    if (method.equals("POST") && path.equals("/xevis")) {
      try {
        String text = new String(body, StandardCharsets.UTF_8);
        JsonObject root = new JsonParser().parse(text).getAsJsonObject();
        if (root.has("data") && root.get("data").isJsonObject()) {
          JsonObject data = root.getAsJsonObject("data");
          XevisConfig loaded = GSON.fromJson(data, XevisConfig.class);
          if (loaded != null) {
            // holo positions live in the game: editor sends style only,
            // so keep the current list unless it explicitly sent one
            List<XevisConfig.Holo> keep = XevisConfig.INSTANCE.holos;
            loaded.normalize();
            if (!data.has("holos")) {
              loaded.holos = keep;
            }
            XevisConfig.INSTANCE = loaded;
            applyHoloStyle(data);
            XevisConfig.saveDefault();
          }
        }
        payload = "{\"ok\":true}";
      } catch (Exception e) {
        code = 400;
        payload = "{\"ok\":false}";
      }
    } else if (method.equals("POST") && path.equals("/xevis-png")) {
      try {
        String type = queryParam(rawPath, "type");
        if (type.equals("drop")) {
          final String key = queryParam(rawPath, "key");
          if (key.isEmpty()) {
            throw new IllegalArgumentException("no key");
          }
          Minecraft.getMinecraft().addScheduledTask(() -> PngPlates.drop(key));
          payload = "{\"ok\":true}";
        } else {
          int slot = 0;
          try {
            slot = Integer.parseInt(queryParam(rawPath, "slot"));
          } catch (Exception ignored) {
          }
          if (!type.equals("plate") && !type.equals("beam")
              && !type.equals("pad") && !type.equals("nick")) {
            throw new IllegalArgumentException("bad type");
          }
          BufferedImage img = ImageIO.read(new ByteArrayInputStream(body));
          if (img == null) {
            throw new IllegalArgumentException("not an image");
          }
          final String key = (type.equals("plate") || type.equals("nick")) ? type + slot : type;
          Minecraft.getMinecraft().addScheduledTask(() -> {
            PngPlates.put(key, img);
            PngPlates.saveAll();
          });
          payload = "{\"ok\":true}";
        }
      } catch (Exception e) {
        code = 400;
        payload = "{\"ok\":false}";
      }
    } else if (method.equals("GET") && path.equals("/xevis")) {
      payload = "{\"ok\":true,\"game\":\"xevis\",\"version\":\"" + XevisMod.VERSION + "\"}";
    } else {
      code = 404;
      payload = "{\"ok\":false}";
    }
    byte[] pb = payload.getBytes(StandardCharsets.UTF_8);
    String h = "HTTP/1.1 " + code + (code == 200 ? " OK" : " Error") + "\r\n" + cors
        + "Content-Type: application/json\r\nContent-Length: " + pb.length
        + "\r\nConnection: close\r\n\r\n";
    out.write(h.getBytes(StandardCharsets.UTF_8));
    out.write(pb);
    out.flush();
  }

  /** ?name=value from a raw request path (no decoding needed for our keys). */
  private static String queryParam(String rawPath, String name) {
    int qi = rawPath.indexOf('?');
    if (qi < 0) {
      return "";
    }
    String[] pairs = rawPath.substring(qi + 1).split("&");
    for (String p : pairs) {
      int eq = p.indexOf('=');
      if (eq > 0 && p.substring(0, eq).equals(name)) {
        return p.substring(eq + 1);
      }
    }
    return "";
  }

  /** Editor sends holo *style* (no coordinates): apply it to slot 0, keep xyz. */  private static void applyHoloStyle(JsonObject data) {
    if (data == null || !data.has("holo") || !data.get("holo").isJsonObject()) {
      return;
    }
    JsonObject h = data.getAsJsonObject("holo");
    List<XevisConfig.Holo> list = XevisConfig.INSTANCE.holos;
    if (list == null || list.isEmpty()) {
      list = new ArrayList<XevisConfig.Holo>();
      list.add(new XevisConfig.Holo());
      XevisConfig.INSTANCE.holos = list;
    }
    XevisConfig.Holo t = list.get(0);
    if (h.has("title")) {
      t.title = h.get("title").getAsString();
    }
    if (h.has("titleColor")) {
      t.titleColor = h.get("titleColor").getAsString();
    }
    if (h.has("scale")) {
      try {
        t.scale = h.get("scale").getAsFloat();
      } catch (Exception ignored) {
      }
    }
    if (h.has("floatAnim")) {
      try {
        t.floatAnim = h.get("floatAnim").getAsBoolean();
      } catch (Exception ignored) {
      }
    }
    if (h.has("throughWalls")) {
      try {
        t.throughWalls = h.get("throughWalls").getAsBoolean();
      } catch (Exception ignored) {
      }
    }
    if (h.has("bgDark")) {
      try {
        t.bgDark = h.get("bgDark").getAsBoolean();
      } catch (Exception ignored) {
      }
    }
    if (h.has("padSize")) {
      try {
        t.padSize = h.get("padSize").getAsFloat();
        if (t.padSize <= 0.0f) {
          t.padSize = 1.0f;
        }
      } catch (Exception ignored) {
      }
    }
    if (h.has("lines") && h.get("lines").isJsonArray()) {
      List<String> texts = new ArrayList<String>();
      List<String> colors = new ArrayList<String>();
      for (int i = 0; i < h.getAsJsonArray("lines").size() && i < 7; i++) {
        try {
          JsonObject ln = h.getAsJsonArray("lines").get(i).getAsJsonObject();
          texts.add(ln.has("text") ? ln.get("text").getAsString() : "");
          colors.add(ln.has("c") ? ln.get("c").getAsString() : "#e4e4e7");
        } catch (Exception ignored) {
        }
      }
      t.lines = texts.toArray(new String[texts.size()]);
      t.lineColors = colors.toArray(new String[colors.size()]);
      if (!colors.isEmpty()) {
        t.lineColor = colors.get(0);
      }
    }
  }
}
