package com.tterrag.registrarrp.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SpawnEggItem.class)
public interface SpawnEggItemAccessor {
	@Accessor("SPAWN_EGGS")
	static Map<EntityType<?>, SpawnEggItem> getEggMap() {
		throw new AssertionError();
	}
}
