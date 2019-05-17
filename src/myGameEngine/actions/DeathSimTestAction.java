package myGameEngine.actions;

import a3.MyGame;
import myGameEngine.Networking.GhostAvatar;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rml.Vector3f;

import java.io.IOException;
import java.util.UUID;

public class DeathSimTestAction extends AbstractInputAction {
    
    private MyGame myGame;
    private boolean pressed = false;
    private boolean removed = false;
    private GhostAvatar ufo;
    
    public DeathSimTestAction(MyGame myGame){
        this.myGame = myGame;
    }
    
    @Override
    public void performAction(float v, Event event) {
        if(!pressed){
            try {
                ufo = myGame.createGhostAvatar(UUID.randomUUID(), "ufo", Vector3f.createFrom(0f, 1f, 8f), Vector3f.createFrom(1f, 0f, 0f));
            } catch (IOException e) {
                e.printStackTrace();
            }
            pressed = true;
        } else if (!removed){
            myGame.removeGhostAvatar(ufo);
            removed = true;
        }
    }
}
