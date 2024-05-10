package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
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
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.util.SkyFactory;

public class Main extends SimpleApplication {

    Node sown;
    CameraNode camNode;

    float velocidadCam = 200;
    float velocidad = 0.6f; // Velocidad de movimiento

    boolean dashActivado = false;
    boolean dashDirectionSet = false; // Indica si la dirección del dash ha sido establecida
    float duracionDash = 0.5f; // Duración del dash en s
    float cooldownDash = 2.5f; // Tiempo de enfriamiento del dash s
    float tiempoTranscurridoDash = 0f; // Tiempo desde que se activó el Dash
    float tiempoTranscurridoCooldown = 0f; // Tiempo desde que se desactivó el Dash
    float velocidadDash = 30f; // Velocidad de dash 

    BulletAppState bulletAppState; // Declaración de la variable bulletAppState

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        setupKeys();
        setupLight();
        setupPlayer();
        setupMap();
        setupCamera();
    }

    private void setupKeys() {
        inputManager.addMapping("Dash", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addListener(actionListener, "Dash");

        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_D));

        inputManager.addListener(analogListener, "Mover_Adelante", "Mover_Atras", "Mover_Izq", "Mover_Der");
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

    private void setupPlayer() {
        Spatial sown_model = assetManager.loadModel("Models/sown.j3o");
        sown_model.setLocalScale(0.5f);

        sown = new Node("sown_node");
        sown.setLocalTranslation(-30f,0, -19f);
        sown.attachChild(sown_model);
        rootNode.attachChild(sown);
        
    }

    private void setupMap() {
        Node map = new Node("map_node");
        Spatial map_model = assetManager.loadModel("Models/scene.j3o");
        map.attachChild(map_model);
        map.setLocalScale(10f);
        rootNode.attachChild(map);
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(map_model);
        RigidBodyControl mapControl = new RigidBodyControl(sceneShape, 0);
        map_model.addControl(mapControl);
    }

    private void setupCamera() {
        flyCam.setEnabled(false);

        camNode = new CameraNode("Camera Node", cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        sown.attachChild(camNode);
        camNode.setLocalTranslation(new Vector3f(0, 3, -10));
        camNode.lookAt(sown.getLocalTranslation(), Vector3f.UNIT_Y);
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Dash") && isPressed && !dashActivado && tiempoTranscurridoCooldown >= cooldownDash) {
                dashActivado = true;
                tiempoTranscurridoDash = 0f;
                dashDirectionSet = false; // Reiniciar la dirección del dash
            }
        }
    };

    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (name.equals("Mover_Adelante")) {
                
                Vector3f direccionCamara = cam.getDirection().clone().multLocal(1, 0, 1).normalizeLocal();
                direccionCamara.multLocal(velocidad);
                sown.move(direccionCamara);
            }
            if (name.equals("Mover_Atras")) {
                Vector3f direccionCamara = cam.getDirection().clone().multLocal(1, 0, 1).normalizeLocal();
                direccionCamara.multLocal(-velocidad*0.3f);
                sown.move(direccionCamara);
            }
            if (name.equals("Mover_Izq")) {
                sown.rotate(0, velocidadCam * (value * tpf), 0); // Girar hacia la izquierda
            }
            if (name.equals("Mover_Der")) {
                sown.rotate(0, -velocidadCam *(value * tpf), 0); // Girar hacia la derecha
            }
        }
    };

    private void handleDash(float tpf) {
        if (dashActivado) {
            tiempoTranscurridoDash += tpf;
            if (tiempoTranscurridoDash >= duracionDash) {
                dashActivado = false;
                tiempoTranscurridoCooldown = 0f; // Reiniciar el cooldown
            } else {
                if (!dashDirectionSet) { // Si la dirección del dash no está establecida
                    Vector3f direccionCamara = cam.getDirection().clone().multLocal(1, 0, 1).normalizeLocal();
                    direccionCamara.multLocal(velocidadDash); // Multiplicar por la velocidad del dash
                    dashDirectionSet = true; // Marcar la dirección del dash como establecida
                    sown.setLocalTranslation(sown.getLocalTranslation().add(direccionCamara)); // Mover el personaje en la dirección de la cámara
                }
            }
        } else {
            tiempoTranscurridoCooldown += tpf;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        handleDash(tpf);
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
