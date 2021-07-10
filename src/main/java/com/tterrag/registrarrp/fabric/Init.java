package com.tterrag.registrarrp.fabric;

import com.tterrag.registrarrp.Registrate;
import com.tterrag.registrarrp.util.entry.RegistryEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.nio.file.Paths;

public class Init implements ModInitializer {
	public static Registrate REGISTRATE;
	public static RegistryEntry<Block> TEST_BLOCK;
	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			// tests
			REGISTRATE = Registrate.create("registrarrp");
			TEST_BLOCK = REGISTRATE.object("test")
					.block(Block::new)
					.initialProperties(() -> Blocks.DIRT)
					.item()
					.build()
					.defaultLang()
					.register();
			
			REGISTRATE.register();
			REGISTRATE.getResourcePack().dump(Paths.get(FabricLoader.getInstance().getGameDir().toString() + "/asset_dump"));
		}
	}
}
