package myGameEngine.Managers;

import myGameEngine.Networking.ProtocolClient;
import ray.rage.scene.Node;
import ray.rage.scene.SkeletalEntity;
import ray.rml.Vector3;

import java.util.Hashtable;

public class AstronautAnimator {
    
    private SkeletalEntity skeleton;
    private Movement2DManager mm;
    private ProtocolClient protoClient;
    private String onMove;
    private final float speedMove;
    private String onIdle;
    private final float speedIdle;
    
    private String currentAnimation = "";
    
    public AstronautAnimator(SkeletalEntity skeleton, Movement2DManager mm, ProtocolClient protoClient, String onMove, float speedMove, String onIdle, float speedIdle) {
        this.skeleton = skeleton;
        this.mm = mm;
        this.protoClient = protoClient;
        this.onMove = onMove;
        this.speedMove = speedMove;
        this.onIdle = onIdle;
        this.speedIdle = speedIdle;
        playIdleAnimation();
    }
    
    public AstronautAnimator(SkeletalEntity skeleton, ProtocolClient protoClient, String onMove, float speedMove, String onIdle, float speedIdle){
        this(skeleton, null, protoClient, onMove, speedMove, onIdle, speedIdle);
    }
    
    public AstronautAnimator(SkeletalEntity skeleton, String onMove, float speedMove, String onIdle, float speedIdle){
        this(skeleton, null, null, onMove, speedMove, onIdle, speedIdle);
    }
    
    public void updateAnimationState(float elapsTime){
        if (mm != null) {
            boolean beingMoved = false;
            Hashtable<Node, Vector3> nodeDistanceMovedTable = mm.getNodeDistanceMovedTable();
            Vector3 distance;
            if ((distance = nodeDistanceMovedTable.get(skeleton.getParentNode())) != null) {
                //Checking near-zero threshold
                if(distance.length() > 0.005f/elapsTime) {
                    beingMoved = true;
                }
            }
            if (!beingMoved) {
                playIdleAnimation();
            } else {
                playMoveAnimation();
            }
        } else
            System.err.println("AstronautAnimator - No MovementManager has been attached to node '" + skeleton.getParentNode().getName() + "'");
    }
    
    public void attachMovementManager(Movement2DManager mm){
        this.mm = mm;
    }
    
    /**
     * Plays the animation on astronaut if it isn't already running
     */
    public void playMoveAnimation(){
        if (!currentAnimation.equals(onMove)) {
            currentAnimation = onMove;
            skeleton.stopAnimation();
            skeleton.playAnimation(onMove, speedMove, SkeletalEntity.EndType.LOOP, 0);
            if(protoClient != null){
                protoClient.sendAnimMessage(onMove);
            }
        }
    }
    
    /**
     * Plays the animation on astronaut if it isn't already running
     */
    public void playIdleAnimation(){
        if (!currentAnimation.equals(onIdle)) {
            currentAnimation = onIdle;
            skeleton.stopAnimation();
            skeleton.playAnimation(onIdle, speedIdle, SkeletalEntity.EndType.LOOP, 0);
            if(protoClient != null){
                protoClient.sendAnimMessage(onIdle);
            }
        }
    }
    
    public void setClient(ProtocolClient protClient) {
        this.protoClient = protClient;
    }
}
