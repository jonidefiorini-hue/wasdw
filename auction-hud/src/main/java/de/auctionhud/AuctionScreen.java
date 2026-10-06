package de.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Steuer-Menü: Item wählen, Mindestgebot + Dauer setzen, Auktion starten/abbrechen, HUD/Farbe. */
public class AuctionScreen extends Screen {
    private static final int SLOT = 18;
    private static final int COLS = 9;

    private static String lastMin = "1000";
    private static String lastDuration = "60s";
    private static int selectedSlot = -1;

    private TextFieldWidget minBidField;
    private TextFieldWidget durationField;
    private ButtonWidget hudButton;
    private String error = "";
    private int gridX, gridY;

    public AuctionScreen() {
        super(Text.literal("Auktion"));
    }

    @Override
    protected void init() {
        AuctionState st = AuctionState.INSTANCE;
        int cx = width / 2;
        gridX = cx - COLS * SLOT / 2;
        gridY = 38;

        minBidField = new TextFieldWidget(textRenderer, cx - 81, 134, 78, 18, Text.literal("Mindestgebot"));
        minBidField.setMaxLength(24);
        minBidField.setText(lastMin);
        addDrawableChild(minBidField);

        durationField = new TextFieldWidget(textRenderer, cx + 3, 134, 78, 18, Text.literal("Dauer"));
        durationField.setMaxLength(16);
        durationField.setText(lastDuration);
        addDrawableChild(durationField);

        String label = switch (st.phase) {
            case IDLE -> "Auktion starten";
            case RUNNING -> "Auktion abbrechen";
            case ENDED -> "Zurücksetzen";
        };
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
            if (st.phase == AuctionState.Phase.IDLE) start();
            else { st.reset(); close(); }
        }).dimensions(cx - 81, 160, 162, 20).build());

        hudButton = addDrawableChild(ButtonWidget.builder(hudLabel(), b -> {
            AuctionConfig.INSTANCE.hudVisible = !AuctionConfig.INSTANCE.hudVisible;
            AuctionConfig.save();
            b.setMessage(hudLabel());
        }).dimensions(cx - 81, 184, 79, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Farbe \u2026"),
            b -> client.setScreen(new ColorScreen(this))).dimensions(cx + 2, 184, 79, 20).build());
    }

    private static Text hudLabel() {
        return Text.literal("HUD: " + (AuctionConfig.INSTANCE.hudVisible ? "AN" : "AUS"));
    }

    private void start() {
        if (client == null || client.player == null) return;
        if (selectedSlot < 0 || client.player.getInventory().getStack(invIndex(selectedSlot)).isEmpty()) {
            error = "Wähle zuerst ein Item aus.";
            return;
        }
        double min = AuctionState.parseAmount(minBidField.getText());
        if (Double.isNaN(min) || min < 0) { error = "Ungültiges Mindestgebot."; return; }
        long dur = AuctionState.parseDuration(durationField.getText());
        if (dur <= 0) { error = "Ungültige Dauer (z.B. 30s, 5m, 1h30m)."; return; }

        lastMin = minBidField.getText();
        lastDuration = durationField.getText();
        ItemStack stack = client.player.getInventory().getStack(invIndex(selectedSlot)).copy();
        AuctionState.INSTANCE.start(stack, min, dur);
        close();
    }

    /** Anzeige-Slot (0..35) -> Inventar-Index: erst Hauptinventar, unten die Hotbar. */
    private static int invIndex(int display) {
        return display < 27 ? 9 + display : display - 27;
    }

    private int slotX(int d) { return gridX + (d % COLS) * SLOT; }

    private int slotY(int d) {
        int row = d / COLS;
        return gridY + row * SLOT + (row == 3 ? 4 : 0);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && AuctionState.INSTANCE.phase == AuctionState.Phase.IDLE) {
            for (int d = 0; d < 36; d++) {
                int sx = slotX(d), sy = slotY(d);
                if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) {
                    selectedSlot = d;
                    error = "";
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int accent = 0xFF000000 | AuctionConfig.INSTANCE.hudColor;
        AuctionState st = AuctionState.INSTANCE;

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("TikTokSW auction"), cx, 12, accent);
        String sub = st.phase == AuctionState.Phase.IDLE ? "Item für die Auktion wählen"
            : st.phase == AuctionState.Phase.RUNNING ? "Auktion läuft" : "Auktion beendet";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(sub), cx, 24, 0xFFAAAAAA);

        for (int d = 0; d < 36; d++) {
            int sx = slotX(d), sy = slotY(d);
            boolean sel = d == selectedSlot;
            boolean hover = mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT;
            ctx.fill(sx, sy, sx + SLOT, sy + SLOT, sel ? (accent & 0x00FFFFFF) | 0x66000000 : 0x88000000);
            ctx.drawBorder(sx, sy, SLOT, SLOT, sel ? accent : hover ? 0xFFFFFFFF : 0x55FFFFFF);
            if (client != null && client.player != null) {
                ItemStack stack = client.player.getInventory().getStack(invIndex(d));
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx + 1, sy + 1);
                    ctx.drawStackOverlay(textRenderer, stack, sx + 1, sy + 1);
                }
            }
        }

        ctx.drawText(textRenderer, "Mindestgebot", cx - 81, 123, 0xFFAAAAAA, false);
        ctx.drawText(textRenderer, "Dauer", cx + 3, 123, 0xFFAAAAAA, false);
        if (!error.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(error), cx, 210, 0xFFFF5555);
    }
}
