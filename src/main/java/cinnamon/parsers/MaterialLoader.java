package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cinnamon.events.Events.LOGGER;

public class MaterialLoader {

    public static Map<String, Material> load(Resource res) {
        LOGGER.debug("Loading material %s", res.getPath());

        InputStream stream = IOUtils.getResource(res);
        Map<String, Material> map = new HashMap<>();

        if (stream == null)
            return map;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            Material material = new Material("");

            for (String line; (line = br.readLine()) != null; ) {
                //skip comments and empty lines
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                //grab first word on the line
                String[] split = line.trim().replaceAll(" +", " ").split(" ");

                //create new material
                switch (split[0]) {
                    case "newmtl" -> {
                        if (!material.getName().isBlank() && !material.getName().equals("none"))
                            map.put(material.getName(), material);

                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < split.length; i++)
                            sb.append(split[i]).append(" ");

                        String materialName = sb.toString().trim();
                        material = new Material(materialName);
                    }

                    //textures
                    case "map_Kd", "albedo", "diffuse" -> material.setAlbedo(parseTexture(split, res));
                    case "map_disp", "bump", "height" -> {
                        for (int i = 1; i < split.length; i++) {
                            if (split[i].equals("-dm")) {
                                material.setHeightScale(Float.parseFloat(split[i + 1]));
                                break;
                            }
                        }
                        material.setHeight(parseTexture(split, res));
                    }
                    case "map_Bump", "norm", "normal", "map_Kn" -> material.setNormal(parseTexture(split, res));
                    case "map_ao", "map_AO", "ao", "ambient_occlusion" -> material.setAO(parseTexture(split, res));
                    case "map_Pr", "roughness" -> material.setRoughness(parseTexture(split, res));
                    case "map_Pm", "metallic" -> material.setMetallic(parseTexture(split, res));
                    case "map_Ke", "emissive" -> material.setEmissive(parseTexture(split, res));
                }
            }

            map.put(material.getName(), material);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load material \"" + res.getPath() + "\"", e);
        }
    }

    private static MaterialTexture parseTexture(String[] split, Resource res) {
        Resource path = res.resolveSibling(split[split.length - 1]);

        Set<Texture.TextureParams> params = new HashSet<>();
        for (int i = 1; i < split.length - 1; i++) {
            Texture.TextureParams param = Texture.TextureParams.getByAlias(split[i].substring(1));
            if (param != null) params.add(param);
        }

        return new MaterialTexture(path, params.toArray(new Texture.TextureParams[0]));
    }
}
