package myGameEngine;

import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.util.UUID;

public class Enemy {

    private Vector3 location;
    private Vector3 orbitDest;
    private Vector3 targetPos;
    private float angle;
    private UUID target;


    private int ammo;
    private boolean orbiting = false;

    private final float ORBIT;
    private final float SPEED;

    public Enemy(UUID target, float angle, int ammo, float orbit, float speed) {
        this.target = target;
        this.angle = angle;
        this.ammo = ammo;
        this.ORBIT = orbit;
        this.SPEED = speed;
        this.orbitDest = calcPos(angle, ORBIT);
    }

    public void move() {
        Vector3 nextMove;
        float nextAngle;
        if(!orbiting) {
            nextMove = calcPos(angle, location.length() - SPEED);
            if (nextMove.length() > ORBIT) {
                location = nextMove;
            } else {
                location = orbitDest;
                orbiting = true;
            }
        }
        else if(ammo > 0) {
            nextAngle = angle - calcAngle(orbitDest);
            if(nextAngle > 0.0f) { location = calcPos(nextAngle, ORBIT); }
            else location = orbitDest;
        }
        else {
            nextMove = targetPos.sub(location);
            nextMove = nextMove.mult(SPEED / nextMove.length());
            location = nextMove;
        }
    }

    private void updateDestination (Vector3 target) {
        float targetAngle = calcAngle(targetPos);
        targetPos = target;
        orbitDest = calcPos(targetAngle, ORBIT);
    }

    private Vector3 calcPos(float angle, float radius) {
        return Vector3f.createFrom(
                (float) Math.sin(Math.toRadians(angle)) * radius,
                0.0f,
                (float) Math.cos(Math.toRadians(angle)) * radius
        );
    }

    private float calcAngle(Vector3 pos) { return (float) Math.atan(pos.x() / pos.z()); }
}
