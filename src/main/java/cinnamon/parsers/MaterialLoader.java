package cinnamon.parsers;

import cinnamon.model.obj.material.Material;
import cinnamon.model.obj.material.PBRMaterial;
import cinnamon.model.obj.material.MtlMaterial;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static cinnamon.utils.Maths.parseVec3;

public class MaterialLoader {

    public static Map<String, Material> load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        Map<String, Material> map = new HashMap<>();

        if (stream == null)
            return map;

        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);

        boolean pbr = path.endsWith(".pbr");
        if (!pbr && !path.endsWith(".mtl"))
            throw new RuntimeException("Unsupported material file: " + path);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            Material material = pbr ? new PBRMaterial("") : new MtlMaterial("");

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
                        material = pbr ? new PBRMaterial(materialName) : new MtlMaterial(materialName);
                    }
                    case "smooth" -> material.setSmooth(parseInt(split[1]) > 0);
                    case "mipmap" -> material.setMipmap(parseInt(split[1]) > 0);
                }

                if (pbr) processPBR(split, (PBRMaterial) material, res, folder);
                else     processMtl(split, (MtlMaterial) material, res, folder);
            }

            map.put(material.getName(), material);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load material \"" + path + "\"", e);
        }
    }

    private static void processMtl(String[] split, MtlMaterial material, Resource res, String folder) {
        switch (split[0]) {
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
            case "map_Ke" -> material.setEmissiveTex(new Resource(res.getNamespace(), folder + split[1]));
            case "map_d" -> material.setAlphaTex(new Resource(res.getNamespace(), folder + split[1]));
            case "map_bump", "bump" -> material.setBumpTex(new Resource(res.getNamespace(), folder + split[1]));
            case "disp" -> material.setDisplacementTex(new Resource(res.getNamespace(), folder + split[1]));
            case "decal" -> material.setStencilDecalTex(new Resource(res.getNamespace(), folder + split[1]));
        }
    }

    private static void processPBR(String[] split, PBRMaterial material, Resource res, String folder) {
        switch (split[0]) {
            //textures
            case "albedo" -> material.setAlbedo(new Resource(res.getNamespace(), folder + split[1]));
            case "height" -> material.setHeight(new Resource(res.getNamespace(), folder + split[1]));
            case "normal" -> material.setNormal(new Resource(res.getNamespace(), folder + split[1]));
            case "roughness" -> material.setRoughness(new Resource(res.getNamespace(), folder + split[1]));
            case "metallic" -> material.setMetallic(new Resource(res.getNamespace(), folder + split[1]));
            case "ao" -> material.setAO(new Resource(res.getNamespace(), folder + split[1]));
            case "emissive" -> material.setEmissive(new Resource(res.getNamespace(), folder + split[1]));

            //height
            case "ps" -> material.setHeightScale(parseFloat(split[1]));
        }
    }
}
