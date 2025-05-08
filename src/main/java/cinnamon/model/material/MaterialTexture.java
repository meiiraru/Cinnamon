package cinnamon.model.material;

import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;

public record MaterialTexture(Resource texture, Texture.TextureParams... params) {}
