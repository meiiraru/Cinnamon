package cinnamon.world.entity.vehicle;

import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.utils.UIHelper;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class Car extends Vehicle {

    protected final float
            steeringFactor,
            accelerationFactor,
            maxAcceleration,
            maxReverseAcceleration,
            brakeDamping;

    private float steeringInput = 0;
    private float accelerationInput = 0;
    private boolean breakInput = false;

    protected float steering = 0;
    protected float acceleration = 0;

    public Car(UUID uuid, Resource model, int maxRiders, float steering, float acceleration, float maxAcceleration, float maxReverseAcceleration, float brakeDamping) {
        super(uuid, model, maxRiders);
        this.steeringFactor = steering;
        this.accelerationFactor = acceleration;
        this.maxAcceleration = maxAcceleration;
        this.maxReverseAcceleration = maxReverseAcceleration;
        this.brakeDamping = brakeDamping;
    }

    @Override
    protected void tickPhysics() {
        //no rider is present, so we can just use the default physics
        if (getRiders().isEmpty()) {
            super.tickPhysics();
            return;
        }

        //reset things back to zero
        float d = UIHelper.tickDelta(0.1f);
        steering = Math.lerp(steering, steeringInput, d);
        if (accelerationInput == 0)
            acceleration = Math.lerp(acceleration, 0, d);

        //calculate acceleration
        boolean isMovingForwards = motion.dot(getLookDir()) > 0;
        boolean isBraking = breakInput || (accelerationInput < 0 && isMovingForwards) || (accelerationInput > 0 && !isMovingForwards);
        acceleration += accelerationInput * accelerationFactor;

        //clamp and apply acceleration
        acceleration = Maths.clamp(acceleration, -maxReverseAcceleration, maxAcceleration);
        this.motion.add(getLookDir().mul(acceleration));

        //brakes
        if (isBraking)
            this.motion.mul(brakeDamping);

        //System.out.println("Braking: " + isBraking + " | Acceleration: " + acceleration + " | Speed: " + this.motion.length());

        //apply steering
        //only allow steering if the car has some speed
        float speed = this.motion.length();
        if (speed > 0.001f) {
            //rotate the acceleration vector based on the steering input
            float steeringAngle = steering * speed * steeringFactor * (isMovingForwards ? 1 : -1);
            this.motion.rotate(Rotation.Y.rotationDeg(-steeringAngle));
            rotateToWithRiders(rot.x, rot.y + steeringAngle);
        } else {
            this.motion.set(0f);
        }

        //reset the input values
        steeringInput = 0;
        accelerationInput = 0;
        breakInput = false;

        super.tickPhysics();
    }

    @Override
    protected void motionFallout() {
        if (onGround) {
            //drag if the motion direction with the look direction is high enough
            if (Math.abs(motion.normalize(new Vector3f()).dot(getLookDir())) > 0.8f) {
                this.motion.mul(0.98f);
            } else {
                //if the car is not moving forwards or backwards, apply a very stronger drag
                this.motion.mul(0.95f);
            }
        } else {
            super.motionFallout();
        }
    }

    @Override
    public void impulse(float left, float up, float forwards) {
        if (riding != null) {
            riding.impulse(left, up, forwards);
            return;
        }

        this.steeringInput = left;
        this.accelerationInput = forwards;
        this.breakInput = up > 0;
    }

    @Override
    public void rotate(float pitch, float yaw) {
        //super.rotate(pitch, yaw);
    }

    @Override
    protected void collide(Entity entity, CollisionResult result, Vector3f toMove) {
        if (entity instanceof Car)
            CollisionResolver.slide(result, motion, toMove);
        super.collide(entity, result, toMove);
    }
}
