
package mygame;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.system.AppSettings;

public class Main extends SimpleApplication {
    
    
    Node sown;
    CameraNode camNode;
    
    private AnimComposer animComposer;
    
    private BetterCharacterControl playerSown;
    private Vector3f walkDirection = new Vector3f(0,0,0); // stop
    
    private boolean adelante=false,atras=false,izq=false,der=false;
    
    float velocidadCam = 1.5f;
    float velocidad = 30f; // Velocidad de movimiento
    

    boolean dashActivado = false;
    boolean dashDirectionSet = false; // Indica si la dirección del dash ha sido establecida
    float duracionDash = 0.5f; // Duración del dash en s
    float cooldownDash = 2.5f; // Tiempo de enfriamiento del dash s
    float tiempoTranscurridoDash = 0f; // Tiempo desde que se activó el Dash
    float tiempoTranscurridoCooldown = 0f; // Tiempo desde que se desactivó el Dash
    float velocidadDash = 30f; // Velocidad de dash 

    BulletAppState fisica; // Declaración de la variable bulletAppState

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
        //fisica.setDebugEnabled(true);

        setupKeys();
        setupLight();
        setupPlayer();
        setupMap();
        setupCamera();
    }
    
    private void setupPlayer() {
        Spatial sown_model = assetManager.loadModel("Models/SownFinal/SownFinal.j3o");
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
        
        sown.setLocalTranslation(-30f, 0, -19f);
        
        sown.addControl(playerSown);
        playerSown.setGravity(new Vector3f(0, -9.81f, 0)); // -9.81f es la aceleración gravitacional estándar en la Tierra

        // Agregar el control al BulletAppState para manejar las colisiones
        fisica.getPhysicsSpace().add(playerSown);
        
    }
    
    private void setupMap() {
        Node map = new Node("map_node");
        Spatial map_model = assetManager.loadModel("Models/scene.j3o");
        map_model.setLocalScale(10f);
        
        map.attachChild(map_model);
        
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(map_model);
        
        RigidBodyControl mapControl = new RigidBodyControl(sceneShape, 0);
        map_model.addControl(mapControl);
        
        // Agrega el RigidBodyControl al BulletAppState
        fisica.getPhysicsSpace().add(mapControl);
        
        rootNode.attachChild(map);
    }

    private void setupKeys() {
        inputManager.addMapping("Dash", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addListener(actionListener, "Dash");

        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_D));

        inputManager.addListener(actionListener, "Mover_Adelante", "Mover_Atras");
        inputManager.addListener(actionListener,"Mover_Izq", "Mover_Der");
    }

    private void setupLight() {
        // We add light so we see the scene
        
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.5f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White.mult(1.5f));
        dl.setDirection(new Vector3f(-1f, -1f, -1f).normalizeLocal());
        rootNode.addLight(dl);
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
            }
        }
    };

    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            
        }
    };

    private void handleDash(float tpf) {
        
    }
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
        handleDash(tpf);
    }

    @Override
    public void simpleUpdate(float tpf) {
        handleMovement(tpf);
        
    }
    /* Dash sin desaparecer
    @Override
    public void simpleUpdate(float tpf) {
        if (dashActivado) {
            tiempoTranscurridoDash += tpf;
            if (tiempoTranscurridoDash >= duracionDash) {
                dashActivado = false;
                tiempoTranscurridoCooldown = 0f; // Reiniciar el cooldown
            } else {
                if (!dashDirectionSet) { // Si la dirección del dash no está establecida
                    direccionDash = cam.getDirection().clone().multLocal(1, 0, 1).normalizeLocal();
                    direccionDash.multLocal(velocidadDash); // Multiplicar por la velocidad del dash
                    dashDirectionSet = true; // Marcar la dirección del dash como establecida
                }
                sown.move(direccionDash.mult(tpf));
            }
        } else {
            tiempoTranscurridoCooldown += tpf;
        }
    }
    */

    @Override
    public void simpleRender(RenderManager rm) {
        // TODO: add render code
    }
}
