package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.ActionListener;
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
    boolean moverAdelante = false;
    boolean moverAtras = false;
    boolean moverIzq = false;
    boolean moverDer = false;
    
    boolean dashActivado = false;
    float duracionDash = 0.5f; // Duración del dash en s
    float cooldownDash = 2.5f; // Tiempo de enfriamiento del dash s
    float tiempoTranscurridoDash = 0f; // Tiempo desde que se activó el Dash
    float tiempoTranscurridoCooldown = 0f; // Tiempo desde que se desactivó el Dash

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //flyCam.setEnabled(false);
        flyCam.setMoveSpeed(10);
        
        inputManager.addMapping("Dash", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addListener(actionListener, "Dash");

        //Asignar las teclas de movimiento
        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_D));
        
        //Añadir los Listeners
        inputManager.addListener(actionListener, "Mover_Adelante", "Mover_Atras",
                "Mover_Izq", "Mover_Der");
        
        // Crear y añadir una luz direccional
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -1).normalizeLocal());
        rootNode.addLight(dl);

        // Cargar el modelo con su material desde el archivo 
        Spatial sown_model = assetManager.loadModel("Models/sown.j3o");
        sown_model.setLocalScale(0.5f);
        
        // Crear un nodo para el modelo y agregarlo al nodo raíz
        sown = new Node("sown_node");
        sown.attachChild(sown_model);
        rootNode.attachChild(sown);
        
        // Crear el suelo
        Box floorMesh = new Box(20, 0.1f, 20);
        Geometry floorGeometry = new Geometry("Floor", floorMesh);
        Material floorMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMaterial.setColor("Color", ColorRGBA.Gray);
        floorGeometry.setMaterial(floorMaterial);
        rootNode.attachChild(floorGeometry);

        // Posicionar el suelo debajo del personaje
        floorGeometry.setLocalTranslation(0, -0.1f, 0);
        
        //camara
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
    
    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Dash") && isPressed && !dashActivado 
                    && tiempoTranscurridoCooldown >= cooldownDash) {
                dashActivado = true;
                tiempoTranscurridoDash = 0f;
            }
            switch (name) {
                case "Mover_Adelante":
                    moverAdelante = isPressed;
                    break;
                case "Mover_Atras":
                    moverAtras = isPressed;
                    break;
                case "Mover_Izq":
                    moverIzq = isPressed;
                    break;
                case "Mover_Der":
                    moverDer = isPressed;
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf) {
        float vel_mov_normal = 8f; // Velocidad de movimiento normal
        float vel_mov_dash = 30f; // Velocidad de movimiento del Dash

        if (dashActivado) {
            tiempoTranscurridoDash += tpf;
            if (tiempoTranscurridoDash >= duracionDash) {
                dashActivado = false;
                tiempoTranscurridoCooldown = 0f; // Reiniciar el cooldown
            }
        } else {
            tiempoTranscurridoCooldown += tpf;
        }

        float vel_mov_actual;//La velocidad actual
        if (dashActivado) {
            vel_mov_actual = vel_mov_dash;
        } else {
            vel_mov_actual = vel_mov_normal;
        } 

        // Mover hacia adelante 
        if (moverAdelante) {
            sown.move(0, 0, vel_mov_actual * tpf);
        }

        // Mover hacia atrás 
        if (moverAtras) {
            sown.move(0, 0, -vel_mov_actual * tpf);
        }

        // Mover a la izquierda 
        if (moverIzq) {
            sown.move(vel_mov_actual * tpf, 0, 0);
        }

        // Mover a la derecha 
        if (moverDer) {
            sown.move(-vel_mov_actual * tpf, 0, 0);
        }
    }   

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
