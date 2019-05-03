package myGameEngine.actions;

import myGameEngine.controller.SquishyBounceController;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsObject;
import ray.rage.scene.Node;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3;
import ray.rml.Vector3;

import java.util.Iterator;

public class ThrowItemAction extends AbstractInputAction {

    private SceneNode thrower;
    private SceneManager sm;
    private PhysicsEngine pe;
    private SquishyBounceController sc;

    public ThrowItemAction(SceneNode thrower, SceneManager sm, PhysicsEngine pe, SquishyBounceController sc) {
        this.thrower = thrower;
        this.sm = sm;
        this.pe = pe;
        this.sc = sc;
    }

    @Override
    public void performAction(float v, Event event) {
        Iterator<Node> children = thrower.getChildNodes().iterator();
        SceneNode root = (SceneNode) thrower.getParent();
        Vector3 worldPosition, localScale;
        Matrix3 worldRotation;
        SceneNode hold;
        double[] temptf;
        float mass = .5f;
        PhysicsObject physicsObject;
        while(children.hasNext()) {
            hold = (SceneNode) children.next();
            if(hold.getName().startsWith("Part"))
            {
                worldPosition = hold.getWorldPosition();
                worldRotation = hold.getWorldRotation();
                localScale = hold.getLocalScale();
                thrower.detachChild(hold);
                hold.setLocalPosition(worldPosition);
                hold.setLocalRotation(worldRotation);
                hold.setLocalScale(localScale);
                temptf = toDoubleArray(hold.getLocalTransform().toFloatArray());
                physicsObject = pe.addSphereObject(
                        pe.nextUID(),
                        mass,
                        temptf,
                        .5f
                );
                physicsObject.setBounciness(0.8f);
                physicsObject.setFriction(.1f);
                hold.setPhysicsObject(physicsObject);
                sc.removeNode(hold);
                root.attachChild(hold);
                hold.getPhysicsObject().applyForce(
                        thrower.getLocalForwardAxis().x() * 100,
                        thrower.getLocalForwardAxis().y() * 100,
                        thrower.getLocalForwardAxis().z() * 100,
                        hold.getLocalPosition().x(),
                        hold.getLocalPosition().y(),
                        hold.getLocalPosition().z()
                );
                /*
                try {
                    Entity e = sm.createEntity("Test", "cube.obj");
                    e.setPrimitive(Renderable.Primitive.TRIANGLES);
                    hold.attachObject(e);
                }
                catch (IOException e) { e.printStackTrace(); }*/

            }
        }
    }

    private double[] toDoubleArray(float[] in) {
        int size = in.length;
        double[] ret = new double[size];
        for(int i = 0; i < size; ++i) ret[i] = (double) in[i];
        return ret;
    }
}
