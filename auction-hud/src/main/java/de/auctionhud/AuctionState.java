package de.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuctionState {
    public enum Phase { IDLE, RUNNING, ENDED }

    public record Bid(String player, double amount) {}

    public static final AuctionState INSTANCE = new AuctionState();

    public Phase phase = Phase.IDLE;
    public ItemStack item = ItemStack.EMPTY;
    public double minBid;
    public long endTime;
    public long durationMs;
    /** Alle gültigen Gebote, das letzte ist das höchste. */
    public final List<Bid> bids = new ArrayList<>();
    public final Map<String, Double> totals = new HashMap<>();

    public void start(ItemStack stack, double min, long durationMs) {
        reset();
        this.item = stack;
        this.minBid = min;
        this.durationMs = durationMs;
        this.endTime = System.currentTimeMillis() + durationMs;
        this.phase = Phase.RUNNING;
        play(false);
    }

    public void reset() {
        phase = Phase.IDLE;
        item = ItemStack.EMPTY;
        bids.clear();
        totals.clear();
        minBid = 0;
        endTime = 0;
        durationMs = 0;
    }

    public Bid top() {
        return bids.isEmpty() ? null : bids.get(bids.size() - 1);
    }

    public long remainingMs() {
        return Math.max(0L, endTime - System.currentTimeMillis());
    }

    /** Anteil der verbleibenden Zeit, 1.0 = voll, 0.0 = abgelaufen. */
    public double fraction() {
        if (phase == Phase.ENDED || durationMs <= 0) return 0;
        return Math.min(1.0, remainingMs() / (double) durationMs);
    }

    public void tick() {
        if (phase == Phase.RUNNING && remainingMs() <= 0) {
            phase = Phase.ENDED;
            play(true);
        }
    }

    public void onPayment(String player, double amount) {
        if (phase != Phase.RUNNING) return;
        double value = amount;
        if (AuctionConfig.INSTANCE.cumulativeBids) {
            totals.merge(player, amount, Double::sum);
            value = totals.get(player);
        }
        Bid top = top();
        if (value >= minBid && (top == null || value > top.amount())) {
            bids.add(new Bid(player, value));
            play(false);
        }
    }

    private static void play(boolean end) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(
            end ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
    }

    // ---------------------------------------------------------------- Helfer

    public static String money(double v) {
        if (v == Math.floor(v)) return "$" + String.format(Locale.GERMANY, "%,d", (long) v);
        return "$" + String.format(Locale.GERMANY, "%,.2f", v);
    }

    public static String formatTime(long ms) {
        long sec = (ms + 999) / 1000;
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    /** "1500", "1.5k", "2m", "1,000", "1.000,50" ... -> Betrag, NaN bei Fehler. */
    public static double parseAmount(String raw) {
        try {
            String s = raw.trim().toLowerCase(Locale.ROOT).replace("$", "");
            double mult = 1;
            if (s.endsWith("k")) { mult = 1e3; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("m")) { mult = 1e6; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("b")) { mult = 1e9; s = s.substring(0, s.length() - 1); }
            s = s.trim();
            int dot = s.lastIndexOf('.'), comma = s.lastIndexOf(',');
            if (dot >= 0 && comma >= 0) {
                char dec = dot > comma ? '.' : ',';
                char thou = dec == '.' ? ',' : '.';
                s = s.replace(String.valueOf(thou), "").replace(dec, '.');
            } else if (dot >= 0 || comma >= 0) {
                char sep = dot >= 0 ? '.' : ',';
                int idx = s.lastIndexOf(sep);
                boolean many = s.indexOf(sep) != idx;
                boolean thousands = many || (mult == 1 && s.length() - idx - 1 == 3);
                s = thousands ? s.replace(String.valueOf(sep), "") : s.replace(sep, '.');
            }
            return Double.parseDouble(s) * mult;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** "30s", "5m", "1h30m", "90" (= Sekunden) -> Millisekunden, -1 bei Fehler. */
    public static long parseDuration(String raw) {
        try {
            String s = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
            if (s.isEmpty()) return -1;
            if (s.matches("\\d+")) return Long.parseLong(s) * 1000L;
            if (!s.matches("(\\d+[hms])+")) return -1;
            long total = 0;
            var m = java.util.regex.Pattern.compile("(\\d+)([hms])").matcher(s);
            while (m.find()) {
                long n = Long.parseLong(m.group(1));
                total += switch (m.group(2)) { case "h" -> n * 3600; case "m" -> n * 60; default -> n; };
            }
            return total * 1000L;
        } catch (Exception e) {
            return -1;
        }
    }
}
