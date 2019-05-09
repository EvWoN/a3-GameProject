package myGameEngine.actions;

import a3.MyGame;
import javafx.beans.property.BooleanProperty;
import myGameEngine.Managers.Builder;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class BuildAction extends AbstractInputAction {
    
    private MyGame myGame;
    private float buttonPushedTimer = 0;
    private float holdDownTimer;
    private BooleanProperty holdingItem;
    private Builder builder;
    
    /**
     * InputAction which adds a build point to the Builder on key hold
     * @param myGame
     * @param holdingItems boolean if item is being held
     * @param builder the builder to whom a build point is awarded on key press
     * @param holdDownTimer time in milliseconds until input press awards a build point
     */
    public BuildAction(MyGame myGame, BooleanProperty holdingItems, Builder builder, float holdDownTimer){
        this.holdingItem = holdingItems;
        this.myGame = myGame;
        this.holdDownTimer = holdDownTimer;
        this.builder = builder;
    }
    
    @Override
    public void performAction(float v, Event event) {
        
        if(holdingItem.getValue()){
            buttonPushedTimer += v;
            if(buttonPushedTimer>=holdDownTimer){
                holdRelease();
                builder.setBuiltPoints(builder.getBuiltPoints()+1);
            }
        }
    }
    
    public void holdRelease(){
        buttonPushedTimer = 0;
    }
}
