package mygame;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;

import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
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
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class Main extends SimpleApplication {
    
    //spawncoords1= 90f, 0.1f, -30f
    //spawncoords2= -51f, 0.1f, 90f
    //spawncoords3= -50f, 0.1f, -90f
    //spawncoords4= -100f, 0.1f, -30f
    
    private Node sown;
    private CameraNode camNode;
    
    private AnimComposer animComposer;
    
    private BetterCharacterControl playerSown;
    
    private boolean adelante=false,atras=false,izq=false,der=false, jump=false;
    
    float velocidad = 15f; // Velocidad de movimiento

    private BulletAppState fisica; // Declaración de la variable bulletAppState
    
    private long lastShootTime = 0;
    private static final long SHOOT_COOLDOWN = 500; 


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
        spawnEnemies(1);
        setupKeys();
        setupPlayer();
        setupLight();
        setupMap();
        setupBoundaries();
        setupCamera();
        
        
    }
    
    private void setupPlayer() {
        Spatial sown_model = assetManager.loadModel("Models/SownFinalTest/SownFinalTest.j3o");

        sown_model.setLocalScale(0.4f);

        sown = new Node("sown_node");
        
        sown.attachChild(sown_model);

        rootNode.attachChild(sown);
        // Obtener el AnimComposer del modelo
        animComposer = sown_model.getControl(AnimComposer.class);
        
        //-------------------------------------radio,altura,forma
        //playerSown = new BetterCharacterControl(0.6f,1.3f,0.4f); //forma
        playerSown = new BetterCharacterControl(0.6f,1.3f,25f);
        
        sown.setLocalTranslation(-30f, 1.6f, -19f);
        
        sown.addControl(playerSown);
        
        playerSown.setGravity(new Vector3f(0, -9.8f, 0));
        // -9.81f es la aceleración gravitacional estándar en la Tierra

        // Agregar el control al BulletAppState para manejar las colisiones
        fisica.getPhysicsSpace().add(playerSown);
        
    }
    
    private void spawnEnemies(int difficulty) {
        int spawnRate = 0;

        // Calcular el spawnRate según la dificultad
        switch (difficulty) {
            case 1: // Fácil
                spawnRate = 2; // Por ejemplo, un spawnRate bajo para fácil
                break;
            case 2: // Normal
                spawnRate = 4; // Por ejemplo, un spawnRate medio para normal
                break;
            case 3: // Difícil
                spawnRate = 6; // Por ejemplo, un spawnRate alto para difícil
                break;
            default:
                spawnRate = 2; // Por defecto, un spawnRate bajo
                break;
        }

        int numEnemies = spawnRate * difficulty; // Cálculo de la cantidad de enemigos
        Random random = new Random();

        // Generar los enemigos en el punto deseado
        for (int i = 0; i < numEnemies; i++) {
            Spatial enemy = assetManager.loadModel("Models/enemy/enemy.j3o");
            enemy.setName("enemy");
            // Configurar posición del enemigo en el punto deseado
            enemy.setLocalTranslation(-100f, 1.1f, -30f); // Usar las coordenadas adecuadas

            // Asignar una velocidad aleatoria entre 2f y 6f
            enemy.setUserData("speed", 2f + random.nextFloat() * 4f);

            // Crear una forma de colisión para el cubo del enemigo
            CollisionShape enemyShape = CollisionShapeFactory.createBoxShape(enemy);
            // Crear un RigidBodyControl para el enemigo y añadirlo
            RigidBodyControl enemyControl = new RigidBodyControl(enemyShape, 1f); // Masa de 1f
            enemy.addControl(enemyControl);

            // Añadir el control de física al espacio de física
            fisica.getPhysicsSpace().add(enemyControl);

            rootNode.attachChild(enemy); // Agregar el cubo del enemigo al nodo principal de la escena
        }
    }
    
    private void updateEnemies(float tpf) {
        Vector3f portalPos = rootNode.getChild("portal_node").getWorldTranslation(); // Obtener la posición del portal

        // Recorrer todos los nodos hijos del rootNode para buscar los cubos de los enemigos
        for (Spatial spatial : rootNode.getChildren()) {
            if (spatial.getName().equals("enemy")) { // Buscar cubos de enemigos
                // Calcular la dirección hacia la que deben moverse los enemigos (hacia el portal)
                Vector3f enemyPos = spatial.getWorldTranslation();
                Vector3f directionToPortal = portalPos.subtract(enemyPos).normalizeLocal();

                // Obtener la velocidad del enemigo
                float speed = spatial.getUserData("speed");

                // Mover el cubo del enemigo en la dirección calculada
                RigidBodyControl enemyControl = spatial.getControl(RigidBodyControl.class);
                if (enemyControl != null) {
                    enemyControl.setLinearVelocity(directionToPortal.mult(speed)); // Multiplicar por la velocidad del enemigo
                }
            }
        }
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
        
        // Agregar emisor de partículas al portal
        ParticleEmitter particleEmitter = new ParticleEmitter("PortalParticles", ParticleMesh.Type.Triangle, 30);
        Material particleMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        //particleMat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        particleEmitter.setMaterial(particleMat);
        particleEmitter.setImagesX(2);
        particleEmitter.setImagesY(2);
        particleEmitter.setEndColor(new ColorRGBA(0.8f, 0, 0.8f, 1)); // Color morado
        particleEmitter.setStartColor(new ColorRGBA(0.8f, 0, 0.8f, 0.5f)); // Color morado con transparencia
        particleEmitter.setStartSize(1.5f);
        particleEmitter.setEndSize(0.1f);
        particleEmitter.setGravity(0, 0, 0);
        particleEmitter.setLowLife(0.5f);
        particleEmitter.setHighLife(10f);
        particleEmitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0)); // Velocidad inicial
        particleEmitter.getParticleInfluencer().setVelocityVariation(0.3f); // Variación de velocidad

        portal.attachChild(particleEmitter);
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
        inputManager.addMapping("Mover_Adelante", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Mover_Atras", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Mover_Izq", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Mover_Der", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        
        
        inputManager.addListener(actionListener, "Dash", "Jump");
        inputManager.addListener(actionListener, "Mover_Adelante", "Mover_Atras");
        inputManager.addListener(actionListener,"Mover_Izq", "Mover_Der");
        inputManager.addListener(actionListener,"Attack");
    }

    private void setupLight() {
        // Luz direccional principal
        DirectionalLight mainLight = new DirectionalLight();
        mainLight.setColor(ColorRGBA.LightGray.mult(0.8f)); // Aumentar la intensidad para una iluminación más brillante
        mainLight.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        rootNode.addLight(mainLight);

        // Configurar sombras en la luz direccional
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 3);
        dlsr.setLight(mainLight);
        dlsr.setShadowIntensity(0.5f); // Ajustar la intensidad de las sombras
        dlsr.setShadowZExtend(150f); // Aumentar la distancia de renderizado de sombras
         // Ajustar la distancia de atenuación de sombras
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
            }else if (name.equals("Attack") && isPressed) {
                handleProjectileAttack();
            }
        }
    };
    
    public void handleProjectileAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime < SHOOT_COOLDOWN) {
            return; // Salir si el cooldown no ha terminado
        }
        lastShootTime = currentTime;
        enqueue(() -> {
            
            // Crea y configura el proyectil
            Geometry projectile = new Geometry("Projectile", new Box(0.2f, 0.1f, 0.1f));
            Material projectileMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            projectileMat.setColor("Color", ColorRGBA.Red);
            projectile.setMaterial(projectileMat);

            Vector3f playerPos = sown.getWorldTranslation();
            Vector3f projectilePos = playerPos.add(cam.getDirection().mult(2f)).addLocal(0, 1f, 0f);
            projectile.setLocalTranslation(projectilePos);

            RigidBodyControl projectileControl = new RigidBodyControl(1f);
            projectile.addControl(projectileControl);

            Vector3f projectileDir = playerSown.getViewDirection().normalize().mult(100f);
            projectileControl.setLinearVelocity(projectileDir);

            rootNode.attachChild(projectile);
            fisica.getPhysicsSpace().add(projectileControl);

            // Crea un executor para gestionar la eliminación del proyectil después de 2 segundos
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
            executor.schedule(() -> {
                enqueue(() -> {
                    // Remueve el proyectil del nodo de la escena y del espacio de física
                    rootNode.detachChild(projectile);
                    fisica.getPhysicsSpace().remove(projectileControl);

                    // Limpia el proyectil y su control
                    projectile.removeControl(projectileControl);
                    projectileControl.setEnabled(false);
                    projectile.removeFromParent();

                    // Cierra el executor
                    executor.shutdown();
                    return null;
                });
            }, 2000, java.util.concurrent.TimeUnit.MILLISECONDS);

            return null; // Necesario para Callable
        });
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
        updateEnemies(tpf);
        handleMovement(tpf);
    }
    
    @Override
    public void simpleRender(RenderManager rm) {
        // TODO: add render code
        
    }
}