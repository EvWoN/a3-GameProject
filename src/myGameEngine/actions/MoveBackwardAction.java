package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;

public class MoveBackwardAction extends AbstractConstraintMoveAction {

    private MyGame myGame;
    private Node nodeToMove;
    private ProtocolClient protoClient;

    public MoveBackwardAction(MyGame game, Node c, ProtocolClient protocolClient) {
        myGame = game;
        nodeToMove = c;
        protoClient = protocolClient;
    }

    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.05f*updates;
            nodeToMove.moveBackward(value);
            //Check boundaries
            if(!isLegal(nodeToMove)){
                nodeToMove.moveForward(value);
            }
            protoClient.sendMoveMessage(nodeToMove.getWorldPosition(),nodeToMove.getWorldForwardAxis());
            myGame.updateVerticalPosition((SceneNode) nodeToMove);
        }
    }
}
