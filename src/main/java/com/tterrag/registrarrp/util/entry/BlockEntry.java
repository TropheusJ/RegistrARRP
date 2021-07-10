package com.tterrag.registrarrp.util.entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import com.tterrag.registrarrp.AbstractRegistrate;
import com.tterrag.registrarrp.fabric.RegistryObject;

public class BlockEntry<T extends Block> extends ItemProviderEntry<T> {

    public BlockEntry(AbstractRegistrate<?> owner, RegistryObject<T> delegate) {
        super(owner, delegate);
    }

    public BlockState getDefaultState() {
        return get().getDefaultState();
    }

    public boolean has(BlockState state) {
        return is(state.getBlock());
    }
    
    public static <T extends Block> BlockEntry<T> cast(RegistryEntry<T> entry) {
        return RegistryEntry.cast(BlockEntry.class, entry);
    }
}
