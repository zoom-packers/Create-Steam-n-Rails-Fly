package com.railwayteam.railways.internal.compat.registrate.builders;

import com.railwayteam.railways.internal.compat.registrate.Registrate;
import com.railwayteam.railways.internal.compat.registrate.util.entry.MenuEntry;
import com.railwayteam.railways.internal.compat.registrate.util.nullness.NonNullBiFunction;
import com.railwayteam.railways.neoforge.RailwaysNeoForgeInit;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class MenuBuilder<T extends AbstractContainerMenu, P> extends AbstractBuilder<MenuType<T>, P, MenuBuilder<T, P>> {
    @FunctionalInterface
    public interface ForgeMenuFactory<T extends AbstractContainerMenu> {
        T create(MenuType<T> type, int id, Inventory inventory, FriendlyByteBuf buffer);
    }

    @FunctionalInterface
    public interface ScreenFactory<C extends AbstractContainerMenu, S> {
        S create(C menu, Inventory inventory, net.minecraft.network.chat.Component title);
    }

    private final NonNullBiFunction<MenuType<T>, Integer, T> factory;
    private final ForgeMenuFactory<T> extendedFactory;
    private java.util.function.Supplier<?> screenFactory = () -> null;

    public MenuBuilder(Registrate owner, String name, NonNullBiFunction<MenuType<T>, Integer, T> factory) {
        super(owner, name, null);
        this.factory = factory;
        this.extendedFactory = null;
    }

    public MenuBuilder(Registrate owner, String name, ForgeMenuFactory<T> factory) {
        super(owner, name, null);
        this.factory = null;
        this.extendedFactory = factory;
    }

    public MenuBuilder<T, P> screen(java.util.function.Supplier<?> screenFactory) {
        this.screenFactory = screenFactory;
        return this;
    }

    public MenuEntry<T> register() {
        MenuType<T> type;
        if (extendedFactory != null) {
            // NeoForge: menu types with extra data instead of Fabric's ExtendedMenuType
            type = IMenuTypeExtension.create((syncId, inventory, data) -> extendedFactory.create(typeRef[0], syncId, inventory, data));
        } else {
            type = new MenuType<>((syncId, inventory) -> factory.apply(null, syncId), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        }
        owner.registerVanilla(BuiltInRegistries.MENU, name, type);
        registerScreen(type);
        return new MenuEntry<>(owner.id(name), type);
    }

    private final MenuType<T>[] typeRef = new MenuType[1];

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <S extends Screen & MenuAccess<T>> void registerScreen(MenuType<T> type) {
        typeRef[0] = type;
        if (FMLEnvironment.getDist() != Dist.CLIENT)
            return;
        Object supplied = screenFactory.get();
        if (!(supplied instanceof ScreenFactory))
            return;
        ScreenFactory<T, S> factory = (ScreenFactory<T, S>) supplied;
        MenuScreens.ScreenConstructor<T, S> constructor = (menu, inventory, title) -> factory.create(menu, inventory, title);
        // NeoForge: screens are registered in RegisterMenuScreensEvent
        RailwaysNeoForgeInit.onModBus(bus -> bus.addListener(RegisterMenuScreensEvent.class, event -> event.register(type, constructor)));
    }
}
