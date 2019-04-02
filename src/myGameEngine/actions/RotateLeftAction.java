package myGameEngine.actions;

import myGameEngine.Networking.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rml.*;

public class RotateLeftAction extends AbstractInputAction {
    private Node nodeToMove;
    ProtocolClient protoClient;

    public RotateLeftAction(Node c, ProtocolClient protocolClient) {
        nodeToMove = c;
        protoClient = protocolClient;
    }

    @Override
    public void performAction(float v, Event event) {
        float value = event.getValue();
        Matrix4 rotationFrom = Matrix4f.createRotationFrom(Degreef.createFrom(value), Vector3f.createFrom(0, 1, 0));
        Matrix3 mult = rotationFrom.toMatrix3().mult(nodeToMove.getLocalRotation());
        nodeToMove.setLocalRotation(mult);
        protoClient.sendMoveMessage(nodeToMove.getWorldPosition(),nodeToMove.getWorldForwardAxis());
    }
}
