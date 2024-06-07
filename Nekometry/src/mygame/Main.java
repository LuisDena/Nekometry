package mygame;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
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
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
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
import com.jme3.ui.Picture;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


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
    private Action caminataAction, saltoAction, dashAction;
    
    private float velocidad = 15f; // Velocidad de movimiento

    private BulletAppState fisica; // Declaración de la variable bulletAppState
    
    private long lastShootTime = 0;
    private static final long SHOOT_COOLDOWN = 200; 

    //private int hitCount = 0;
    private static final int MAX_HITS = 2; // Define el máximo de golpes para eliminar al enemigo
    private float timeElapsed = 0;
    private int numEnemiesToSpawn = 1;
    private static final float SPAWN_INTERVAL = 5f; // Intervalo de 10 segundos para generar un nuevo enemigo
    private float currentSpawnInterval = SPAWN_INTERVAL;
    
    private int portalCollisionCount = 0;
    private static final int MAX_PORTAL_COLLISIONS = 5;
    
    private AudioNode backgroundMusic;
    private AudioNode gameOverMusic;
    private AudioNode walkSound;
    private AudioNode dashSound;
    private AudioNode shootSound;
    private AudioNode hitSound;
    private AudioNode enemyDeathSound;
    private AudioNode healthDownSound;
    private AudioNode warningHealthSound;
    
    private float timer = 0.0f;
    private BitmapText timerText;
    private int score = 0;
    private BitmapText scoreText;
    
    private Picture[] hearts;
    private static final int MAX_HEARTS = 5;        
    
    private boolean isDashing = false; // Nueva variable para controlar el estado de dash
    private static final float DASH_SPEED_MULTIPLIER = 4f; // Multiplicador de velocidad durante el dash
    
    private BitmapFont myFont;
    private BitmapText controlsText;
    
    private boolean gameOver=false;
    private float countdownTimer;
    private BitmapText countdownText;
    
    

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
        
        
        setDisplayStatView(false);
        setDisplayFps(false);
        /** Set up Physics */
        fisica = new BulletAppState();
        stateManager.attach(fisica);
        //fisica.setDebugEnabled(true);
        myFont = assetManager.loadFont("Font/upheaval.fnt");
        
        
        setupControls();
        setupTimer();
        setupScore();
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
        setupHearts();
        setupSky();
    }
    
    private void setupSky(){
        // Cargar las texturas del cubemap
        Texture west = assetManager.loadTexture("Textures/fondo.png");
        Texture east = assetManager.loadTexture("Textures/fondo.png");
        Texture north = assetManager.loadTexture("Textures/fondo.png");
        Texture south = assetManager.loadTexture("Textures/fondo.png");
        Texture down = assetManager.loadTexture("Textures/fondo.png");
        Texture up = assetManager.loadTexture("Textures/fondo.png");
        
        // Crear el skybox
        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        sky.setShadowMode(ShadowMode.Off);
        rootNode.attachChild(sky);
    }
    
    private void setupControls(){
        String controlsInfo = "WASD: Moverse\nSpace: Saltar\nLShift: Dash\nMouse Left: Disparar";
        controlsText = new BitmapText(myFont, false);
        controlsText.setSize(myFont.getCharSet().getRenderedSize());
        controlsText.setSize(24);
        controlsText.setColor(ColorRGBA.White);
        
        controlsText.setText(controlsInfo);
        controlsText.setLocalTranslation(10, 100, 0); // Posición en la esquina inferior izquierda
        guiNode.attachChild(controlsText);

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> {
            enqueue(() -> guiNode.detachChild(controlsText));
        }, 10, TimeUnit.SECONDS);
    }
    
    private void setupHearts(){
        hearts = new Picture[MAX_HEARTS];
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i] = new Picture("Heart" + i);
            hearts[i].setImage(assetManager, "Interface/heart.png", true);
            hearts[i].setWidth(50);
            hearts[i].setHeight(50);
            hearts[i].setPosition(10 + i * 60, settings.getHeight() - 60);
            guiNode.attachChild(hearts[i]);
        }
        
    }
    
    private void setupTimer() {
        timerText = new BitmapText(myFont,false);
        timerText.setSize(myFont.getCharSet().getRenderedSize());
        timerText.setSize(24);
        timerText.setColor(ColorRGBA.White);
        
        // Ajusta la posición horizontal y vertical
        timerText.setLocalTranslation(settings.getWidth() - 120, 50, 0); 
        guiNode.attachChild(timerText);
    }
    
    private void setupScore() {
        scoreText = new BitmapText(myFont,false);
        scoreText.setSize(24);
        scoreText.setColor(ColorRGBA.White);
        scoreText.setText("Score: 0");
        scoreText.setLocalTranslation(settings.getWidth() - 150, settings.getHeight() - 10, 0);
        guiNode.attachChild(scoreText);
    }
    
    private void setupPlayer() {
        Spatial sown_model = assetManager.loadModel("Models/Sown/SownFinal.j3o");

        sown_model.setLocalScale(0.4f);

        sown = new Node("sown_node");
        
        sown.attachChild(sown_model);
        
        animComposer = findAnimComposer(sown);
        if (animComposer != null) {
            animComposer.setCurrentAction("Caminata");
            animComposer.setGlobalSpeed(1.5f);
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

        for (int i = 0; i < numEnemies; i++) {
            Spatial enemy = assetManager.loadModel("Models/enemy/enemy.j3o");
            enemy.setName("enemy");

            enemy.setUserData("hitCount", 0);
            enemy.setUserData("isEliminated", false);

            Vector3f spawnPoint = spawnPoints[random.nextInt(spawnPoints.length)];
            enemy.setLocalTranslation(spawnPoint);

            enemy.setUserData("speed", 3f + random.nextFloat() * 6f);

            // Asignar amplitud y frecuencia aleatorias para el zigzag
            enemy.setUserData("zigzagAmplitude", random.nextFloat() * 10f);
            enemy.setUserData("zigzagFrequency", random.nextFloat() * 10f);

            CollisionShape enemyShape = CollisionShapeFactory.createBoxShape(enemy);
            RigidBodyControl enemyControl = new RigidBodyControl(enemyShape, 1f);
            enemy.addControl(enemyControl);

            fisica.getPhysicsSpace().add(enemyControl);

            rootNode.attachChild(enemy);
        }
    }
    
    private void updateEnemies(float tpf) {
        Vector3f portalPos = rootNode.getChild("portal_node").getWorldTranslation();

        for (Spatial spatial : rootNode.getChildren()) {
            if (spatial.getName() != null && spatial.getName().equals("enemy")) {
                Vector3f enemyPos = spatial.getWorldTranslation();
                Vector3f directionToPortal = portalPos.subtract(enemyPos).normalizeLocal();

                float speed = spatial.getUserData("speed");

                // Obtener amplitud y frecuencia del zigzag del enemigo
                float zigzagAmplitude = spatial.getUserData("zigzagAmplitude");
                float zigzagFrequency = spatial.getUserData("zigzagFrequency");

                Vector3f perpendicularDirection = new Vector3f(directionToPortal.z, 0, -directionToPortal.x).normalizeLocal();

                float zigzagOffset = (float) Math.sin(tpf * zigzagFrequency) * zigzagAmplitude;

                Vector3f zigzagMovement = directionToPortal.add(perpendicularDirection.mult(zigzagOffset));

                RigidBodyControl enemyControl = spatial.getControl(RigidBodyControl.class);
                if (enemyControl != null) {
                    enemyControl.setLinearVelocity(zigzagMovement.mult(speed));
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
        mainLight.setDirection(new Vector3f(-5.5f, -5.5f, -5.5f).normalizeLocal());
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
                }// Comprobar si el jugador colisionó con un enemigo
                else if ((nodeA.getName().equals("sown_node") && nodeB.getName().equals("enemy"))
                        || (nodeB.getName().equals("sown_node") && nodeA.getName().equals("enemy"))) {
                    Spatial enemy = nodeA.getName().equals("enemy") ? nodeA : nodeB;
                    handlePlayerEnemyCollision(enemy);
                }
            }
        });
    }
    
    private void handlePlayerEnemyCollision(Spatial enemy){
        // Eliminar el enemigo
        // Reproducir un sonido de eliminación de enemigo
        enemyDeathSound.play();
        rootNode.detachChild(enemy);
        fisica.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
        // Incrementar el puntaje solo si el enemigo no ha sido eliminado previamente
        Boolean isEliminated = enemy.getUserData("isEliminated");
        if (isEliminated == null || !isEliminated) {
            score += 250;
            scoreText.setText("Score: " + score);
            enemy.setUserData("isEliminated", true);
        }
    }
    
    private void handleEnemyReachedPortal(Spatial enemy) {
        // Incrementar el contador de colisiones con el portal
        portalCollisionCount++;
        // Actualizar los corazones
        int remainingHearts = MAX_PORTAL_COLLISIONS - portalCollisionCount;
        for (int i = 0; i < MAX_HEARTS; i++) {
            if (i < remainingHearts) {
                hearts[i].setImage(assetManager, "Interface/heart.png", true);
            } else {
                healthDownSound.play();
                hearts[i].setImage(assetManager, "Interface/heart_empty.png", true);
            }
        }
        if (remainingHearts<=2){
            warningHealthSound.play();
        }

        // Eliminar el enemigo
        rootNode.detachChild(enemy);
        fisica.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
        
        // Verificar si el contador de colisiones con el portal ha alcanzado el límite
        if (portalCollisionCount >= MAX_PORTAL_COLLISIONS) {
            onPlayerLose();
        }
    }
    
    private void onPlayerLose() {
        gameOver = true;
        clearEnemies();
        showGameOverScreen();
    }
    
    private void clearEnemies() {
        // Recorrer todos los nodos hijos del rootNode
        for (Spatial spatial : rootNode.getChildren()) {
            if (spatial.getName() != null && spatial.getName().equals("enemy")) {
                // Eliminar el enemigo
                rootNode.detachChild(spatial);
                fisica.getPhysicsSpace().remove(spatial.getControl(RigidBodyControl.class));
            }
        }
    }
    
    private void showGameOverScreen() {
        gameOverMusic.play();
        // Detener la música de fondo
        backgroundMusic.stop();
        // Desactivar los controles del personaje
        rootNode.detachChild(sown);
        flyCam.setEnabled(false);
        
        
        walkSound.stop();
        dashSound.stop();
        shootSound.stop();
        hitSound.stop();
        enemyDeathSound.stop();
        healthDownSound.stop();
        warningHealthSound.stop();

        // Ocultar los elementos del HUD
        guiNode.detachChild(timerText);
        guiNode.detachChild(scoreText);
        for (Picture heart : hearts) {
            guiNode.detachChild(heart);
        }

        // Mostrar el puntaje y tiempo final
        BitmapText gameOverText = new BitmapText(myFont, false);
        gameOverText.setSize(48);
        gameOverText.setColor(ColorRGBA.Red);
        gameOverText.setText("Game Over");
        gameOverText.setLocalTranslation(settings.getWidth() / 2 - gameOverText.getLineWidth() / 2, settings.getHeight() / 2 + 50, 0);
        guiNode.attachChild(gameOverText);

        BitmapText finalScoreText = new BitmapText(myFont, false);
        finalScoreText.setSize(36);
        finalScoreText.setColor(ColorRGBA.White);
        finalScoreText.setText("Final Score: " + score);
        finalScoreText.setLocalTranslation(settings.getWidth() / 2 - finalScoreText.getLineWidth() / 2, settings.getHeight() / 2, 0);
        guiNode.attachChild(finalScoreText);

        BitmapText finalTimeText = new BitmapText(myFont, false);
        finalTimeText.setSize(36);
        finalTimeText.setColor(ColorRGBA.White);
        finalTimeText.setText("Time: " + (int) timer);
        finalTimeText.setLocalTranslation(settings.getWidth() / 2 - finalTimeText.getLineWidth() / 2, settings.getHeight() / 2 - 50, 0);
        guiNode.attachChild(finalTimeText);
        
        // Iniciar la cuenta regresiva
        countdownText = new BitmapText(myFont, false);
        countdownText.setSize(36);
        countdownText.setColor(ColorRGBA.White);
        countdownText.setLocalTranslation(settings.getWidth() / 2 - countdownText.getLineWidth() / 2, settings.getHeight() / 2 - 150, 0);
        guiNode.attachChild(countdownText);

        // Iniciar la cuenta regresiva
        countdownTimer=10.0f;
        countdownText.setText("Closing in: " + countdownTimer);
    }
    
    private void handleEnemyHit(Spatial enemy, Spatial projectile) {
        // Obtener el contador de golpes actual del enemigo
        int hitCount = enemy.getUserData("hitCount");

        // Incrementar el contador de golpes
        hitCount++;

        // Almacenar el nuevo contador de golpes en el enemigo
        enemy.setUserData("hitCount", hitCount);

        // Eliminar el proyectil
        hitSound.play();
        rootNode.detachChild(projectile);
        fisica.getPhysicsSpace().remove(projectile.getControl(RigidBodyControl.class));

        // Verificar si el enemigo ha sido golpeado dos veces
        if (hitCount == MAX_HITS) {
            // Eliminar el enemigo
            enemyDeathSound.play();
            rootNode.detachChild(enemy);
            fisica.getPhysicsSpace().remove(enemy.getControl(RigidBodyControl.class));
            score += 100; // Incrementa el puntaje en 100 puntos por cada enemigo eliminado
            scoreText.setText("Score: " + score);
        }
        hitCount=0;
    }
    
    public void handleProjectileAttack() {
        if(gameOver){
            return;
        }
        
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
        AnimComposer animacion = findAnimComposer(sown);
        
        
        if(gameOver){
            return;
        }
        
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
        
        
        //findAnimComposer(sown).reset();
        
        if (jump) {// Manejar el salto
            walkSound.stop();
            if (playerSown.isOnGround()) { // Salta solo si está en el suelo
                Action salto = animComposer.action("Salto");
                Tween doneTween = Tweens.callMethod(animComposer, "setCurrentAction", "Caminata");
                Action saltoOnce = animComposer.actionSequence("SaltoOnce", salto, doneTween);
                animComposer.setCurrentAction("SaltoOnce");
                animComposer.setGlobalSpeed(2f);
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
                Action dash = animComposer.action("Dash");
                Tween doneTween = Tweens.callMethod(animComposer, "setCurrentAction", "Caminata");
                Action DashOnce = animComposer.actionSequence("DashOnce", dash, doneTween);
                animComposer.setCurrentAction("DashOnce");
                animComposer.setGlobalSpeed(5f);
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
        backgroundMusic.setVolume(0.1f);  // Volumen de la música
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
        
        healthDownSound = new AudioNode(assetManager, "Sounds/healthdown.ogg", DataType.Stream);
        healthDownSound.setLooping(false);
        healthDownSound.setPositional(false);
        healthDownSound.setVolume(1);
        healthDownSound.setPitch(0.8f);
        rootNode.attachChild(healthDownSound);
        
        warningHealthSound = new AudioNode(assetManager, "Sounds/alarm.ogg", DataType.Stream);
        warningHealthSound.setLooping(true);
        warningHealthSound.setPositional(false);
        warningHealthSound.setVolume(0.7f);
        warningHealthSound.setPitch(1.25f);
        rootNode.attachChild(warningHealthSound);
        
        // Cargar y reproducir música de fondo
        gameOverMusic = new AudioNode(assetManager, "Music/gameOver.ogg", DataType.Stream);
        gameOverMusic.setLooping(true);  // Reproducir en bucle
        gameOverMusic.setPositional(false);  // No espacial
        gameOverMusic.setVolume(0.1f);  // Volumen de la música
        rootNode.attachChild(gameOverMusic);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        if (!gameOver) {
            updateEnemies(tpf);
            updateTimer(tpf);
            handleMovement(tpf);
            handleSpawnEnemies(tpf, 0.8f);
        }else {
            // Actualizar la cuenta regresiva
            updateCountDownTimer(tpf);
        }
    }
    
    private void updateCountDownTimer(float tpf){
        countdownTimer -= tpf;
        if (countdownTimer <= 0) {
            this.stop();
        } else {
            int countdown = (int) Math.ceil(countdownTimer);
            countdownText.setText("Closing in: " + countdown);
        }
    }
    
    private void updateTimer(float tpf) {
        timer += tpf;
        int seconds = (int) timer;
        timerText.setText("Time: " + seconds);
    }
    
    public void handleSpawnEnemies(float tpf,float spawnInterval){
        timeElapsed += tpf;
        if (timeElapsed >= currentSpawnInterval) {
            timeElapsed = 0;
            numEnemiesToSpawn++;
            spawnEnemies(1); // Genera un nuevo enemigo cada intervalo de tiempo

            // Reduce el intervalo de generación gradualmente
            currentSpawnInterval *= spawnInterval; 
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.5f); // Asegura que el intervalo no sea menor a 1 segundo
        }
    }
    
    private AnimComposer findAnimComposer(Node node) {
        if (node.getControl(AnimComposer.class) != null) {
            //System.out.println("AnimComposer encontrado en el nodo: " + node.getName());
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