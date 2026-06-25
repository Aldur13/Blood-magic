package dev.gamingartum.bloodmagic.client.screen;

import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import dev.gamingartum.bloodmagic.network.ActivateBloodBuffPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BloodMenuScreen extends Screen {

    // Layout — targets 427×240 GUI space (854×480 at scale 2)
    private static final int PANEL_W  = 280;
    private static final int ROW_H    = 18;
    private static final int ROW_PAD  = 1;
    private static final int HEADER_H = 16;
    private static final int FOOTER_H = 8;
    private static final int PANEL_H  = HEADER_H + BloodBuff.values().length * (ROW_H + ROW_PAD) + FOOTER_H;

    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    // Cost column x offset from panel left
    private static final int COST_X_OFFSET = 140;

    private int px, py;

    public BloodMenuScreen() {
        super(Component.literal("Blood Magic"));
    }

    @Override
    protected void init() {
        px = (this.width  - PANEL_W) / 2;
        py = (this.height - PANEL_H) / 2;

        BloodBuff[] buffs = BloodBuff.values();
        for (int i = 0; i < buffs.length; i++) {
            final BloodBuff buff = buffs[i];
            int rowY = py + HEADER_H + i * (ROW_H + ROW_PAD);

            boolean canAfford = canAfford(buff);
            boolean alreadyOn = BloodData.hasActiveBuff(minecraft.player, buff);

            Button btn = Button.builder(
                    Component.literal("► Activate"),
                    b -> {
                        ClientPlayNetworking.send(new ActivateBloodBuffPayload(buff));
                        this.onClose();
                    })
                .bounds(px + PANEL_W - BTN_W - 4, rowY + (ROW_H - BTN_H) / 2, BTN_W, BTN_H)
                .build();
            btn.active = canAfford && !alreadyOn;
            this.addRenderableWidget(btn);
        }

        this.addRenderableWidget(
            Button.builder(Component.literal("x"), b -> this.onClose())
                .bounds(px + PANEL_W - 14, py + 1, 13, 13).build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        BloodBuff[] buffs = BloodBuff.values();

        // ── Step 1: all fills (backgrounds) ─────────────────────────────────
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, 0xFF660000);
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF1A0000);
        g.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, 0xFF440000);

        for (int i = 0; i < buffs.length; i++) {
            BloodBuff buff = buffs[i];
            int rowY = py + HEADER_H + i * (ROW_H + ROW_PAD);
            boolean alreadyOn = BloodData.hasActiveBuff(minecraft.player, buff);
            boolean canAfford = canAfford(buff);
            int bg = alreadyOn  ? 0xFF3A1A00
                   : !canAfford ? 0xFF1E1E1E
                   :              0xFF280000;
            g.fill(px + 2, rowY, px + PANEL_W - 2, rowY + ROW_H, bg);
        }

        // ── Step 2: widgets (buttons render on top of backgrounds) ──────────
        super.extractRenderState(g, mouseX, mouseY, delta);

        // ── Step 3: text (drawn on top of everything) ────────────────────────
        g.text(this.font, "§c§lBlood Magic", px + 4, py + 5, 0xFFFF4444);

        for (int i = 0; i < buffs.length; i++) {
            BloodBuff buff = buffs[i];
            int rowY  = py + HEADER_H + i * (ROW_H + ROW_PAD);
            boolean alreadyOn = BloodData.hasActiveBuff(minecraft.player, buff);
            boolean canAfford = canAfford(buff);

            int nameColor = alreadyOn ? 0xFFFFAA00 : canAfford ? 0xFFFF8888 : 0xFF666666;
            int descColor = alreadyOn ? 0xFFCC8800 : canAfford ? 0xFFAA5555 : 0xFF444444;
            int costColor = canAfford && !alreadyOn ? 0xFFFF4444 : 0xFF555555;

            g.text(this.font, buff.displayName, px + 6,              rowY + 2,  nameColor);
            g.text(this.font, buff.description,  px + 6,             rowY + 10, descColor);

            int costX = px + COST_X_OFFSET;
            g.text(this.font, heartStr(buff.heartCost), costX,        rowY + 2,  costColor);
            g.text(this.font, buff.durationString(),    costX,        rowY + 10, descColor);

            if (alreadyOn) {
                g.text(this.font, "ACTIVE",
                    px + PANEL_W - BTN_W - 4 + 6, rowY + (ROW_H - 6) / 2, 0xFFFFAA00);
            }
        }

        g.centeredText(this.font,
            Component.literal("§7Hearts regen 10min after buff expires"),
            px + PANEL_W / 2, py + PANEL_H - 7, 0xFF883333);
    }

    private boolean canAfford(BloodBuff buff) {
        if (minecraft.player == null) return false;
        return minecraft.player.getHealth() > buff.healthCost();
    }

    private static String heartStr(int hearts) {
        return "♥".repeat(Math.min(hearts, 5)) + (hearts > 5 ? "+" : "") + " " + hearts + "♥";
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
