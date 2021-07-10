package com.tterrag.registrarrp.util;

import com.tterrag.registrarrp.fabric.Lazy;
import com.tterrag.registrarrp.util.nullness.NonNullSupplier;
import com.tterrag.registrarrp.util.nullness.NonnullType;

public class NonNullLazyValue<T> extends Lazy<T> implements NonNullSupplier<T> {

    public NonNullLazyValue(NonNullSupplier<T> supplier) {
        super(supplier);
    }

    @Override
    public @NonnullType T get() {
        return super.get();
    }
}
