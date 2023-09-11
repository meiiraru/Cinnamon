package mayo.parsers;

import mayo.model.obj.Material;
import mayo.render.Texture;
import mayo.utils.IOUtils;
import mayo.utils.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static mayo.utils.Meth.parseVec3;

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
                        if (!material.getName().isBlank())
                            map.put(material.getName(), material);
                        material = new Material(split[1]);
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
                    case "map_Kd" -> material.setDiffuseTex(new Texture(new Resource(res.getNamespace(), folder + split[1])));
                }
            }

            map.put(material.getName(), material);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load material \"" + path + "\"", e);
        }
    }
}
