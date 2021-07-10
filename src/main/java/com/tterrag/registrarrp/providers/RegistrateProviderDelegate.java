package com.tterrag.registrarrp.providers;

import net.minecraft.data.DataProvider;
import net.minecraft.util.Identifier;

public interface RegistrateProviderDelegate<R, T extends R> extends DataProvider {
    
    String getName();
    
    Identifier getId();
    
    T getEntry();
}