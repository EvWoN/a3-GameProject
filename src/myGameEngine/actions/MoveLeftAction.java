package myGameEngine.actions;

import net.java.games.input.Event;
import ray.rage.scene.Node;

public class MoveLeftAction extends AbstractConstraintMoveAction {
    private Node nodeToMove;

    public MoveLeftAction(Node c) {
        nodeToMove = c;
    }

    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.01f*updates;
            System.out.println("UpAxis in left mov." + nodeToMove.getLocalUpAxis());
            nodeToMove.moveLeft(-value);
            //Check boundaries
            if(!isLegal(nodeToMove)){
                nodeToMove.moveRight(-value);
            }
        }
    }
}
