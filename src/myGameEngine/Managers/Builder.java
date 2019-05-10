package myGameEngine.Managers;

import ray.rage.scene.Light;
import ray.rage.scene.Node;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;

import java.awt.*;

public class Builder {

    SceneManager sm;

    private Node toBuild;
    private Light light;
    private int pointsToBuild = 1;
    private int builtPoints;
    float minRange = .2f;
    float maxRange = 5f;

    public Builder(SceneNode toBuild, SceneManager sm){
        this.toBuild = toBuild;
        this.sm = sm;

        toBuild.getWorldPosition();
        Light light = sm.createLight(toBuild.getName() + "BuildLight", Light.Type.POINT);
        this.light = light;
        setBuiltPoints(0);
        toBuild.attachObject(light);
    }
    
    private Color getLightColor(){
        System.out.println("Built: "+builtPoints+"\nToBuild:"+pointsToBuild+"\nLight color: " + ((double)builtPoints)/pointsToBuild);
       return new Color(1f,1f,(float) (1f-(((double)builtPoints)/pointsToBuild)));
    }
    
    public void setPointsToBuild(int pointsToBuild){
        if(pointsToBuild >= 1) {
            if(builtPoints>pointsToBuild){
                setBuiltPoints(pointsToBuild);
            }
            this.pointsToBuild = pointsToBuild;
            light.setDiffuse(getLightColor());
        }
    }
    
    public Node getToBuild() {
        return toBuild;
    }
    
    public int getPointsToBuild(){
        return pointsToBuild;
    }

    public void setBuiltPoints(int builtPoints){
        if(builtPoints>pointsToBuild){
            builtPoints = pointsToBuild;
        }
        if(builtPoints >= 0){
            this.builtPoints = builtPoints;
            Color lightColor = getLightColor();
            System.out.println(light +" Color:" + lightColor + " Points:" + builtPoints);
            light.setDiffuse(lightColor);
            light.setRange((float) (minRange+((maxRange-minRange)*((double)builtPoints)/pointsToBuild)));
        }
    }
    
    public int getBuiltPoints(){
        return builtPoints;
    }

    public boolean buildCompleted(){
        return builtPoints >= pointsToBuild;
    }
}
