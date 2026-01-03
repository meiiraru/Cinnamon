package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cinnamon.events.Events.LOGGER;

public class MaterialLoader {

    public static Map<String, Material> load(Resource res) throws IOException {
        LOGGER.debug("Loading material \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        Map<String, Material> map = new HashMap<>();
        try (stream; InputStreamReader reader = new InputStreamReader(stream); BufferedReader br = new BufferedReader(reader)) {
            Material material = new Material("");

            for (String line; (line = br.readLine()) != null; ) {
                //skip comments and empty lines
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                //grab first word on the line
                String[] split = line.split(" +", 2);

                //create new material
                switch (split[0]) {
                    case "newmtl" -> {
                        if (!material.getName().isBlank() && !material.getName().equals("none"))
                            map.put(material.getName(), material);

                        material = new Material(split[1]);
                    }

                    //textures
                    case "map_Kd", "albedo", "diffuse" -> material.setAlbedo(parseTexture(split[1], res));
                    case "map_disp", "bump", "height" -> {
                        String[] bumpData = split[1].split(" +");
                        for (int i = 0; i < bumpData.length; i++) {
                            if (bumpData[i].equals("-dm")) {
                                material.setHeightScale(Float.parseFloat(bumpData[i + 1]));
                                break;
                            }
                        }
                        material.setHeight(parseTexture(split[1], res));
                    }
                    case "map_Bump", "norm", "normal", "map_Kn" -> material.setNormal(parseTexture(split[1], res));
                    case "map_ao", "map_AO", "ao", "ambient_occlusion" -> material.setAO(parseTexture(split[1], res));
                    case "map_Pr", "roughness" -> material.setRoughness(parseTexture(split[1], res));
                    case "map_Pm", "metallic" -> material.setMetallic(parseTexture(split[1], res));
                    case "map_Ke", "emissive" -> material.setEmissive(parseTexture(split[1], res));
                }
            }

            map.put(material.getName(), material);
            return map;
        }
    }

    private static MaterialTexture parseTexture(String texture, Resource res) {
        String[] split = texture.split(" +");
        Resource path = res.resolveSibling(split[split.length - 1]);

        Set<Texture.TextureParams> params = new HashSet<>();
        for (String s : split) {
            Texture.TextureParams param = Texture.TextureParams.getByAlias(s.substring(1));
            if (param != null) params.add(param);
        }

        return new MaterialTexture(path, params.toArray(new Texture.TextureParams[0]));
    }
}
