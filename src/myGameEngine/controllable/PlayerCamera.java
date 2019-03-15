package myGameEngine.controllable;

import net.java.games.input.Event;
import ray.rage.rendersystem.Viewport;
import ray.rage.scene.*;
import ray.rml.Vector3f;

/**
 * A wrapper class that wraps an implementation of camera and allows additional player camera based manipulation.
 */
public class PlayerCamera implements Camera {

    private Camera camera;

    //Move speed
    private float movSpeed = .1f;

    //Rotational speed (in degrees)
    private float rotSpeed = 1f;


    public PlayerCamera(Camera c){
        camera = c;
    }

    public void moveLeft(float time, Event evt){

    }

    public void moveRight(){

    }

    public void moveForward(){

    }

    public void moveBackward(){

    }

    public void rotateLeft(){

    }

    public void rotateRight(){

    }

    public void rotateUp(){

    }

    public void rotateDown(){

    }



    @Override
    public Frustum getFrustum() {
        return camera.getFrustum();
    }

    @Override
    public void renderScene() {
        camera.renderScene();
    }

    @Override
    public void notifyViewport(Viewport viewport) {
        camera.notifyViewport(viewport);
    }

    @Override
    public Viewport getViewport() {
        return null;
    }

    @Override
    public void addListener(Listener listener) {

    }

    @Override
    public void removeListener(Listener listener) {

    }

    @Override
    public void setFd(Vector3f vector3f) {

    }

    @Override
    public void setRt(Vector3f vector3f) {

    }

    @Override
    public void setUp(Vector3f vector3f) {

    }

    @Override
    public void setPo(Vector3f vector3f) {

    }

    @Override
    public void setMode(char c) {

    }

    @Override
    public Vector3f getFd() {
        return null;
    }

    @Override
    public Vector3f getRt() {
        return null;
    }

    @Override
    public Vector3f getUp() {
        return null;
    }

    @Override
    public Vector3f getPo() {
        return null;
    }

    @Override
    public char getMode() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Node getParentNode() {
        return null;
    }

    @Override
    public SceneNode getParentSceneNode() {
        return null;
    }

    @Override
    public void notifyAttached(SceneNode sceneNode) {

    }

    @Override
    public void notifyDetached() {

    }

    @Override
    public boolean isAttached() {
        return false;
    }

    @Override
    public void detachFromParent() {

    }

    @Override
    public boolean isInScene() {
        return false;
    }

    @Override
    public void setListener(SceneObject.Listener listener) {

    }

    @Override
    public SceneObject.Listener getListener() {
        return null;
    }

    @Override
    public void notifyDispose() {

    }

    @Override
    public SceneManager getManager() {
        return null;
    }

    @Override
    public void setVisible(boolean b) {

    }

    @Override
    public boolean isVisible() {
        return false;
    }
}
