package dev.gamingartum.bloodmagic.menu;

import dev.gamingartum.bloodmagic.buff.BuffManager;
import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * The buff menu, built entirely on the server out of a vanilla {@code minecraft:generic_9x2}
 * container. An unmodified client renders it as an ordinary double-row chest, which is what
 * lets this mod run server-side only.
 */
public final class BloodMenu extends ChestMenu {

    private static final int ROWS = 2;
    private static final int SIZE = ROWS * 9;

    /** Slot for each buff, in {@link BloodBuff} declaration order: two centred rows of five. */
    private static final int[] BUFF_SLOTS = { 2, 3, 4, 5, 6, 11, 12, 13, 14, 15 };
    private static final int CLOSE_SLOT = 17;

    private final ServerPlayer owner;
    private final SimpleContainer display;

    private BloodMenu(int containerId, Inventory playerInventory, ServerPlayer owner) {
        super(MenuType.GENERIC_9x2, containerId, playerInventory, new SimpleContainer(SIZE), ROWS);
        this.owner = owner;
        this.display = (SimpleContainer) getContainer();
        refresh();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, p) -> new BloodMenu(containerId, inventory, (ServerPlayer) p),
            Component.literal("Blood Magic").withStyle(ChatFormatting.DARK_RED)));
    }

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    /**
     * Every click is intercepted. Nothing in this container is a real item, so no click may
     * ever move a stack: clicks on a buff icon spend hearts, and everything else is swallowed.
     */
    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < SIZE && !handleClick(slotId)) {
            return;   // menu was closed; nothing left to resync
        }

        // The client optimistically applied whatever the click looked like, so put its view
        // back the way the server sees it.
        setCarried(ItemStack.EMPTY);
        sendAllDataToRemote();
    }

    /** Returns false if the menu closed itself and should not be resynced. */
    private boolean handleClick(int slotId) {
        if (slotId == CLOSE_SLOT) {
            owner.closeContainer();
            return false;
        }

        BloodBuff buff = buffForSlot(slotId);
        if (buff == null) return true;

        BuffManager.Result result = BuffManager.activate(owner, buff);

        switch (result) {
            case OK -> owner.sendSystemMessage(Component.literal(
                "You sacrifice " + buff.heartCost + " hearts for " + buff.displayName + ".")
                .withStyle(ChatFormatting.DARK_RED));
            case ALREADY_ACTIVE -> deny(buff.displayName + " is already coursing through you.");
            case COOLING_DOWN -> deny("Your blood has not recovered from " + buff.displayName + " yet.");
            case NOT_ENOUGH_BLOOD -> deny("You do not have " + buff.heartCost + " hearts to give.");
            case BODY_TOO_WEAK -> deny("Your body cannot survive another sacrifice.");
        }

        refresh();
        return true;
    }

    private void deny(String message) {
        owner.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    /** No shift-click transfers: there is nothing here to take. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive();
    }

    private static BloodBuff buffForSlot(int slotId) {
        BloodBuff[] buffs = BloodBuff.values();
        for (int i = 0; i < BUFF_SLOTS.length && i < buffs.length; i++) {
            if (BUFF_SLOTS[i] == slotId) return buffs[i];
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void refresh() {
        long now = BuffManager.gameTime(owner);
        BloodBuff[] buffs = BloodBuff.values();

        for (int slot = 0; slot < SIZE; slot++) {
            display.setItem(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < BUFF_SLOTS.length && i < buffs.length; i++) {
            display.setItem(BUFF_SLOTS[i], icon(buffs[i], now));
        }
        display.setItem(CLOSE_SLOT, closeIcon());

        broadcastChanges();
    }

    private ItemStack icon(BloodBuff buff, long now) {
        ActiveBuffEntry entry = BloodData.entry(owner, buff);
        boolean active = entry != null && entry.isEffectActive(now);
        boolean cooling = entry != null && !active;
        boolean affordable = owner.getHealth() > buff.healthCost()
            && owner.getMaxHealth() - buff.healthCost() >= 2.0f;

        ItemStack stack = new ItemStack(buff.icon, Math.max(1, buff.heartCost));

        ChatFormatting nameColor = active ? ChatFormatting.GOLD
            : cooling ? ChatFormatting.DARK_GRAY
            : affordable ? ChatFormatting.RED
            : ChatFormatting.DARK_GRAY;
        stack.set(DataComponents.CUSTOM_NAME, plain(buff.displayName).withStyle(nameColor));

        List<Component> lore = new ArrayList<>();
        lore.add(plain(buff.description).withStyle(ChatFormatting.GRAY));
        lore.add(plain("Cost: " + buff.heartCost + " hearts").withStyle(ChatFormatting.DARK_RED));
        lore.add(plain("Lasts: " + buff.durationString()).withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.empty());

        if (active) {
            lore.add(plain("Active - " + clock(entry.remaining(now)) + " left")
                .withStyle(ChatFormatting.GOLD));
        } else if (cooling) {
            lore.add(plain("Hearts return in " + clock(entry.remaining(now)))
                .withStyle(ChatFormatting.BLUE));
        } else if (!affordable) {
            lore.add(plain("Not enough blood").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lore.add(plain("Click to sacrifice").withStyle(ChatFormatting.GREEN));
        }

        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (active) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        // Swords and armour would otherwise print their own damage/armour lines under ours.
        stack.set(DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true));

        return stack;
    }

    private ItemStack closeIcon() {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.set(DataComponents.CUSTOM_NAME, plain("Close").withStyle(ChatFormatting.RED));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            plain("Hearts return 10 minutes after a buff ends")
                .withStyle(ChatFormatting.DARK_GRAY))));
        return stack;
    }

    /** Item name and lore components default to italic; this turns that off. */
    private static MutableComponent plain(String text) {
        return Component.literal(text).setStyle(Style.EMPTY.withItalic(false));
    }

    private static String clock(long ticks) {
        long seconds = ticks / 20L;
        return (seconds / 60L) + ":" + String.format("%02d", seconds % 60L);
    }
}
