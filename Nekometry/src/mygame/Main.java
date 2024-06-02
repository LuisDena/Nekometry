package mygame;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
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
import com.jme3.texture.Texture;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class Main extends SimpleApplication {
    // Personaje 
    private Node sown;
    private CameraNode camNode;
    private BetterCharacterControl playerSown;
    
    // Controles
    private boolean adelante=false,atras=false,izq=false,der=false, jump=false;
    
    // Portal
    private Node portal;
    // Animaciones
    private AnimComposer animComposer;
    
    private float velocidad = 15f; // Velocidad de movimiento

    private BulletAppState fisica; // Declaración de la variable bulletAppState
    
    private long lastShootTime = 0;
    private static final long SHOOT_COOLDOWN = 500; 

    private int hitCount = 0;
    private static final int MAX_HITS = 2; // Define el máximo de golpes para eliminar al enemigo
    private float timeElapsed = 0;
    private int numEnemiesToSpawn = 1;
    private static final float SPAWN_INTERVAL = 5f; // Intervalo de 10 segundos para generar un nuevo enemigo
    private float currentSpawnInterval = SPAWN_INTERVAL;
    
    private int portalCollisionCount = 0;
    private static final int MAX_PORTAL_COLLISIONS = 3;
    
    private AudioNode backgroundMusic;
    private AudioNode walkSound;
    private AudioNode dashSound;
    private AudioNode shootSound;
    
    private AudioNode hitSound;
    private AudioNode enemyDeathSound;
    
    private boolean isDashing = false; // Nueva variable para controlar el estado de dash
    private static final float DASH_SPEED_MULTIPLIER = 4f; // Multiplicador de velocidad durante el dash
    

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
        
        
        //setupSky();
        spawnEnemies(numEnemiesToSpawn);
        setupKeys();
        setupPlayer();
        setupLight();
        setupMap();
        setupBoundaries();
        setupCamera();
        setupCollisionListener();
        handleBackgroundMusic();
        handleSounds();
        
    }
    
    /*private void setupSky(){
        // Cargar las texturas del cubemap
        Texture west = assetManager.loadTexture("Textures/Skybox/west.jpg");
        Texture east = assetManager.loadTexture("Textures/Skybox/east.jpg");
        Texture north = assetManager.loadTexture("Textures/Skybox/north.jpg");
        Texture south = assetManager.loadTexture("Textures/Skybox/south.jpg");
        Texture down = assetManager.loadTexture("Textures/Skybox/down.jpg");

        // Crear el skybox
        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        rootNode.attachChild(sky);
    }
    */
    private void setupPlayer() {
        Spatial sown_model = assetManager.loadModel("Models/Sown/SownFinal.j3o");

        sown_model.setLocalScale(0.4f);

        sown = new Node("sown_node");
        
        sown.attachChild(sown_model);
        
        animComposer = findAnimComposer(sown);
        if (animComposer != null) {
            System.out.println("Animaciones disponibles:");
            for (String animacion : animComposer.getAnimClipsNames()) {
                System.out.println("- " + animacion);
            }
        } else {
            System.out.println("El modelo no tiene un AnimComposer.");
        }

        rootNode.attachChild(sown);
        
        //-------------------------------------radio,altura,masa
        playerSown = new BetterCharacterControl(0.6f,1.3f,25f);
        
        sown.setLocalTranslation(-30f, 1.6f, -19f);
        
        sown.addControl(playerSown);
        
        playerSown.setGravity(new Vector3f(0, -9.8f, 0));
        // -9.81f es la aceleración gravitacional estándar en la Tierra

        // Agregar el control al BulletAppState para manejar las colisiones
        fisica.getPhysicsSpace().add(playerSown);
        
    }
    
    private void spawnEnemies(int numEnemies) {
        Random random = new Random();

        Vector3f[] spawnPoints = {
            new Vector3f(90f, 0.1f, -30f),
            new Vector3f(-51f, 0.1f, 90f),
            new Vector3f(-50f, 0.1f, -90f),
            new Vector3f(-100f, 0.1f, -30f)
        };

        // Generar los enemigos en el punto deseado
        for (int i = 0; i < numEnemies; i++) {
            Spatial enemy = assetManager.loadModel("Models/enemy/enemy.j3o");
            enemy.setName("enemy");
            
            // Inicializar el contador de golpes
            enemy.setUserData("hitCount", 0);

            // Seleccionar un punto de generación aleatorio de la lista
            Vector3f spawnPoint = spawnPoints[random.nextInt(spawnPoints.length)];
            enemy.setLocalTranslation(spawnPoint);

            // Asignar una velocidad aleatoria entre 2f y 6f
            enemy.setUserData("speed", 3f + random.nextFloat() * 5f);

            // Crear una forma de colisión para el enemigo
            CollisionShape enemyShape = CollisionShapeFactory.createBoxShape(enemy);
            RigidBodyControl enemyControl = new RigidBodyControl(enemyShape, 1f); // Masa de 1f
            enemy.addControl(enemyControl);

            // Añadir el control de física al espacio de física
            fisica.getPhysicsSpace().add(enemyControl);

            rootNode.attachChild(enemy); // Agregar el enemigo al nodo principal de la escena
        }
    }
    
    private void updateEnemies(float tpf) {
        Vector3f portalPos = rootNode.getChild("portal_node").getWorldTranslation(); // Obtener la posición del portal

        // Recorrer todos los nodos hijos del rootNode para buscar los cubos de los enemigos
        for (Spatial spatial : rootNode.getChildren()) {
            if ( spatial.getName() != null && spatial.getName().equals("enemy")) { // Buscar cubos de enemigos
                // Calcular la dirección hacia la que deben moverse los enemigos (hacia el portal)
                Vector3f enemyPos = spatial.getWorldTranslation();
                Vector3f directionToPortal = portalPos.subtract(enemyPos).normalizeLocal();

                // Obtener la velocidad del enemigo
                float speed = spatial.getUserData("speed");

                // Mover el cubo del enemigo en la dirección calculada
                RigidBodyControl enemyControl = spatial.getControl(RigidBodyControl.class);
                if (enemyControl != null) {
                    enemyControl.setLinearVelocity(directionToPortal.mult(speed)); // Multiplicar por la velocidad del enemigo
                    // Comprobar si el enemigo ha llegado al portal (colisión simple)
                    if (enemyPos.distance(portalPos) < 5.9f) {
                        handleEnemyReachedPortal(spatial);
                    }
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
        portal = new Node("portal_node");
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
        mainLight.setColor(ColorRGBA.White.mult(0.5f)); // Aumentar la intensidad para una iluminación más brillante
        mainLight.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        rootNode.addLight(mainLight);

        // Configurar sombras en la luz direccional
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 3);
        dlsr.setLight(mainLight);
        dlsr.setShadowIntensity(0.5f); // Ajustar la intensidad de las sombras
        dlsr.setShadowZExtend(150f); // Aumentar la distancia de renderizado de sombras
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
        
        // Ajustar la distancia de atenuación de sombras
        viewPort.addProcessor(dlsr);

        // Configurar sombras en los objetos
        rootNode.setShadowMode(ShadowMode.CastAndReceive);
        sown.setShadowMode(ShadowMode.CastAndReceive);
        
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
            }if (name.equals("Dash")) {
                isDashing = isPressed; // Activar o desactivar el dash según el estado de la tecla
            }
        }
    };
    
    private void setupCollisionListener() {
        fisica.getPhysicsSpace().addCollisionListener(event -> {
            Spatial nodeA = event.getNodeA();
            Spatial nodeB = event.getNodeB();

            if (nodeA != null && nodeB != null) { // Verificar que nodeA y nodeB no sean nulos
                // Comprobar si el proyectil colisionó con un enemigo
                if (nodeA.getName() != null && nodeB.getName() != null && nodeA.getName().equals("Projectile") && nodeB.getName().equals("enemy")) {
                    handleEnemyHit(nodeB, nodeA);
                } else if (nodeB.getName() != null && nodeA.getName() != null && nodeB.getName().equals("Projectile") && nodeA.getName().equals("enemy")) {
                    handleEnemyHit(nodeA, nodeB);
                }// Comprobar si el enemigo colisionó con el portal
                else if (nodeA.getName().equals("enemy") && nodeB.getName().equals("portal_node")) {
                    handleEnemyReachedPortal(nodeA);
                } else if (nodeB.getName().equals("enemy") && nodeA.getName().equals("portal_node")) {
                    handleEnemyReachedPortal(nodeB);
                }
            }
        });
    }
    
    private void handleEnemyReachedPortal(Spatial enemy) {
        // Incrementar el contador de colisiones con el portal
        portalCollisionCount++;

        // Eliminar el enemigo
        rootNode.detachChild(enemy);
        fisica.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));

        // Verificar si el contador de colisiones con el portal ha alcanzado el límite
        if (portalCollisionCount >= MAX_PORTAL_COLLISIONS) {
            onPlayerLose();
        }
    }
    
    private void onPlayerLose() {
        System.out.println("¡Has perdido! Los enemigos han alcanzado el portal 3 veces.");
        stop();
    }
    
    private void handleEnemyHit(Spatial enemy, Spatial projectile) {
        // Obtener el contador de golpes actual del enemigo
        hitCount = enemy.getUserData("hitCount");

        // Incrementar el contador de golpes
        hitCount++;

        // Almacenar el nuevo contador de golpes en el enemigo
        enemy.setUserData("hitCount", hitCount);

        // Eliminar el proyectil
        hitSound.play();
        rootNode.detachChild(projectile);
        fisica.getPhysicsSpace().remove(projectile.getControl(RigidBodyControl.class));

        // Verificar si el enemigo ha sido golpeado dos veces
        if (hitCount >= MAX_HITS) {
            // Eliminar el enemigo
            enemyDeathSound.play();
            rootNode.detachChild(enemy);
            fisica.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
            // Reiniciar el contador de golpes solo para este enemigo
            enemy.setUserData("hitCount", 0);
        }
    }
    
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
            projectileMat.setColor("Color", ColorRGBA.Magenta);
            projectile.setMaterial(projectileMat);

            Vector3f playerPos = sown.getWorldTranslation();
            Vector3f projectilePos = playerPos.add(cam.getDirection().mult(2f)).addLocal(0, 1f, 0f);
            projectile.setLocalTranslation(projectilePos);

            RigidBodyControl projectileControl = new RigidBodyControl(10f);
            projectile.addControl(projectileControl);

            Vector3f projectileDir = playerSown.getViewDirection().normalize().mult(100f);
            projectileControl.setLinearVelocity(projectileDir);
            shootSound.play();
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
            walkSound.play();
        }
        if (atras) {
            Vector3f camOppositeDir = cam.getDirection().negate().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camOppositeDir);
            walkSound.play();
        }
        if (izq) {
            Vector3f camLeft = cam.getLeft().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camLeft);
             walkSound.play();
        }
        if (der) {
            Vector3f camRight = cam.getLeft().negate().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
            moveDirection.addLocal(camRight);
            walkSound.play();
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
            // Aplicar el multiplicador de velocidad si se está realizando un dash
            float speedMultiplier;
            if (isDashing) {
                speedMultiplier = DASH_SPEED_MULTIPLIER;
                walkSound.stop();
                dashSound.play();
            } else {
                speedMultiplier = 1f;
            }
            moveDirection.multLocal(velocidad * speedMultiplier); // Multiplica por la velocidad de movimiento y el multiplicador de dash
        }
        // Obtiene la dirección de la cámara y rota al personaje hacia esa dirección
        Vector3f camDir = cam.getDirection().clone().mult(new Vector3f(1, 0, 1)).normalizeLocal();
        playerSown.setViewDirection(camDir);
        
        // Establece la dirección de movimiento al personaje
        playerSown.setWalkDirection(moveDirection);
    }
    
    private void handleBackgroundMusic(){
        // Cargar y reproducir música de fondo
        backgroundMusic = new AudioNode(assetManager, "Music/MusicaFondo.ogg", DataType.Stream);
        backgroundMusic.setLooping(true);  // Reproducir en bucle
        backgroundMusic.setPositional(false);  // No espacial
        backgroundMusic.setVolume(0.05f);  // Volumen de la música
        rootNode.attachChild(backgroundMusic);
        backgroundMusic.play();  // Reproducir la música
    }
    
    private void handleSounds(){
        // Cargar los archivos de sonido
        walkSound = new AudioNode(assetManager, "Sounds/walk.ogg", DataType.Stream);
        walkSound.setLooping(false);
        walkSound.setPositional(false);
        walkSound.setVolume(0.8f);
        walkSound.setPitch(0.9f);
        
        rootNode.attachChild(walkSound);
        
        dashSound = new AudioNode(assetManager, "Sounds/dash.ogg", DataType.Stream);
        dashSound.setLooping(false);
        dashSound.setPositional(false);
        dashSound.setVolume(1);
        dashSound.setPitch(1.9f);
        rootNode.attachChild(dashSound);
        
        shootSound = new AudioNode(assetManager, "Sounds/shoot.ogg", DataType.Stream);
        shootSound.setLooping(false);
        shootSound.setPositional(false);
        shootSound.setVolume(1);
        shootSound.setPitch(2f);
        rootNode.attachChild(shootSound);
        
        hitSound = new AudioNode(assetManager, "Sounds/hit.ogg", DataType.Stream);
        hitSound.setLooping(false);
        hitSound.setPositional(false);
        hitSound.setVolume(1);
        hitSound.setPitch(1.3f);
        rootNode.attachChild(hitSound);
        
        
        enemyDeathSound = new AudioNode(assetManager, "Sounds/enemyDeath.ogg", DataType.Stream);
        enemyDeathSound.setLooping(false);
        enemyDeathSound.setPositional(false);
        enemyDeathSound.setVolume(1);
        enemyDeathSound.setPitch(1.8f);
        rootNode.attachChild(enemyDeathSound);
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        updateEnemies(tpf);
        handleMovement(tpf);
        handleSpawnEnemies(tpf,0.8f);
    }
    
    public void handleSpawnEnemies(float tpf,float spawnInterval){
        timeElapsed += tpf;
        if (timeElapsed >= currentSpawnInterval) {
            timeElapsed = 0;
            numEnemiesToSpawn++;
            spawnEnemies(1); // Genera un nuevo enemigo cada intervalo de tiempo

            // Reduce el intervalo de generación gradualmente
            currentSpawnInterval *= spawnInterval; // Ajusta este factor según tus preferencias
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.5f); // Asegura que el intervalo no sea menor a 1 segundo
        }
    }
    
    private AnimComposer findAnimComposer(Node node) {
        if (node.getControl(AnimComposer.class) != null) {
            System.out.println("AnimComposer encontrado en el nodo: " + node.getName());
            return node.getControl(AnimComposer.class);
        }

        for (Spatial child : node.getChildren()) {
            if (child instanceof Node) {
                AnimComposer childAnimComposer = findAnimComposer((Node) child);
                if (childAnimComposer != null) {
                    return childAnimComposer;
                }
            }
        }

        return null;
    }
    
    @Override
    public void simpleRender(RenderManager rm) {
        // TODO: add render code
        
    }
}