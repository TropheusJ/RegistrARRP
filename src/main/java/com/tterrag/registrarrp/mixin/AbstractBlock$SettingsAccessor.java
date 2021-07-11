package com.tterrag.registrarrp.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBlock.Settings.class)
public interface AbstractBlock$SettingsAccessor {
	@Accessor("lootTableId")
	Identifier getLootTableId();
}
