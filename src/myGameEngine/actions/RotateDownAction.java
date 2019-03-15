package myGameEngine.actions;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rml.Degreef;

public class RotateDownAction extends AbstractInputAction {
    private Node nodeToMove;

    public RotateDownAction(Node c) {
        nodeToMove = c;
    }

    @Override
    public void performAction(float v, Event event) {
        float value = event.getValue();
        nodeToMove.pitch(Degreef.createFrom(value));
    }
}
