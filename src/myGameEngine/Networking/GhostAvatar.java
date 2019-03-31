package myGameEngine.Networking;

import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.util.UUID;

public class GhostAvatar {
    private UUID id;
    private SceneNode node;
    private Entity entity;
    private Vector3 position;
    private Vector3 heading;

    public GhostAvatar(UUID id, SceneNode node, Entity entity) {
        this.id = id;
        this.node = node;
        this.entity = entity;
    }

    public UUID getId() {
        return id;
    }

    public SceneNode getNode() {
        return node;
    }

    public void setNode(SceneNode node) {
        this.node = node;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Vector3 getPosition() {
        if(node == null){
            return position;
        } else return getNode().getWorldPosition();
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public Vector3 getHeading() {
        return heading;
    }

    public void setHeading(Vector3 heading) {
        this.heading = heading;
    }
}
