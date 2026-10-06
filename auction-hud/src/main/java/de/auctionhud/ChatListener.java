package de.auctionhud;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {
    private static final List<Pattern> PATTERNS = new ArrayList<>();

    public static void compile() {
        PATTERNS.clear();
        for (String p : AuctionConfig.INSTANCE.paymentPatterns) {
            try {
                PATTERNS.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                AuctionHudClient.LOGGER.error("Ungültiges Pattern: {}", p, e);
            }
        }
    }

    public static void handle(String msg) {
        if (AuctionConfig.INSTANCE.debug) AuctionHudClient.LOGGER.info("[AuctionHUD] {}", msg);
        if (AuctionState.INSTANCE.phase != AuctionState.Phase.RUNNING) return;
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(msg);
            if (m.find()) {
                double amount = AuctionState.parseAmount(m.group("amount"));
                if (!Double.isNaN(amount) && amount > 0) {
                    AuctionState.INSTANCE.onPayment(m.group("player"), amount);
                }
                return;
            }
        }
    }
}
