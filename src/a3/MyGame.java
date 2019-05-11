package a3;

import javafx.beans.property.SimpleBooleanProperty;
import myGameEngine.Managers.AstronautAnimator;
import myGameEngine.Managers.Builder;
import myGameEngine.Managers.Movement2DManager;
import myGameEngine.Networking.GhostAvatar;
import myGameEngine.Networking.ProtocolClient;
import myGameEngine.actions.*;
import myGameEngine.controller.OrbitCameraController;
import myGameEngine.controller.SquishyBounceController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.networking.IGameConnection;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsEngineFactory;
import ray.physics.PhysicsObject;
import ray.rage.Engine;
import ray.rage.asset.material.Material;
import ray.rage.asset.texture.Texture;
import ray.rage.asset.texture.TextureManager;
import ray.rage.game.Game;
import ray.rage.game.VariableFrameRateGame;
import ray.rage.rendersystem.RenderSystem;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.Renderable.Primitive;
import ray.rage.rendersystem.Viewport;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.states.FrontFaceState;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.Projection;
import ray.rage.scene.controllers.RotationController;
import ray.rage.util.BufferUtil;
import ray.rage.util.Configuration;
import ray.rml.*;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class MyGame extends VariableFrameRateGame {

    // to minimize variable allocation in update()
    private Engine eng;
    private GL4RenderSystem rs;
    private InputManager im;
    private SceneManager sm;
    private Movement2DManager mm;
    private AstronautAnimator animator;
    private String serverAddress;
    private int serverPort;
    private IGameConnection.ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private List<UUID> gameObjectsToRemove;

    //Controllers
    private RotationController rc;
    private SquishyBounceController sc;
    private OrbitCameraController occ;

    public boolean isClientConnected = false;
    private boolean alone = true;
    private boolean placed = false;

    private SimpleBooleanProperty holdingItem = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty followGround = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty host = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty ghostHolding = new SimpleBooleanProperty(false);

    private SceneNode astronautNode, groundNode, ufoNode1, ufoNode2;
    private Tessellation tesselationEntity;
    private SkeletalEntity astronautSkeleton;
    private SkeletalEntity otherPlayer;

    private ArrayList<SceneNode> partsList;

    private PhysicsEngine physicsEngine;
    private PhysicsObject ground;

    private Random rand;

    private float totalTime;

    private int itemCount;

    Camera camera;

    //Forward, up, right
    public Matrix3 LEFT = Matrix3f.createFrom(
            Vector3f.createFrom(0.0f, 0.0f, -1.0f),
            Vector3f.createFrom(0.0f, 1.0f, 0.0f),
            Vector3f.createFrom(1.0f, 0.0f, 0.0f)
    );
    public Matrix3 RIGHT = Matrix3f.createFrom(
            Vector3f.createFrom(0.0f, 0.0f, 1.0f),
            Vector3f.createFrom(0.0f, 1.0f, 0.0f),
            Vector3f.createFrom(-1.0f, 0.0f, 0.0f)
    );
    public Matrix3 UP = Matrix3f.createFrom(
            Vector3f.createFrom(1.0f, 0.0f, 0.0f),
            Vector3f.createFrom(0.0f, 1.0f, 0.0f),
            Vector3f.createFrom(0.0f, 0.0f, 1.0f)
    );
    public Matrix3 DOWN = Matrix3f.createFrom(
            Vector3f.createFrom(-1.0f, 0.0f, 0.0f),
            Vector3f.createFrom(0.0f, 1.0f, 0.0f),
            Vector3f.createFrom(0.0f, 0.0f, -1.0f)
    );

    public MyGame(String serverAddr, int sPort, String protocol) {
        super();
        this.serverAddress = serverAddr;
        this.serverPort = sPort;
        this.rand = new Random();
        this.partsList = new ArrayList<>();
        this.totalTime = 0;
        this.itemCount = 0;
        if (protocol.toUpperCase().compareTo("UDP") == 0) {
            this.serverProtocol = IGameConnection.ProtocolType.UDP;
        }
    }

    public static void main(String[] args) {
        Game game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
        try {
            game.startup();
            game.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            game.shutdown();
            game.exit();
        }
    }
    
    private void setupNetworking() {
        gameObjectsToRemove = new LinkedList<UUID>();
        isClientConnected = false;
        try {
            System.out.println("Networking is being set up");
            this.protClient = new ProtocolClient(InetAddress
                    .getByName(serverAddress), serverPort, serverProtocol, this);
            System.out.println(protClient);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (protClient == null) {
            System.out.println("missing protocol host");
        } else { // ask client protocol to send initial join message
            //to server, with a unique identifier for this client
            System.out.println("Trying to join");
            protClient.sendJoinMessage();
            //TODO - boolean return on join
            //isClientConnected = true;
        }
        mm.setClient(protClient);
        animator.setClient(protClient);
        
        //Bye on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            protClient.sendByeMessage();
        }));
    }

    @Override
    protected void update(Engine engine) {
        // build and set HUD
        rs = (GL4RenderSystem) engine.getRenderSystem();
        float elapsedTimeMillis = engine.getElapsedTimeMillis();
        im.update(elapsedTimeMillis);
        totalTime += elapsedTimeMillis;
        occ.updateCameraPosition();
        
        //Update all skeletons
        sm.getRootSceneNode().getChildNodes().forEach(new Consumer<Node>() {
            @Override
            public void accept(Node node) {
                if(node instanceof SceneNode){
                    ((SceneNode) node).getAttachedObjects().forEach(new Consumer<SceneObject>() {
                        @Override
                        public void accept(SceneObject sceneObject) {
                            if(sceneObject instanceof SkeletalEntity){
                                ((SkeletalEntity) sceneObject).update();
                            }
                        }
                    });
                }
            }
        });
        
        if(host.get()) {
            updatePhysics();
            pickupItems();
            checkItems();
            moveEnemies();
            if (Math.round(totalTime / 1000) % 8 == 0) {
                if (!placed) {
                    try {
                        makeParts();
                    } catch (IOException e) {
                        System.out.println("Error in part creation.");
                    }
                    placed = true;
                }
            } else placed = false;
        }
        rs.setHUD(
                "Seconds: " + Math.round(totalTime / 1000) + " " +
                "Host: " + host.get()
        );
        //Networking, process packet
        processNetworking(elapsedTimeMillis);
        animator.updateAnimationState(elapsedTimeMillis);
        mm.updateMovements();
    }

    private void updatePhysics() {
        float time = eng.getElapsedTimeMillis();
        Matrix4 mat;
        physicsEngine.update(time);
        for(SceneNode node : eng.getSceneManager().getSceneNodes()) {
            if(node.getName().equals("GroundNode")) continue;
            if(node.getPhysicsObject() != null) {
                mat = Matrix4f.createFrom(toFloatArray(node.getPhysicsObject().getTransform()));
                node.setLocalPosition(mat.value(0, 3), mat.value(1, 3), mat.value(2, 3));
                protClient.sendMoveItemMessage(
                        node.getName().substring("PartNode".length() + 1),
                        node.getLocalPosition(),
                        node.getLocalForwardAxis()
                );
                System.out.println("Sending MoveItem: " + node.getName().substring("PartNode".length() + 1));
            }
        }
    }

    private void pickupItems() {
        if (holdingItem.getValue()) return;
        SceneNode hold = null;
        for(SceneNode node : partsList){
            if (isClose3D(node, astronautNode, 1.0f)) {
                /*
                holdingItem.set(true);
                node.setPhysicsObject(null);
                node.getParent().detachChild(node);
                astronautNode.attachChild(node);
                node.setLocalRotation(astronautNode.getLocalRotation());
                node.setLocalPosition(0.0f, 5.0f, 0.0f);
                */
                pickup(node, astronautNode);
                holdingItem.set(true);
                hold = node;
            }
            //TODO Dude, this is broke, ghostNode is reassigned with each new avatar which you coded to be parts too.... othe than the first instance, you are essentially checking if parts are close to parts
//            if(ghostNode != null && isClose3D(node, ghostNode, 1.0f)) {
//                pickup(node, ghostNode);
//                ghostHolding.set(true);
//                hold = node;
//            }
            if(holdingItem.get() && ghostHolding.get()) break;
        }
        if(hold != null) partsList.remove(hold);
    }

    private void pickup(SceneNode item, SceneNode thrower) {
        String id = item.getName().substring("PartNode".length() + 1);
        astronautNode.setPhysicsObject(null);
        item.getParent().detachChild(item);
        thrower.attachChild(item);
        item.setLocalRotation(thrower.getLocalRotation());
        item.setLocalPosition(0.0f, 5.0f, 0.0f);
        /*System.out.println(
                "\nSending message: move " + id +
                "\nFor object: " + item.getName() + "\n");
        protClient.sendMoveItemMessage(id, item.getLocalPosition(), item.getLocalForwardAxis());*/
    }

    private void checkItems() {
        Iterator<Node> nodeIterator = sm.getRootSceneNode().getChildNodes().iterator();
        ArrayList<Node> toBeRemoved = new ArrayList<>();
        Node node;
        while(nodeIterator.hasNext())
        {
            node = nodeIterator.next();
            if(node.getPhysicsObject() != null && !node.getName().startsWith("Ground"))
                if(!isClose2D((SceneNode) node, groundNode, 8.0f)) toBeRemoved.add(node);
        }
        nodeIterator = toBeRemoved.iterator();
        while(nodeIterator.hasNext()) {
            node = nodeIterator.next();
            partsList.remove(node);
            sm.getRootSceneNode().detachChild(node);
        }
    }

    private void moveEnemies() {
        double distance = 10.0f;
        double speed = 1.0f;
        double radius = getDistance2D(groundNode, astronautNode);
        double angleToDestination = (180f * speed) / (Math.PI * distance);
        Vector3 astronautPos = astronautNode.getLocalPosition();
        Vector3 destination = Vector3f.createFrom(
                (float) (distance * (astronautPos.x() / radius)),
                0.0f,
                (float) (distance * (astronautPos.z() / radius))
        );
        ufoNode1.lookAt(astronautSkeleton.getParentNode());
    }

    private boolean processNetworking(float elapsTime) { // Process packets received by the client from the server
        if (protClient != null)
            protClient.processPackets();
        // remove ghost avatars for players who have left the game
        Iterator<UUID> it = gameObjectsToRemove.iterator();
        while (it.hasNext()) { sm.destroySceneNode(it.next().toString()); }
        gameObjectsToRemove.clear();
        return true;
    }

    @Override
    protected void setupWindowViewports(RenderWindow rw) {
        System.out.println("SetupWindowViewports");
        Viewport topViewport = rw.getViewport(0);
        topViewport.setClearColor(new Color(.1f, .1f, .1f));
        //This is throwing an error on "ESC" key
//        rw.addKeyListener(this);
    }

    @Override
    protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
        System.out.println("SetupWindow");
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenPortion = 0.60; //mitigate taskbar in windowed mode
        int width = (int) (screenSize.width * screenPortion);
        int height = (int) (screenSize.height * screenPortion);
        rs.createRenderWindow(new DisplayMode(width, height, 24, 60), false);
        
        /** Display modeSelect
        DisplaySettingsDialog settingsDialog = new DisplaySettingsDialog(ge.getDefaultScreenDevice());
        settingsDialog.showIt();
        DisplayMode selectedDisplayMode = settingsDialog.getSelectedDisplayMode();
        if (selectedDisplayMode != null) {
            rs.createRenderWindow(selectedDisplayMode,settingsDialog.isFullScreenModeSelected());
        } else this.exit();
         */
    }

    private void setupControls(SceneManager sm) {
        System.out.println("SetupControls" + protClient);
        mm = new Movement2DManager(8f, protClient);
        animator = new AstronautAnimator(astronautSkeleton,mm, protClient,"run",.8f,"idle",.4f);
        
        im = new GenericInputManager();
        ArrayList<Controller> controllers = im.getControllers();
        
        //CameraOrbitalController
        SceneNode cameraNode = sm.getSceneNode("CameraNode");
        Camera camera = sm.getCamera("Camera");
        occ = new OrbitCameraController(camera,cameraNode,groundNode,astronautNode);
    
        Builder builder = new Builder(sm.getSceneNode("spaceshipNode"),sm);
        builder.setPointsToBuild(10);
        
        //Movement
        MoveForwardAction moveForwardAction = new MoveForwardAction(astronautNode, occ, mm);
        MoveBackwardAction moveBackwardAction = new MoveBackwardAction(astronautNode, occ, mm);
        MoveRightAction moveRightAction = new MoveRightAction   (astronautNode, occ, mm);
        MoveLeftAction moveLeftAction = new MoveLeftAction      (astronautNode, occ, mm);
        //Actions
        ThrowItemAction throwItemAction = new ThrowItemAction   (astronautNode, holdingItem, partsList, physicsEngine, sc);
        GameQuitAction gameQuitAction = new GameQuitAction(this);
        BuildAction buildAction = new BuildAction(astronautSkeleton.getParentSceneNode(),holdingItem,builder,1000);
        ToggleMovementAction toggleMovementAction = new ToggleMovementAction(followGround, astronautNode);
        ToggleHostAction toggleHostAction = new ToggleHostAction(host);

        for (Controller c : controllers) {
            occ.setupInput(im,c);
            if (c.getType() == Controller.Type.KEYBOARD) {
                im.associateAction(
                        c,
                        Component.Identifier.Key.W,
                        moveForwardAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.S,
                        moveBackwardAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.D,
                        moveRightAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.A,
                        moveLeftAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.SPACE,
                        throwItemAction,
                        InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.ESCAPE,
                        gameQuitAction,
                        InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.P,
                        toggleMovementAction,
                        InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.H,
                        toggleHostAction,
                        InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
                );
                im.associateAction(c,
                        Component.Identifier.Key.E,
                        buildAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            }
    
            if (c.getType() == Controller.Type.GAMEPAD) {
                im.associateAction(
                        c,
                        Component.Identifier.Axis.X,
                        moveRightAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Axis.Y,
                        moveBackwardAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Button._1,
                        throwItemAction,
                        InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
                );
            }
        }
    }

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
        System.out.println("SetupCamera");
        SceneNode rootNode = sm.getRootSceneNode();

        //Camera
        camera = sm.createCamera("Camera", Projection.PERSPECTIVE);
        rw.getViewport(0).setCamera(camera);
        SceneNode p1CameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
        p1CameraNode.attachObject(camera);
        
        camera.setMode('n');
        camera.setRt((Vector3f) Vector3f.createFrom(1.0f, 0.0f, 0.0f));
        camera.setUp((Vector3f) Vector3f.createFrom(0.0f, 0.0f, 1.0f));
        camera.setFd((Vector3f) Vector3f.createFrom(0.0f, -1.0f, 0.0f));
        camera.setPo((Vector3f) Vector3f.createFrom(0.0f, 5.0f, 0.0f));
    }

    private void initControllers(SceneManager sm) {
        sc = new SquishyBounceController();
        rc = new RotationController(Vector3f.createUnitVectorY(), .1f);
        sm.addController(rc);
        sm.addController(sc);
    }

    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException {
        System.out.println("SetupScene");

        this.sm = sm;
        this.eng = eng;

        initControllers(sm);

        astronautSkeleton = rigSkeleton("astronaut","astronaut", "run", "jump", "idle");
        astronautNode = sm.getRootSceneNode().createChildSceneNode("astronautNode");
        astronautNode.attachObject(astronautSkeleton);
        astronautNode.scale(0.3f, 0.3f, 0.3f);
        astronautNode.setLocalRotation(UP);
        setAstronautTexture(astronautSkeleton);
    
        //Ground floor
        Entity spaceshipObj = sm.createEntity("spaceshipObj", "assembeled_ship.obj");
        SceneNode spaceshipObjNode = sm.createSceneNode("spaceshipObjNode");
        SceneNode spaceshipNode = sm.getRootSceneNode().createChildSceneNode("spaceshipNode");
        spaceshipObjNode.attachObject(spaceshipObj);
//        spaceshipObjNode.scale(9f,9f,9f);
        spaceshipNode.attachChild(spaceshipObjNode);
        spaceshipObjNode.moveForward(.8f);
//        spaceshipNode.moveUp(10f);
        spaceshipNode.setLocalPosition(0.0f, 0.0f, 0.0f);
        spaceshipNode.moveLeft(6f);
        spaceshipNode.moveUp(.2f);
//        spaceshipNode.moveDown(.2f);
    
        //Scene axis
        showAxis(eng, sm);

        Entity ufo1, ufo2;
        ufo1 = sm.createEntity("Ufo1", "Ufo.obj");
        ufo2 = sm.createEntity("Ufo2", "Ufo.obj");
        ufo1.setPrimitive(Primitive.TRIANGLES);
        ufo2.setPrimitive(Primitive.TRIANGLES);
        ufoNode1 = sm.getRootSceneNode().createChildSceneNode("UfoNode1");
        ufoNode2 = sm.getRootSceneNode().createChildSceneNode("UfoNode2");
        ufoNode1.attachObject(ufo1);
        ufoNode2.attachObject(ufo2);
        ufoNode1.setLocalPosition(8.0f, 0.0f, 0.0f);
        ufoNode2.setLocalPosition(-8.0f, 0.0f, 0.0f);

        Light plight = sm.createLight(ufoNode1.getName()+"Light", Light.Type.POINT);
        plight.setDiffuse(new Color(.2f, .2f, 1f));
        plight.setSpecular(new Color(.2f, .2f, 1.0f));
        plight.setRange(10f);
        plight.setFalloffExponent(.2f);
//        plight.setConeCutoffAngle(Degreef.createFrom(70));
        plight.setLinearAttenuation(.1f);
//        plight.setConstantAttenuation();
        SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode(plight.getName() + "Node");
        plightNode.attachObject(plight);
        plightNode.moveUp(1f);
        ufoNode1.attachChild(plightNode);

        //Ground floor
        Entity groundEntity = sm.createEntity("GroundEntity", "mainPlatform.obj");
        groundNode = sm.getRootSceneNode().createChildSceneNode("GroundNode");
        SceneNode groundMeshNode = sm.createSceneNode("groundMesh");
        groundMeshNode.attachObject(groundEntity);
        groundMeshNode.setLocalPosition(0.0f, -9f, 0.0f);
        groundMeshNode.scale(9f,9f,9f);
        groundNode.attachChild(groundMeshNode);
        groundNode.moveDown(.2f);

        setupLighting();

        //Make Skybox
        SkyBox startBox = makeSkyBox("red");
        sm.setActiveSkyBox(startBox);
        sc = new SquishyBounceController();
        sm.addController(sc);

        initPhysicsSystem();
        createPhysicsWorld();
        setupControls(sm);
//        makeGroundFloor();
        makeHeightMap();
        setupNetworking();
        processNetworking(eng.getElapsedTimeMillis());
        System.out.println(isClientConnected);
    }

    private SkeletalEntity rigSkeleton(String entityName, String objName, String... actions) throws IOException {
        Material material = sm.getMaterialManager().getAssetByPath(objName.concat(".mtl"));
        SkeletalEntity skeleton = sm.createSkeletalEntity(entityName, objName.concat("Mesh.rkm"), objName.concat("Skeleton.rks"));
        for(String action : actions)
            skeleton.loadAnimation(action, action.concat("Action.rka"));
        skeleton.playAnimation("idle",0.4f, SkeletalEntity.EndType.LOOP,0);
        skeleton.setMaterial(material);
        return skeleton;
    }

    private void setupLighting() {
//        sm.getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));
        
        Light plight = sm.createLight("testLamp1", Light.Type.POINT);
        plight.setAmbient(new Color(.2f,.2f,.2f));
        plight.setDiffuse(new Color(.9f, .9f, .9f));
        plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        plight.setRange(10f);
        SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
        plightNode.attachObject(plight);
        plightNode.moveUp(3f);
        
        Light spotlight = sm.createLight("spotlight", Light.Type.SPOT);
        spotlight.setDiffuse(new Color(.9f, .9f, .9f));
        spotlight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        spotlight.setRange(10f);
//        spotlight.setAmbient(new Color(.1f, .1f, .1f));
        spotlight.setFalloffExponent(10f);
        SceneNode spotlightNode = sm.getRootSceneNode().createChildSceneNode("spotlightNode");
        spotlightNode.attachObject(spotlight);
        astronautSkeleton.getParentSceneNode().attachChild(spotlightNode);
        spotlightNode.moveUp(1f);
    
        Light lavaLight1 = sm.createLight("lavaLight1", Light.Type.POINT);
        lavaLight1.setDiffuse(new Color(.9f, .2f, .2f));
        lavaLight1.setSpecular(new Color(.9f, .2f, .2f));
        lavaLight1.setRange(40f);
        SceneNode lavaLight1Node = sm.getRootSceneNode().createChildSceneNode(lavaLight1.getName()+"Node");
        lavaLight1Node.attachObject(lavaLight1);
        lavaLight1Node.moveRight(15f);
        lavaLight1Node.moveDown(8f);
    
        Light lavaLight2 = sm.createLight("lavaLight2", Light.Type.POINT);
        lavaLight2.setDiffuse(new Color(.9f, .2f, .2f));
        lavaLight2.setSpecular(new Color(.9f, .2f, .2f));
        lavaLight2.setRange(40f);
        SceneNode lavaLight2Node = sm.getRootSceneNode().createChildSceneNode(lavaLight2.getName()+"Node");
        lavaLight2Node.attachObject(lavaLight2);
        lavaLight2Node.moveLeft(15f);
        lavaLight2Node.moveDown(8f);
    
        Light lavaLight3 = sm.createLight("lavaLight3", Light.Type.POINT);
        lavaLight3.setDiffuse(new Color(.9f, .2f, .2f));
        lavaLight3.setSpecular(new Color(.9f, .2f, .2f));
        lavaLight3.setRange(40f);
        SceneNode lavaLight3Node = sm.getRootSceneNode().createChildSceneNode(lavaLight3.getName()+"Node");
        lavaLight3Node.attachObject(lavaLight3);
        lavaLight3Node.moveForward(15f);
        lavaLight3Node.moveDown(8f);
        
        Light lavaLight4 = sm.createLight("lavaLight4", Light.Type.POINT);
        lavaLight4.setDiffuse(new Color(.9f, .2f, .2f));
        lavaLight4.setSpecular(new Color(.9f, .2f, .2f));
        lavaLight4.setRange(40f);
        SceneNode lavaLight4Node = sm.getRootSceneNode().createChildSceneNode(lavaLight4.getName()+"Node");
        lavaLight4Node.attachObject(lavaLight4);
        lavaLight4Node.moveBackward(15f);
        lavaLight4Node.moveDown(8f);
    
//        Light predlight = sm.createLight("redLamp1", Light.Type.POINT);
//        predlight.setAmbient(new Color(.5f, .5f, .5f));
//        predlight.setDiffuse(new Color(.9f, .0f, .0f));
//        predlight.setSpecular(new Color(.9f, .0f, .0f));
//        predlight.setRange(10f);
//        SceneNode predlightNode = sm.getRootSceneNode().createChildSceneNode(predlight.getName() + "Node");
//        predlightNode.attachObject(predlight);
//        predlightNode.moveLeft(30f);
//        predlightNode.moveDown(30f);
        
//        Light dlight = sm.createLight("sun", Light.Type.DIRECTIONAL);
//        dlight.setAmbient(new Color(.1f, .1f, .1f));
//        dlight.setDiffuse(new Color(.5f, .5f, .5f));
//        dlight.setSpecular(new Color(.1f, .1f, .1f));
//        dlight.setRange(50f);
//        SceneNode dlightNode = sm.getRootSceneNode().createChildSceneNode("dlightNode");
//        dlightNode.attachObject(dlight);
//        dlightNode.rotate(Degreef.createFrom(180f), Vector3f.createFrom(0,0,1));
//        dlightNode.moveUp(2f);
        
        
    }

    private void runScript(ScriptEngine engine, String fileName) {
        try {
            FileReader fr = new FileReader("scripts/" + fileName);
            engine.eval(fr);
            fr.close();
        } catch (IOException io) {
            System.out.println("File \"" + fileName + "\" not found.");
        } catch (ScriptException s) {
            System.out.println("Script error");
        }
    }

    //Path should be a string denoting the folder within "skyboxes" that holds the front, back, left, right, etc.
    //If you have a sub-folder "red" inside of "skyboxes" the path should be "red"
    //If it is multiple folders deep, do not include the last "/" ie "space/galaxies/red"
    private SkyBox makeSkyBox(String path) throws IOException {
        //Skybox software licensed freely from https://github.com/wwwtyro/space-3d/blob/gh-pages/LICENSE
        Engine eng = getEngine();
        SceneManager sm = eng.getSceneManager();
        Configuration conf = eng.getConfiguration();
        TextureManager textureManager = eng.getTextureManager();
        textureManager.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
        Texture front, back, left, right, top, bottom;
        front = textureManager.getAssetByPath(path + "/front.png");
        back = textureManager.getAssetByPath(path + "/back.png");
        left = textureManager.getAssetByPath(path + "/left.png");
        right = textureManager.getAssetByPath(path + "/right.png");
        top = textureManager.getAssetByPath(path + "/top.png");
        bottom = textureManager.getAssetByPath(path + "/bottom.png");
        textureManager.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));

        //Flipping the textures, as they're upside down by default.
        AffineTransform transform = new AffineTransform();
        AffineTransform xform;
        transform.translate(0, front.getImage().getHeight());
        transform.scale(1d, -1d);

        front.transform(transform);
        back.transform(transform);
        left.transform(transform);
        right.transform(transform);
        top.transform(transform);
        bottom.transform(transform);

        SkyBox skyBox = sm.createSkyBox("SkyBox");
        skyBox.setTexture(front, SkyBox.Face.FRONT);
        skyBox.setTexture(back, SkyBox.Face.BACK);
        skyBox.setTexture(left, SkyBox.Face.LEFT);
        skyBox.setTexture(right, SkyBox.Face.RIGHT);
        skyBox.setTexture(top, SkyBox.Face.TOP);
        skyBox.setTexture(bottom, SkyBox.Face.BOTTOM);

        return skyBox;
    }

    private void showAxis(Engine eng, SceneManager sm) throws IOException {
        //lineX
        ManualObject lineX = makeXIndicator(eng, sm);
        lineX.setPrimitive(Primitive.LINES);
        SceneNode lineXN = sm.getRootSceneNode().createChildSceneNode(lineX.getName() + "Node");
        lineXN.attachObject(lineX);

        //lineY
        ManualObject lineY = makeYIndicator(eng, sm);
        lineY.setPrimitive(Primitive.LINES);
        SceneNode lineYN = sm.getRootSceneNode().createChildSceneNode(lineY.getName() + "Node");
        lineYN.attachObject(lineY);

        //lineZ
        ManualObject lineZ = makeZIndicator(eng, sm);
        lineZ.setPrimitive(Primitive.LINES);
        SceneNode lineZN = sm.getRootSceneNode().createChildSceneNode(lineZ.getName() + "Node");
        lineZN.attachObject(lineZ);
    }

    private void makeGroundFloor() throws IOException {
        ManualObject groundFloorObj = sm.createManualObject("groundFloor");
        ManualObjectSection groundFloorSect = groundFloorObj.createManualSection("groundFloorSection");
        groundFloorObj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        float[] vertices = new float[]{
                5.0f, 0.0f, 5.0f, 5.0f, 0.0f, -5.0f, -5.0f, 0.0f, 5.0f,//tri`1
                -5.0f, 0.0f, -5.0f, -5.0f, 0.0f, 5.0f, 5.0f, 0.0f, -5.0f//tri`2

        };

            int[] indices = new int[]{0, 1, 2, 3, 4, 5};
        groundFloorSect.setVertexBuffer(BufferUtil.directFloatBuffer(vertices));
        groundFloorSect.setIndexBuffer(BufferUtil.directIntBuffer(indices));
        //Texture
        Texture tex = eng.getTextureManager().getAssetByPath("sun.jpeg");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
            texState.setTexture(tex);

        FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);
        groundFloorObj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        groundFloorObj.setRenderState(texState);
        groundFloorObj.setRenderState(faceState);
        groundFloorObj.setPrimitive(Primitive.TRIANGLES);


        SceneNode groundFloorNode = sm.getRootSceneNode().createChildSceneNode(groundFloorObj.getName() + "Node");
        groundFloorNode.attachObject(groundFloorObj);
        groundFloorNode.moveDown(8f);
        groundFloorNode.scale(10f,10f,10f);
    }

    private void makeHeightMap() {
        tesselationEntity = sm.createTessellation("TesselationEntity", 6);
        SceneNode tesselationNode = sm.getRootSceneNode().createChildSceneNode("TesselationNode");

        tesselationEntity.setSubdivisions(25f);
        tesselationNode.attachObject(tesselationEntity);
        tesselationNode.setLocalScale(100, 100, 100);
        tesselationNode.moveDown(9.5f);
        tesselationEntity.setHeightMap(eng, "sunheight.jpeg");
        tesselationEntity.setTexture(eng, "sun.jpeg");
    }

    private ManualObject makeXIndicator(Engine eng, SceneManager sm) throws IOException {
        ManualObject lineX = sm.createManualObject("xLine");
        ManualObjectSection lineXSect = lineX.createManualSection("xLineSection");
        lineX.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        float[] vertices = new float[]{
                0.0f, 0.0f, 0.0f, //origin
                5.0f, 0.0f, 0.0f, //X-Axis
        };
        int[] indices = new int[]{0, 1};
        IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);

        FloatBuffer vertBuffer = BufferUtil.directFloatBuffer(vertices);

        lineXSect.setVertexBuffer(vertBuffer);
        lineXSect.setIndexBuffer(indexBuf);

        Texture tex = eng.getTextureManager().getAssetByPath("red.jpeg");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);

        lineX.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        lineX.setRenderState(texState);
        lineX.setRenderState(faceState);
        return lineX;
    }

    private ManualObject makeYIndicator(Engine eng, SceneManager sm) throws IOException {
        ManualObject lineX = sm.createManualObject("yLine");
        ManualObjectSection lineXSect = lineX.createManualSection("yLineSection");
        lineX.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        float[] vertices = new float[]{
                0.0f, 0.0f, 0.0f, //origin
                0.0f, 5.0f, 0.0f, //Z-Axis
        };
        int[] indices = new int[]{0, 1};
        IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);

        FloatBuffer vertBuffer = BufferUtil.directFloatBuffer(vertices);


        lineXSect.setVertexBuffer(vertBuffer);
        lineXSect.setIndexBuffer(indexBuf);

        Texture tex = eng.getTextureManager().getAssetByPath("green.png");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);

        lineX.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        lineX.setRenderState(texState);
        lineX.setRenderState(faceState);
        return lineX;
    }

    private ManualObject makeZIndicator(Engine eng, SceneManager sm) throws IOException {
        ManualObject lineX = sm.createManualObject("zLine");
        ManualObjectSection lineXSect = lineX.createManualSection("zLineSection");
        lineX.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        float[] vertices = new float[]{
                0.0f, 0.0f, 0.0f, //origin
                0.0f, 0.0f, 5.0f, //Y-Axis
        };
        int[] indices = new int[]{0, 1};
        IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);

        FloatBuffer vertBuffer = BufferUtil.directFloatBuffer(vertices);

        lineXSect.setVertexBuffer(vertBuffer);
        lineXSect.setIndexBuffer(indexBuf);

        Texture tex = eng.getTextureManager().getAssetByPath("blue.jpeg");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);

        lineX.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        lineX.setRenderState(texState);
        lineX.setRenderState(faceState);
        return lineX;
    }

    private void makeParts() throws IOException {
        Entity entity;
        SceneNode node;
        UUID id = UUID.randomUUID();
        ++itemCount;
        entity = sm.createEntity("part" + id.toString(), "Thruster.obj");
        entity.setPrimitive(Primitive.TRIANGLES);
        node = sm.getRootSceneNode().createChildSceneNode("PartNode" + id.toString());
        node.attachObject(entity);
        setRandomPoint(node, 0.0f, 8.0f);
        node.setLocalScale(.25f, .25f, .25f);
        node.setLocalRotation(UP);
//            sc.addNode(node);
        partsList.add(node);
    }

    private void setRandomPoint(Node node, float minRadius, float maxRadius) {
        float degree = randomFloat(0, 359);
        float radius = randomFloat(Math.round(minRadius), Math.round(maxRadius));
        node.setLocalPosition(
                (float) (radius * Math.sin(Math.toRadians(degree))),
                .25f,
                (float) (radius * Math.cos(Math.toRadians(degree)))
        );
    }

    private float randomFloat (int min, int max) { return (rand.nextInt(max) + min) + rand.nextFloat(); }

    private boolean isClose2D(SceneNode node1, SceneNode node2, double minimum) {
        return getDistance2D(node1, node2) <= minimum;
    }

    private boolean isClose3D(SceneNode node1, SceneNode node2, double minimum) {
        return getDistance3D(node1, node2) <= minimum;
    }

    private double getDistance2D(SceneNode node1, SceneNode node2) {
        Vector3 a, b;
        double distance, dx, dz;
        a = node1.getLocalPosition();
        b = node2.getLocalPosition();
        dx = a.x() - b.x();
        dz = a.z() - b.z();
        dx *= dx;
        dz *= dz;
        distance = dx + dz;
        return Math.sqrt(distance);
    }

    private double getDistance3D(SceneNode node1, SceneNode node2) {
        Vector3 a, b;
        double distance, dx, dy, dz;
        a = node1.getLocalPosition();
        b = node2.getLocalPosition();
        dx = a.x() - b.x();
        dy = a.y() - b.y();
        dz = a.z() - b.z();
        dx *= dx;
        dy *= dy;
        dz *= dz;
        distance = dx + dy + dz;
        return Math.sqrt(distance);
    }

    private void initPhysicsSystem() {
        String engine = "ray.physics.JBullet.JBulletPhysicsEngine";
        float[] gravity = { 0.0f, -3.0f, 0.0f };

        physicsEngine = PhysicsEngineFactory.createPhysicsEngine(engine);
        physicsEngine.initSystem();
        physicsEngine.setGravity(gravity);
    }

    private void createPhysicsWorld() {
        float[] up = { 0.0f, 1.0f, 0.0f };
        double[] temptf;
        PhysicsObject ballObject;

        temptf = toDoubleArray(groundNode.getLocalTransform().toFloatArray());
        ground = physicsEngine.addStaticPlaneObject(
                physicsEngine.nextUID(),
                temptf,
                up,
                0.0f);
        ground.setBounciness(0.0f);
        ground.setFriction(100.0f);
        ground.setFriction(100.0f);
        groundNode.setPhysicsObject(ground);
    }

    private double[] toDoubleArray(float[] in) {
        int size = in.length;
        double[] ret = new double[size];
        for(int i = 0; i < size; ++i) ret[i] = (double) in[i];
        return ret;
    }

    private float[] toFloatArray(double[] in) {
        int size = in.length;
        float[] ret = new float[size];
        for(int i = 0; i < size; ++i) ret[i] = (float) in[i];
        return ret;
    }

    public void setIsConnected(boolean b) {
        isClientConnected = b;
    }

    public Vector3 getPlayerPosition() { return astronautNode.getWorldPosition(); }

    public Vector3 getPlayerHeading() {
        System.out.println("Get Heading: " + astronautNode.getWorldRotation());
        System.out.println(astronautNode.getWorldForwardAxis());
        return astronautNode.getWorldForwardAxis();
    }

    public GhostAvatar createGhostAvatar(UUID uuid, String type, Vector3 position, Vector3 heading) throws IOException {
        //Player
        alone = false;

        String file = "sphere.obj";
        float scale = type.equals("part") ? .25f : 1.2f;

        switch(type) {
            case "astronaut": file = "astronaut.obj"; break;
            case "ufo": file = "Ufo.obj"; break;
            case "part": file = "Thruster.obj"; break;
        }
        
        Entity ghostEntity;
        SceneNode ghostNode;
        
        //Astronaut creation
        if(type.equals("astronaut")) {
            ghostEntity = rigSkeleton(type + uuid.toString(), "astronaut", "run", "jump", "idle");
            ghostNode = sm.getRootSceneNode().createChildSceneNode(ghostEntity.getName() + "Node");
            ghostNode.attachObject(ghostEntity);
            ghostNode.scale(0.3f, 0.3f, 0.3f);
            ghostNode.setLocalRotation(UP);
            setAstronautTexture(ghostEntity);
        } else {
            ghostEntity = sm.createEntity(type + uuid.toString(), file);
            ghostNode = sm.getRootSceneNode().createChildSceneNode(ghostEntity.getName() + "Node");
            ghostEntity.setPrimitive(Primitive.TRIANGLES);
            ghostNode.attachObject(ghostEntity);
            ghostNode.setLocalScale(scale, scale, scale);
        }
        
            GhostAvatar ghostAvatar = new GhostAvatar(uuid, ghostNode, ghostEntity);
            System.out.println("Position:: " + position + "Heading:: " + heading);
            ghostAvatar.setPosition(position);
            ghostAvatar.setHeading(heading);
            
            if(type.equals("astronaut")){
                AstronautAnimator animatorBean = new AstronautAnimator((SkeletalEntity) ghostEntity, "run", .8f, "idle", .4f);
                ghostAvatar.setBean(animatorBean);
                System.out.println("Playing idle animation");
                animatorBean.playIdleAnimation();
            }
        
            System.out.println("Ghost is being created: " + ghostAvatar + " Node:" + ghostNode.toString());
        
            return ghostAvatar;
    }

    public boolean updateGhostAvatar(GhostAvatar avatar, Vector3 position, Vector3 heading){
        System.out.println("\\u003B[31mAVATAR:\\u003B[0m" + avatar + " POSI:" + position + " HEADING:" + heading);
        System.out.println("Avatar is in scene? " + avatar.getNode());
        avatar.setPosition(position);
        avatar.setHeading(heading);
        return avatar.getNode().isInSceneGraph();
    }

    public void removeGhostAvatar(GhostAvatar avatar){
        sm.destroySceneNode(avatar.getNode());
    }

    private void setAstronautTexture(Entity astronaut) {
        String fileName = (alone) ? "AstronautDiffuse-01.png" : "AstronautDiffuse-02.png";
        try {
            Texture tex = eng.getTextureManager().getAssetByPath(fileName);
            TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
            texState.setTexture(tex);

            astronaut.setRenderState(texState);
            astronaut.setPrimitive(Primitive.TRIANGLES);
        }
        catch(IOException e) { System.out.println(fileName + " not found."); }
    }

    private void updateVerticalPosition() {
        Vector3 worldAvatarPosition = astronautNode.getWorldPosition();
        Vector3 localAvatarPosition = astronautNode.getLocalPosition();
        Vector3 newAvatarPosition = Vector3f.createFrom(
            localAvatarPosition.x(),
            tesselationEntity.getWorldHeight(
                worldAvatarPosition.x(),
                worldAvatarPosition.z()
            ),
            localAvatarPosition.z()
        );
        astronautNode.setLocalPosition(newAvatarPosition);
    }
    
    public void updateGhostAvatarAnim(GhostAvatar ghostAvatar, String animState) {
        Entity entity = ghostAvatar.getEntity();
        if (entity instanceof SkeletalEntity) {
            AstronautAnimator aaBean = (AstronautAnimator) ghostAvatar.getBean();
            switch(animState){
                case "run": aaBean.playMoveAnimation();
                    break;
                case "idle": aaBean.playIdleAnimation();
                    break;
            }
        } else {
            System.err.println("Received animation state for for a non-skeletalEntity: " + ghostAvatar.getNode());
        }
    }
}
