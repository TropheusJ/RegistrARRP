package com.tterrag.registrarrp.util.entry;

import net.minecraft.item.Item;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.RegistryObject;

public class ItemEntry<T extends Item> extends ItemProviderEntry<T> {

    public ItemEntry(AbstractRegistrate<?> owner, RegistryObject<T> delegate) {
        super(owner, delegate);
    }
    
    public static <T extends Item> ItemEntry<T> cast(RegistryEntry<T> entry) {
        return RegistryEntry.cast(ItemEntry.class, entry);
    }
}
