package de.auctionhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AuctionConfig {
    public static AuctionConfig INSTANCE = new AuctionConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Verschiebung vom Bildschirmmittelpunkt (Pixel). 0/0 = exakt in der Mitte. */
    public int hudOffsetX = 0;
    public int hudOffsetY = 0;
    /** Akzentfarbe des HUDs als RGB (ohne Alpha). */
    public int hudColor = 0x00E5FF;
    /** HUD sichtbar? (Taste "Auktions-HUD ein-/ausblenden" oder Button im Menü) */
    public boolean hudVisible = true;
    public boolean debug = false;
    public boolean cumulativeBids = false;
    public List<String> paymentPatterns = new ArrayList<>(List.of(
        "(?<player>\\w{3,16}) (?:hat dir|has sent you|sent you|has paid you|paid you|zahlte dir|hat dir gezahlt) \\$?(?<amount>[\\d.,]+[kKmMbB]?)",
        "(?:Du hast|You received|You have received) \\$?(?<amount>[\\d.,]+[kKmMbB]?)\\$? (?:von|from) (?<player>\\w{3,16})"
    ));

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("auctionhud.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                AuctionConfig c = GSON.fromJson(Files.readString(f), AuctionConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            AuctionHudClient.LOGGER.error("Config konnte nicht geladen werden", e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(INSTANCE));
        } catch (Exception e) {
            AuctionHudClient.LOGGER.error("Config konnte nicht gespeichert werden", e);
        }
    }
}
