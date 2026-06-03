package cinnamon.world.terrain;

import cinnamon.animation.Animation;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.WorldClient;

import java.util.function.Consumer;

public class Button extends Terrain {

    public static final Resource
        CLICK_SOUND   = new Resource("sounds/terrain/button/click.ogg"),
        PRESS_SOUND   = new Resource("sounds/terrain/button/toggle_on.ogg"),
        RELEASE_SOUND = new Resource("sounds/terrain/button/toggle_off.ogg");

    protected boolean pressed;
    protected Consumer<Entity> onPress, onRelease;

    public Button() {
        super(TerrainModelRegistry.BUTTON.resource, TerrainRegistry.CUSTOM);
        getCollisionMask().setExcludeMask(0, true);
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setOnPress(Consumer<Entity> onPress) {
        this.onPress = onPress;
    }

    public void setOnRelease(Consumer<Entity> onRelease) {
        this.onRelease = onRelease;
    }

    @Override
    public boolean interact(Entity entity) {
        //button press
        if (!isPressed()) {
            //no press function
            if (onPress == null)
                return super.interact(entity);

            //run press function
            onPress.accept(entity);

            //play press animation and set pressed state only if we have a release function
            if (onRelease != null) {
                getAnimation("press").setLoop(Animation.Loop.HOLD).play();
                ((WorldClient) getWorld()).playSound(PRESS_SOUND, SoundCategory.TERRAIN, getTransform().getPos());
                pressed = true;
            //otherwise only play a click animation
            } else {
                getAnimation("click").setLoop(Animation.Loop.ONCE).play();
                ((WorldClient) getWorld()).playSound(CLICK_SOUND, SoundCategory.TERRAIN, getTransform().getPos());
            }
        } else {
            //if we got into this state, we must reset regardless if we have a function
            if (onRelease != null)
                onRelease.accept(entity);

            //reset animation and pressed state
            getAnimation("press").stop();
            getAnimation("release").setLoop(Animation.Loop.ONCE).play();
            ((WorldClient) getWorld()).playSound(RELEASE_SOUND, SoundCategory.TERRAIN, getTransform().getPos());
            pressed = false;

        }

        return true;
    }
}
