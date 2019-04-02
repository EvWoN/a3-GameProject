package a3;

import myGameEngine.Networking.GhostAvatar;
import myGameEngine.Networking.ProtocolClient;
import myGameEngine.PointSystem;
import myGameEngine.actions.*;
import myGameEngine.controller.OrbitCameraController;
import myGameEngine.controller.SquishyBounceController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.networking.IGameConnection;
import ray.rage.Engine;
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
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.awt.event.KeyEvent;
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

    private PointSystem ps;

    private OrbitCameraController p1CameraController;
    private OrbitCameraController p2CameraController;

    private float elapsTime = 0.0f;

    public MyGame(String serverAddr, int sPort, String protocol) {
        super();
        System.out.println("WIN by touching the most planets the fastest. Best of 15 planets.");
        System.out.println("Player 1: Keyboard: WASD to move around, E/Q to strafe, SHIFT/CTRL for dive up and down, </> to zoom in and out, Arrow keys to look around");
        System.out.println("Player 2: Controller: LEFT_STICK to move around, RIGHT_STICK to look around, Back Z_PEDALS to dive up and down, Buttons 4/5 to turn left and right, Buttons 1/2 to zoom in and out ");
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
            this.protClient = new ProtocolClient(InetAddress.
                    getByName(serverAddress), serverPort, serverProtocol, this);
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

        ps.updateScores();
        String dispStr1 = "Score = " + ps.getP1Score();
        String dispStr2 = "Score = " + ps.getP2Score();

        rs.setHUD(dispStr1, 20, height / 2 + 20);
        rs.setHUD2(dispStr2, 20, 20);
        float elapsedTimeMillis = engine.getElapsedTimeMillis();
        im.update(elapsedTimeMillis);
        p1CameraController.updateCameraPosition();
        p2CameraController.updateCameraPosition();


        //Networking, process packets
        processNetworking(elapsedTimeMillis);
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
        topViewport.setDimensions(.51f, .01f, .99f, .49f);
        topViewport.setClearColor(new Color(.1f, .1f, .1f));

        Viewport bottomViewport = rw.createViewport(.01f, .01f, .99f, .49f);
        bottomViewport.setClearColor(new Color(.1f, .1f, .1f));
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
        im = new GenericInputManager();
        ArrayList<Controller> controllers = im.getControllers();

        //Player 1: Link and setup controls
        Camera player1Camera = sm.getCamera("Player1Camera");
        SceneNode p1DolphinNode = sm.getSceneNode("p1DolphinNode");
        p1CameraController = new OrbitCameraController(player1Camera, player1Camera.getParentNode(), p1DolphinNode);

        //Player 2: Link and setup controls
        Camera player2Camera = sm.getCamera("Player2Camera");
        SceneNode p2DolphinNode = sm.getSceneNode("p2DolphinNode");
        p2CameraController = new OrbitCameraController(player2Camera, player2Camera.getParentNode(), p2DolphinNode);


        //Actions
        MoveForwardAction moveForwardAction = new MoveForwardAction(p1DolphinNode,protClient);
        MoveBackwardAction moveBackwardAction = new MoveBackwardAction(p1DolphinNode, protClient);
        MoveRightAction moveRightAction = new MoveRightAction(p1DolphinNode,protClient);
        MoveLeftAction moveLeftAction = new MoveLeftAction(p1DolphinNode,protClient);
        RotateDownAction rotateDownAction = new RotateDownAction(p1DolphinNode);
        RotateUpAction rotateUpAction = new RotateUpAction(p1DolphinNode);
        RotateLeftAction rotateLeftAction = new RotateLeftAction(p1DolphinNode,protClient);
        RotateRightAction rotateRightAction = new RotateRightAction(p1DolphinNode,protClient);

        for (Controller c : controllers) {
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
                        Component.Identifier.Key.E,
                        moveRightAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.Q,
                        moveLeftAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.LCONTROL,
                        rotateDownAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.LSHIFT,
                        rotateUpAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.A,
                        rotateLeftAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );
                im.associateAction(
                        c,
                        Component.Identifier.Key.D,
                        rotateRightAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
                );

                p1CameraController.setupInput(im, c);
            }
        }

//        moveBackwardAction = new MoveBackwardAction(p2DolphinNode);
//        moveRightAction = new MoveRightAction(p2DolphinNode);
//        rotateUpAction = new RotateUpAction(p2DolphinNode);
//        rotateLeftAction = new RotateLeftAction(p2DolphinNode);
//        rotateRightAction = new RotateRightAction(p2DolphinNode);
//
//        for (Controller c : controllers) {
//            if (c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK) {
//                im.associateAction(
//                        c,
//                        Component.Identifier.Axis.Y,
//                        moveBackwardAction,
//                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
//                );
//                im.associateAction(
//                        c,
//                        Component.Identifier.Axis.X,
//                        moveRightAction,
//                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
//                );
//                im.associateAction(
//                        c,
//                        Component.Identifier.Axis.Z,
//                        rotateUpAction,
//                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
//                );
//                im.associateAction(
//                        c,
//                        Component.Identifier.Button._4,
//                        rotateLeftAction,
//                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
//                );
//                im.associateAction(
//                        c,
//                        Component.Identifier.Button._5,
//                        rotateRightAction,
//                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
//                );
//                p2CameraController.setupInput(im, c);
//            }
//        }
    }

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
        System.out.println("SetupCamera");
        SceneNode rootNode = sm.getRootSceneNode();

        //Player 1 Camera
        Camera p1Camera = sm.createCamera("Player1Camera", Projection.PERSPECTIVE);
        rw.getViewport(0).setCamera(p1Camera);
        SceneNode p1CameraNode = rootNode.createChildSceneNode(p1Camera.getName() + "Node");
        p1CameraNode.attachObject(p1Camera);
        p1Camera.setMode('n');
        p1Camera.setRt((Vector3f) Vector3f.createFrom(1.0f, 0.0f, 0.0f));
        p1Camera.setUp((Vector3f) Vector3f.createFrom(0.0f, 1.0f, 0.0f));
        p1Camera.setFd((Vector3f) Vector3f.createFrom(0.0f, 0.0f, -1.0f));
        p1Camera.setPo((Vector3f) Vector3f.createFrom(0f, 0f, 0f));

        //Player 2 Camera
        Camera p2Camera = sm.createCamera("Player2Camera", Projection.PERSPECTIVE);
        rw.getViewport(1).setCamera(p2Camera);
        SceneNode p2CameraNode = rootNode.createChildSceneNode(p2Camera.getName() + "Node");
        p2CameraNode.attachObject(p2Camera);
        p2Camera.setMode('n');
        p2Camera.setRt((Vector3f) Vector3f.createFrom(1.0f, 0.0f, 0.0f));
        p2Camera.setUp((Vector3f) Vector3f.createFrom(0.0f, 1.0f, 0.0f));
        p2Camera.setFd((Vector3f) Vector3f.createFrom(0.0f, 0.0f, -1.0f));
        p2Camera.setPo((Vector3f) Vector3f.createFrom(0f, 0f, 0f));
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

        this.sm = sm;
        this.eng = eng;


        ScriptEngineManager scriptManager = new ScriptEngineManager();
        String scriptName = "PlanetGen.js";
        List<ScriptEngineFactory> scriptList = scriptManager.getEngineFactories();
        ScriptEngine scriptEngine = scriptManager.getEngineByName("js");

        initControllers(sm);

        //p1Dolphin
        Entity dolphinE_1 = sm.createEntity("p1Dolphin", "dolphinHighPoly.obj");
        dolphinE_1.setPrimitive(Primitive.TRIANGLES);
        SceneNode dolphinN_1 = sm.getRootSceneNode().createChildSceneNode(dolphinE_1.getName() + "Node");
        dolphinN_1.moveRight(.5f);
        dolphinN_1.attachObject(dolphinE_1);

        //p2Dolphin
        Entity dolphinE_2 = sm.createEntity("p2Dolphin", "dolphinHighPoly.obj");
        dolphinE_2.setPrimitive(Primitive.TRIANGLES);
        SceneNode dolphinN_2 = sm.getRootSceneNode().createChildSceneNode(dolphinE_2.getName() + "Node");
        dolphinN_2.moveLeft(.5f);
        dolphinN_2.attachObject(dolphinE_2);

        //New texture for player two
        Texture tex = eng.getTextureManager().getAssetByPath("DolphinPink_HighPolyUV.png");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        dolphinE_2.setRenderState(texState);

        //set up Setup Point system
        ps = new PointSystem(dolphinN_1, rc, dolphinN_2, sc);

        System.out.println(dolphinN_1.getWorldRotation());

        //Scene axis
        showAxis(eng, sm);

        //Ground floor
        SceneNode groundFloor = makeGroundFloor(eng, sm);
        groundFloor.moveDown(.5f);
        groundFloor.scale(10f, 10f, 10f);


        //Make planets
        SceneNode planetsParentNode = sm.getRootSceneNode().createChildSceneNode("planetsCenterNode");

        runScript(scriptEngine, scriptName);
        int numOfPlanets = (int) scriptEngine.get("numPlanets");
        for (int i = 0; i < numOfPlanets; i++) {
            SceneNode randPlanet = generateRandPlanet(eng, sm, planetsParentNode);
            ps.addPointNode(randPlanet);
        }

        //Lighting
        sm.getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));
        Light plight = sm.createLight("testLamp1", Light.Type.POINT);
        plight.setAmbient(new Color(.5f, .5f, .5f));
        plight.setDiffuse(new Color(.9f, .9f, .9f));
        plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        plight.setRange(20f);
        SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
        plightNode.attachObject(plight);

        //Make Skybox
        SkyBox startBox = makeSkyBox("red");

        sm.setActiveSkyBox(startBox);

        setupNetworking();
        setupControls(sm);
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
        //Skybox software liscensed freely from https://github.com/wwwtyro/space-3d/blob/gh-pages/LICENSE
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

    @Override
    public void keyPressed(KeyEvent e) {
    }

    private int PlanetNum = 0;

    public SceneNode generateRandPlanet(Engine eng, SceneManager sm, Node parentNode) throws IOException {
        //Basic attributes
        float minDistance = 1f, maxDistance = 10f, minSize = .1f, maxSize = .2f;
        Random r = new Random();

        Entity planetE = sm.createEntity("myPlanet_" + PlanetNum++, "earth.obj");

        //Set up texture
        String textureName = "";
        switch (r.nextInt(5)) {
            case 0:
                textureName = "earth-day.jpeg";
                break;
            case 1:
                textureName = "earth-night.jpeg";
                break;
            case 2:
                textureName = "moon.jpeg";
                break;
            case 3:
                textureName = "red.jpeg";
                break;
            case 4:
                textureName = "sun.jpeg";
                break;
        }

        planetE.setPrimitive(Primitive.TRIANGLES);
        Texture tex = eng.getTextureManager().getAssetByPath(textureName);
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        planetE.setRenderState(texState);

        //Build node into world
        SceneNode planetN = sm.getRootSceneNode().createChildSceneNode(planetE.getName() + "Node");
        planetN.attachObject(planetE);
        parentNode.attachChild(planetN);

        //Set Random distance from origin
        float[] sizes = new float[3];
        for (int i = 0; i < sizes.length; i++) {
            float newDistance = minDistance + r.nextFloat() * (maxDistance - minDistance);
            //Polarity
            if (i != 1 && r.nextBoolean()) {
                newDistance = newDistance * -1;
            }
            sizes[i] = (newDistance);
        }
        planetN.setLocalPosition(sizes[0], sizes[1], sizes[2]);

        //Generating random scale
        float random = minSize + r.nextFloat() * (maxSize - minSize);
        planetN.setLocalScale(random, random, random);
        //Returning value
        return planetN;
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

    public ManualObject makeXIndicator(Engine eng, SceneManager sm) throws IOException {
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

    public ManualObject makeYIndicator(Engine eng, SceneManager sm) throws IOException {
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

    public ManualObject makeZIndicator(Engine eng, SceneManager sm) throws IOException {
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

    protected ManualObject makePyramid(Engine eng, SceneManager sm) throws IOException {
        ManualObject pyr = sm.createManualObject("Pyramid");
        ManualObjectSection pyrSec =
                pyr.createManualSection("PyramidSection");
        pyr.setGpuShaderProgram(sm.getRenderSystem().
                getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
        float[] vertices = new float[]
                {-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //front
                        1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //right
                        1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //back
                        -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //left
                        -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, //LF
                        1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f //RR
                };
        float[] texcoords = new float[]
                {0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                        1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
                };
        float[] normals = new float[]
                {0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f,
                        -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f
                };
        int[] indices = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
        FloatBuffer texBuf = BufferUtil.directFloatBuffer(texcoords);
        FloatBuffer normBuf = BufferUtil.directFloatBuffer(normals);
        IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
        pyrSec.setVertexBuffer(vertBuf);
        pyrSec.setTextureCoordsBuffer(texBuf);
        pyrSec.setNormalsBuffer(normBuf);
        pyrSec.setIndexBuffer(indexBuf);
        Texture tex = eng.getTextureManager().getAssetByPath("chain-fence.jpeg");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().
                createRenderState(RenderState.Type.FRONT_FACE);
        pyr.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        pyr.setRenderState(texState);
        pyr.setRenderState(faceState);
        return pyr;
    }

    public void setIsConnected(boolean b) {

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
        Entity dolphinE_2 = sm.createEntity("p2Dolphin" + uuid.toString(), "dolphinHighPoly.obj");
        SceneNode dolphinN_2 = sm.getRootSceneNode().createChildSceneNode(dolphinE_2.getName() + "Node");

        dolphinE_2.setPrimitive(Primitive.TRIANGLES);
        dolphinN_2.attachObject(dolphinE_2);

        Texture tex = eng.getTextureManager().getAssetByPath("DolphinPink_HighPolyUV.png");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        dolphinE_2.setRenderState(texState);

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

}
