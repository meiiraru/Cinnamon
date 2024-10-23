package cinnamon.render;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.model.obj.Mesh;
import cinnamon.utils.Pair;

import java.util.List;

public class AnimatedModel extends Model {

    private final Bone bone;
    private final List<Animation> animations;

    public AnimatedModel(Mesh mesh, Pair<Bone, List<Animation>> animations) {
        super(mesh);
        this.bone = animations.first();
        this.animations = animations.second();
    }

    public void render(MatrixStack matrices) {
        for (Animation animation : animations)
            animation.update();
        bone.render(this, matrices);
    }

    public Animation getAnimation(String name) {
        for (Animation animation : animations) {
            if (animation.getName().equals(name))
                return animation;
        }

        return null;
    }
}
