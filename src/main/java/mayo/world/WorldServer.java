package mayo.world;

import mayo.utils.Resource;
import mayo.world.entity.vehicle.Cart;
import mayo.world.light.Light;
import mayo.world.terrain.Terrain;

import java.util.List;

public class WorldServer extends World {

    @Override
    public void init() {
        //load level
        LevelLoad.load(this, new Resource("data/levels/level0.json"));

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);
        //rip for-loop
        addLight(new Light().pos(-5.5f, 0.5f, 2f).color(0x000000));
        addLight(new Light().pos(-3.5f, 0.5f, 2f).color(0xFF0000));
        addLight(new Light().pos(-1.5f, 0.5f, 2f).color(0x00FF00));
        addLight(new Light().pos(0.5f, 0.5f, 2f).color(0x0000FF));
        addLight(new Light().pos(2.5f, 0.5f, 2f).color(0x00FFFF));
        addLight(new Light().pos(4.5f, 0.5f, 2f).color(0xFF00FF));
        addLight(new Light().pos(6.5f, 0.5f, 2f).color(0xFFFF00));
        addLight(new Light().pos(8.5f, 0.5f, 2f).color(0xFFFFFF));

        Cart c = new Cart();
        c.setPos(10, 2, 10);
        this.addEntity(c);

        Cart c2 = new Cart();
        c2.setPos(15, 2, 10);
        this.addEntity(c2);

        runScheduledTicks();
    }

    @Override
    public void close() {}

    public List<Terrain> getTerrain() {
        return this.terrain;
    }
}
