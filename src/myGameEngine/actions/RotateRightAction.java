package myGameEngine.actions;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rml.*;

public class RotateRightAction extends AbstractInputAction {
    private Node nodeToMove;

    public RotateRightAction(Node c) {
        nodeToMove = c;
    }

    @Override
    public void performAction(float v, Event event) {
        float value = event.getValue();
        Matrix4 rotationFrom = Matrix4f.createRotationFrom(Degreef.createFrom(-value), Vector3f.createFrom(0, 1, 0));
        Matrix3 mult = rotationFrom.toMatrix3().mult(nodeToMove.getLocalRotation());
        nodeToMove.setLocalRotation(mult);
    }
}
