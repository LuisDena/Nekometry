package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Box;

public class Main extends SimpleApplication {

    Node sown;
    CameraNode camNode;
    
    float velocidadMov = 10f;
    
    boolean dashActivado = false;
    float duracionDash = 0.5f; // Duración del dash en s
    float cooldownDash = 2.5f; // Tiempo de enfriamiento del dash s
    float tiempoTranscurridoDash = 0f; // Tiempo desde que se activó el Dash
    float tiempoTranscurridoCooldown = 0f; // Tiempo desde que se desactivó el Dash
    
    BulletAppState bulletAppState; // Declaración de la variable bulletAppState

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //Dash
        inputManager.addMapping("Dash", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addListener(actionListener, "Dash");

        //Asignar las teclas de movimiento del personaje
        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_D));
        
        //Añadir los Listeners de movimiento
        inputManager.addListener(analogListener, "Mover_Adelante", "Mover_Atras",
                "Mover_Izq", "Mover_Der");
        
        
        // Crear y añadir una luz direccional
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-2f, -1f, -1).normalizeLocal());
        rootNode.addLight(dl);

        // Cargar el modelo con su material desde el archivo 
        Spatial sown_model = assetManager.loadModel("Models/sown.j3o");
        sown_model.setLocalScale(0.5f);
        
        
        //Agregar físicas/Colisiones al personaje
        
        // Crear un nodo para el modelo y agregarlo al nodo raíz
        sown = new Node("sown_node");
        sown.setLocalTranslation(-30f, 0, -19f);
        sown.attachChild(sown_model);
        rootNode.attachChild(sown);
        
        //Mapa
        Node map = new Node("map_node");
        Spatial map_model = assetManager.loadModel("Models/scene.j3o");
        map.attachChild(map_model);
        map.setLocalScale(10f);
        rootNode.attachChild(map);

        // Añadir físicas/colisiones al mapa
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        RigidBodyControl mapControl = new RigidBodyControl(0f); // Masa cero para que el mapa no sea afectado por la gravedad
        map.addControl(mapControl);
        bulletAppState.getPhysicsSpace().add(mapControl);
        
        // Cámara
        // Disable the default flyby cam
        flyCam.setEnabled(false);
        //create the camera Node
        camNode = new CameraNode("Camera Node", cam);
        //This mode means that camera copies the movements of the target:
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        //Attach the camNode to the target:
        sown.attachChild(camNode);
        //Move camNode, e.g. behind and above the target:
        camNode.setLocalTranslation(new Vector3f(0, 3, -7));
        //Rotate the camNode to look at the target:
        camNode.lookAt(sown.getLocalTranslation(), Vector3f.UNIT_Y);
    }
    
    // Action para acciones absolutas, presionado o soltado, encendido o apagado
    //Ejemplos: pausar/reanudar, un disparo de rifle o revólver, saltar, hacer clic para seleccionar.
    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Dash") && isPressed && !dashActivado 
                    && tiempoTranscurridoCooldown >= cooldownDash) {
                dashActivado = true;
                tiempoTranscurridoDash = 0f;
            }
        }
    };

    // Analog para movimientos continuos o progresivos
    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (name.equals("Mover_Adelante")) {
                float velocidad = 10f; // Velocidad de movimiento
                sown.move(0, 0, velocidad * value);
            }
            switch (name) {
                case "Mover_Adelante":
                    sown.move(0, 0, velocidadMov * value );
                    break;
                case "Mover_Atras":
                    sown.move(0, 0, -velocidadMov * value );
                    break;
                case "Mover_Izq":
                    sown.move(velocidadMov * value , 0,0 );
                    break;
                case "Mover_Der":
                    sown.move(-velocidadMov * value, 0,0 );
                    break;
                default:
                    break;
            }
            
        }
    };
    
    @Override
    public void simpleUpdate(float tpf) {
        if (dashActivado) {
            tiempoTranscurridoDash += tpf;
            if (tiempoTranscurridoDash >= duracionDash) {
                dashActivado = false;
                tiempoTranscurridoCooldown = 0f; // Reiniciar el cooldown
            } else {
                // Lógica de movimiento durante el dash
                float velocidadDash = 30f; // Velocidad de dash 
                sown.move(0, 0, velocidadDash * tpf);
            }
        } else {
            tiempoTranscurridoCooldown += tpf;
        }
    }   

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
