package myGameEngine.actions;

import javafx.beans.property.SimpleBooleanProperty;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class ToggleMovementAction extends AbstractInputAction {

    private SimpleBooleanProperty followingGround;
    private SceneNode astronaut;

    public ToggleMovementAction (SimpleBooleanProperty followingGround, SceneNode astronaut) {
        this.followingGround = followingGround;
        this.astronaut = astronaut;
    }

    @Override
    public void performAction(float v, Event event) {
        if(followingGround.get()) astronaut.setLocalPosition(0.0f, 0.0f, 0.0f);
        followingGround.set(!followingGround.get());
    }
}
