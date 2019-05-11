package myGameEngine.Networking;

import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.*;

import java.util.UUID;

public class GhostAvatar {
    private UUID id;
    private SceneNode node;
    private Entity entity;
    private Object bean;

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

        if(!(heading.x() == 0 && heading.y() == 0 && heading.z() == 0)) {

            Vector3 rightVector = heading.rotate(Degreef.createFrom(90f), Vector3f.createFrom(0, 1, 0));
            Vector3 topVector = Vector3f.createFrom(0, 1, 0);

            System.out.println("Heading: " + heading + "\nRightVector: " + rightVector + "\nTopVector: " + topVector);

            node.setLocalRotation(Matrix3f.createFrom(rightVector, topVector, heading));
        }
//        node.setLocalRotation(Matrix3f.createFrom());
//        double atan = Math.atan(x / z);
//        if(!Double.isNaN(atan)) {
//            Matrix4 rotationFrom = Matrix4f.createRotationFrom(Degreef.createFrom((float) atan), Vector3f.createFrom(0, 1, 0));
//            Matrix3 mult = rotationFrom.toMatrix3().mult(node.getLocalRotation());
//            System.out.println(mult);
//            node.setLocalRotation(mult);
//        }
    }
    
    @Override
    public String toString() {
        return "Obj: Ghost Avatar of UUID: " + id;
    }
    
    /**
     * Get an attached reference
     * @return a reference to any object associated with this GhostAvatar. Caller is responsible for knowing what type of reference object is expected.
     */
    public Object getBean() {
        return bean;
    }
    
    /**
     * Attach any reference you want to this GhostAvatar
     * @param bean a reference to any object. Caller for the getter will be responsible for knowing what reference object is expected from this ghost.
     */
    public void setBean(Object bean) {
        this.bean = bean;
    }
}
