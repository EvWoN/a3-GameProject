package a3;

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

public class MyGame extends VariableFrameRateGame {

    // to minimize variable allocation in update()
    private Engine eng;
    private GL4RenderSystem rs;
    private InputManager im;
    private SceneManager sm;
    private String serverAddress;
    private int serverPort;
    private IGameConnection.ProtocolType serverProtocol;
    private ProtocolClient protClient;
    public boolean isClientConnected;
    private List<UUID> gameObjectsToRemove;

    //Controllers
    private RotationController rc;
    private SquishyBounceController sc;
    private OrbitCameraController occ;

    private float elapsTime = 0.0f;

    private boolean alone = true;
    private boolean holdingItem = false;
    private boolean running = false;

    private SceneNode astronautNode, groundNode;
    private SkeletalEntity astronautSkeleton;

    private ArrayList<SceneNode> partsList;

    private PhysicsEngine physicsEngine;
    private PhysicsObject part, ground;

    Camera camera;

    SceneNode follow = null;

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
            isClientConnected = true;
        }
    }

    @Override
    protected void update(Engine engine) {
        // build and set HUD
        rs = (GL4RenderSystem) engine.getRenderSystem();
        int height = rs.getCanvas().getHeight();
        float elapsedTimeMillis = engine.getElapsedTimeMillis();
        im.update(elapsedTimeMillis);
        occ.updateCameraPosition();
        astronautSkeleton.update();
        /*if(follow != null)
        {
            Vector3 pos = Vector3f.createFrom(
                    follow.getWorldPosition().x(),
                    follow.getWorldPosition().y() + 2f,
                    follow.getWorldPosition().z()
            );
            camera.setPo((Vector3f) pos);
        }*/
        //Networking, process packets
        //if(running) updatePhysics();
        updatePhysics();
        pickupItems();
        processNetworking(elapsedTimeMillis);
    }

    private void updatePhysics() {
        float time = eng.getElapsedTimeMillis();
        Matrix4 mat;
        physicsEngine.update(time);
        for(SceneNode node : eng.getSceneManager().getSceneNodes()) {
            if(node.getPhysicsObject() != null) {
                mat = Matrix4f.createFrom(toFloatArray(node.getPhysicsObject().getTransform()));
                node.setLocalPosition(mat.value(0, 3), mat.value(1, 3), mat.value(2, 3));
            }
        }
    }

    private void pickupItems() {
        if (holdingItem) return;
        SceneNode hold = null;
        for(SceneNode node : partsList){
            if(isClose(node, astronautNode)) {
                holdingItem = true;
                node.getParent().detachChild(node);
                astronautNode.attachChild(node);
                node.setLocalRotation(astronautNode.getLocalRotation());
                node.setLocalPosition(0.0f, 3.0f, 0.0f);
                hold = node;
                follow = node;
                break;
            }
        }
        if(hold != null) partsList.remove(hold);
    }

    protected void processNetworking(float elapsTime) { // Process packets received by the client from the server
        if (protClient != null)
            protClient.processPackets();
        // remove ghost avatars for players who have left the game
        Iterator<UUID> it = gameObjectsToRemove.iterator();
        while (it.hasNext()) {
            sm.destroySceneNode(it.next().toString());
        }
        gameObjectsToRemove.clear();
    }

    @Override
    protected void setupWindowViewports(RenderWindow rw) {
        System.out.println("SetupWindowViewports");
        rw.addKeyListener(this);
        Viewport topViewport = rw.getViewport(0);
        topViewport.setClearColor(new Color(.1f, .1f, .1f));
    }

    @Override
    protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
        System.out.println("SetupWindow");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenPortion = 0.85; //mitigate taskbar in windowed mode
        int width = (int) (screenSize.width * screenPortion);
        int height = (int) (screenSize.height * screenPortion);
        rs.createRenderWindow(new DisplayMode(width, height, 24, 60), false);
    }

    protected void setupControls(SceneManager sm) {
        System.out.println("SetupControls");
        //im = new GenericInputManager();
        ArrayList<Controller> controllers = im.getControllers();
        
        //CameraOrbitalController
        SceneNode cameraNode = sm.getSceneNode("CameraNode");
        SceneNode astronautNode = sm.getSceneNode("astronautNode");
        Camera camera = sm.getCamera("Camera");
        occ = new OrbitCameraController(camera,cameraNode,astronautNode);
    
        //Actions
        //Movement
        MoveUpAction moveUpAction = new MoveUpAction            (this, astronautNode, protClient);
        MoveDownAction moveDownAction = new MoveDownAction      (this, astronautNode, protClient);
        MoveRightAction moveRightAction = new MoveRightAction   (this, astronautNode, protClient);
        MoveLeftAction moveLeftAction = new MoveLeftAction      (this, astronautNode,protClient);
        ThrowItemAction throwItemAction = new ThrowItemAction   (astronautNode, sm, physicsEngine, sc);
        //CameraRotate
//        RotateLeftAction rotateLeftAction = new RotateLeftAction();

        for (Controller c : controllers) {
            if (c.getType() == Controller.Type.KEYBOARD) {
                im.associateAction(
                        c,
                        Component.Identifier.Key.W,
                        moveUpAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.S,
                        moveDownAction,
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
                
                occ.setupInput(im,c);
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

    protected void initControllers(SceneManager sm) {
        sc = new SquishyBounceController();
        rc = new RotationController(Vector3f.createUnitVectorY(), .1f);
        sm.addController(rc);
        sm.addController(sc);
    }

    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException {
        System.out.println("SetupScene");

        setupNetworking();
        processNetworking(eng.getElapsedTimeMillis());

        this.sm = sm;
        this.eng = eng;

        initControllers(sm);

        astronautSkeleton = rigSkeleton("astronaut", "run", "jump", "idle");

        astronautNode = sm.getRootSceneNode().createChildSceneNode("astronautNode");
        astronautNode.attachObject(astronautSkeleton);
        astronautNode.scale(0.3f, 0.3f, 0.3f);
        astronautNode.setLocalRotation(UP);
        setAstronautTexture(astronautSkeleton);

        //Scene axis
        showAxis(eng, sm);
/*
        Entity ball = sm.createEntity("Ball", "sphere.obj");
        ball.setPrimitive(Primitive.TRIANGLES);
        SceneNode ballNode = sm.getRootSceneNode().createChildSceneNode("BallNode");
        ballNode.attachObject(ball);
        ballNode.setLocalPosition(0.0f, 2.0f, 0.0f);*/

        //Ground floor
        Entity groundEntity = sm.createEntity("GroundEntity", "platform1.obj");
        groundNode = sm.getRootSceneNode().createChildSceneNode("GroundNode");
        groundNode.attachObject(groundEntity);
        groundNode.setLocalPosition(0.0f, -1.0f, 0.0f);

        setupLighting();

        //Make Skybox
        SkyBox startBox = makeSkyBox("red");
        sm.setActiveSkyBox(startBox);

        sc = new SquishyBounceController();
        sm.addController(sc);

        makeParts();

        initPhysicsSystem();
        createPhysicsWorld();
        setupControls(sm);
    }

    private SkeletalEntity rigSkeleton(String name, String... actions) throws IOException {
        Material material = sm.getMaterialManager().getAssetByPath(name.concat(".mtl"));
        SkeletalEntity skeleton = sm.createSkeletalEntity(name, name.concat("Mesh.rkm"), name.concat("Skeleton.rks"));
        for(String action : actions)
            skeleton.loadAnimation(action, action.concat("Action.rka"));
        skeleton.playAnimation("idle",1.0f, SkeletalEntity.EndType.LOOP,0);
        skeleton.setMaterial(material);
        skeleton.setPrimitive(Primitive.TRIANGLES);
        bindAnim(skeleton);
        return skeleton;
    }

    private void bindAnim (SkeletalEntity skeleton) {
        im = new GenericInputManager();
        ArrayList<Controller> controllers = im.getControllers();

        StartAnimationAction startAnimationAction = new StartAnimationAction(
                skeleton,
                "run",
                1.0f,
                SkeletalEntity.EndType.LOOP,
                0);

        for(Controller c : controllers) {
            if (c.getType() == Controller.Type.KEYBOARD)
                im.associateAction(
                        c,
                        Component.Identifier.Key.Q,
                        startAnimationAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
        }
    }

    private void setupLighting() {
        sm.getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));
        Light plight = sm.createLight("testLamp1", Light.Type.POINT);
        plight.setAmbient(new Color(.5f, .5f, .5f));
        plight.setDiffuse(new Color(.9f, .9f, .9f));
        plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        plight.setRange(20f);
        SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
        plightNode.attachObject(plight);
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

    private SceneNode makeGroundFloor(Engine eng, SceneManager sm) throws IOException {
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
        Texture tex = eng.getTextureManager().getAssetByPath("green.png");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);

        FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);
        groundFloorObj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        groundFloorObj.setRenderState(texState);
        groundFloorObj.setRenderState(faceState);
        groundFloorObj.setPrimitive(Primitive.TRIANGLES);


        SceneNode groundFloorNode = sm.getRootSceneNode().createChildSceneNode(groundFloorObj.getName() + "Node");
        groundFloorNode.attachObject(groundFloorObj);

        return groundFloorNode;
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
        int numParts = 1;
        partsList = new ArrayList<>();
        Entity entity;
        SceneNode node;
        for(int i = 0; i < numParts; ++i){
            entity = sm.createEntity("PartEntity" + Integer.toString(i), "Thruster.obj");
            entity.setPrimitive(Primitive.TRIANGLES);
            node = sm.getRootSceneNode().createChildSceneNode("PartNode" +Integer.toString(i));
            node.attachObject(entity);
            node.setLocalPosition(-2.0f, 0.25f , 0.0f);
            node.setLocalScale(.25f, .25f, .25f);
            node.setLocalRotation(UP);
            sc.addNode(node);
            partsList.add(node);
        }
    }

    private boolean isClose(SceneNode node1, SceneNode node2){
        Vector3 a, b;
        float distance, minimum;
        float dx, dz;
        a = node1.getLocalPosition();
        b = node2.getLocalPosition();
        minimum = 1.0f;
        minimum *= minimum;
        dx = a.x() - b.x();
        dz = a.z() - b.z();
        dx *= dx;
        dz *= dx;
        distance = dx + dz;
        distance *= distance;
        return distance <= minimum;
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
        ground.setBounciness(1.0f);
//        groundNode.scale(10.0f, .5f, 10.0f);
        groundNode.setPhysicsObject(ground);
        /*
        temptf = toDoubleArray(sm.getRootSceneNode().getChild("BallNode").getLocalTransform().toFloatArray());
        ballObject = physicsEngine.addSphereObject(
                physicsEngine.nextUID(),
                1.0f,
                temptf,
                0.1f
        );
        ballObject.setBounciness(1.0f);
        sm.getRootSceneNode().getChild("BallNode").setPhysicsObject(ballObject);*/
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

    public Vector3 getPlayerPosition() {
        SceneNode player1 = sm.getSceneNode("p1DolphinNode");
        return player1.getWorldPosition();
    }

    public Vector3 getPlayerHeading() {
        SceneNode player1 = sm.getSceneNode("p1DolphinNode");
        System.out.println("Get Heading: " + player1.getWorldRotation());
        System.out.println(player1.getWorldForwardAxis());
        return player1.getWorldForwardAxis();
    }

    public GhostAvatar createGhostAvatar(UUID uuid, Vector3 position, Vector3 heading) throws IOException {
        //Player
        alone = false;

        Entity dolphinE_2 = sm.createEntity("p2Dolphin" + uuid.toString(), "astronaut.obj");

        setAstronautTexture(dolphinE_2);

        SceneNode dolphinN_2 = sm.getRootSceneNode().createChildSceneNode(dolphinE_2.getName() + "Node");

        dolphinE_2.setPrimitive(Primitive.TRIANGLES);
        dolphinN_2.attachObject(dolphinE_2);

        GhostAvatar ghostAvatar = new GhostAvatar(uuid, dolphinN_2, dolphinE_2);
        System.out.println("Position:: " + position + "Heading:: " + heading);
        ghostAvatar.setPosition(position);
        ghostAvatar.setHeading(heading);

        System.out.println("Ghost is being created: " + ghostAvatar + " Node:" + dolphinN_2.toString());

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
}
