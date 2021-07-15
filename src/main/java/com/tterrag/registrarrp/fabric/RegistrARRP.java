package com.tterrag.registrarrp.fabric;

import com.tterrag.registrarrp.Registrate;
import com.tterrag.registrarrp.util.RecipeTypes;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RegistrARRP implements ModInitializer {
	public static final String MODID = "registrarrp";
	public static Logger LOGGER = LogManager.getLogger("RegistrARRP");
	public static Registrate REGISTRATE;
	public static RegistryEntry<? extends Fluid> TEST_FLUID;
	public static RegistryEntry<? extends Item> TEST_ITEM;
	public static String[] blockNames = new String[]{"test1", "test2", "test3"};
	public static Map<Identifier, Block> blocks = new HashMap<>();
	
	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			// tests
			REGISTRATE = Registrate.create(MODID);
			for (String blockName : blockNames) {
				blocks.put(new Identifier(MODID, blockName),
						REGISTRATE.object(blockName)
								.block(Block::new)
								.recipe(Items.STONE, 4, RecipeTypes.STONECUTTING)
								.simpleItem()
								.register()
								.get());
			}
			
			blocks.put(new Identifier(MODID, "test4"),
					REGISTRATE.object("test4")
							.block(PillarBlock::new)
							.simpleItem()
							.pillarModel(new Identifier(MODID, "block/test1"), new Identifier(MODID, "block/test2"))
							.recipe(((Block) blocks.values().toArray()[0]).asItem(), 1, RecipeTypes.SMOKING)
							.register()
							.get());
			
			blocks.put(new Identifier(MODID, "test5"),
					REGISTRATE.object("test5")
							.block(Stairs::new)
							.stairsModel(new Identifier(MODID, "block/test1"), new Identifier(MODID, "block/test2"), new Identifier(MODID, "block/test3"))
							.simpleItem()
							.register()
							.get());
			
			blocks.put(new Identifier(MODID, "test6"),
					REGISTRATE.object("test6")
							.block(WallBlock::new)
							.wallModel(new Identifier(MODID, "block/test3"))
							.simpleItem()
							.tag(BlockTags.WALLS)
							.register()
							.get());
			
			blocks.put(new Identifier(MODID, "test7"),
					REGISTRATE.object("test7")
							.block(FenceBlock::new)
							.fenceModel(new Identifier(MODID, "items/test_item"))
							.simpleItem()
							.tag(BlockTags.FENCES)
							.register()
							.get());
			
			TEST_ITEM = REGISTRATE.object("test_item")
					.item(Item::new)
					.defaultModel()
					.register();
			
			TEST_FLUID = REGISTRATE.object("test_fluid")
					.fluid("test_fluid", new Identifier(MODID, "block/test1"), new Identifier(MODID, "block/test1"))
					.bucket(new Identifier(MODID, "block/test1"))
					.block(new Identifier(MODID, "block/test1"))
					.register();
			
			REGISTRATE.register();
		}
	}
	
	static class Stairs extends StairsBlock {
		public Stairs(Settings settings) {
			super(((Block) blocks.values().toArray()[0]).getDefaultState(), settings);
		}
	}
}






