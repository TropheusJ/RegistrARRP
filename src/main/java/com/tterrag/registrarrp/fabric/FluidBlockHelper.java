package com.tterrag.registrarrp.fabric;

import com.tterrag.registrarrp.mixin.FluidBlockAccessor;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;

public class FluidBlockHelper {
	public static FluidBlock createFluidBlock(FlowableFluid fluid, AbstractBlock.Settings settings) {
		return FluidBlockAccessor.callInit(fluid, settings);
	}
}
