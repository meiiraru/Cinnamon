package cinnamon.parsers;

import cinnamon.model.MeshHelper;
import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cinnamon.events.Events.LOGGER;

public class VoxLoader {

    //ccw vertices for each face
    private static final int[][][] faceVerts = {
            {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}}, //-X
            {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}}, //+X
            {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}, //-Y
            {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}, //+Y
            {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}}, //-Z
            {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}}  //+Z
    };

    //6 directions: -X, +X, -Y, +Y, -Z, +Z
    private static final int[][] dirs = {
            {-1,  0,  0}, {1, 0, 0},
            { 0, -1,  0}, {0, 1, 0},
            { 0,  0, -1}, {0, 0, 1}
    };

    private static final float padding = 1f / 64f; //padding (radius) from the uv center
    private static final float scale = 1f / 16f; //global model scaling factor

    public static Mesh load(Resource res) throws IOException {
        LOGGER.debug("Loading model \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        //read all bytes into a byte buffer
        byte[] data = stream.readAllBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        stream.close();

        //parse header
        String magic = readString(buffer);
        if (!magic.equals("VOX "))
            throw new RuntimeException("Invalid VOX file: " + res);

        int version = buffer.getInt();
        LOGGER.debug("VOX version: %d", version);

        //main chunk
        String mainID = readString(buffer);
        if (!mainID.equals("MAIN"))
            throw new RuntimeException("Invalid VOX file: " + res);

        int mainChunkSize = buffer.getInt();
        int mainChildChunksSize = buffer.getInt();

        Set<String> unknownChunkIDs = new HashSet<>();

        //prepare mesh
        Mesh mesh = new Mesh();

        Material mat = new Material("default");
        mesh.getMaterials().put(mat.getName(), mat);

        List<VoxModel> models = new ArrayList<>();
        VoxModel currentModel = null;

        //parse chunks
        while (buffer.hasRemaining()) {
            String chunkID = readString(buffer);
            int chunkSize = buffer.getInt();
            int childChunksSize = buffer.getInt();

            switch (chunkID) {
                case "SIZE" -> {
                    int x = buffer.getInt();
                    int y = buffer.getInt();
                    int z = buffer.getInt();
                    currentModel = new VoxModel(x, z, y); //MagicaVoxel uses Z-up
                    models.add(currentModel);
                    LOGGER.debug("Model %d", models.size());
                    LOGGER.debug("SIZE: %d %d %d", x, z, y);
                }
                case "XYZI" -> {
                    int numVoxels = buffer.getInt();
                    if (currentModel != null) {
                        LOGGER.debug("XYZI: %d voxels", numVoxels);
                        for (int i = 0; i < numVoxels; i++) {
                            byte x = buffer.get();
                            byte y = buffer.get();
                            byte z = buffer.get();
                            byte colID = buffer.get();
                            //flip x to match the y/z swap
                            int flippedX = (currentModel.sizeX - 1) - (x & 0xFF);
                            currentModel.set(flippedX, z & 0xFF, y & 0xFF, colID);
                        }
                    }
                }
                case "RGBA" -> {
                    Resource tex = new Resource("generated/vox/" + res + "/texture.png");
                    mat.setAlbedo(new MaterialTexture(tex));

                    //create a palette png buffer
                    ByteBuffer paletteBuffer = MemoryUtil.memAlloc(256 * 4);
                    for (int i = 0; i < 256; i++) {
                        paletteBuffer.put(buffer.get()); //r
                        paletteBuffer.put(buffer.get()); //g
                        paletteBuffer.put(buffer.get()); //b
                        paletteBuffer.put(buffer.get()); //a
                    }
                    paletteBuffer.flip();
                    ByteBuffer imgBuffer = TextureIO.encodeRGBAtoPNG(paletteBuffer, 16, 16);

                    //force texture load
                    Texture.of(tex, imgBuffer);

                    //free buffers
                    MemoryUtil.memFree(paletteBuffer);
                    MemoryUtil.memFree(imgBuffer);
                    LOGGER.debug("RGBA: palette loaded");
                }
                //skip unknown chunk
                default -> {
                    if (!unknownChunkIDs.contains(chunkID)) {
                        unknownChunkIDs.add(chunkID);
                        LOGGER.debug("Skipping chunk ID: %s", chunkID);
                    }
                    buffer.position(buffer.position() + chunkSize);
                }
            }
        }

        //bake mesh from voxels
        for (VoxModel model : models)
            bakeMesh(mesh, model, mat);

        //calculate bounds
        MeshHelper.recalculateBounds(mesh);

        return mesh;
    }

    private static String readString(ByteBuffer buffer) {
        byte[] str = new byte[4];
        buffer.get(str);
        return new String(str);
    }

    private static void bakeMesh(Mesh mesh, VoxModel model, Material mat) {
        //create a new group
        Group group = new Group("model_" + mesh.getGroups().size());
        group.setMaterial(mat);
        mesh.getGroups().add(group);

        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs      = mesh.getUVs();
        List<Vector3f> normals  = mesh.getNormals();

        float offsetX = -model.sizeX / 2f;
        float offsetZ = -model.sizeZ / 2f;

        //iterate over all voxels in the model
        for (int z = 0; z < model.sizeZ; z++) {
            for (int y = 0; y < model.sizeY; y++) {
                for (int x = 0; x < model.sizeX; x++) {
                    byte colID = model.get(x, y, z);

                    //empty voxel (0) skip
                    if (colID == 0)
                        continue;

                    int palIndex = (colID & 0xFF) - 1;

                    //find the uv center of the palette index in a 16x16 grid
                    float centerU = (palIndex % 16 + 0.5f) / 16f;
                    float centerV = (15 - palIndex / 16 + 0.5f) / 16f; //flip v

                    //apply the padding to the uv coordinates
                    float minU = centerU - padding;
                    float maxU = centerU + padding;
                    float minV = centerV - padding;
                    float maxV = centerV + padding;

                    //cycle through all voxel sides
                    for (int d = 0; d < 6; d++) {
                        int[] dir = dirs[d];

                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        int nz = z + dir[2];

                        //if the neighboring voxel is solid (not 0), we can skip this face
                        if (model.get(nx, ny, nz) != 0)
                            continue;

                        //save the normal for this face
                        int normalIdx = normals.size();
                        normals.add(new Vector3f(dir[0], dir[1], dir[2]));

                        //add the uvs
                        int uvIdx = uvs.size();
                        uvs.add(new Vector2f(minU, maxV));
                        uvs.add(new Vector2f(maxU, maxV));
                        uvs.add(new Vector2f(maxU, minV));
                        uvs.add(new Vector2f(minU, minV));

                        //add this face vertices with this voxel offset
                        int vIdx = vertices.size();
                        for (int i = 0; i < 4; i++) {
                            int[] v = faceVerts[d][i];
                            vertices.add(new Vector3f(v[0] + x + offsetX, v[1] + y, v[2] + z + offsetZ).mul(scale));
                        }

                        //create and add the face index data
                        int[] fVerts = {vIdx, vIdx + 1, vIdx + 2, vIdx + 3};
                        int[] fUvs   = {uvIdx, uvIdx + 1, uvIdx + 2, uvIdx + 3};
                        int[] fNorms = {normalIdx, normalIdx, normalIdx, normalIdx};
                        group.getFaces().add(new Face(fVerts, fUvs, fNorms));
                    }
                }
            }
        }
    }

    private static class VoxModel {
        private final int sizeX, sizeY, sizeZ;
        private final byte[] voxels;

        private VoxModel(int sizeX, int sizeY, int sizeZ) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.voxels = new byte[sizeX * sizeY * sizeZ];
        }

        public void set(int x, int y, int z, byte colID) {
            voxels[x + (y * sizeX) + (z * sizeX * sizeY)] = colID;
        }

        public byte get(int x, int y, int z) {
            if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ)
                return 0;
            return voxels[x + (y * sizeX) + (z * sizeX * sizeY)];
        }
    }
}
