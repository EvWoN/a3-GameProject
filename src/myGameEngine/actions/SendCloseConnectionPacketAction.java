package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import ray.input.action.AbstractInputAction;

public class SendCloseConnectionPacketAction extends AbstractInputAction { // for leaving the game... need to attach to an input device

    ProtocolClient protClient;
    MyGame mg;

    public SendCloseConnectionPacketAction(ProtocolClient protClient, MyGame mg) {
        this.protClient = protClient;
    }

    @Override
    public void performAction(float v, net.java.games.input.Event event) {
        if (protClient != null && mg.isClientConnected == true) {
            protClient.sendByeMessage();
        }
    }
}