package myGameEngine.Networking;

import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.*;

import java.util.UUID;

public class GhostAvatar {
    private UUID id;
    private SceneNode node;
    private Entity entity;

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

    public Entity getEntity() {
        return entity;
    }

    public Vector3 getPosition() {
        return node.getWorldPosition();
    }

    public void setPosition(Vector3 position) {
        node.setLocalPosition(position);
    }

    public Vector3 getHeading() {
        return node.getWorldForwardAxis();
    }

    public void setHeading(Vector3 heading) {
        Vector3 sub = node.getLocalForwardAxis().sub(heading);
        float z = sub.z();
        float x = sub.x();

        double atan = Math.atan(x / z);

        Matrix4 rotationFrom = Matrix4f.createRotationFrom(Degreef.createFrom((float) atan), Vector3f.createFrom(0, 1, 0));
        Matrix3 mult = rotationFrom.toMatrix3().mult(node.getLocalRotation());
        node.setLocalRotation(mult);
    }
}
