package myGameEngine.controller;

import ray.rage.scene.Node;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class SquishyBounceController extends AbstractController {
    private float bounceTravel = .005f;//bounce speed
    private float cycleTime = 1000.0f;// default cycle time
    private float totalTime = 0.0f;
    private float direction = 1.0f;
    
    @Override
    protected void updateImpl(float elapsedTimeMillis) {
        totalTime += elapsedTimeMillis;
        float bounceAmt = direction* bounceTravel;
        if (totalTime > cycleTime) {
            direction = -direction;
            totalTime = 0.0f;
        }
        for (Node n : super.controlledNodesList) {
            Vector3 localPosition = n.getLocalPosition();
            n.setLocalPosition(localPosition.x(),localPosition.y() + bounceAmt, localPosition.z());
        }
    }
}