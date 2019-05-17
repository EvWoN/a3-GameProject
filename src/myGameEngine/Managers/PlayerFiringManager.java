package myGameEngine.Managers;

import myGameEngine.Networking.ProtocolClient;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class PlayerFiringManager {
    private SceneManager sm;
    private SceneNode playerNode;
    private float firingRate = 1000f;
    private float lazorVelocity = 3f;
    private float timeSinceLastShot = 0f;
    private boolean fired = false;
    private ProtocolClient protocolClient;

    LinkedList<LazorInstances> lazors = new LinkedList<>();

    public PlayerFiringManager(SceneNode playerNode, SceneManager sm, ProtocolClient protocolClient){
        this.playerNode = playerNode;
        this.sm = sm;
        this.protocolClient = protocolClient;
    }

    public boolean playerFire(){
        if(!fired){
            fired = true;
            lazors.add(new LazorInstances(playerNode));
            return true;
        }
        return false;
    }

    public void update(float timeSinceLast){
        if(fired){
            if(timeSinceLastShot > firingRate){
                fired = false;
                timeSinceLastShot = 0f;
            } else timeSinceLastShot+=timeSinceLast;
        }
        List<LazorInstances> toRemove = new LinkedList<>();
        for (LazorInstances lazor : lazors) {
            if (!lazor.isAlive()){
                toRemove.add(lazor);
            } else {
                lazor.update(timeSinceLast);
            }
        }
        for (LazorInstances lazorInstances : toRemove) {
            lazors.remove(lazors);
        }
    }

    private class LazorInstances{
        private SceneNode lazorNode;
        private float timeAlive = 0;
        private float timeDeath = 4000f;
        private UUID uuid = UUID.randomUUID();

        public LazorInstances(SceneNode playerNode){
            Vector3 worldPosition = playerNode.getWorldPosition();
            Vector3 worldForwardAxis = playerNode.getWorldForwardAxis();
            Vector3 newPosition = worldPosition.add(worldForwardAxis.div(3f));
            lazorNode = sm.getRootSceneNode().createChildSceneNode(uuid.toString()+"Lazor");
            try {
                SceneNode childSceneNode = lazorNode.createChildSceneNode(lazorNode.getName() + "Obj");
                Entity entity = sm.createEntity(uuid.toString(), "lazor.obj");
                childSceneNode.attachObject(entity);
                childSceneNode.moveUp(.5f);
                childSceneNode.scale(3f,3f,3f);
            } catch (IOException e){
                e.printStackTrace();
            }
            lazorNode.setLocalPosition(worldPosition);
            lazorNode.lookAt(newPosition);
            lazorNode.setLocalPosition(newPosition);
            protocolClient.sendCreateClientNode(uuid,"lazor",lazorNode.getWorldPosition(),lazorNode.getWorldForwardAxis());
        }

        public boolean isAlive(){
            return (timeAlive < timeDeath) && (playerNode.isInSceneGraph());

        }

        public void update(float timeSinceLast){
            timeAlive += timeSinceLast;
            if(timeAlive > timeDeath){
                destroy();
            } else {
                lazorNode.setLocalPosition(lazorNode.getWorldPosition().add(lazorNode.getWorldForwardAxis().div(lazorVelocity)));
                protocolClient.sendMoveClientNode(uuid,lazorNode.getWorldPosition(),lazorNode.getWorldForwardAxis());
            }
        }

        public void destroy(){
            if(lazorNode.isInSceneGraph()) {
                sm.destroySceneNode(lazorNode);
                protocolClient.sendDestroyClientNode(uuid);
            }
        }
    }

}
