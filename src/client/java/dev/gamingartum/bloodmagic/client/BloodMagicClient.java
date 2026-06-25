package dev.gamingartum.bloodmagic.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.gamingartum.bloodmagic.client.hud.BloodHud;
import dev.gamingartum.bloodmagic.client.screen.BloodMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class BloodMagicClient implements ClientModInitializer {

    private static boolean prevBPressed = false;

    @Override
    public void onInitializeClient() {
        BloodHud.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gui == null) return;

            boolean bPressed = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_B);

            if (bPressed && !prevBPressed && client.gui.screen() == null) {
                Minecraft.getInstance().gui.setScreen(new BloodMenuScreen());
            }
            prevBPressed = bPressed;
        });
    }
}
