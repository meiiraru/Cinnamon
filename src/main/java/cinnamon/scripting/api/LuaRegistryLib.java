package cinnamon.scripting.api;

import cinnamon.registry.*;
import cinnamon.sound.SoundCategory;
import cinnamon.world.DamageType;
import cinnamon.world.light.Light;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Lua "registry" library - read-only access to all enum registries.
 */
public class LuaRegistryLib extends LuaTable {

    public LuaRegistryLib() {
        set("entities", enumToTable(EntityRegistry.values()));
        set("terrain", enumToTable(TerrainRegistry.values()));
        set("materials", enumToTable(MaterialRegistry.values()));
        set("particles", enumToTable(ParticlesRegistry.values()));
        set("damageTypes", enumToTable(DamageType.values()));
        set("soundCategories", enumToTable(SoundCategory.values()));
        set("lightTypes", enumToTable(Light.Type.values()));
    }

    private static <E extends Enum<E>> LuaTable enumToTable(E[] values) {
        LuaTable table = new LuaTable();
        for (int i = 0; i < values.length; i++) {
            table.set(i + 1, LuaValue.valueOf(values[i].name()));
        }
        return table;
    }
}
