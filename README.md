# RegistrARRP
Modified Fabric port of Registrate. Helper library to make creating and registering objects simpler.

Fabric API must be installed for it to work.

Reminder: RegistrARRP is not Registrate! Report issues for RegistrARRP here, not at the Registrate repository.

# Changes
- Because Fabric does not have registry events, `AbstractRegistrate` now has a `register` method that must be called once all builders have been registered. The registrate object should not be used anymore after this call.
- Datagen works entirely different from the Forge version, now utilizing ARRP. 
- `EntityBuilder` now has three different types and `FluidBuilder` has a few new methods.
- All new classes have been put into the fabric package. These mostly replace Forge classes and functionality.
- Lombok usage has been removed.

# Links
- [Original Forge version repository](https://github.com/tterrag1098/Registrate)
- [Original Fabric port](https://github.com/PepperCode1/Registrate-Fabric)
- [Fabric API repository](https://github.com/FabricMC/fabric)

# Credit
I, Tropheus Jay, have not really done a lot of RegistrARRP. The original Registrate was created by [Tterrag](https://github.com/tterrag1098), and the majority of the code belongs to them. [Pepper](https://github.com/PepperCode1) did all the work of the initial port to Fabric. This project could not exist without them.

# Use
RegistrARRP's features can be split into two categories: registration and data. This is an example of a block being registered using RegistrARRP:
```java
public class TestMod implements ModInitializer {
	public static final String MODID = "registrarrp";
	public static Registrate REGISTRATE;
	public static RegistryEntry<? extends Block> TEST_BLOCK;
	@Override
	public void onInitialize() {
		REGISTRATE = Registrate.create(MODID);
		TEST_BLOCK = REGISTRATE.object(blockName)
				.block(Block::new)
				.recipe(Items.STONE, 4, RecipeTypes.STONECUTTING)
				.tag(BlockTags.ANVIL)
				.simpleLoot(CommonLootTableTypes.SILK_TOUCH_REQUIRED)
				.simpleItem()
				.register();
    }
}
```
This will create a plain block with:
- A stonecutting recipe, where 1 stone creates 4 test blocks.
- A loot table which requires silk touch in order to get the item.
- The `BlockTags.ANVIL` tag.
- A corresponding `BlockItem`.
- An item model.
- Lang entries for both block and item.
- One model for the block.
- A simple BlockState with one model.

Out of all of these, the recipe, the loot table, the tag, models, blockstates, and lang are all part of data. The actual block and item are part of registration. Registration is generally left untouched from the original Registrate, apart from where differences from Forge to Fabric require changes. Data has been rewritten to take advantage of ARRP.

### Registration
To start with RegistrARRP, you need to start by creating a Registrate. RegistrARRP already comes with the Registrate class which may be used, but you can extend AbstractRegistrate to add on to it however you want.<br>
A Registrate can be created through `Registrate.create(modid)`. Once you have your Registrate instance, the fun can begin.<br>
Adding an entry starts with calling `AbstractRegistrate.object(name)`, as seen in the example. From here, you can choose from the other available methods, such as `block()` or `item()`, to register anything you want!<br>
Each builder has its own methods to customize your object as needed. The builders are all documented well, so take a look at the javadoc to see your options.
### Data
Data, such as models and loot tables, are created through ARRP. Info on ARRP can be found [here](https://github.com/Devan-Kerman/ARRP). Builders which take advantage of ARRP, such as `BlockBuilder`, have methods which abstract away ARRP for you, such as `defaultBlockState()`, simplifying use. In the event helper methods cannot help you, you can directly access the runtime resource pack. The method of doing so varies per builder, but the methods allowing for this generally take in an object prefixed with `J`, such as `JLootTable` or `JModel`. <br>
A common criticism of runtime resource packs is speed. RegistrARRP currently does not do anything to alleviate this on its own, however may use [ARRP's optimization options](https://github.com/Devan-Kerman/ARRP/wiki/Optimization) in the future. <br>
In the meantime, if performance is significant, you can take advantage of ARRP's ability to dump generated assets. When run in the development environment, RegistrARRP will handle this for you, and you can find your generated assets in `<run folder>/registrarrp_asset_dump`. The full path can be found in the log. These assets should Just Work™ once placed into the correct folder.<br>
Once you have successfully gotten your generated assets functioning when not generated, remember to call `AbstractRegistrate.doDatagen(false)` to disable the data generation! This should speed up load times.
