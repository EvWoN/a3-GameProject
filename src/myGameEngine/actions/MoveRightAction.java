package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;

public class MoveRightAction extends AbstractConstraintMoveAction {

    private MyGame myGame;
    private Node nodeToMove;
    private ProtocolClient protoClient;

    public MoveRightAction(MyGame game, Node c, ProtocolClient protocolClient) {
        myGame = game;
        nodeToMove = c;
        protoClient = protocolClient;
    }

    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.01f*updates;
            nodeToMove.moveRight(-value);
            //Check boundaries
            if(!isLegal(nodeToMove)){
                nodeToMove.moveLeft(-value);
            }
            protoClient.sendMoveMessage(nodeToMove.getWorldPosition(),nodeToMove.getWorldForwardAxis());
            myGame.updateVerticalPosition((SceneNode) nodeToMove);
        }
    }
}
