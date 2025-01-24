package cinnamon.model.material;

import cinnamon.utils.Resource;

public record MaterialTexture(Resource texture, boolean smooth, boolean mipmap) {

    public MaterialTexture(Resource texture) {
        this(texture, false, false);
    }
}
