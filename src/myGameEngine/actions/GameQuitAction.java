package myGameEngine.actions;

import a3.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class GameQuitAction extends AbstractInputAction {
    
    MyGame myGame;
    
    public GameQuitAction(MyGame myGame){
        this.myGame = myGame;
    }
    
    @Override
    public void performAction(float v, Event event) {
        myGame.exit();
    }
}
