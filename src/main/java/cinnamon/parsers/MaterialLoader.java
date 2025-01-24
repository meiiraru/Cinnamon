package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MaterialLoader {

    public static Map<String, Material> load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        Map<String, Material> map = new HashMap<>();

        if (stream == null)
            return map;

        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);

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
                    case "map_Kd", "albedo", "diffuse" -> material.setAlbedo(parseTexture(split, folder, res));
                    case "map_disp", "bump", "height" -> material.setHeight(parseTexture(split, folder, res));
                    case "map_Bump", "norm", "normal", "map_Kn" -> {
                        for (int i = 1; i < split.length; i++) {
                            if (split[i].equals("-bm")) {
                                material.setHeightScale(Float.parseFloat(split[i + 1]));
                                break;
                            }
                        }
                        material.setNormal(parseTexture(split, folder, res));
                    }
                    case "map_ao", "map_AO", "ao", "ambient_occlusion" -> material.setAO(parseTexture(split, folder, res));
                    case "map_Pr", "roughness" -> material.setRoughness(parseTexture(split, folder, res));
                    case "map_Pm", "metallic" -> material.setMetallic(parseTexture(split, folder, res));
                    case "map_Ke", "emissive" -> material.setEmissive(parseTexture(split, folder, res));
                }
            }

            map.put(material.getName(), material);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load material \"" + path + "\"", e);
        }
    }

    private static MaterialTexture parseTexture(String[] split, String folder, Resource res) {
        Resource path = new Resource(res.getNamespace(), folder + split[split.length - 1]);
        Set<String> flags = new HashSet<>(Arrays.asList(split).subList(1, split.length - 1));
        return new MaterialTexture(path, flags.contains("-smooth"), flags.contains("-mip") || flags.contains("-mipmap"));
    }
}
