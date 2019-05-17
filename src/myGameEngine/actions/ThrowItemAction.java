package myGameEngine.actions;

import javafx.beans.property.SimpleBooleanProperty;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsObject;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3;
import ray.rml.Vector3;

import java.util.ArrayList;
import java.util.Iterator;

public class ThrowItemAction extends AbstractInputAction {

    private SceneNode thrower;
    private SimpleBooleanProperty holding;
    private ArrayList<SceneNode> partsList;
    private PhysicsEngine pe;

    public ThrowItemAction(SceneNode thrower, SimpleBooleanProperty holding, ArrayList<SceneNode> partsList, PhysicsEngine pe) {
        this.thrower = thrower;
        this.partsList = partsList;
        this.pe = pe;
        this.holding = holding;
    }

    @Override
    public void performAction(float v, Event event) {
        Iterator<Node> children = thrower.getChildNodes().iterator();
        SceneNode root = (SceneNode) thrower.getParent();
        Vector3 worldPosition, localScale;
        Matrix3 worldRotation;
        SceneNode hold;
        double[] temptf;
        float[] boxDim = { .5f, .5f, .5f };
        float mass = 10.0f;
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
                physicsObject = pe.addBoxObject(
                        pe.nextUID(),
                        mass,
                        temptf,
                        boxDim
                );
                physicsObject.setBounciness(0.0f);
                physicsObject.setFriction(100.0f);
                hold.setPhysicsObject(physicsObject);
                root.attachChild(hold);
                hold.getPhysicsObject().applyForce(
                        thrower.getLocalForwardAxis().x() * 1000,
                        thrower.getLocalForwardAxis().y() + 1000,
                        thrower.getLocalForwardAxis().z() * 1000,
                        hold.getLocalPosition().x(),
                        hold.getLocalPosition().y(),
                        hold.getLocalPosition().z()
                );
                partsList.add(hold);
                holding.set(false);
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
