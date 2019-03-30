package myGameEngine.Networking;

import a3.MyGame;
import ray.networking.client.GameConnectionClient;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.vecmath.Vector3d;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.Vector;

public class ProtocolClient extends GameConnectionClient {
    private MyGame game;
    private UUID id;
    private Vector<GhostAvatar> ghostAvatars;

    public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException {
        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        this.ghostAvatars = new Vector<GhostAvatar>();
    }

    @Override
    protected void processPacket(Object o) {
        String message = (String) o;
        String[] msgTokens = message.split(",");
        if (msgTokens.length > 0) {
            if (msgTokens[0].compareTo("join") == 0) // receive “join”
            { // format: join, success or join, failure
                if (msgTokens[1].compareTo("success") == 0) {
                    game.setIsConnected(true);
                    sendCreateMessage(game.getPlayerPosition());
                }
                if (msgTokens[1].compareTo("failure") == 0) {
                    game.setIsConnected(false);
                }
            }
            if (msgTokens[0].compareTo("bye") == 0) // receive “bye”
            { // format: bye, remoteId
                UUID ghostID = UUID.fromString(msgTokens[1]);
                removeGhostAvatar(ghostID);
            }
            if ((msgTokens[0].compareTo("dsfr") == 0) // receive “dsfr”
                    || (msgTokens[0].compareTo("create") == 0)) { // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
                UUID ghostID = UUID.fromString(msgTokens[1]);
                Vector3 ghostPosition = Vector3f.createFrom(
                        Float.parseFloat(msgTokens[2]),
                        Float.parseFloat(msgTokens[3]),
                        Float.parseFloat(msgTokens[4]));
                try {
                    createGhostAvatar(ghostID, ghostPosition);
                } catch (IOException e) {
                    System.out.println("error creating ghost avatar");
                }
            }
            if (msgTokens[0].compareTo("wsds") == 0) // rec. “create…”
            { // etc….. }
            }
            if (msgTokens[0].compareTo("wsds") == 0) // rec. “wants…”
            { // etc….. }
            }
            if (msgTokens[0].compareTo("move") == 0) // rec. “move...”
            { // etc….. }
            }
        }
    }


    private void createGhostAvatar(UUID ghostID, Vector3 ghostPosition) throws IOException {

    }

    private void removeGhostAvatar(UUID ghostID) {

    }


    //Also need functions to instantiate ghost avatar, remove a ghost avatar,
//look up a ghost in the ghost table, update a ghost’s position, and
//accessors as needed.

    public void sendJoinMessage() // format: join, localId
    {
        try {
            sendPacket(new String("join," + id.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCreateMessage(Vector3 pos) { // format: (create, localId, x,y,z)
        try {
            String message = new String("create," + id.toString());
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendByeMessage() { // etc….. }
    }

    public void sendDetailsForMessage(UUID remId, Vector3d pos) { // etc….. }
    }

    public void sendMoveMessage(Vector3d pos) { // etc….. }
    }
}