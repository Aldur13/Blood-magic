package dev.gamingartum.bloodmagic.registry;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.menu.BloodAltarMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {

    public static final MenuType<BloodAltarMenu> BLOOD_ALTAR_MENU = new MenuType<>(
        BloodAltarMenu::new, FeatureFlags.VANILLA_SET
    );

    public static void initialize() {
        Registry.register(BuiltInRegistries.MENU, BloodMagic.id("blood_altar"), BLOOD_ALTAR_MENU);
    }
}
