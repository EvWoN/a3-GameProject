package myGameEngine.actions;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.Node;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3;
import ray.rml.Vector3;

import java.io.IOException;
import java.util.Iterator;

public class ThrowItemAction extends AbstractInputAction {

    SceneNode thrower;
    SceneManager sm;

    public ThrowItemAction(SceneNode thrower, SceneManager sm) {
        this.thrower = thrower;
        this.sm = sm;
    }

    @Override
    public void performAction(float v, Event event) {
        Iterator<Node> children = thrower.getChildNodes().iterator();
        SceneNode root = (SceneNode) thrower.getParent();
        Vector3 worldPosition, localScale;
        Matrix3 worldRotation;
           SceneNode hold;
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
                try {
                    Entity e = sm.createEntity("Test", "cube.obj");
                    e.setPrimitive(Renderable.Primitive.TRIANGLES);
                    hold.attachObject(e);
                }
                catch (IOException e) { e.printStackTrace(); }

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
