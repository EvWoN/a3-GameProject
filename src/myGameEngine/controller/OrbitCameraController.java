package myGameEngine.controller;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import ray.input.InputManager;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.rage.scene.Camera;
import ray.rage.scene.Node;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OrbitCameraController {
    private Camera camera;//the camera being controlled
    private Node cameraN;//the node the camera is attached to
    private List<Node> targets;//the targets the camera looks at
    private Vector3 cameraFocus;
    private float cameraAzimuth;//rotation of camera around Y axis
    private float cameraElevation;//elevation of camera above targets
    private float minElev = 5f, maxElev = 80f;//Elevation constraints in deg
    private float radius;//distance between camera and targets
    private float minRad = 10f, maxRad = 17f;//radius constraints
    private Vector3 targetPos;//targets’s position in the world
    private Vector3 worldUpVec;
    
    public OrbitCameraController(Camera cam, Node camN, List targs) {
        camera = cam;
        cameraN = camN;
        targets = targs;
        cameraAzimuth = 180.0f;// start from BEHIND and ABOVE the targets
        cameraElevation = 20.0f;// elevation is in degrees
        radius = 12.0f;
        worldUpVec = Vector3f.createFrom(0.0f, 1.0f, 0.0f);
        updateCameraPosition();
    }

    public OrbitCameraController(Camera cam, Node camN, Node... targs) {
        this(cam, camN, Arrays.asList(targs));
    }

    public OrbitCameraController(Camera cam, Node camN, Node targs) {
        this(cam, camN, Collections.singletonList(targs));
    }
    
    // Updates camera position: computes azimuth, elevation, and distance
    // relative to the targets in spherical coordinates, then convertsthose
    // to world Cartesian coordinates and setting the camera position
    public void updateCameraPosition() {
        updateFocusLocation();
        double theta = Math.toRadians(cameraAzimuth);// rot around targets
        double phi = Math.toRadians(cameraElevation);// altitude angle
        double x = radius * Math.cos(phi) * Math.sin(theta);
        double y = radius * Math.sin(phi);
        double z = radius * Math.cos(phi) * Math.cos(theta);
//        System.out.println("Camera focus: " + cameraFocus); //Debug
        cameraN.setLocalPosition(Vector3f.createFrom((float) x, (float) y, (float) z).add(cameraFocus));
        cameraN.lookAt(cameraFocus, worldUpVec);
    }

    private void updateFocusLocation(){
        cameraFocus = Vector3f.createFrom(0,0,0);
            for (Node target : targets) {
//                System.out.println("\tNode: " + target); //Debug
                cameraFocus = cameraFocus.add(target.getWorldPosition());
            }
            cameraFocus = cameraFocus.div(targets.size());
    }
    
    public void setupInput(InputManager im, Controller controller) {
        Action orbitAAction = new OrbitAroundAction();
        Action orbitRAction = new OrbitRadiusAction();
        Action orbitEAction = new OrbitElevationAction();
    
        Action orbitAActionKb = new OrbitAroundActionKeyboard(orbitAAction);
        Action orbitRActionKb = new OrbitRadiusActionKeyboard(orbitRAction);
        Action orbitEActionKb = new OrbitElevationActionKeyboard(orbitEAction);
        if(controller.getType().equals(Controller.Type.GAMEPAD)) {
            im.associateAction(controller, Component.Identifier.Axis.RX, orbitAAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Button._3, orbitRAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Button._2, orbitRActionKb, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Axis.RY, orbitEAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        } else if(controller.getType().equals(Controller.Type.KEYBOARD)) {
            im.associateAction(controller, Component.Identifier.Key.RIGHT, orbitAAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Key.COMMA, orbitRAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Key.DOWN, orbitEAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Key.LEFT, orbitAActionKb, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Key.PERIOD, orbitRActionKb, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            im.associateAction(controller, Component.Identifier.Key.UP, orbitEActionKb,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        }
    }
    
    private class OrbitAroundActionKeyboard extends AbstractInputAction{
    
        Action orbitAAction;
        
        OrbitAroundActionKeyboard(Action orbitAAction){
            this.orbitAAction = orbitAAction;
        }
    
        @Override
        public void performAction(float v, Event event) {
            event.set(event.getComponent(),-1,event.getNanos());
            orbitAAction.performAction(v,event);
        }
    }
    
    private class OrbitRadiusActionKeyboard extends AbstractInputAction{
    
        Action orbitRAction;
        
        OrbitRadiusActionKeyboard(Action orbitRAction){
            this.orbitRAction = orbitRAction;
        }
        
        @Override
        public void performAction(float v, Event event) {
            event.set(event.getComponent(),-1,event.getNanos());
            orbitRAction.performAction(v,event);
        }
    }
    
    private class OrbitElevationActionKeyboard extends AbstractInputAction{
    
        Action orbitEAction;
        
        OrbitElevationActionKeyboard(Action orbitEAction){
            this.orbitEAction = orbitEAction;
        }
        
        @Override
        public void performAction(float v, Event event) {
            event.set(event.getComponent(),-1,event.getNanos());
            orbitEAction.performAction(v,event);
        }
    }
    
    private class OrbitAroundAction extends AbstractInputAction {
        //Moves the camera around the targets (changes camera azimuth).
        public void performAction(float time, net.java.games.input.Event evt) {
            float rotAmount;
            float strength = evt.getValue()*2;
            if (strength <- 0.2) {
                rotAmount = strength/(time/2);
            } else {
                if (strength > 0.2) {
                    rotAmount = strength/(time/2);
                } else {
                    rotAmount = 0.0f;
                }
            }
            cameraAzimuth += rotAmount;
            cameraAzimuth = cameraAzimuth % 360;
            updateCameraPosition();
        }
    }
    
    private class OrbitRadiusAction extends AbstractInputAction {
        //Moves the camera in and out of the targets (ZOOM)
        public void performAction(float time, net.java.games.input.Event evt) {
            float moveInAmount;
            float strength = evt.getValue();
            if (strength < -0.2) {
                moveInAmount = strength/(time/2);
            } else {
                if (strength > 0.2) {
                    moveInAmount = strength/(time/2);
                } else {
                    moveInAmount = 0.0f;
                }
            }
            float newRad = radius + moveInAmount/20;
            //Checking zoom values
            if(minRad > newRad){
                radius = minRad;
            } else if (maxRad < newRad){
                radius = maxRad;
            } else radius = newRad;
            updateCameraPosition();
        }
    }
    
    private class OrbitElevationAction extends AbstractInputAction {
        //Moves the camera up and down around the targets
        public void performAction(float time, net.java.games.input.Event evt) {
            float moveUpAmount;
            float strength = evt.getValue()*2;
            if (strength < -0.2) {
                moveUpAmount = -strength/(time/2);
            } else {
                if (strength > 0.2) {
                    moveUpAmount = -strength/(time/2);
                } else {
                    moveUpAmount = 0.0f;
                }
            }
            float newElev = cameraElevation + moveUpAmount;
            //Checking zoom values
            if(minElev > newElev){
                cameraElevation = minElev;
            } else if (maxElev < newElev){
                cameraElevation = maxElev;
            } else cameraElevation = newElev;
            updateCameraPosition();
        }
    }
    
    public float getCameraAzimuth() {
        return (cameraAzimuth+180)%380;
    }
}
