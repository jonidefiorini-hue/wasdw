package de.auctionhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionHudClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("auctionhud");

    private static KeyBinding openKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        AuctionConfig.load();
        ChatListener.compile();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.auctionhud.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.auctionhud"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.auctionhud.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.auctionhud"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AuctionState.INSTANCE.tick();
            while (toggleKey.wasPressed()) {
                AuctionConfig.INSTANCE.hudVisible = !AuctionConfig.INSTANCE.hudVisible;
                AuctionConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Auktions-HUD: "
                        + (AuctionConfig.INSTANCE.hudVisible ? "an" : "aus")), true);
                }
            }
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new AuctionScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register(AuctionHud::render);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) ChatListener.handle(message.getString());
        });
    }
}
