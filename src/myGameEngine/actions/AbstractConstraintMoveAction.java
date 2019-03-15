package myGameEngine.actions;

import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rml.Vector3;

public abstract class AbstractConstraintMoveAction extends AbstractInputAction {
    //Sphere Constraint (within)
    float sphereRadiusConstraint = 15f;
    //Box Constraint (within)
    float xUpperConstraint = 20f;
    float xLowerConstraint = -20f;
    float yUpperConstraint = 15f;
    float yLowerConstraint = 0f;
    float zUpperConstraint = 20f;
    float zLowerConstraint = -20f;
    
    //Checking if a node is within constraints
    protected boolean isLegal(Node node){
        Vector3 localPosition = node.getLocalPosition();
        //checking sphere
        float length = localPosition.length();
        if(length > sphereRadiusConstraint){
            System.out.println("Case sphere: " + length);
            return false;
        }
        //checking box
        if(xLowerConstraint > localPosition.x() || localPosition.x() > xUpperConstraint){
            System.out.println("Case 1: " + localPosition.x());
            return false;
        } else if(yLowerConstraint > localPosition.y() || localPosition.y() > yUpperConstraint){
            System.out.println("Case 2: " + localPosition.y());
            return false;
        } else if(zLowerConstraint > localPosition.z() || localPosition.z() > zUpperConstraint){
            System.out.println("Case 3: " + localPosition.z());
            return false;
        }
        return true;
    }
}
