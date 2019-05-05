package myGameEngine.actions;

import myGameEngine.Managers.Movement2DManager;
import myGameEngine.controller.OrbitCameraController;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;

public class MoveRightAction extends AbstractInputAction {
    
    private Node nodeToMove;
    OrbitCameraController occ;
    Movement2DManager mm;
    
    public MoveRightAction(Node c, OrbitCameraController occ, Movement2DManager mm) {
        nodeToMove = c;
        this.occ = occ;
        this.mm = mm;
    }
    
    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.05f*updates;
            float direction = occ.getCameraAzimuth() - 90; //Right
            mm.queueMovementEvent(nodeToMove,direction,value);
        }
    }
}
