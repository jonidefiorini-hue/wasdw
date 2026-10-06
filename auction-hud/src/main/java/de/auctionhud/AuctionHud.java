package de.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Futuristisches Auktions-HUD, mittig auf dem Bildschirm. */
public final class AuctionHud {
    private static final int W = 250;
    private static final int TITLE_H = 20;
    private static final int BODY_H = 64;
    private static final int ROW_H = 14;
    private static final int MAX_ROWS = 5;
    private static final int RING_R = 26;

    private static final Map<String, SkinTextures> SKINS = new HashMap<>();

    private AuctionHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        AuctionState s = AuctionState.INSTANCE;
        AuctionConfig cfg = AuctionConfig.INSTANCE;
        if (!cfg.hudVisible || s.phase == AuctionState.Phase.IDLE || mc.options.hudHidden || mc.player == null) return;

        TextRenderer font = mc.textRenderer;
        boolean running = s.phase == AuctionState.Phase.RUNNING;
        long remaining = running ? s.remainingMs() : 0;
        boolean urgent = running && remaining <= 10_000;
        long now = System.currentTimeMillis();

        int accent = 0xFF000000 | cfg.hudColor;
        int ring = urgent ? 0xFFFF3B3B : accent;

        int rows = Math.max(1, Math.min(MAX_ROWS, s.bids.size()));
        int listH = 14 + rows * ROW_H;
        int h = TITLE_H + BODY_H + listH + 6;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int x = (sw - W) / 2 + cfg.hudOffsetX;
        int y = (sh - h) / 2 + cfg.hudOffsetY;

        // --- Panel ---------------------------------------------------------
        ctx.fillGradient(x, y, x + W, y + h, 0xE0101828, 0xE0050810);
        ctx.fill(x, y, x + W, y + TITLE_H, alpha(accent, 0x30));
        ctx.fill(x, y + TITLE_H, x + W, y + TITLE_H + 1, alpha(accent, 0xB0));

        // dezenter Scan-Strahl
        int sweep = y + TITLE_H + 2 + (int) ((now / 25) % (h - TITLE_H - 4));
        ctx.fill(x + 1, sweep, x + W - 1, sweep + 1, alpha(accent, 0x22));

        // Rand + Ecken (bei wenig Zeit pulsierend)
        int borderA = urgent ? 0x60 + (int) (0x50 * (0.5 + 0.5 * Math.sin(now / 120.0))) : 0x70;
        ctx.drawBorder(x, y, W, h, alpha(ring, borderA));
        corners(ctx, x, y, W, h, ring);

        // --- Titel ---------------------------------------------------------
        ctx.drawCenteredTextWithShadow(font, Text.literal("TikTokSW auction").formatted(Formatting.BOLD),
            x + W / 2, y + 6, 0xFFFFFFFF);

        // --- Timer-Ring ----------------------------------------------------
        int cx = x + 16 + RING_R;
        int cy = y + TITLE_H + 2 + BODY_H / 2;
        drawTimer(ctx, cx, cy, s.fraction(), ring, now);
        String time = running ? AuctionState.formatTime(remaining) : "00:00";
        ctx.drawCenteredTextWithShadow(font, Text.literal(time), cx, cy - 4, urgent || !running ? 0xFFFF6B6B : 0xFFFFFFFF);

        // --- Item + Infos --------------------------------------------------
        int x0 = x + 16 + RING_R * 2 + 16;
        int avail = x + W - 10 - x0;
        int iy = y + TITLE_H + 8;
        ctx.drawItem(s.item, x0, iy);
        String name = s.item.isEmpty() ? "-" : s.item.getName().getString();
        if (s.item.getCount() > 1) name += " x" + s.item.getCount();
        ctx.drawText(font, font.trimToWidth(name, avail - 22), x0 + 22, iy + 4, 0xFFFFFFFF, true);
        ctx.drawText(font, "Mindestgebot: " + AuctionState.money(s.minBid), x0, iy + 24, 0xFF9AA7B8, false);

        if (running) {
            boolean blink = (now / 500) % 2 == 0;
            ctx.fill(x0, iy + 39, x0 + 5, iy + 44, blink ? 0xFFFF3B3B : 0x66FF3B3B);
            ctx.drawText(font, "LIVE", x0 + 9, iy + 38, 0xFFFF6B6B, false);
        } else {
            ctx.drawText(font, "BEENDET", x0, iy + 38, 0xFFFF6B6B, false);
        }

        // --- Gebote --------------------------------------------------------
        int ly = y + TITLE_H + BODY_H + 4;
        ctx.drawText(font, "GEBOTE", x + 10, ly, alpha(accent, 0xFF), false);
        ctx.fill(x + 48, ly + 4, x + W - 10, ly + 5, alpha(accent, 0x50));

        if (s.bids.isEmpty()) {
            ctx.drawCenteredTextWithShadow(font, Text.literal("Noch kein Gebot"), x + W / 2, ly + 17, 0xFF8090A0);
        } else {
            for (int i = 0; i < rows; i++) {
                AuctionState.Bid b = s.bids.get(s.bids.size() - 1 - i);
                int ry = ly + 14 + i * ROW_H;
                boolean first = i == 0;
                boolean winner = first && !running;
                if (first) {
                    ctx.fill(x + 6, ry - 2, x + W - 6, ry + ROW_H - 2, alpha(winner ? 0xFFFFC107 : accent, 0x30));
                    ctx.fill(x + 6, ry - 2, x + 8, ry + ROW_H - 2, winner ? 0xFFFFC107 : accent);
                }
                ctx.drawText(font, "#" + (i + 1), x + 12, ry + 1, first ? 0xFFFFFFFF : 0xFF7F8C9B, false);
                head(ctx, mc, b.player(), x + 32, ry - 1, 10, accent);
                int col = winner ? 0xFFFFD54F : (first ? 0xFFFFFFFF : 0xFFC4CDD8);
                String label = (winner ? "\u2605 " : "") + b.player();
                ctx.drawText(font, font.trimToWidth(label, 100), x + 48, ry + 1, col, true);
                String amt = AuctionState.money(b.amount());
                ctx.drawText(font, amt, x + W - 12 - font.getWidth(amt), ry + 1, first ? accent : 0xFFC4CDD8, first);
            }
        }
    }

    // ------------------------------------------------------------ Zeichnen

    private static void drawTimer(DrawContext ctx, int cx, int cy, double frac, int color, long now) {
        int steps = (int) (2 * Math.PI * RING_R * 2);
        // Spur
        for (int i = 0; i < steps; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / steps;
            int px = cx + (int) Math.round(Math.cos(a) * RING_R);
            int py = cy + (int) Math.round(Math.sin(a) * RING_R);
            if ((double) i / steps < frac) ctx.fill(px - 1, py - 1, px + 2, py + 2, color);
            else ctx.fill(px, py, px + 1, py + 1, 0x33FFFFFF);
        }
        // Skala (60 Ticks), verbleibende leuchten
        int ticks = 60;
        for (int i = 0; i < ticks; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / ticks;
            int len = i % 5 == 0 ? 3 : 1;
            for (int d = 0; d <= len; d++) {
                int px = cx + (int) Math.round(Math.cos(a) * (RING_R + 5 + d));
                int py = cy + (int) Math.round(Math.sin(a) * (RING_R + 5 + d));
                boolean on = (double) i / ticks < frac;
                ctx.fill(px, py, px + 1, py + 1, on ? alpha(color, 0x90) : 0x22FFFFFF);
            }
        }
        // rotierender Punkt am Ring-Ende
        if (frac > 0) {
            double a = -Math.PI / 2 + 2 * Math.PI * frac;
            int px = cx + (int) Math.round(Math.cos(a) * RING_R);
            int py = cy + (int) Math.round(Math.sin(a) * RING_R);
            ctx.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFFFFF);
        }
    }

    private static void corners(DrawContext ctx, int x, int y, int w, int h, int c) {
        int l = 10, t = 2;
        ctx.fill(x - 1, y - 1, x + l, y - 1 + t, c);          // oben links
        ctx.fill(x - 1, y - 1, x - 1 + t, y + l, c);
        ctx.fill(x + w - l, y - 1, x + w + 1, y - 1 + t, c);  // oben rechts
        ctx.fill(x + w + 1 - t, y - 1, x + w + 1, y + l, c);
        ctx.fill(x - 1, y + h + 1 - t, x + l, y + h + 1, c);  // unten links
        ctx.fill(x - 1, y + h - l, x - 1 + t, y + h + 1, c);
        ctx.fill(x + w - l, y + h + 1 - t, x + w + 1, y + h + 1, c); // unten rechts
        ctx.fill(x + w + 1 - t, y + h - l, x + w + 1, y + h + 1, c);
    }

    /** Zeichnet den Minecraft-Skin-Kopf des Spielers (Fallback: Buchstabe). */
    private static void head(DrawContext ctx, MinecraftClient mc, String name, int x, int y, int size, int accent) {
        String key = name.toLowerCase(Locale.ROOT);
        SkinTextures tex = null;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(name);
            if (entry != null) {
                tex = entry.getSkinTextures();
                SKINS.put(key, tex);
            }
        }
        if (tex == null) tex = SKINS.get(key);

        ctx.drawBorder(x - 1, y - 1, size + 2, size + 2, alpha(accent, 0xA0));
        if (tex != null) {
            PlayerSkinDrawer.draw(ctx, tex, x, y, size);
        } else {
            ctx.fill(x, y, x + size, y + size, alpha(accent, 0x55));
            String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            ctx.drawText(mc.textRenderer, letter, x + (size - mc.textRenderer.getWidth(letter)) / 2 + 1, y + 1, 0xFFFFFFFF, false);
        }
    }

    static int alpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
