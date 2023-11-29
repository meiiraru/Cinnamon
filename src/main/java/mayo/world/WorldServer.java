package mayo.world;

import mayo.utils.Resource;
import mayo.world.entity.vehicle.Cart;
import mayo.world.terrain.Terrain;

import java.util.List;

public class WorldServer extends World {

    @Override
    public void init() {
        //load level
        LevelLoad.load(this, new Resource("data/levels/level0.json"));

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

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
