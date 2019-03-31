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
            // receive “join”
            // format: join, success/join, failure
            if (msgTokens[0].compareTo("join") == 0)
            {
                if (msgTokens[1].compareTo("success") == 0) {
                    game.setIsConnected(true);
                    sendJoinMessage();
                    sendCreateMessage(game.getPlayerPosition(), game.getPlayerHeading());
                }
                if (msgTokens[1].compareTo("failure") == 0) {
                    game.setIsConnected(false);
                }
            }
            // receive “bye”
            // format: bye, remoteId
            if (msgTokens[0].compareTo("bye") == 0)
            {
                UUID ghostID = UUID.fromString(msgTokens[1]);
                try { removeGhostAvatar(ghostID); }
                catch (IOException e) { System.out.println("Error removing ghost avatar."); }
            }
            // receive “dsfr”
            // format: dsfr, clientid, remoteId, x, y, z, u, v, n
            if (msgTokens[0].compareTo("dsfr") == 0) {
                UUID ghostID = UUID.fromString(msgTokens[1]);
                Vector3 ghostPosition = Vector3f.createFrom(
                        Float.parseFloat(msgTokens[3]),
                        Float.parseFloat(msgTokens[4]),
                        Float.parseFloat(msgTokens[5]));
                Vector3 ghostHeading = Vector3f.createFrom(
                        Float.parseFloat(msgTokens[6]),
                        Float.parseFloat(msgTokens[7]),
                        Float.parseFloat(msgTokens[8]));
                try { updateGhostAvatar(ghostID, ghostPosition, ghostHeading); }
                catch (IOException e) { System.out.println("Error updating ghost avatar"); }
            }
            // receive “create…”
            // format: create, clientid, x, y, z, u, v, n
            if (msgTokens[0].compareTo("create") == 0)
            {
                UUID ghostID = UUID.fromString(msgTokens[1]);
                Vector3 ghostPosition = Vector3f.createFrom(
                        Float.parseFloat(msgTokens[2]),
                        Float.parseFloat(msgTokens[3]),
                        Float.parseFloat(msgTokens[4]));
                Vector3 ghostHeading = Vector3f.createFrom(
                        Float.parseFloat(msgTokens[5]),
                        Float.parseFloat(msgTokens[6]),
                        Float.parseFloat(msgTokens[7]));
                try { createGhostAvatar(ghostID, ghostPosition, ghostHeading); }
                catch (IOException e) { System.out.println("Error creating ghost avatar"); }
            }
            // rec. “wants…”
            // format: wsds, clientid
            if (msgTokens[0].compareTo("wsds") == 0)
            {
                UUID clientId = UUID.fromString(msgTokens[1]);
                sendDetailsForMessage(clientId, game.getPlayerPosition(), game.getPlayerHeading());
            }
            // rec. “move...”
            // formate: move, clientid, x, y, z
            if (msgTokens[0].compareTo("move") == 0)
            { sendMoveMessage(game.getPlayerPosition(), game.getPlayerHeading()); }
        }
    }

    private void createGhostAvatar(UUID ghostID, Vector3 ghostPosition, Vector3 ghostHeading) throws IOException {
    }

    private void updateGhostAvatar(UUID ghostID, Vector3 ghostPosition, Vector3 ghostHeading) throws IOException {
    }

    private void removeGhostAvatar(UUID ghostID) throws IOException {
    }


    //Also need functions to instantiate ghost avatar, remove a ghost avatar,
    //look up a ghost in the ghost table, update a ghost’s position, and
    //accessors as needed.
    // send join
    // format: join, localId
    public void sendJoinMessage()
    {
        try {
            sendPacket("join," + id.toString());
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    // send create
    // format: create, localId, x, y, z
    public void sendCreateMessage(Vector3 pos, Vector3 head) {
        try {
            String message =    "create," +
                                id.toString() + ","  +
                                pos.x() + ","  +
                                pos.y() + ","  +
                                pos.z() + "," +
                                head.x() + "," +
                                head.y() + "," +
                                head.z();
            sendPacket(message);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    // send bye
    // format: bye, localId
    public void sendByeMessage() {
        try {
            String message = "bye," +
                    id.toString();
            sendPacket(message);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendDetailsForMessage(UUID remId, Vector3 pos, Vector3 head) {
        try {
            String message =    "dsfr," +
                                id.toString() + ","  +
                                remId.toString() + "," +
                                pos.x() + ","  +
                                pos.y() + ","  +
                                pos.z() + "," +
                                head.x() + "," +
                                head.y() + "," +
                                head.z();
            sendPacket(message);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendMoveMessage(Vector3 pos, Vector3 head) {
        try {
            String message =    "move," +
                                id.toString() + ","  +
                                pos.x() + ","  +
                                pos.y() + ","  +
                                pos.z() + "," +
                                head.x() + "," +
                                head.y() + "," +
                                head.z();
            sendPacket(message);
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}