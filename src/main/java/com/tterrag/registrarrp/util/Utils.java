package com.tterrag.registrarrp.util;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Utils {
	
	public static final List<Tag.Identified<Block>> BLOCK_TAGS = cast(Arrays.stream(BlockTags.class.getDeclaredFields())
			.filter(field -> returnTrue(() -> field.setAccessible(true)))
			.filter(field -> Modifier.isStatic(field.getModifiers()))
			.filter(field -> getStatically(field) instanceof Tag.Identified)
			.map(Utils::getStatically)
			.toList());
	
	public static final List<Tag.Identified<Item>> ITEM_TAGS = cast(Arrays.stream(ItemTags.class.getDeclaredFields())
			.filter(field -> returnTrue(() -> field.setAccessible(true)))
			.filter(field -> Modifier.isStatic(field.getModifiers()))
			.filter(field -> getStatically(field) instanceof Tag.Identified)
			.map(Utils::getStatically)
			.toList());
	
	public static final List<Tag.Identified<Fluid>> FLUID_TAGS = cast(Arrays.stream(FluidTags.class.getDeclaredFields())
			.filter(field -> returnTrue(() -> field.setAccessible(true)))
			.filter(field -> Modifier.isStatic(field.getModifiers()))
			.filter(field -> getStatically(field) instanceof Tag.Identified)
			.map(Utils::getStatically)
			.toList());
	
	public static String toEnglishName(String internalName) {
		return Arrays.stream(internalName.toLowerCase(Locale.ROOT).split("_"))
				.map(StringUtils::capitalize)
				.collect(Collectors.joining(" "));
	}
	
	public static Object getStatically(Field field) {
		try {
			return field.get(null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	// jank
	public static boolean returnTrue(Runnable thing) {
		thing.run();
		return true;
	}
	
	public static <T> T cast(Object o) {
		return (T) o;
	}
}
