package cinnamon.commands;

import cinnamon.world.entity.Entity;
import cinnamon.world.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CommandSource {

    private Entity entity;
    private String name;
    private World world;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f direction;

    public CommandSource(Entity entity, String name, World world, Vector3f position, Quaternionf rotation, Vector3f direction) {
        this.entity = entity;
        this.name = name;
        this.world = world;
        this.position = position;
        this.rotation = rotation;
        this.direction = direction;
    }

    public Entity entity() {
        return entity;
    }

    public String name() {
        return name;
    }

    public World world() {
        return world;
    }

    public Vector3f position() {
        return position;
    }

    public Quaternionf rotation() {
        return rotation;
    }

    public Vector3f direction() {
        return direction;
    }

    public CommandSource setEntity(Entity entity) {
        this.entity = entity;
        return this;
    }

    public CommandSource setName(String name) {
        this.name = name;
        return this;
    }

    public CommandSource setWorld(World world) {
        this.world = world;
        return this;
    }

    public CommandSource setPosition(Vector3f position) {
        this.position = position;
        return this;
    }

    public CommandSource setRotation(Quaternionf rotation) {
        this.rotation = rotation;
        return this;
    }

    public CommandSource setDirection(Vector3f direction) {
        this.direction = direction;
        return this;
    }
}