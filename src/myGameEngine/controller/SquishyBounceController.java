package myGameEngine.controller;

import ray.rage.scene.Node;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class SquishyBounceController extends AbstractController {
    private float scaleRate = .003f;//growth per second
    private float bounceTravel = .004f;//bounce speed
    private float cycleTime = 500.0f;// default cycle time
    private float totalTime = 0.0f;
    private float direction = 1.0f;
    
    @Override
    protected void updateImpl(float elapsedTimeMillis) {
        totalTime += elapsedTimeMillis;
        float scaleAmt = 1.0f + direction * scaleRate;
        float bounceAmt = direction* bounceTravel;
        if (totalTime > cycleTime) {
            direction = -direction;
            totalTime = 0.0f;
        }
        for (Node n : super.controlledNodesList) {
            Vector3 curScale = n.getLocalScale();
            curScale = Vector3f.createFrom(curScale.x(), curScale.y()*scaleAmt, curScale.z());
            n.setLocalScale(curScale);
            
            Vector3 localPosition = n.getLocalPosition();
            n.setLocalPosition(localPosition.x(),localPosition.y() + bounceAmt, localPosition.z());
        }
    }
}