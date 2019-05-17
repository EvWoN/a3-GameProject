package myGameEngine;

import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.util.UUID;

public class Enemy {

    private UUID id;
    private Vector3 location;
    private Vector3 orbitDest;
    private Vector3 targetPos;
    private Vector3 heading;
    private float angle;

    private int ammo;
    private boolean orbiting = false;

    private final float ORBIT;
    private final float SPEED;
    private final float START;

    public Enemy(UUID uuid, float angle, int ammo, float orbit, float speed, float start) {
        this.id = uuid;
        this.angle = angle;
        this.ammo = ammo;
        this.ORBIT = orbit;
        this.SPEED = speed;
        this.START = start;
        this.location = calcPos(angle, START);
        this.orbitDest = calcPos(angle, ORBIT);
        this.heading = Vector3f.createFrom(0.0f, 0.0f, 1.0f);
    }

    public void move(float elapsedTime) {
        Vector3 nextMove;
        float nextAngle;
        if(!orbiting) {
            nextMove = calcPos(angle, location.length() - (SPEED / elapsedTime));
            if (nextMove.length() > ORBIT) {
                location = nextMove;
            } else {
                location = orbitDest;
                orbiting = true;
            }
        }
        else if(ammo > 0) {
            nextAngle = angle - calcAngle(orbitDest);
            //if(nextAngle > 0.0f) { location = calcPos(nextAngle, ORBIT); }
            //else location = orbitDest;
            location = orbitDest;
        }
        else {
            nextMove = targetPos.sub(location);
            nextMove = nextMove.mult(SPEED / nextMove.length());
            location = nextMove;
        }

        if(ammo > 0 && !orbiting) heading = unit(orbitDest.sub(location));
        else heading = unit((targetPos != null) ? targetPos.sub(location) : orbitDest.sub(location));
        //System.out.println("Loc: " + location);
    }

    public void updateDestination (Vector3 target) {
        float targetAngle = (target.isZeroLength()) ? angle : calcAngle(target);
        targetPos = target;
        orbitDest = calcPos((float) Math.toDegrees(targetAngle), ORBIT);
        System.out.println("Target Angle: " + targetAngle + " TargetPos: " + targetPos + " Destination: " + orbitDest);
    }

    public Vector3 getLocation() { return this.location; }

    public UUID getUUID() { return this.id; }

    public Vector3 getHeading() { return this.heading; }

    private Vector3 unit(Vector3 ret) { return ret.div(ret.length()); }

    private Vector3 calcPos(float angle, float radius) {
        return Vector3f.createFrom(
                (float) Math.sin(Math.toRadians(angle)) * radius,
                0.0f,
                (float) Math.cos(Math.toRadians(angle)) * radius
        );
    }

    private float calcAngle(Vector3 pos) { return (float) Math.atan(pos.x() / pos.z()); }

    private void updateHeading() {
        if(ammo > 0 && !orbiting) heading = orbitDest.sub(location);
        else heading = (targetPos != null) ? targetPos.sub(location) : orbitDest.sub(location);
    }
}
