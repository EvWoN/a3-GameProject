package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import myGameEngine.controller.OrbitCameraController;
import net.java.games.input.Event;
import ray.rage.scene.Node;

public class MoveLeftAction extends MoveAction2D {

    private MyGame myGame;
    private Node nodeToMove;
    ProtocolClient protoClient;
    OrbitCameraController occ;

    public MoveLeftAction(MyGame game, Node c, OrbitCameraController occ, ProtocolClient protocolClient) {
        myGame = game;
        nodeToMove = c;
        protoClient = protocolClient;
        this.occ = occ;
    }

    @Override
    public void performAction(float v, Event event) {
        float updates = (v/16f);
        if(event.getValue() > .2f || event.getValue() < -.2f) {
            float value = event.getValue() * 0.05f*updates;
        
            move(nodeToMove,occ.getCameraAzimuth(),90f,value);
        
            protoClient.sendMoveMessage(nodeToMove.getWorldPosition(),nodeToMove.getWorldForwardAxis());
        }
    }
}
