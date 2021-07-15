package com.tterrag.registrarrp.util;

import com.google.common.collect.Lists;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Utils {
	public static String toEnglishName(String internalName) {
		return Arrays.stream(internalName.toLowerCase(Locale.ROOT).split("_"))
				.map(StringUtils::capitalize)
				.collect(Collectors.joining(" "));
	}
	
	/**
	 * Use {@link #getStatically to access values}
	 */
	public static final List<Field> BLOCK_TAGS = Lists.newArrayList(
			BlockTags.class.getDeclaredFields()).stream()
			.filter(field -> returnTrue(() -> field.setAccessible(true)))
			.filter(field -> Modifier.isStatic(field.getModifiers()))
			.filter(field -> getStatically(field) instanceof Tag.Identified)
			.toList();
	/**
	 * Use {@link #getStatically to access values}
	 */
	public static final List<Field> ITEM_TAGS = Lists.newArrayList(
			ItemTags.class.getDeclaredFields()).stream()
			.filter(field -> returnTrue(() -> field.setAccessible(true)))
			.filter(field -> Modifier.isStatic(field.getModifiers()))
			.filter(field -> getStatically(field) instanceof Tag.Identified)
			.toList();
	/**
	 * Use {@link #getStatically to access values}
	 */
	public static final List<Field> FLUID_TAGS = Lists.newArrayList(
			FluidTags.class.getDeclaredFields()).stream()
			.filter(field -> returnTrue(() -> field.setAccessible(true)))
			.filter(field -> Modifier.isStatic(field.getModifiers()))
			.filter(field -> getStatically(field) instanceof Tag.Identified)
			.toList();

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
	
	public static boolean tagInSet(Tag.Identified<?> tag, List<Field> tags) {
		for (Field field : tags) {
			if (getStatically(field).equals(tag)) {
				return true;
			}
		}
		return false;
	}
}
