package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.ActionListener;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class Main extends SimpleApplication {

    Node sown;
    boolean mover_Adelante = false;
    boolean mover_Atras = false;
    boolean mover_Izq = false;
    boolean mover_Der = false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10f);
        //Asignar las teclas de movimiento
        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_L));
        
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
        
        // Definir el material y asignarlo
        

        // Crear un nodo para el modelo y agregarlo al nodo raíz
        sown = new Node("sown_node");
        sown.attachChild(sown_model);
        rootNode.attachChild(sown);
        
        // Crear el suelo pa probar
        Box floorMesh = new Box(20, 0.1f, 20);
        Geometry floorGeometry = new Geometry("Floor", floorMesh);
        Material floorMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMaterial.setColor("Color", ColorRGBA.Gray);
        floorGeometry.setMaterial(floorMaterial);
        rootNode.attachChild(floorGeometry);

        // Colocar el suelo debajo del personaje
        floorGeometry.setLocalTranslation(0, -0.1f, 0);
        
        // Actualizar la posición de la cam para que siga al personaje
        cam.setLocation(sown.getWorldTranslation().add(new Vector3f(0, 7, -5)));
        // Hacer que la cámara mire al personaje
        cam.lookAt(sown.getWorldTranslation(), Vector3f.UNIT_Y); 
    }
    
    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            switch (name) {
                case "Mover_Adelante":
                    mover_Adelante = isPressed;
                    break;
                case "Mover_Atras":
                    mover_Atras = isPressed;
                    break;
                case "Mover_Izq":
                    mover_Izq = isPressed;
                    break;
                case "Mover_Der":
                    mover_Der = isPressed;
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf) {
        float vel_mov = 10f; // Velocidad de movimiento del personaje

        // Mover hacia adelante 
        if (mover_Adelante) {
            sown.move(0, 0, vel_mov * tpf);
        }

        // Mover hacia atrás 
        if (mover_Atras) {
            sown.move(0, 0, -vel_mov * tpf);
        }

        // Mover a la izquierda
        if (mover_Izq) {
            sown.move(vel_mov * tpf, 0, 0);
        }

        // Mover a la derecha 
        if (mover_Der) {
            sown.move(-vel_mov * tpf, 0, 0);
        }   
        
        // Actualizar la posición de la cámara para seguir al personaje en tercera persona
        // Distancia detrás del personaje
        Vector3f camDir = cam.getDirection().clone().multLocal(-5); 
        Vector3f camOffset = sown.getWorldTranslation().add(new Vector3f(0, 7, -4)); // Offset vertical
        cam.setLocation(camOffset.add(camDir));
        cam.lookAt(sown.getWorldTranslation(), Vector3f.UNIT_Y); // Hacer que la cámara mire al personaje
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
