package myGameEngine.actions;

import ray.rage.scene.Node;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public abstract class MoveAction2D extends ConstraintTransformAction{
    protected void move(Node nodeToMove, float referenceAzimuth, float offsetAzimuth, float moveValue){
        Vector3 originalPosition = nodeToMove.getLocalPosition();
        double theta = Math.toRadians(referenceAzimuth+offsetAzimuth);
        double x = moveValue * Math.sin(theta);
        double y = 0;
        double z = moveValue * Math.cos(theta);
    
        Vector3 newPosition = Vector3f.createFrom((float) x, (float) y, (float) z).add(nodeToMove.getWorldPosition());
        nodeToMove.lookAt(newPosition);
        nodeToMove.setLocalPosition(newPosition);
    
        //Check boundaries
        if(!isLegal(nodeToMove)){
            nodeToMove.setLocalPosition(originalPosition.x(), originalPosition.y(), originalPosition.z());
        }
    }
}
