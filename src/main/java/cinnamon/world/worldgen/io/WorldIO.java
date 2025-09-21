package cinnamon.world.worldgen.io;

import cinnamon.utils.Version;
import cinnamon.utils.IOUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles world folder layout, metadata, and chunk IO.
 */
public class WorldIO {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final int CHUNK_VERSION = 1;

    public static Path worldsRoot() {
        return IOUtils.ROOT_FOLDER.resolve("worlds");
    }

    public static Path worldFolder(String name) {
        return worldsRoot().resolve(name);
    }

    public static Path metadataFile(String name) {
        return worldFolder(name).resolve("world.json");
    }

    public static Path chunkFile(String name, int cx, int cy, int cz) {
        return worldFolder(name).resolve("chunks").resolve(cx + "_" + cy + "_" + cz + ".json.gz");
    }

    // --- Metadata --- //

    public static WorldMetadata createWorld(String name, long seed) {
        WorldMetadata md = new WorldMetadata();
        md.name = name;
        md.seed = seed;
    md.generator = new GeneratorConfig();
    md.generator.type = "default";
    md.generator.octaves = 4;
    md.generator.scale = 0.01f;
    md.generator.lacunarity = 2f;
    md.generator.persistence = 0.5f;
    md.generator.amplitude = 10f;
    md.generator.baseHeight = 12f;
    md.generator.waterLevel = 16;
    md.version = Version.CLIENT_VERSION.toStringNoBuild();
        md.createdAt = Instant.now().toEpochMilli();
        md.lastPlayed = md.createdAt;

        Path meta = metadataFile(name);
        IOUtils.writeFile(meta, GSON.toJson(md).getBytes(StandardCharsets.UTF_8));
        return md;
    }

    public static WorldMetadata readMetadata(String name) {
        Path meta = metadataFile(name);
        byte[] data = IOUtils.readFile(meta);
        if (data == null) return null;
        return GSON.fromJson(new String(data, StandardCharsets.UTF_8), WorldMetadata.class);
    }

    public static void writeMetadata(WorldMetadata md) {
        Path meta = metadataFile(md.name);
        IOUtils.writeFile(meta, GSON.toJson(md).getBytes(StandardCharsets.UTF_8));
    }

    public static List<WorldMetadata> listWorlds() {
        List<WorldMetadata> list = new ArrayList<>();
        Path root = worldsRoot();
        try {
            if (!Files.exists(root)) return list;
            try (var stream = Files.list(root)) {
                stream.filter(Files::isDirectory).forEach(dir -> {
                    Path meta = dir.resolve("world.json");
                    byte[] data = IOUtils.readFile(meta);
                    if (data != null) list.add(GSON.fromJson(new String(data, StandardCharsets.UTF_8), WorldMetadata.class));
                });
            }
        } catch (Exception ignored) {}
        return list;
    }

    // --- Chunk IO --- //

    public static void saveChunk(String worldName, ChunkData data) {
        data.version = CHUNK_VERSION;
        Path file = chunkFile(worldName, data.cx, data.cy, data.cz);
        IOUtils.writeFileCompressed(file, GSON.toJson(data).getBytes(StandardCharsets.UTF_8));
    }

    public static ChunkData loadChunk(String worldName, int cx, int cy, int cz) {
        Path file = chunkFile(worldName, cx, cy, cz);
        byte[] bytes = IOUtils.readFileCompressed(file);
        if (bytes == null) return null;
        ChunkData data = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), ChunkData.class);
        return data;
    }

    // --- DTOs --- //

    public static class WorldMetadata {
        public String name;
        public long seed;
        public String version;
        public long createdAt;
        public long lastPlayed;
        public GeneratorConfig generator;
    }

    public static class GeneratorConfig {
    public String type;
    // heightmap params
    public int octaves; // 1..8
    public float scale; // world units to noise frequency multiplier (e.g., 0.01)
    public float lacunarity; // 2.0
    public float persistence; // 0.5
    public float amplitude; // 10
    public float baseHeight; // 12
    public int waterLevel; // y level (e.g., 16)
    }

    public static class ChunkData {
        public int version;
        public int cx, cy, cz;
        public long seedHash;
        public BlockSpec[] palette; // index 0 reserved for air
        public byte[] data; // palette indices (optionally RLE-encoded later)
    }
}
