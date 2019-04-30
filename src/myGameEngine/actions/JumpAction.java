package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;

public class JumpAction extends AbstractConstraintMoveAction {

    private Node nodeToMove;

    public JumpAction(Node c) {
        nodeToMove = c;
    }

    @Override
    public void performAction(float v, Event event) {
        float[] pos = { nodeToMove.getLocalPosition().x(), nodeToMove.getLocalPosition().y(), nodeToMove.getLocalPosition().z() };
        //System.out.println("Angular: " + nodeToMove.getPhysicsObject().getAngularSleepThreshold() + " Linear: " + nodeToMove.getPhysicsObject().getLinearSleepThreshold());
        if(event.getValue() > .2f || event.getValue() < -.2f)
            nodeToMove.getPhysicsObject().applyForce(10f, 10f, 0.0f, 0.0f, 0.0f, 0.0f);
    }
}
