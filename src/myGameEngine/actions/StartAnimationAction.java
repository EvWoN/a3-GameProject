package myGameEngine.actions;

import net.java.games.input.Event;
import ray.rage.scene.SkeletalEntity;

public class StartAnimationAction extends ConstraintTransformAction {

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
