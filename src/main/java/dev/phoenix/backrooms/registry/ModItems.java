package dev.phoenix.backrooms.registry;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.item.KeyFobItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	private static final List<Item> ALL = new ArrayList<>();

	static {
		for (Block block : List.of(ModBlocks.DAMP_CARPET, ModBlocks.WALLPAPER, ModBlocks.WALLPAPER_BASEBOARD, ModBlocks.CEILING_TILE,
			ModBlocks.FLUORESCENT_LIGHT, ModBlocks.POOL_TILE, ModBlocks.POOL_TILE_BLUE, ModBlocks.POOL_LIGHT, ModBlocks.GARAGE_CONCRETE,
			ModBlocks.GARAGE_FLOOR, ModBlocks.GARAGE_LINE, ModBlocks.HAZARD_PILLAR, ModBlocks.SODIUM_LAMP, ModBlocks.THRESHOLD, ModBlocks.EXIT_SIGN)) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			register(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
		}
	}

	/** The glint is the tell: an ordinary-looking key fob that shimmers like something enchanted. */
	public static final Item KEY_FOB = register("key_fob", KeyFobItem::new,
		new Item.Properties().stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

	static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Backrooms.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ALL.add(item);
		return item;
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> ALL.forEach(output::accept));
	}

	private ModItems() {
	}
}
