package myGameEngine.Networking;

import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

public class GameServerUDP extends GameConnectionServer<UUID> {
    public GameServerUDP(int localPort) throws IOException {
        super(localPort, ProtocolType.UDP);
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int senderPort) {
        String message = (String) o;
        String[] msgTokens = message.split(",");
        if (msgTokens.length > 0) {
            // case where server receives a JOIN message
            // format: join,localid
            if (msgTokens[0].compareTo("join") == 0) {
                try {
                    IClientInfo ci;
                    ci = getServerSocket().createClientInfo(senderIP, senderPort);

                    UUID clientID = UUID.fromString(msgTokens[1]);

                    System.out.println("Client connected, UUID: " + clientID);
                    addClient(ci, clientID);
                    sendJoinedMessage(clientID, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // case where server receives a CREATE message
            // format: create,localid,x,y,z
            if (msgTokens[0].compareTo("create") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
                String[] head = { msgTokens[5], msgTokens[6], msgTokens[7] };
                sendCreateMessages(clientID, pos, head);
                sendWantsDetailsMessages(clientID);
            }
            // case where server receives a BYE message
            // format: bye, localid
            if (msgTokens[0].compareTo("bye") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                sendByeMessages(clientID);
                removeClient(clientID);
            }
            // case where server receives a DETAILS-FOR message
            // format: dsfr, localId, remoteId, x, y, z
            if (msgTokens[0].compareTo("dsfr") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                UUID remoteID = UUID.fromString(msgTokens[2]);
                String[] pos = { msgTokens[3], msgTokens[4], msgTokens[5] };
                String[] head = { msgTokens[5], msgTokens[6], msgTokens[7] };
                sendDetailsMessage(clientID, remoteID, pos, head);
            }
            // case where server receives a MOVE message
            // format: move, localId, x, y, z, u, v, n
            if (msgTokens[0].compareTo("move") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                String[] head = { msgTokens[5], msgTokens[6], msgTokens[7] };
                sendMoveMessages(clientID, pos, head);
            }
        }
    }

    public void sendJoinedMessage(UUID clientID, boolean success) { // format: join, success or join, failure
        try {
            String message =    "join" +
                                ((success) ? "success" : "failure");
            sendPacket(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // format: create, remoteId, x, y, z, u, v, n
    public void sendCreateMessages(UUID clientID, String[] position, String[] head) {
        try {
            String message =    "create,"  +
                                clientID.toString() + "," +
                                position[0] + "," +
                                position[1] + "," +
                                position[2] + "," +
                                head[0] + "," +
                                head[1] + "," +
                                head[2];
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendDetailsMessage(UUID clientID, UUID remoteId, String[] position, String[] head) {
        try {
            String message =    "dsfr," +
                                clientID.toString() + "," +
                                remoteId.toString() + "," +
                                position[0] + "," +
                                position[1] + "," +
                                position[2] +
                                head[0] + "," +
                                head[1] + "," +
                                head[2];
            sendPacket(message, clientID);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendWantsDetailsMessages(UUID clientID) {
        try {
            String message =    "wsds," +
                                clientID.toString();
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendMoveMessages(UUID clientID, String[] position, String[] head) {
        try {
            String message =    "move," +
                                clientID.toString() + "," +
                                position[0] + "," +
                                position[1] + "," +
                                position[2] + "," +
                                head[0] + "," +
                                head[1] + "," +
                                head[2];
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendByeMessages(UUID clientID) {
        try {
            String message =    "bye," +
                                clientID.toString();
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}