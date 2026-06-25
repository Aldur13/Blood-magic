package dev.gamingartum.bloodmagic.client.hud;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class BloodHud implements HudElement {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(BloodMagic.MOD_ID, "blood_hud");

    private static final int X         = 10;
    private static final int Y_START   = 10;
    private static final int ROW_H     = 11;
    private static final int BAR_W     = 80;
    private static final int BAR_H     = 4;

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, ID, new BloodHud());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        List<ActiveBuffEntry> entries = BloodData.get(mc.player);
        if (entries.isEmpty()) return;

        long currentTick = mc.level.getGameTime();
        int y = Y_START;

        for (ActiveBuffEntry entry : entries) {
            boolean active = entry.isEffectActive(currentTick);
            long endTick   = active ? entry.expiresAtTick() : entry.regenAtTick();
            long remaining = endTick - currentTick;

            int totalTicks = active
                ? entry.buff().durationTicks
                : (int) ActiveBuffEntry.REGEN_COOLDOWN_TICKS;
            float ratio = Math.max(0f, Math.min(1f, (float) remaining / totalTicks));
            int filled = (int)(BAR_W * ratio);

            int barColor  = active   ? 0xFFCC2222 : 0xFF444488;
            int textColor = active   ? 0xFFFF6666 : 0xFF8888BB;
            String label  = active
                ? entry.buff().displayName + " §c" + ticksToTime(remaining)
                : "§7" + entry.buff().displayName + " regen " + ticksToTime(remaining);

            // Bar background
            g.fill(X, y + ROW_H - BAR_H, X + BAR_W, y + ROW_H, 0xFF222222);
            // Bar fill
            if (filled > 0) g.fill(X, y + ROW_H - BAR_H, X + filled, y + ROW_H, barColor);
            // Label above bar
            g.text(mc.font, label, X, y, textColor);

            y += ROW_H + BAR_H + 4;
        }
    }

    private static String ticksToTime(long ticks) {
        long seconds = ticks / 20;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
