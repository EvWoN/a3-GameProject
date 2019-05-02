package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

public class MoveDownAction extends AbstractConstraintMoveAction {

    private MyGame myGame;
    private Node nodeToMove;
    ProtocolClient protoClient;

    public MoveDownAction(MyGame game, Node c, ProtocolClient protocolClient) {
        myGame = game;
        nodeToMove = c;
        protoClient = protocolClient;
    }

    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.05f*updates;
            Vector3 pos = nodeToMove.getLocalPosition();
            nodeToMove.setLocalPosition(pos.x(), pos.y(), pos.z() - value);
            nodeToMove.setLocalRotation(myGame.DOWN);
            //Check boundaries
            if(!isLegal(nodeToMove)){
                nodeToMove.setLocalPosition(pos.x(), pos.y(), pos.z() + value);
            }
            protoClient.sendMoveMessage(nodeToMove.getWorldPosition(),nodeToMove.getWorldForwardAxis());
        }
    }
}
