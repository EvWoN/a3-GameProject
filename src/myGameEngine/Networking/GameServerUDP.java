package myGameEngine.Networking;

import myGameEngine.Enemy;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class GameServerUDP extends GameConnectionServer<UUID> {

    private HashMap<UUID, Vector3> lastKnownPositions;
    private HashMap<UUID, Enemy> enemyList;

    private Random rand;
    private ScriptEngine se;

    private long totalTime, elapsedTime, lastTime, timeSec, lastSec;

    private int SPAWNRATE, UPDATESCRIPT, //Server
                AMMO; //Enemy

    private float ORBIT, SPEED, START; //Enemy

    private boolean addedEnemy, updatedScripts;

    public GameServerUDP(int localPort) throws IOException {
        super(localPort, ProtocolType.UDP);
        lastKnownPositions = new HashMap<>();
        rand = new Random();
        lastTime = System.nanoTime();
        totalTime = 0;
        lastSec = -1;
        enemyList = new HashMap<>();
        addedEnemy = updatedScripts = false;
        runScript();
        setConstants();
        System.out.println("Local IP: " + InetAddress.getLocalHost() + " Port: " +localPort);
        while(true) {
            this.update();
        }
    }

    private void runScript() {
        ScriptEngineManager factory = new ScriptEngineManager();
        String scriptName = "scripts/Parameters.js";

        se = factory.getEngineByName("js");

        try {
            FileReader fr = new FileReader(scriptName);
            se.eval(fr);
            fr.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void setConstants() {
        SPAWNRATE = (Integer) se.get("spawnrate");
        UPDATESCRIPT = (Integer) se.get("updatescript");
        AMMO = (Integer) se.get("ammo");
        ORBIT = ((Double) se.get("orbit")).floatValue();
        SPEED = ((Double) se.get("speed")).floatValue();
        START = ((Double) se.get("start")).floatValue();
    }

    public static void main(String[] args) {
        try {
            GameServerUDP server = new GameServerUDP(Integer.getInteger(args[0]));
            while(true) {
                server.update();
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void update() {
        ArrayList<String> moveMessages = new ArrayList<>();
        elapsedTime = System.nanoTime() - lastTime;
        if(elapsedTime == 0) return;
        totalTime += elapsedTime;
        timeSec = totalTime / 1000000000;
        lastTime = System.nanoTime();
        if(lastKnownPositions.size() < 1) return;
        if (timeSec % UPDATESCRIPT == 0)
        {
            if(!updatedScripts) {
                updatedScripts = true;
                System.out.println("Updated script.");
                runScript();
            }
        }
        else updatedScripts = false;
        if (timeSec % SPAWNRATE == 0) {
            if(!addedEnemy && enemyList.size() < 1) {
                System.out.println("Spawning an enemy.");
                addedEnemy = true;
                UUID uuid = UUID.randomUUID();
                Enemy enemy = new Enemy(
                        uuid,
                        rand.nextInt(360) + rand.nextFloat(),
                        AMMO,
                        ORBIT,
                        SPEED,
                        START
                );
                enemyList.put(
                        uuid,
                        enemy
                );
                sendEnemy(enemy);
            }
        }
        else addedEnemy = false;
        enemyList.forEach((uuid, enemy) -> {
            UUID targ = closestTarget(enemy);
            if(targ != null) {
                enemy.updateDestination(lastKnownPositions.get(targ));
                enemy.move(elapsedTime);
                //System.out.println("Enemy Loc: " + enemy.getLocation());
                String message = createEnemyMoveMessage(enemy);
                //System.out.println("Cons Message:" + message);
                moveMessages.add(message);
            }
        });
        if(moveMessages.size() > 0)
        {
            //System.out.println("\n" + moveMessages);
            sendEnemyMoveMessage(moveMessages);
        }
    }

    private UUID closestTarget(Enemy enemy) {
        //if(lastKnownPositions.size() < 0) return null;
        Vector3 enemyPos = enemy.getLocation();
        AtomicReference<UUID> targ = new AtomicReference<>();
        AtomicReference<Float> min = new AtomicReference<>();
        AtomicReference<Float> dist = new AtomicReference<>();
        min.set(Float.MAX_VALUE);
        lastKnownPositions.forEach((uuid, pos) -> {
            dist.set((pos.sub(enemyPos)).length());
            if(dist.get() < min.get()) {
                min.set(dist.get());
                targ.set(uuid);
            }
        });
        return targ.get();
    }

    private String createEnemyMoveMessage(Enemy enemy) {
        Vector3 position = enemy.getLocation();
        Vector3 heading = enemy.getHeading();
        String message =
                "enemymove,"  +
                enemy.getUUID().toString() + "," +
                position.x() + "," +
                position.y() + "," +
                position.z() + "," +
                heading.x() + "," +
                heading.y() + "," +
                heading.z() + ",";
        //System.out.println(message);
        return message;
    }

    private void sendEnemyMoveMessage(ArrayList<String> messages) {
        String message = "";
        for (String msg : messages) { message += msg + ";"; }
        try { sendPacketToAll(message); }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int senderPort) {
        String message = (String) o;
        String[] msgTokens = message.split(",");
        if (msgTokens.length > 0) {
            // case where server receives a JOIN message
            // format: join,localid
            System.out.println("Message Received: " + msgTokens[0] + " from " + msgTokens[1]);
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
            // format: create, clientId, itemId, type, x, y, z, u, v, n
            if (msgTokens[0].compareTo("create") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                UUID itemID = UUID.fromString(msgTokens[2]);
                String type = msgTokens[3];
                String[] pos = {msgTokens[4], msgTokens[5], msgTokens[6]};
                String[] head = { msgTokens[7], msgTokens[8], msgTokens[9] };
                if(!lastKnownPositions.containsKey(clientID))
                    lastKnownPositions.put(
                        clientID,
                        Vector3f.createFrom(
                            Float.parseFloat(msgTokens[4]),
                            Float.parseFloat(msgTokens[5]),
                            Float.parseFloat(msgTokens[6])
                        )
                    );
                sendCreateMessages(clientID, itemID, type, pos, head);
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
                String type = msgTokens[3];
                String[] pos = { msgTokens[4], msgTokens[5], msgTokens[6] };
                String[] head = { msgTokens[7], msgTokens[8], msgTokens[9] };
                sendDetailsMessage(clientID, remoteID, type, pos, head);
            }
            // case where server receives a MOVE message
            // format: move, localId, x, y, z, u, v, n
            if (msgTokens[0].compareTo("move") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                String[] head = { msgTokens[5], msgTokens[6], msgTokens[7] };
                lastKnownPositions.put(clientID,
                        Vector3f.createFrom(
                                Float.parseFloat(msgTokens[2]),
                                Float.parseFloat(msgTokens[3]),
                                Float.parseFloat(msgTokens[4])
                        ));
                sendMoveMessages(clientID, pos, head);
            }
            if (msgTokens[0].compareTo("moveItem") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                UUID itemID = UUID.fromString(msgTokens[2]);
                String[] pos = { msgTokens[3], msgTokens[4], msgTokens[5] };
                String[] head = { msgTokens[6], msgTokens[7], msgTokens[8] };
                System.out.println("itemId: " + itemID + "\ntoString: " + itemID.toString());
                sendMoveItemMessages(clientID, itemID, pos, head);
            }
            if(msgTokens[0].compareTo("anim") == 0){
                UUID clientID = UUID.fromString(msgTokens[1]);
                String animationState = msgTokens[2];
                sendAnimMessage(clientID,animationState);
            }
        }
    }
    
    private void sendAnimMessage(UUID clientID, String animationState) {
        try {
            String message =    "anim," +
                    clientID.toString() + "," +
                    animationState;
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendJoinedMessage(UUID clientID, boolean success) { // format: join, success or join, failure
        try {
            String message =    "join," +
                                ((success) ? "success" : "failure");
            sendPacket(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // format: create, remoteId, x, y, z, u, v, n
    public void sendCreateMessages(UUID clientID, UUID itemID, String type, String[] position, String[] head) {
        try {
            String message =    "create,"  +
                                itemID.toString() + "," +
                                type + "," +
                                position[0] + "," +
                                position[1] + "," +
                                position[2] + "," +
                                head[0] + "," +
                                head[1] + "," +
                                head[2] + ",";
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) { e.printStackTrace(); }
    }
    
    
    //Debug
    @Override
    protected void forwardPacketToAll(Serializable object, UUID originalClientUID) throws IOException {
        System.out.println("Forwarding to all: " + object);
        super.forwardPacketToAll(object, originalClientUID);
    }
    
    /**
     * Send details for clientID to remoteID
     * @param clientID
     * @param remoteId
     * @param position
     * @param head
     */
    public void sendDetailsMessage(UUID clientID, UUID remoteId, String type, String[] position, String[] head) {
        try {
            String message =    "dsfr," +
                                clientID.toString() + "," +
                                remoteId.toString() + "," +
                                type + "," +
                                position[0] + "," +
                                position[1] + "," +
                                position[2] + "," +
                                head[0] + "," +
                                head[1] + "," +
                                head[2];
            sendPacket(message, remoteId);
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

    public void sendMoveItemMessages(UUID clientID, UUID itemID, String[] position, String[] head) {
        try {
            String message =
                    "move," +
                    itemID.toString() + "," +
                    position[0] + "," +
                    position[1] + "," +
                    position[2] + "," +
                    head[0] + "," +
                    head[1] + "," +
                    head[2];
            System.out.println(message);
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

    public void sendEnemy(Enemy enemy) {
        UUID itemID = enemy.getUUID();
        String type = "ufo";
        Vector3 position = enemy.getLocation();
        Vector3 head = enemy.getHeading();
        try {
            String message =    "create,"  +
                    itemID.toString() + "," +
                    type + "," +
                    position.x() + "," +
                    position.y() + "," +
                    position.z() + "," +
                    head.x() + "," +
                    head.y() + "," +
                    head.z() + ",";
            super.sendPacketToAll(message);
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}