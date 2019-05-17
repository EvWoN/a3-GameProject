package myGameEngine.Managers;

import myGameEngine.Tools.ArrayHelper;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsObject;
import ray.rage.scene.*;
import ray.rml.Matrix3;
import ray.rml.Matrix4;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.function.Consumer;

public class UFODeathSimManager {
    private PhysicsEngine pe;
    private SceneManager sm;
    private float timeTillDeath;
    private LinkedList<DeathSimInstance> instances = new LinkedList<>();
    private float  heightTreshold = -9f; //Ground floor threshold
    
    /**
     *
     * @param pe physics engine
     * @param sm scene manager
     * @param timeTillDeath time until death in millisec
     */
    public UFODeathSimManager(PhysicsEngine pe, SceneManager sm, float timeTillDeath) {
        this.pe = pe;
        this.sm = sm;
        this.timeTillDeath = timeTillDeath;
    }
    
    public void performDeathSim(Node node) {
        String name = node.getName();
        SceneNode simNode = sm.getRootSceneNode().createChildSceneNode(node.getName() + "SimMainNode");
        
        try {
            Entity shipBody = sm.createEntity(name + "shipBody", "shipBody.obj");
            SceneNode shipBodyNode = simNode.createChildSceneNode(shipBody.getName() + "Node");
            shipBodyNode.attachObject(shipBody);
            Entity shipCannon = sm.createEntity(name + "shipCannon", "shipCannon.obj");
            SceneNode shipCannonNode = simNode.createChildSceneNode(shipCannon.getName() + "Node");
            shipCannonNode.attachObject(shipCannon);
            Entity shipCockpit = sm.createEntity(name + "shipCockpit", "shipCockpit.obj");
            SceneNode shipCockpitNode = simNode.createChildSceneNode(shipCockpit.getName() + "Node");
            shipCockpitNode.attachObject(shipCockpit);
            Entity leftThruster = sm.createEntity(name + "leftThruster", "leftThruster.obj");
            SceneNode leftThrusterNode = simNode.createChildSceneNode(leftThruster.getName() + "Node");
            leftThrusterNode.attachObject(leftThruster);
            Entity rightThruster = sm.createEntity(name + "rightThruster", "rightThruster.obj");
            SceneNode rightThrusterNode = simNode.createChildSceneNode(rightThruster.getName() + "Node");
            rightThrusterNode.attachObject(rightThruster);
            
            Matrix3 worldRotation = node.getLocalRotation();
            
            Matrix4 localTransform = node.getLocalTransform();
            System.out.println("Transform:\t" + localTransform);
            System.out.println("TRotation:\t" + localTransform.toMatrix3());
            System.out.println("Rotation:\t" + node.getLocalRotation());
            
            
            shipBodyNode.setLocalPosition(node.getWorldPosition());
            shipCannonNode.setLocalPosition(node.getWorldPosition());
            shipCockpitNode.setLocalPosition(node.getWorldPosition());
            leftThrusterNode.setLocalPosition(node.getWorldPosition());
            rightThrusterNode.setLocalPosition(node.getWorldPosition());
            
            shipBodyNode.setLocalRotation(worldRotation);
            shipCannonNode.setLocalRotation(worldRotation);
            shipCockpitNode.setLocalRotation(worldRotation);
            leftThrusterNode.setLocalRotation(worldRotation);
            rightThrusterNode.setLocalRotation(worldRotation);
            
            shipCockpitNode.moveUp(.23f);
            shipCannonNode.moveForward(.25f);
            shipCannonNode.moveUp(.14f);
            leftThrusterNode.moveBackward(.37f);
            leftThrusterNode.moveRight(.12f);
            rightThrusterNode.moveBackward(.37f);
            rightThrusterNode.moveLeft(.12f);
            
            attachPhysicsObject(shipBodyNode);
            attachPhysicsObject(shipCannonNode);
            attachPhysicsObject(shipCockpitNode);
            attachPhysicsObject(leftThrusterNode);
            attachPhysicsObject(rightThrusterNode);
            
            instances.add(new DeathSimInstance(simNode));

        } catch (IOException e) {
            System.err.println("Was unable to create DeathSim for " + name);
            e.printStackTrace();
        }
    }
    
    /**
     * @param elapsedTime time since last frame
     */
    public void updateInstances(float elapsedTime) {
        LinkedList<DeathSimInstance> toRemove = new LinkedList<>();
        
        instances.forEach(deathSimInstance -> {
            if(deathSimInstance.getTimeElapsedTotal() > timeTillDeath) {
                toRemove.add(deathSimInstance);
            } else {
                deathSimInstance.update(elapsedTime);
            }
        });
    
        for (DeathSimInstance deathSimInstance : toRemove) {
            deathSimInstance.removeAllPhysics();
            instances.remove(deathSimInstance);
        }
    }
    
    /**
     * Helper class that attaches a physics object to the given SceneNode
     *
     * @param part node to attach physics object to
     * @return
     */
    private PhysicsObject attachPhysicsObject(Node part) {
        float[] boxDim = {.4f, .4f, .4f};
        float mass = 10.0f;
        PhysicsObject physicsObject = pe.addBoxObject(pe.nextUID(), mass, ArrayHelper.toDoubleArray(part.getWorldTransform().toFloatArray()), boxDim);
        part.setPhysicsObject(physicsObject);
        return physicsObject;
    }
    
    /**
     * Instance that holds elapsed time attribute of deathSim
     */
    private class DeathSimInstance {
        private float timeElapsedTotal = 0;
        private float explosionTime = 400f;
        private Node simNode;
        private Light explosionLight;
        private LinkedList<Node> parts;
    
        public DeathSimInstance(Node simNode) {
            this.simNode = simNode;
    
            parts = new LinkedList();
            for (Node childNode : simNode.getChildNodes()) {
                parts.add(childNode);
            }
            
            explosionLight = sm.createLight(simNode.getName() + "Light", Light.Type.POINT);
            setLightColor();
            
            SceneNode lightNode = sm.createSceneNode(explosionLight.getName() + "N");
            simNode.attachChild(lightNode);
            lightNode.attachObject(explosionLight);
            lightNode.setLocalPosition(parts.getFirst().getWorldPosition());
        }
    
        private void setLightColor() {
            explosionLight.setRange(40f-(30*timeElapsedTotal/explosionTime));
            explosionLight.setAmbient(new Color(.1f-.1f*(timeElapsedTotal/explosionTime), .1f-.1f*(timeElapsedTotal/explosionTime), 0));
            explosionLight.setSpecular(new Color(1f-(timeElapsedTotal/explosionTime), 1f-(timeElapsedTotal/explosionTime), 0));
            explosionLight.setDiffuse(new Color(1f-(timeElapsedTotal/explosionTime), 1f-(timeElapsedTotal/explosionTime), 0));
        }
        
        public void update(float elapsedTime){
            this.timeElapsedTotal += elapsedTime;
            if(timeElapsedTotal<explosionTime) {
                setLightColor();
            } else {
                if(explosionLight.getParentNode() != null) {
                    sm.destroySceneNode(explosionLight.getParentSceneNode());
                }
            }
            LinkedList<Node> toRemove = new LinkedList();
            parts.forEach(new Consumer<Node>() {
                @Override
                public void accept(Node node) {
                    if(node.getWorldPosition().y() < heightTreshold){
                        System.out.println(node.getWorldPosition().y());
                        pe.removeObject(node.getPhysicsObject().getUID());
                        sm.destroySceneNode((SceneNode) node);
                        toRemove.add(node);
                    } else {
                        //Scaling based on time alive and time till death
//                        float v = 1 - timeElapsedTotal / (timeTillDeath);
//                        float v = 1-(1-(((float) Math.exp(-3*((timeElapsedTotal / timeTillDeath))))));
//                        System.out.println(v);
                        float v = 1 - timeElapsedTotal / (timeTillDeath+timeTillDeath*.4f);
                        node.setLocalScale(v,v,v);
                    }
                }
            });
    
            for (Node o : toRemove) {
                parts.remove(o);
            }
        }
        
        public void removeAllPhysics(){
            parts.forEach(new Consumer<Node>() {
                @Override
                public void accept(Node node) {
                    pe.removeObject(node.getPhysicsObject().getUID());
                    sm.destroySceneNode((SceneNode) node);
                }
            });
            sm.destroySceneNode((SceneNode) simNode);
        }
    
        public float getTimeElapsedTotal() {
            return timeElapsedTotal;
        }
    }
    
    public float getTimeTillDeath() {
        return timeTillDeath;
    }
    
    public void setTimeTillDeath(float timeTillDeath) {
        this.timeTillDeath = timeTillDeath;
    }
}
