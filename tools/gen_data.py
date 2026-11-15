#!/usr/bin/env python3
"""Writes all the boilerplate JSON (blockstates, models, loot tables, lang, sounds.json,
dimension types, dimensions, biomes, features, recipe) so none of it is hand-maintained.
Usage: python3 tools/gen_data.py"""
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "backrooms")
DATA = os.path.join(RES, "data", "backrooms")
MOD = "backrooms"


def write(base, rel, obj):
    path = os.path.join(base, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


# name -> English name. Simple cubes with one texture on every face.
CUBES = {
    "damp_carpet": "Damp Carpet",
    "wallpaper": "Yellowed Wallpaper",
    "ceiling_tile": "Ceiling Tile",
    "pool_tile": "Pool Tile",
    "pool_tile_blue": "Blue Pool Tile",
    "pool_light": "Pool Light",
    "garage_concrete": "Garage Concrete",
    "garage_floor": "Garage Floor",
    "sodium_lamp": "Sodium Lamp",
    "threshold": "Threshold",
}
NO_DROP = {"threshold", "exit_sign"}


def block_assets():
    for name in CUBES:
        write(ASSETS, f"blockstates/{name}.json", {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        write(ASSETS, f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{name}"}})

    # wallpaper with a skirting board: sides differ from top/bottom
    write(ASSETS, "blockstates/wallpaper_baseboard.json", {"variants": {"": {"model": f"{MOD}:block/wallpaper_baseboard"}}})
    write(ASSETS, "models/block/wallpaper_baseboard.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": f"{MOD}:block/wallpaper", "side": f"{MOD}:block/wallpaper_baseboard"}})

    # hazard-striped pillar base
    write(ASSETS, "blockstates/hazard_pillar.json", {"variants": {"": {"model": f"{MOD}:block/hazard_pillar"}}})
    write(ASSETS, "models/block/hazard_pillar.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": f"{MOD}:block/garage_concrete", "side": f"{MOD}:block/hazard_pillar"}})

    # exit sign: lettering on the sides only
    write(ASSETS, "blockstates/exit_sign.json", {"variants": {"": {"model": f"{MOD}:block/exit_sign"}}})
    write(ASSETS, "models/block/exit_sign.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": f"{MOD}:block/threshold", "side": f"{MOD}:block/exit_sign"}})

    # painted parking line, rotatable
    write(ASSETS, "blockstates/garage_line.json", {"variants": {
        "axis=z": {"model": f"{MOD}:block/garage_line"},
        "axis=y": {"model": f"{MOD}:block/garage_line"},
        "axis=x": {"model": f"{MOD}:block/garage_line", "y": 90}}})
    write(ASSETS, "models/block/garage_line.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{MOD}:block/garage_line", "bottom": f"{MOD}:block/garage_floor", "side": f"{MOD}:block/garage_floor"}})

    # fluorescent light: lit / unlit, the flickering flag does not change the model
    variants = {}
    for lit in ("true", "false"):
        for flick in ("true", "false"):
            variants[f"flickering={flick},lit={lit}"] = {"model": f"{MOD}:block/fluorescent_light" + ("" if lit == "true" else "_off")}
    write(ASSETS, "blockstates/fluorescent_light.json", {"variants": variants})
    for suffix in ("", "_off"):
        write(ASSETS, f"models/block/fluorescent_light{suffix}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{MOD}:block/ceiling_tile", "side": f"{MOD}:block/ceiling_tile", "bottom": f"{MOD}:block/fluorescent_light{suffix}"}})


ALL_BLOCKS = list(CUBES) + ["wallpaper_baseboard", "hazard_pillar", "exit_sign", "garage_line", "fluorescent_light"]
FLAT_ITEMS = ["key_fob", "fixture_spawn_egg", "tube_moth_spawn_egg"]


def item_assets():
    for name in ALL_BLOCKS:
        write(ASSETS, f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{name}"}})
    for name in FLAT_ITEMS:
        write(ASSETS, f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}})
        write(ASSETS, f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})


def loot_and_tags():
    for name in ALL_BLOCKS:
        if name in NO_DROP:
            continue
        write(DATA, f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                       "condition": {"type": "minecraft:survives_explosion"}}]})
    write(os.path.join(RES, "data", "minecraft"), "tags/block/mineable/pickaxe.json", {
        "replace": False, "values": [f"{MOD}:{n}" for n in ALL_BLOCKS if n not in NO_DROP and n != "damp_carpet"]})
    write(DATA, "recipe/key_fob.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot", "R": "minecraft:redstone", "E": "minecraft:ender_pearl"},
        "pattern": [" N ", "IRI", " E "],
        "result": {"id": f"{MOD}:key_fob"}})


# sound event id -> (file, subtitle, stream)
SOUNDS = {
    "ambient.level_0": ("ambient/level_0", None, True),
    "ambient.poolrooms": ("ambient/poolrooms", None, True),
    "ambient.carpark": ("ambient/carpark", None, True),
    "block.fluorescent_light.buzz": ("block/light_buzz", "Light buzzes", False),
    "entity.fixture.ambient": ("entity/fixture_ambient", "Filament rings", False),
    "entity.fixture.step": ("entity/fixture_step", "Something drags", False),
    "entity.fixture.attack": ("entity/fixture_attack", "Bulb pops", False),
    "entity.fixture.hurt": ("entity/fixture_hurt", "Fixture clangs", False),
    "entity.fixture.death": ("entity/fixture_death", "Fixture shatters", False),
    "entity.tube_moth.ambient": ("entity/moth_flutter", "Moth flutters", False),
    "entity.tube_moth.hurt": ("entity/moth_hurt", "Moth chirps", False),
    "entity.tube_moth.death": ("entity/moth_death", "Moth dies", False),
    "event.noclip_hum": ("event/noclip_hum", "The wall hums", False),
    "event.transit": ("event/transit", "Reality slips", False),
    "item.key_fob.beep": ("item/key_fob_beep", "Key fob beeps", False),
}


def sounds_and_lang():
    sounds = {}
    lang = {
        "block.backrooms.wallpaper_baseboard": "Wallpaper with Skirting Board",
        "block.backrooms.hazard_pillar": "Hazard-Striped Pillar",
        "block.backrooms.exit_sign": "Exit Sign",
        "block.backrooms.garage_line": "Painted Bay Line",
        "block.backrooms.fluorescent_light": "Fluorescent Light",
        "item.backrooms.key_fob": "Key Fob (No Car)",
        "item.backrooms.fixture_spawn_egg": "Fixture Spawn Egg",
        "item.backrooms.tube_moth_spawn_egg": "Tube Moth Spawn Egg",
        "entity.backrooms.fixture": "Fixture",
        "entity.backrooms.tube_moth": "Tube Moth",
        "itemGroup.backrooms": "Backrooms: Thresholds",
        "message.backrooms.nowhere": "The key fob beeps, but nothing answers.",
    }
    for name, english in CUBES.items():
        lang[f"block.backrooms.{name}"] = english
    for event, (file, subtitle, stream) in SOUNDS.items():
        entry = {"sounds": [{"name": f"{MOD}:{file}", "stream": True} if stream else f"{MOD}:{file}"]}
        if subtitle:
            entry["subtitle"] = f"subtitles.backrooms.{event}"
            lang[f"subtitles.backrooms.{event}"] = subtitle
        sounds[event] = entry
    write(ASSETS, "sounds.json", sounds)
    write(ASSETS, "lang/en_us.json", lang)


NEVER_SLEEP = {"can_set_spawn": "never", "can_sleep": "never"}

# per dimension: generator style, lighting, fog, sky, ambient loop
DIMENSIONS = {
    "level_0": dict(
        ambient_light=0.10, has_skylight=False, has_ceiling=True, skybox="none",
        fog="#b3a258", fog_start=6.0, fog_end=80.0, sky="#b3a258", ambient_color="#3b3520", water="#8f8a4a"),
    "poolrooms": dict(
        ambient_light=0.30, has_skylight=True, has_ceiling=False, skybox="overworld",
        fog="#d9f1f5", fog_start=24.0, fog_end=160.0, sky="#aee4f2", ambient_color="#33474d", water="#57d3e6"),
    "carpark": dict(
        ambient_light=0.03, has_skylight=False, has_ceiling=True, skybox="none",
        fog="#17110b", fog_start=2.0, fog_end=56.0, sky="#17110b", ambient_color="#1c140c", water="#3a3a3a"),
}

# Flip to True once the entity types exist (biomes may only reference registered entities).
WITH_ENTITIES = True
# Dimension -> chunk generator style. "blank" is the flat checkpoint-1 test floor.
STYLES = {"level_0": "level_0", "poolrooms": "poolrooms", "carpark": "carpark"}
ENABLED_DIMENSIONS = ["level_0", "poolrooms", "carpark"]


def dimensions():
    for stale in ("dimension", "dimension_type", "worldgen"):
        shutil.rmtree(os.path.join(DATA, stale), ignore_errors=True)
    for name in ENABLED_DIMENSIONS:
        d = DIMENSIONS[name]
        attributes = {
            "minecraft:audio/ambient_sounds": {"loop": f"{MOD}:ambient.{name}"},
            "minecraft:gameplay/bed_rule": NEVER_SLEEP,
            "minecraft:gameplay/straw_bed_rule": NEVER_SLEEP,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/fog_color": d["fog"],
            "minecraft:visual/fog_start_distance": d["fog_start"],
            "minecraft:visual/fog_end_distance": d["fog_end"],
            "minecraft:visual/sky_color": d["sky"],
            "minecraft:visual/ambient_light_color": d["ambient_color"],
        }
        if not d["has_skylight"]:
            attributes["minecraft:visual/sky_light_factor"] = 0.0
        write(DATA, f"dimension_type/{name}.json", {
            "ambient_light": d["ambient_light"], "attributes": attributes, "coordinate_scale": 1.0,
            "has_ceiling": d["has_ceiling"], "has_ender_dragon_fight": False, "has_fixed_time": True,
            "has_skylight": d["has_skylight"], "height": 64, "logical_height": 64, "min_y": 0,
            "infiniburn": "#minecraft:infiniburn_overworld",
            "monster_spawn_block_light_limit": 15, "monster_spawn_light_level": 15, "skybox": d["skybox"]})
        write(DATA, f"dimension/{name}.json", {
            "type": f"{MOD}:{name}",
            "generator": {"type": f"{MOD}:liminal", "style": STYLES[name],
                          "biome_source": {"type": "minecraft:fixed", "biome": f"{MOD}:{name}"}}})
        spawns = {}
        if WITH_ENTITIES:
            spawns = {
                "monster": [{"type": f"{MOD}:fixture", "count": 1, "weight": 10}],
                "ambient": [{"type": f"{MOD}:tube_moth", "weight": 10,
                             "count": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4}}],
            }
        write(DATA, f"worldgen/biome/{name}.json", {
            "attributes": {"minecraft:gameplay/natural_mob_spawns": {
                "argument": {"spawn_costs": {}, "spawns_by_category": spawns}, "modifier": "overlay"}},
            "carvers": [], "downfall": 0.0, "effects": {"water_color": d["water"]}, "features": [],
            "has_precipitation": False, "temperature": 0.5})


def features():
    """The hidden Overworld structure. Roughly one attempt per 250 chunks."""
    write(DATA, "worldgen/feature/wrong_stairwell.json", {"type": f"{MOD}:wrong_stairwell"})
    write(DATA, "worldgen/placed_feature/wrong_stairwell.json", {
        "feature": f"{MOD}:wrong_stairwell",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 250},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
            {"type": "minecraft:biome"}]})


def main():
    block_assets()
    item_assets()
    loot_and_tags()
    sounds_and_lang()
    dimensions()
    features()
    print("data + asset JSON written")


if __name__ == "__main__":
    main()
