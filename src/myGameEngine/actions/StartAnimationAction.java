package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.ProtocolClient;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;
import ray.rml.Vector3;

public class StartAnimationAction extends AbstractConstraintMoveAction {

    private SkeletalEntity skeleton;
    private String name;
    private float speed;
    private SkeletalEntity.EndType endType;
    private int i;
    private boolean looping;

    public StartAnimationAction (SkeletalEntity skeleton,
                                 String name,
                                 float speed,
                                 SkeletalEntity.EndType endType,
                                 int i) {
        this.skeleton = skeleton;
        this.name = name;
        this.speed = speed;
        this.endType = endType;
        this.i = i;
    }

    @Override
    public void performAction(float v, Event event) {
        System.out.println("Animation: " + event.toString());
        if(!looping)
        {
            skeleton.playAnimation(name, speed, endType, i);
            looping = true;
        }
    }
}
