package com.tterrag.registrarrp.util.nullness;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;

/**
 * An alternative to {@link javax.annotation.Nullable} which works on type parameters (J8 feature).
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nullable
public @interface NullableType {
}