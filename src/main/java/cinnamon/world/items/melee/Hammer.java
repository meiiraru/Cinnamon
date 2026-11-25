package cinnamon.world.items.melee;

import cinnamon.animation.Animation;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.shape.AABB;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.model.AnimatedMeshRenderer;
import cinnamon.utils.Pair;
import cinnamon.world.entity.DamageType;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemCategory;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Hammer extends Item {

    protected final float range = 2.5f;
    protected final int damage = 5;
    protected final float knockback = 0.8f;
    protected final int swingDelay = 20;

    protected final float specialKnockback = 1.5f;
    protected final int specialDelay = 100;

    protected int delayTicks = 0;
    protected int specialTicks = 0;

    public Hammer() {
        super(ItemModelRegistry.HAMMER.id, 1, 1, ItemModelRegistry.HAMMER.resource);
    }

    @Override
    public Item copy() {
        return new Hammer();
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.WEAPON;
    }

    @Override
    public void tick() {
        super.tick();

        if (delayTicks > 0) delayTicks--;
        if (specialTicks > 0) specialTicks--;

        if (isFiring() && canAttack())
            attack();
    }

    @Override
    public boolean fire() {
        boolean canFire = super.fire();

        if (canFire && canAttack())
            attack();

        return canFire;
    }

    @Override
    public boolean use() {
        boolean canUse = super.use();

        if (canUse && canSpecial())
            special();

        return canUse;
    }

    protected void attack() {
        LivingEntity src = getSource();
        Pair<Hit, Entity> lookingTarget = src.getLookingEntity(getRange());

        //apply cooldown
        delayTicks = swingDelay;

        //animation
        if (getModel() instanceof AnimatedMeshRenderer a) {
            List<String> swingAnims = getMatchingAnimations(a, "swing");
            playRandomAnim(a, swingAnims);
        }

        //no target
        if (lookingTarget == null)
            return;

        //knockback target
        Entity target = lookingTarget.second();
        if (target instanceof PhysEntity phys)
            phys.knockback(src.getLookDir(), getKnockback());

        //damage target
        if (target instanceof LivingEntity le)
            le.damage(src, DamageType.MELEE, getDamage(), false);
    }

    protected void special() {
        LivingEntity src = getSource();

        //apply cooldown
        specialTicks = specialDelay;

        //animation
        if (getModel() instanceof AnimatedMeshRenderer a) {
            List<String> swingAnims = getMatchingAnimations(a, "special");
            playRandomAnim(a, swingAnims);
        }

        Vector3f srcPos = src.getEyePos();
        float range = getRange();
        float rangeSq = range * range;

        //get all entities in a circle range
        List<Entity> entities = src.getWorld().getEntities(new AABB(srcPos).inflate(range, 0.25f, range));
        for (Entity entity : entities) {
            if (entity == src || !(entity instanceof PhysEntity phys))
                continue;

            //check if entity is in range
            Vector3f entityPos = entity.getTransform().getPos();
            float distSq = srcPos.distanceSquared(entityPos);
            if (distSq > rangeSq)
                continue;

            //apply knockback from the player direction
            Vector3f knockbackDir = new Vector3f(entityPos).sub(srcPos).normalize();
            knockbackDir.y = 0.25f; //add a little vertical knockback
            phys.knockback(knockbackDir, getSpecialKnockback());
        }
    }

    protected List<String> getMatchingAnimations(AnimatedMeshRenderer renderer, String keyword) {
        List<String> anims = new ArrayList<>();

        //get matching animations
        for (String animation : renderer.getAnimations()) {
            if (animation.toLowerCase().contains(keyword))
                anims.add(animation);
        }

        return anims;
    }

    protected void playRandomAnim(AnimatedMeshRenderer renderer, List<String> anims) {
        if (anims.isEmpty())
            return;

        Animation anim = renderer.getAnimation(anims.get((int) (Math.random() * anims.size())));
        anim.stop();
        anim.play();
    }

    public boolean canAttack() {
        return delayTicks <= 0;
    }

    public boolean canSpecial() {
        return specialTicks <= 0;
    }

    public float getRange() {
        return range;
    }

    public int getDamage() {
        return damage;
    }

    public float getKnockback() {
        return knockback;
    }

    public float getSpecialKnockback() {
        return specialKnockback;
    }

    public Object getCountText() {
        return "\u269E.*\u2606.*.\u269F";
    }
}
