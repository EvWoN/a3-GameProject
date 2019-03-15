package myGameEngine.actions;

import net.java.games.input.Event;
import ray.rage.scene.Node;

public class MoveForwardAction extends AbstractConstraintMoveAction {
    private Node nodeToMove;

    public MoveForwardAction(Node c) {
        nodeToMove = c;
    }

    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.05f*updates;
            nodeToMove.moveForward(value);
            //Check boundaries
            if(!isLegal(nodeToMove)){
                nodeToMove.moveBackward(value);
            }
        }
    }
}
