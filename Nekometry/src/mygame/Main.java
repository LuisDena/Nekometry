package mygame;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;

import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;


public class Main extends SimpleApplication {
    
    
    private Node sown;
    private CameraNode camNode;
    
    private AnimComposer animComposer;
    
    private BetterCharacterControl playerSown;
    
    private boolean adelante=false,atras=false,izq=false,der=false, jump=false;
    
    float velocidad = 15f; // Velocidad de movimiento

    private BulletAppState fisica; // Declaración de la variable bulletAppState

    public static void main(String[] args) {
        
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Nekometry");
        settings.setSettingsDialogImage("Interface/NekoIcon.png");
        settings.setVSync(false);
        
        Main app = new Main();
        
        app.setSettings(settings);
        app.start();
    }
    

    @Override
    public void simpleInitApp() {
        
        /** Set up Physics */
        fisica = new BulletAppState();
        stateManager.attach(fisica);
        fisica.setDebugEnabled(true);
        
        //getRootNode().attachChild(SkyFactory.createSky(getAssetManager(),
        //        "Models/cielo.jpg", SkyFactory.EnvMapType.CubeMap));
        
        setupKeys();
        setupPlayer();
        setupLight();
        setupMap();
        setupBoundaries();
        setupCamera();
        
        
    }
    
    private void setupPlayer() {
        //Spatial sown_model = assetManager.loadModel("Models/SownFinal/SownFinal.j3o");
        Spatial sown_model = assetManager.loadModel("Models/SownFinalTest/SownFinalTest.j3o");

        sown_model.setLocalScale(0.4f);

        sown = new Node("sown_node");
        
        sown.attachChild(sown_model);

        rootNode.attachChild(sown);
        // Obtener el AnimComposer del modelo
        animComposer = sown_model.getControl(AnimComposer.class);

        // Agregar BetterCharacterControl al personaje con un BoxCollisionShape
        
        //-------------------------------------radio,altura,forma
        playerSown = new BetterCharacterControl(0.6f,1.3f,0.4f); //forma
        //playerSown.warp(new Vector3f(-20, 0, -19));
        
        sown.setLocalTranslation(-30f, 1.6f, -19f);
        
        sown.addControl(playerSown);
        
        playerSown.setGravity(new Vector3f(0, -9.8f, 0));
        // -9.81f es la aceleración gravitacional estándar en la Tierra

        // Agregar el control al BulletAppState para manejar las colisiones
        fisica.getPhysicsSpace().add(playerSown);
        
    }
    
    private void setupMap() {
        Node map = new Node("map_node");
        Spatial map_model = assetManager.loadModel("Models/scene/scene.j3o");
        map_model.setLocalScale(10f);
        
        map.attachChild(map_model);
        
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(map_model);
        
        RigidBodyControl mapControl = new RigidBodyControl(sceneShape, 0f);
        map_model.addControl(mapControl);
        
        // Agrega el RigidBodyControl al BulletAppState
        fisica.getPhysicsSpace().add(mapControl);
        setupPortalMap();
        rootNode.attachChild(map);
    }
    
    private void setupPortalMap(){        
        Node portal = new Node("portal_node");
        Spatial portal_model = assetManager.loadModel("Models/portal/portal.j3o");
        portal.setLocalTranslation(-30f, -0.7f, -19f);
        portal_model.setLocalScale(2f);
        
        portal.attachChild(portal_model);
        
        CollisionShape portalShape =
                CollisionShapeFactory.createMeshShape(portal_model);
        
        RigidBodyControl portalControl = new RigidBodyControl(portalShape,0);
        portal_model.addControl(portalControl);
        
        // Agrega el RigidBodyControl al BulletAppState
        fisica.getPhysicsSpace().add(portalControl);
        
       
        rootNode.attachChild(portal);
    }
    
    private void setupBoundaries() {
        // Crear cajas invisibles para los límites del mapa
        float boundaryHeight = 50f;
        float boundaryThickness = 1f;

        // Caja frontal
        createBoundary(new Vector3f(0, boundaryHeight / 2, -100), new Vector3f(100, boundaryHeight, boundaryThickness));
        // Caja trasera
        createBoundary(new Vector3f(0, boundaryHeight / 2, 100), new Vector3f(100, boundaryHeight, boundaryThickness));
        // Caja izquierda
        createBoundary(new Vector3f(-100, boundaryHeight / 2, 0), new Vector3f(boundaryThickness, boundaryHeight, 150));
        // Caja derecha
        createBoundary(new Vector3f(100, boundaryHeight / 2, 0), new Vector3f(boundaryThickness, boundaryHeight, 150));
    }

    private void createBoundary(Vector3f location, Vector3f dimensions) {
        BoxCollisionShape boxShape = new BoxCollisionShape(dimensions);
        RigidBodyControl boxControl = new RigidBodyControl(boxShape, 0);
        Node boxNode = new Node("Boundary");
        boxNode.setLocalTranslation(location);
        boxNode.addControl(boxControl);
        rootNode.attachChild(boxNode);
        fisica.getPhysicsSpace().add(boxControl);
    }

    private void setupKeys() {
        inputManager.addMapping("Dash", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE)); 
        inputManager.addListener(actionListener, "Dash", "Jump");

        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_D));

        inputManager.addListener(actionListener, "Mover_Adelante", "Mover_Atras");
        inputManager.addListener(actionListener,"Mover_Izq", "Mover_Der");
    }

    private void setupLight() {
        // Luz ambiental
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White.mult(0.7f)); // Reducir la intensidad para evitar sobreiluminación
        rootNode.addLight(ambientLight);

        // Luz direccional principal
        DirectionalLight mainLight = new DirectionalLight();
        mainLight.setColor(ColorRGBA.LightGray.mult(0.8f)); // Aumentar la intensidad para una iluminación más brillante
        mainLight.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        rootNode.addLight(mainLight);

        // Configurar sombras en la luz direccional
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 3);
        dlsr.setLight(mainLight);
        dlsr.setShadowIntensity(0.5f); // Ajustar la intensidad de las sombras
        dlsr.setShadowZExtend(50f); // Aumentar la distancia de renderizado de sombras
        dlsr.setShadowZFadeLength(50f); // Ajustar la distancia de atenuación de sombras
        viewPort.addProcessor(dlsr);

        // Configurar sombras en los objetos
        rootNode.setShadowMode(ShadowMode.CastAndReceive);
        sown.setShadowMode(ShadowMode.CastAndReceive);
        // Otros objetos que deseen lanzar y recibir sombras también deben configurarse con el modo adecuado
    }

    private void setupCamera() {
        flyCam.setEnabled(true);
        
        camNode = new CameraNode("Camera Node", cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        sown.attachChild(camNode);
        camNode.setLocalTranslation(new Vector3f(0, 3, -10));
        camNode.lookAt(sown.getLocalTranslation(), Vector3f.UNIT_Y);
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Mover_Izq")) {
                izq = isPressed;
            } else if (name.equals("Mover_Der")) {
                der = isPressed;
            } else if (name.equals("Mover_Adelante")) {
                adelante = isPressed;
            } else if (name.equals("Mover_Atras")) {
                atras = isPressed;
            }else if (name.equals("Jump") && isPressed) {
                jump = true;
            }
        }
    };
    

    private void handleMovement(float tpf) {
        // Calcula la dirección de movimiento basada en la entrada del usuario
        Vector3f moveDirection = new Vector3f(0, 0, 0);
        if (adelante) {
            Vector3f camDir = cam.getDirection().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camDir);
        }
        if (atras) {
            Vector3f camOppositeDir = cam.getDirection().negate().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camOppositeDir);
        }
        if (izq) {
            Vector3f camLeft = cam.getLeft().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camLeft);
        }
        if (der) {
            Vector3f camRight = cam.getLeft().negate().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camRight);
        }
        if (jump) {// Manejar el salto
            if (playerSown.isOnGround()) { // Salta solo si está en el suelo
                playerSown.jump();
            }
            jump = false; // Resetea el estado de salto
        }

        // Normaliza la dirección de movimiento para mantener la misma velocidad en todas las direcciones
        if (!moveDirection.equals(Vector3f.ZERO)) {
            moveDirection.normalizeLocal();
            moveDirection.multLocal(velocidad); // Multiplica por la velocidad de movimiento
        }
        // Obtiene la dirección de la cámara y rota al personaje hacia esa dirección
        Vector3f camDir = cam.getDirection().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
        playerSown.setViewDirection(camDir);
        
        // Establece la dirección de movimiento al personaje
        playerSown.setWalkDirection(moveDirection);
    }

    @Override
    public void simpleUpdate(float tpf) {
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        // Aplicar los filtros al ViewPort
        viewPort.addProcessor(fpp);
        handleMovement(tpf);
        
    }
    
    @Override
    public void simpleRender(RenderManager rm) {
        // TODO: add render code
    }
}
