package dev.phoenix.backrooms.registry;

import dev.phoenix.backrooms.Backrooms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent AMBIENT_LEVEL_0 = register("ambient.level_0");
	public static final SoundEvent AMBIENT_POOLROOMS = register("ambient.poolrooms");
	public static final SoundEvent AMBIENT_CARPARK = register("ambient.carpark");
	public static final SoundEvent LIGHT_BUZZ = register("block.fluorescent_light.buzz");
	public static final SoundEvent FIXTURE_AMBIENT = register("entity.fixture.ambient");
	public static final SoundEvent FIXTURE_STEP = register("entity.fixture.step");
	public static final SoundEvent FIXTURE_ATTACK = register("entity.fixture.attack");
	public static final SoundEvent FIXTURE_HURT = register("entity.fixture.hurt");
	public static final SoundEvent FIXTURE_DEATH = register("entity.fixture.death");
	public static final SoundEvent MOTH_AMBIENT = register("entity.tube_moth.ambient");
	public static final SoundEvent MOTH_HURT = register("entity.tube_moth.hurt");
	public static final SoundEvent MOTH_DEATH = register("entity.tube_moth.death");
	public static final SoundEvent NOCLIP_HUM = register("event.noclip_hum");
	public static final SoundEvent TRANSIT = register("event.transit");
	public static final SoundEvent KEY_FOB_BEEP = register("item.key_fob.beep");

	private static SoundEvent register(String name) {
		Identifier id = Backrooms.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}

	private ModSounds() {
	}
}
