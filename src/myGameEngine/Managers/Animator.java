package myGameEngine.Managers;

import ray.rage.scene.Node;
import ray.rage.scene.SkeletalEntity;
import ray.rml.Vector3;

import java.util.Hashtable;

public class Animator {
    
    private SkeletalEntity skeleton;
    private Movement2DManager mm;
    private String onMove;
    private final float speedMove;
    private String onIdle;
    private final float speedIdle;
    
    private String currentAnimation;
    
    public Animator(SkeletalEntity skeleton, Movement2DManager mm, String onMove, float speedMove, String onIdle, float speedIdle) {
        this.skeleton = skeleton;
        this.mm = mm;
        this.onMove = onMove;
        this.speedMove = speedMove;
        this.onIdle = onIdle;
        this.speedIdle = speedIdle;
        playIdleAnimation();
    }
    
    public Animator(SkeletalEntity skeleton, String onMove, float speedMove, String onIdle, float speedIdle){
        this(skeleton, null,onMove, speedMove, onIdle, speedIdle);
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
            if (currentAnimation.equals(onMove) && !beingMoved) {
                playIdleAnimation();
            } else if (currentAnimation.equals(onIdle) && beingMoved) {
                playMoveAnimation();
            }
        } else
            System.err.println("Animator - No MovementManager has been attached to node '" + skeleton.getParentNode().getName() + "'");
    }
    
    public void attachMovementManager(Movement2DManager mm){
        this.mm = mm;
    }
    
    public void playMove(){
        if (currentAnimation.equals(onIdle)) {
            playMoveAnimation();
        }
    }
    
    public void playIdle(){
        if (currentAnimation.equals(onMove)) {
            playIdleAnimation();
        }
    }
    
    private void playMoveAnimation(){
        currentAnimation = onMove;
        skeleton.stopAnimation();
        skeleton.playAnimation(onMove,speedMove, SkeletalEntity.EndType.LOOP,0);
    }
    
    private void playIdleAnimation(){
        currentAnimation = onIdle;
        skeleton.stopAnimation();
        skeleton.playAnimation(onIdle,speedIdle, SkeletalEntity.EndType.LOOP,0);
    }
}
