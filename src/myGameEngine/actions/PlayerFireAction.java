package myGameEngine.actions;

import myGameEngine.Managers.PlayerFiringManager;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class PlayerFireAction extends AbstractInputAction {

    private PlayerFiringManager firingManager;

    public PlayerFireAction(PlayerFiringManager firingManager){
        this.firingManager = firingManager;
    }
    
    @Override
    public void performAction(float v, Event event) {
        System.out.println(firingManager.playerFire());
    }
}
