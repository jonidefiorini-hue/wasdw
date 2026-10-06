package de.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.IntConsumer;

/** Farbwahl für das HUD: Presets, RGB-Regler und Hex-Eingabe. */
public class ColorScreen extends Screen {
    private static final int[] PRESETS = {
        0x00E5FF, 0x39FF14, 0xFF2BD6, 0xFFB300, 0xFF3B3B, 0x7C4DFF, 0x1E90FF, 0xFFFFFF
    };

    private final Screen parent;
    private ChannelSlider r, g, b;
    private TextFieldWidget hex;
    private boolean syncing;

    public ColorScreen(Screen parent) {
        super(Text.literal("HUD-Farbe"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int c = AuctionConfig.INSTANCE.hudColor;

        r = addDrawableChild(new ChannelSlider(cx - 100, 90, 200, "Rot", (c >> 16) & 0xFF, v -> fromSliders()));
        g = addDrawableChild(new ChannelSlider(cx - 100, 114, 200, "Grün", (c >> 8) & 0xFF, v -> fromSliders()));
        b = addDrawableChild(new ChannelSlider(cx - 100, 138, 200, "Blau", c & 0xFF, v -> fromSliders()));

        hex = new TextFieldWidget(textRenderer, cx - 100, 166, 80, 18, Text.literal("Hex"));
        hex.setMaxLength(7);
        hex.setText(String.format("#%06X", c));
        hex.setChangedListener(this::fromHex);
        addDrawableChild(hex);

        addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), btn -> close())
            .dimensions(cx + 20, 165, 80, 20).build());
    }

    private void apply(int rgb) {
        AuctionConfig.INSTANCE.hudColor = rgb & 0xFFFFFF;
    }

    private void fromSliders() {
        if (syncing) return;
        syncing = true;
        int rgb = (r.get() << 16) | (g.get() << 8) | b.get();
        apply(rgb);
        hex.setText(String.format("#%06X", rgb));
        syncing = false;
    }

    private void fromHex(String text) {
        if (syncing) return;
        String t = text.startsWith("#") ? text.substring(1) : text;
        if (t.length() != 6) return;
        try {
            int rgb = Integer.parseInt(t, 16);
            syncing = true;
            apply(rgb);
            r.set((rgb >> 16) & 0xFF);
            g.set((rgb >> 8) & 0xFF);
            b.set(rgb & 0xFF);
            syncing = false;
        } catch (NumberFormatException ignored) {
        }
    }

    private int swatchX(int i) { return width / 2 - 100 + i * 25; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < PRESETS.length; i++) {
                int sx = swatchX(i);
                if (mx >= sx && mx < sx + 20 && my >= 58 && my < 78) {
                    int rgb = PRESETS[i];
                    syncing = true;
                    apply(rgb);
                    r.set((rgb >> 16) & 0xFF);
                    g.set((rgb >> 8) & 0xFF);
                    b.set(rgb & 0xFF);
                    hex.setText(String.format("#%06X", rgb));
                    syncing = false;
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

        // Vorschau
        ctx.fill(cx - 100, 14, cx + 100, 46, 0xE0101828);
        ctx.drawBorder(cx - 100, 14, 200, 32, accent);
        ctx.fill(cx - 100, 14, cx + 100, 15, accent);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("TikTokSW auction").formatted(Formatting.BOLD), cx, 26, 0xFFFFFFFF);

        // Presets
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = swatchX(i);
            ctx.fill(sx, 58, sx + 20, 78, 0xFF000000 | PRESETS[i]);
            boolean active = PRESETS[i] == AuctionConfig.INSTANCE.hudColor;
            ctx.drawBorder(sx, 58, 20, 20, active ? 0xFFFFFFFF : 0x66FFFFFF);
        }
    }

    @Override
    public void close() {
        AuctionConfig.save();
        client.setScreen(parent);
    }

    private static class ChannelSlider extends SliderWidget {
        private final String label;
        private final IntConsumer onChange;

        ChannelSlider(int x, int y, int w, String label, int value, IntConsumer onChange) {
            super(x, y, w, 20, Text.empty(), value / 255.0);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        int get() { return (int) Math.round(value * 255); }

        void set(int v) {
            this.value = v / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + get()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(get());
        }
    }
}
