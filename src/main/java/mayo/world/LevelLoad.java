package mayo.world;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import mayo.model.ModelRegistry;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import mayo.world.terrain.Terrain;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class LevelLoad {

    private static final ObjectMapper MAPPER = JsonMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    public static void load(World world, Resource level) {
        try {
            //get resource
            InputStream resource = IOUtils.getResource(level);
            if (resource == null)
                throw new RuntimeException("Resource not found: " + level);

            //read as string
            String src = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

            //wrap json to object
            LevelData data = MAPPER.readValue(src, LevelData.class);

            //actually load world
            internalLoad(world, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void internalLoad(World world, LevelData level) {
        for (ChunkData data : level.chunkData) {
            int x0 = data.chunkX * 32;
            int y0 = data.chunkY * 32;
            int z0 = data.chunkZ * 32;

            for (TileData tile : data.mapData) {
                if (tile.x < 0 || tile.x >= 32 || tile.y < 0 || tile.y >= 32 || tile.z < 0 || tile.z >= 32)
                    continue;

                char c = tile.tile;
                ModelRegistry.Terrain terrain = level.terrainMap.get(c);
                if (terrain != null) {
                    Terrain t = terrain.get();
                    t.setPos(tile.x + x0, tile.y + y0, tile.z + z0);
                    world.addTerrain(t);
                }
            }
        }
    }

    private static class LevelData {
        public ArrayList<ChunkData> chunkData;
        public HashMap<Character, ModelRegistry.Terrain> terrainMap;
    }

    private static class ChunkData {
        public int chunkX, chunkY, chunkZ;
        public ArrayList<TileData> mapData;
    }

    private static class TileData {
        public int x, y, z;
        public char tile;
    }
}
