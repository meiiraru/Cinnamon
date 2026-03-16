package cinnamon.physics;

import org.joml.Math;
import org.ode4j.math.DQuaternion;
import org.ode4j.ode.*;

public class PhysicsSystem {

    private static volatile boolean ODE_INITIALIZED = false;

    private final DWorld world;
    private final DSpace space;
    private final DJointGroup contactGroup;
    private DGeom.DNearCallback nearCallback;

    public PhysicsSystem() {
        ensureOdeInitialized();
        world = OdeHelper.createWorld();
        space = OdeHelper.createHashSpace();
        contactGroup = OdeHelper.createJointGroup();

        // world defaults (slightly softer and more iterations)
        world.setGravity(0, -9.81, 0);
        world.setERP(0.2);
        world.setCFM(1e-4);
        world.setQuickStepNumIterations(40);
        world.setContactSurfaceLayer(0.005);
        world.setDamping(1e-3, 1e-3); // global linear, angular damping

        // auto-disable to reduce jitter of resting stacks
        world.setAutoDisableFlag(true);
        world.setAutoDisableLinearThreshold(0.02);
        world.setAutoDisableAngularThreshold(0.02);
        world.setAutoDisableSteps(25);

        // initialize near-callback after world/contactGroup exist
        nearCallback = (data, o1, o2) -> {
            DBody b1 = o1.getBody();
            DBody b2 = o2.getBody();

            // ignore kinematic-kinematic collisions
            if (b1 != null && b2 != null && b1.isKinematic() && b2.isKinematic())
                return;

            final int MAX_CONTACTS = 8;
            DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
            int numContacts = OdeHelper.collide(o1, o2, MAX_CONTACTS, contacts.getGeomBuffer());

            if (numContacts > 0) {
                for (int i = 0; i < numContacts; i++) {
                    DContact contact = contacts.get(i);
                    contact.surface.mode = OdeConstants.dContactSoftERP | OdeConstants.dContactSoftCFM | OdeConstants.dContactApprox1
                            | OdeConstants.dContactSlip1 | OdeConstants.dContactSlip2;
                    contact.surface.soft_erp = 0.2;
                    contact.surface.soft_cfm = 5e-3;    // a bit softer for stability
                    contact.surface.mu = 200;            // moderate friction
                    contact.surface.slip1 = 0.001;       // slight slip to avoid stick-slip bursts
                    contact.surface.slip2 = 0.001;
                    contact.surface.bounce = 0.0;
                    contact.surface.bounce_vel = 0.0;

                    DJoint c = OdeHelper.createContactJoint(world, contactGroup, contact);
                    c.attach(b1, b2);
                }
            }
        };
    }

    private static void ensureOdeInitialized() {
        if (!ODE_INITIALIZED) {
            synchronized (PhysicsSystem.class) {
                if (!ODE_INITIALIZED) {
                    OdeHelper.initODE2(0);
                    ODE_INITIALIZED = true;
                }
            }
        }
    }

    /**
     * Step simulation using substeps for stability. The total deltaTime is split
     * into smaller quickStep substeps (target ~1/120s) to reduce tunneling and jitter.
     */
    public void tick(float deltaTime) {
        double target = 1.0 / 120.0; // 120 Hz internal substep
        int steps = Math.max(1, (int) Math.ceil(deltaTime / target));
        double h = deltaTime / steps;
        for (int i = 0; i < steps; i++) {
            space.collide(null, nearCallback);
            world.quickStep(h);
            contactGroup.empty();
        }
    }

    public void free() {
        contactGroup.destroy();
        space.destroy();
        world.destroy();
        // Do not call OdeHelper.closeODE() here; it's global. Let JVM exit handle it or manage at app shutdown.
    }

    // --- Helpers & Getters --- //

    public DWorld getOdeWorld() {
        return world;
    }

    public DSpace getOdeSpace() {
        return space;
    }

    /** Create an infinite plane: ax + by + cz = d */
    public DGeom createPlane(double a, double b, double c, double d) {
        return OdeHelper.createPlane(space, a, b, c, d);
    }

    /** Create a static box collider (no body) */
    public DGeom createStaticBox(double lx, double ly, double lz, double px, double py, double pz) {
        DGeom box = OdeHelper.createBox(space, lx, ly, lz);
        box.setPosition(px, py, pz);
        return box;
    }

    /** Create a dynamic body with a box geom attached and mass */
    public BodyAndGeom createDynamicBox(double mass, double lx, double ly, double lz, double px, double py, double pz) {
        DBody body = OdeHelper.createBody(world);
        DMass M = OdeHelper.createMass();
        M.setBox(1.0, lx, ly, lz); // density 1.0, adjust to target mass
        M.adjust(mass);
        body.setMass(M);
        body.setPosition(px, py, pz);

        DGeom geom = OdeHelper.createBox(space, lx, ly, lz);
        geom.setBody(body);
        return new BodyAndGeom(body, geom);
    }

    public BodyAndGeom createDynamicSphere(double mass, double radius, double px, double py, double pz) {
        DBody body = OdeHelper.createBody(world);
        DMass M = OdeHelper.createMass();
        M.setSphere(1.0, radius); // density 1.0, adjust to target mass
        M.adjust(mass);
        body.setMass(M);
        body.setPosition(px, py, pz);

        DGeom geom = OdeHelper.createSphere(space, radius);
        geom.setBody(body);
        return new BodyAndGeom(body, geom);
    }

    /** Set body orientation from quaternion (x,y,z,w). */
    public static void setBodyQuaternion(DBody body, double x, double y, double z, double w) {
        body.setQuaternion(new DQuaternion(w, x, y, z));
    }

    /** Convenience record to return both body and geom. */
    public record BodyAndGeom(DBody body, DGeom geom) {}
}
