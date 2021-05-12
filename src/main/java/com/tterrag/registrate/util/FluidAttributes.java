package com.tterrag.registrate.util;

import net.minecraft.fluid.Fluid;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

/**
 * This class aims to replicate the functionality of the class in Forge under the same name.
 */
public class FluidAttributes {
	public Object bucketVolume;
	public String translationKey;
	public Identifier stillTexture;
	public Identifier flowingTexture;
	@Nullable public Identifier overlayTexture;
	public SoundEvent fillSound;
	public SoundEvent emptySound;
	public int luminosity = 0;
	public int density = 1000;
	public int temperature = 300;
	public int viscosity = 1000;
	public boolean isGaseous;
	public Rarity rarity = Rarity.COMMON;
	public int color;
	
	public FluidAttributes() {}
	
	public FluidAttributes(FluidAttributes attributes, Fluid fluid) {
		this.bucketVolume(attributes.bucketVolume);
		this.translationKey(attributes.translationKey);
		this.stillTexture(attributes.stillTexture);
		this.flowingTexture(attributes.flowingTexture);
		this.overlayTexture(attributes.overlayTexture);
		this.fillSound(attributes.fillSound);
		this.emptySound(attributes.emptySound);
		this.luminosity(attributes.luminosity);
		this.density(attributes.density);
		this.temperature(attributes.temperature);
		this.viscosity(attributes.viscosity);
		this.isGaseous(attributes.isGaseous);
		this.rarity(attributes.rarity);
		this.color(attributes.color);
	}
	
	/**
	 * This must be set per fluid, it has no default to prevent types interfering. Setting it to 1000 (an int) is recommended.
	 *
	 * Amount of fluid per bucket. Not static because Fabric still hasn't decided on a standard.
	 * Is an object to allow for whatever type you want. Could be an int, could be a double, could be a float. Up to you.
	 */
	public FluidAttributes bucketVolume(Object volume) {
		this.bucketVolume = volume;
		return this;
	}
	
	public FluidAttributes translationKey(String key) {
		this.translationKey = key;
		return this;
	}
	
	public FluidAttributes stillTexture(Identifier identifier) {
		this.stillTexture = identifier;
		return this;
	}
	
	
	public FluidAttributes flowingTexture(Identifier identifier) {
		this.flowingTexture = identifier;
		return this;
	}
	
	
	public FluidAttributes overlayTexture(Identifier identifier) {
		this.overlayTexture = identifier;
		return this;
	}
	
	
	public FluidAttributes fillSound(SoundEvent sound) {
		this.fillSound = sound;
		return this;
	}
	
	
	public FluidAttributes emptySound(SoundEvent sound) {
		this.emptySound = sound;
		return this;
	}
	
	/**
	 * The light level emitted by this fluid.
	 *
	 * Default value is 0, as most fluids do not actively emit light.
	 */
	public FluidAttributes luminosity(int luminosity) {
		this.luminosity = luminosity;
		return this;
	}
	
	/**
	 * Density of the fluid - completely arbitrary; negative density indicates that the fluid is
	 * lighter than air.
	 *
	 * Default value is approximately the real-life density of water in kg/m^3.
	 */
	public FluidAttributes density(int density) {
		this.density = density;
		return this;
	}
	
	/**
	 * Temperature of the fluid - completely arbitrary; higher temperature indicates that the fluid is
	 * hotter than air.
	 *
	 * Default value is approximately the real-life room temperature of water in degrees Kelvin.
	 */
	public FluidAttributes temperature(int temp) {
		this.temperature = temp;
		return this;
	}
	
	/**
	 * Viscosity ("thickness") of the fluid - completely arbitrary; negative values are not
	 * permissible.
	 *
	 * Default value is approximately the real-life density of water in m/s^2 (x10^-3).
	 *
	 * Higher viscosity means that a fluid flows more slowly, like molasses.
	 * Lower viscosity means that a fluid flows more quickly, like helium.
	 *
	 */
	public FluidAttributes viscosity(int viscosity) {
		this.viscosity = viscosity;
		return this;
	}
	
	/**
	 * This indicates if the fluid is gaseous.
	 *
	 * Generally this is associated with negative density fluids.
	 */
	public FluidAttributes isGaseous(boolean gaseous) {
		this.isGaseous = gaseous;
		return this;
	}
	
	/**
	 * The rarity of the fluid.
	 *
	 * Used primarily in tool tips.
	 */
	public FluidAttributes rarity(Rarity rarity) {
		this.rarity = rarity;
		return this;
	}
	
	/**
	 * Color used by universal bucket and the ModelFluid baked model.
	 * Note that this int includes the alpha so converting this to RGB with alpha would be
	 *   float r = ((color >> 16) & 0xFF) / 255f; // red
	 *   float g = ((color >> 8) & 0xFF) / 255f; // green
	 *   float b = ((color >> 0) & 0xFF) / 255f; // blue
	 *   float a = ((color >> 24) & 0xFF) / 255f; // alpha
	 */
	public FluidAttributes color(int color) {
		this.color = color;
		return this;
	}
}
