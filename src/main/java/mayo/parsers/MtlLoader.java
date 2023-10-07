package mayo.parsers;

import mayo.model.obj.Material;
import mayo.utils.IOUtils;
import mayo.utils.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static mayo.utils.Maths.parseVec3;

public class MtlLoader {

    public static Map<String, Material> load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);
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
                switch (split[0]) {
                    //create new material
                    case "newmtl" -> {
                        if (!material.getName().isBlank() && !material.getName().equals("none"))
                            map.put(material.getName(), material);

                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < split.length; i++)
                            sb.append(split[i]).append(" ");

                        material = new Material(sb.toString().trim());
                    }

                    //ambient color
                    case "Ka" -> material.getAmbientColor().set(parseVec3(split[1], split[2], split[3]));

                    //diffuse color
                    case "Kd" -> material.getDiffuseColor().set(parseVec3(split[1], split[2], split[3]));

                    //specular color
                    case "Ks" -> material.getSpecularColor().set(parseVec3(split[1], split[2], split[3]));

                    //specular exponent
                    case "Ns" -> material.setSpecularExponent(parseFloat(split[1]));

                    //transmission filter color
                    case "Tf" -> material.getFilterColor().set(parseVec3(split[1], split[2], split[3]));

                    //optical density (index of refraction)
                    case "Ni" -> material.setRefractionIndex(parseFloat(split[1]));

                    //illumination model
                    case "illum" -> material.setIllumModel(parseInt(split[1]));

                    //textures
                    case "map_Ka" -> material.setAmbientTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "map_Kd" -> material.setDiffuseTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "map_Ks" -> material.setSpColorTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "map_Ns" -> material.setSpHighlightTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "map_d" -> material.setAlphaTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "map_bump", "bump" -> material.setBumpTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "disp" -> material.setDisplacementTex(new Resource(res.getNamespace(), folder + split[1]));
                    case "decal" -> material.setStencilDecalTex(new Resource(res.getNamespace(), folder + split[1]));
                }
            }

            map.put(material.getName(), material);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load material \"" + path + "\"", e);
        }
    }
}
