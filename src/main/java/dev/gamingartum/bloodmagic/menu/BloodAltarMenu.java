package dev.gamingartum.bloodmagic.menu;

import dev.gamingartum.bloodmagic.BloodMagicLogic;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import dev.gamingartum.bloodmagic.registry.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * Chest-style menu (vanilla AbstractContainerMenu) so any vanilla client can
 * open and use it — item stacks + slot clicks are synced by the base game.
 * The top row of slots is display-only: clicking one activates that buff
 * server-side and never actually moves an item.
 */
public class BloodAltarMenu extends AbstractContainerMenu {

    private static final int COLS = 5;
    private static final BloodBuff[] BUFFS = BloodBuff.values();

    private final Container display = new SimpleContainer(BUFFS.length);
    private final Player player;

    public BloodAltarMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.BLOOD_ALTAR_MENU, containerId);
        this.player = playerInventory.player;
        refreshDisplay();

        for (int i = 0; i < BUFFS.length; i++) {
            int x = 8 + (i % COLS) * 18;
            int y = 18 + (i / COLS) * 18;
            addSlot(new DisplaySlot(display, i, x, y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private void refreshDisplay() {
        boolean isServer = player instanceof ServerPlayer;
        for (int i = 0; i < BUFFS.length; i++) {
            BloodBuff buff = BUFFS[i];
            ItemStack stack = new ItemStack(buff.icon);
            stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(buff.displayName).withStyle(s -> s.withItalic(false)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal(buff.description));
            lore.add(Component.literal(buff.percentString() + "  " + buff.durationString()));

            if (isServer && BloodData.hasActiveBuff((ServerPlayer) player, buff)) {
                lore.add(Component.literal("Already active"));
            } else if (player.getHealth() <= buff.healthCost(player.getMaxHealth())) {
                lore.add(Component.literal("Not enough health"));
            } else {
                lore.add(Component.literal("Click to activate"));
            }

            stack.set(DataComponents.LORE, new ItemLore(lore));
            display.setItem(i, stack);
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < BUFFS.length) {
            if (player instanceof ServerPlayer serverPlayer) {
                BloodMagicLogic.activateBuff(serverPlayer, BUFFS[slotId]);
                refreshDisplay();
                broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** A slot that always shows the current display stack but can never be taken or filled. */
    private static class DisplaySlot extends Slot {
        DisplaySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
