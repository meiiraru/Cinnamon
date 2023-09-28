package mayo.world.entity.collectable;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.effects.Effect;
import mayo.world.entity.Entity;
import mayo.world.entity.living.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EffectBox extends Collectable {

    private static final Model MODEL = ModelManager.load(new Resource("models/entities/mystery_box/mystery_box.obj"));

    private static final List<Supplier<Effect>> EFFECT_LIST = List.of(
            () -> Effect.Type.NEVER_CRIT.create(200),
            () -> Effect.Type.ALWAYS_CRIT.create(300),
            () -> Effect.Type.PACIFIST.create(100),
            () -> Effect.Type.DAMAGE_BOOST.create(150, 2),
            () -> Effect.Type.DAMAGE_BOOST.create(200, 1),
            () -> Effect.Type.HEAL.create(200, 2)
    );

    public EffectBox(World world) {
        super(MODEL, world);
    }

    @Override
    protected boolean onPickUp(Entity entity) {
        if (entity instanceof Player p) {
            p.giveEffect(EFFECT_LIST.get((int) (Math.random() * EFFECT_LIST.size())).get());
            return true;
        }

        return false;
    }
}
