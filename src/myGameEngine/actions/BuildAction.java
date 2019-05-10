package myGameEngine.actions;

import javafx.beans.property.BooleanProperty;
import myGameEngine.Managers.Builder;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.util.Iterator;

public class BuildAction extends AbstractInputAction {
    
    private SceneNode whoBuilds;
    private float buttonPushedTimer = 0;
    private float holdDownTimer;
    private BooleanProperty holdingItem;
    private Builder builder;
    
    /**
     * InputAction which adds a build point to the Builder on key hold
     * @param whoBuilds
     * @param holdingItems boolean if item is being held
     * @param builder the builder to whom a build point is awarded on key press
     * @param holdDownTimer time in milliseconds until input press awards a build point
     */
    public BuildAction(SceneNode whoBuilds, BooleanProperty holdingItems, Builder builder, float holdDownTimer){
        this.holdingItem = holdingItems;
        this.whoBuilds = whoBuilds;
        this.holdDownTimer = holdDownTimer;
        this.builder = builder;
    }
    
    @Override
    public void performAction(float v, Event event) {
        if(isClose()) {
            if (holdingItem.getValue()) {
                buttonPushedTimer += v;
                if (buttonPushedTimer >= holdDownTimer) {
                    holdRelease();
                    removeItem();
                    builder.setBuiltPoints(builder.getBuiltPoints() + 1);
                }
            }
        } else {
            holdRelease();
        }
    }
    
    private boolean isClose(){
        Vector3 sub = whoBuilds.getWorldPosition().sub(builder.getToBuild().getWorldPosition());
        return (sub.length() < 1f);
    }
    
    private void removeItem(){
        Node part = null;
        Iterator<Node> iterator = whoBuilds.getChildNodes().iterator();
        while (iterator.hasNext()){
            part = iterator.next();
            if(part.getName().startsWith("Part")){
                break;
            }
        }
        if (part != null) {
            whoBuilds.detachChild(part);
            holdingItem.setValue(false);
        }
    }
    
    public void holdRelease(){
        buttonPushedTimer = 0;
    }
}
