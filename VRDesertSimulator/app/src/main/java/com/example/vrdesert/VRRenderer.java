package com.example.vrdesert;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.util.Random;

import com.example.vrdesert.shapes.Cube;
import com.example.vrdesert.shapes.Plane;
import com.example.vrdesert.shapes.TextOverlay;
import com.example.vrdesert.shapes.Crosshair;
import com.example.vrdesert.shapes.SkyDome;
import com.example.vrdesert.shapes.WaterCanteen;
import com.example.vrdesert.shapes.FoodCrate;
import com.example.vrdesert.shapes.WoodLog;
import com.example.vrdesert.shapes.MatchboxShape;
import com.example.vrdesert.shapes.SandParticles;
import com.example.vrdesert.shapes.HealthBarGL;
import com.example.vrdesert.shapes.Cactus;
import com.example.vrdesert.shapes.Vignette;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VRRenderer implements GLSurfaceView.Renderer {

    private final SensorHandler sensorHandler;
    private final InteractionManager interactionManager;

    private Plane floor;
    private SkyDome skyDome;
    private WaterCanteen waterModel;
    private FoodCrate foodModel;
    private WoodLog woodModel;
    private MatchboxShape matchboxModel;
    private SandParticles sandParticles;
    private Cactus cactusModel;
    private float[][] cactusPositions;
    private Vignette vignette;
    private int shaderProgram;

    // View & Projection
    private float[] leftProjectionMatrix = new float[16];
    private float[] rightProjectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] vPMatrix = new float[16];
    private float[] scratchMatrix = new float[16];
    private float[] modelMatrix = new float[16];
    private float[] normalMatrix = new float[16];
    private float[] skyViewMatrix = new float[16];
    private float[] skyVPMatrix = new float[16];

    // Camera state
    private float camX = 0f;
    private float camY = 1.0f; // standing height lowered to match block centers
    private float camZ = 0f;
    
    // IPD for VR box stereoscopic — wider separation for clear 3D
    private static final float EYE_OFFSET = 0.10f;
    // Lens center offset (lenses are slightly inward from screen center)
    private static final float LENS_OFFSET = 0.03f;

    // Objects in Scene (Spawns 15 randomized items)
    private GameObject[] objects = new GameObject[15];

    // UI Elements
    private TextOverlay inventoryOverlay;
    private Crosshair crosshair;
    private HealthBarGL healthBarGL;
    private float[] uiProjectionMatrix = new float[16];
    private float[] uiModelMatrix = new float[16];
    private float[] uiMVPMatrix = new float[16];
    private String inventoryString = "";
    private boolean updateInventoryTextFlag = false;

    // Health state
    private volatile int currentHealth = 30;

    public void updateInventory(String text) {
        this.inventoryString = text;
        this.updateInventoryTextFlag = true;
    }

    public void updateHealth(int health) {
        this.currentHealth = health;
    }

    private int width, height;

    // --- ENHANCED SHADERS WITH DIRECTIONAL LIGHTING ---
    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "uniform mat4 uModelMatrix;" +
        "uniform mat4 uNormalMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "attribute vec3 aNormal;" +
        "varying vec4 _vColor;" +
        "varying float vDistance;" +
        "varying vec3 vWorldNormal;" +
        "varying vec3 vWorldPos;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  _vColor = vColor;" +
        "  vDistance = gl_Position.w;" +
        "  vWorldNormal = normalize((uNormalMatrix * vec4(aNormal, 0.0)).xyz);" +
        "  vWorldPos = (uModelMatrix * vPosition).xyz;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 _vColor;" +
        "varying float vDistance;" +
        "varying vec3 vWorldNormal;" +
        "varying vec3 vWorldPos;" +
        "void main() {" +
        // Directional sunlight from upper-right
        "  vec3 lightDir = normalize(vec3(0.5, 0.8, 0.3));" +
        "  float diff = max(dot(normalize(vWorldNormal), lightDir), 0.0);" +
        "  float ambient = 0.4;" +
        "  float lighting = ambient + diff * 0.6;" +
        "  vec3 litColor = _vColor.rgb * lighting;" +
        // Warm desert fog — starts far, gradual fade
        "  float fogFactor = clamp((vDistance - 20.0) / 160.0, 0.0, 1.0);" +
        "  vec3 fogColor = vec3(0.93, 0.84, 0.64);" +
        "  vec3 finalColor = mix(litColor, fogColor, fogFactor);" +
        "  gl_FragColor = vec4(finalColor, _vColor.a);" +
        "}";

    public VRRenderer(SensorHandler sensorHandler, InteractionManager interactionManager) {
        this.sensorHandler = sensorHandler;
        this.interactionManager = interactionManager;
    }

    public void moveForward() {
        // Move camera forward based on its current yaw
        float yawRad = (float) Math.toRadians(sensorHandler.getYaw());
        float forwardX = (float) -Math.sin(yawRad);
        float forwardZ = (float) -Math.cos(yawRad);

        // 1 meter step size
        camX += forwardX * 1.0f;
        camZ += forwardZ * 1.0f;
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Desert fog color (matches shader fog)
        GLES20.glClearColor(0.93f, 0.84f, 0.64f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        // Sky dome (uses its own shader internally)
        skyDome = new SkyDome();

        // Terrain
        floor = new Plane(shaderProgram);

        // UI elements
        inventoryOverlay = new TextOverlay();
        crosshair = new Crosshair();
        vignette = new Vignette();
        healthBarGL = new HealthBarGL();
        healthBarGL.setHealth(currentHealth);

        // Realistic object models
        waterModel = new WaterCanteen(shaderProgram);
        foodModel = new FoodCrate(shaderProgram);
        woodModel = new WoodLog(shaderProgram);
        matchboxModel = new MatchboxShape(shaderProgram);

        // Cactus model + scattered positions
        cactusModel = new Cactus(shaderProgram);
        cactusPositions = new float[][] {
            { 12f, -0.5f,  8f},
            {-15f, -0.5f, 12f},
            { 25f, -0.5f, -10f},
            {-20f, -0.5f, -18f},
            {  8f, -0.5f, -25f},
            {-30f, -0.5f,  5f},
            { 18f, -0.5f,  22f},
            {-10f, -0.5f, -30f}
        };

        // Sand/wind particles (350 dust motes + 60 wind streaks)
        sandParticles = new SandParticles(350);

        // Scatter random objects around the desert immediately
        Random rand = new Random();
        GameObject.Type[] types = GameObject.Type.values();
        for (int i = 0; i < objects.length; i++) {
            float rx = (rand.nextFloat() * 40f) - 20f;
            float rz = (rand.nextFloat() * 40f) - 20f;
            
            // Prevent spawning directly on the origin camera (inside the player)
            if (Math.abs(rx) < 2f) rx += 2f;
            if (Math.abs(rz) < 2f) rz += 2f;
            
            GameObject.Type t = types[rand.nextInt(types.length)];
            objects[i] = new GameObject(rx, 0f, rz, t);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glViewport(0, 0, width, height);

        // Aspect ratio for half the screen (one eye)
        float ratio = (float) (width / 2) / height;
        // VR box FOV: ~90 degrees to match typical cardboard lenses
        float fovY = 90f;

        // Left eye projection — slightly shifted right (lens offset)
        Matrix.perspectiveM(leftProjectionMatrix, 0, fovY, ratio, 0.1f, 500f);
        // Shift frustum for lens center offset
        leftProjectionMatrix[8] = LENS_OFFSET;

        // Right eye projection — slightly shifted left
        Matrix.perspectiveM(rightProjectionMatrix, 0, fovY, ratio, 0.1f, 500f);
        rightProjectionMatrix[8] = -LENS_OFFSET;

        // UI orthographic projection — maps to each eye's viewport (0,0 to viewportW, viewportH)
        Matrix.orthoM(uiProjectionMatrix, 0, 0, width / 2, height, 0, -1, 1);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Handle UI text updates safely on the GL Thread exactly when necessary
        if (updateInventoryTextFlag) {
            if (inventoryOverlay != null) {
                inventoryOverlay.updateText(inventoryString);
            }
            updateInventoryTextFlag = false;
        }

        // Update health bar
        if (healthBarGL != null) {
            healthBarGL.setHealth(currentHealth);
            healthBarGL.update();
        }
        
        // Pass Gaze targeting boolean direct to internal crosshair rendering!
        if (crosshair != null) {
            crosshair.setTargeting(interactionManager.isTargeting());
            crosshair.setGazeProgress(interactionManager.getGazeProgress());
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float pitchInfo = sensorHandler.getPitch();
        float yawInfo = sensorHandler.getYaw();

        // Convert to radians to compute look vectors
        float yawRad = (float) Math.toRadians(yawInfo);
        float pitchRad = (float) Math.toRadians(pitchInfo);

        float forwardX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float forwardY = (float) Math.sin(-pitchRad);
        float forwardZ = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));

        float targetX = camX + forwardX;
        float targetY = camY + forwardY;
        float targetZ = camZ + forwardZ;

        // Up vector (assuming purely vertical Y-up most of the time is safe enough given limits)
        float upX = 0f;
        float upY = 1f;
        float upZ = 0f;

        // Interaction Check once per frame (using center camera gaze)
        for (int i = 0; i < objects.length; i++) {
            interactionManager.checkGaze(
                camX, camY, camZ,
                forwardX, forwardY, forwardZ,
                objects[i],
                i
            );
        }

        // Draw Left Eye
        GLES20.glViewport(0, 0, width / 2, height);
        // compute left eye offset
        float leftOffX =  (float) Math.cos(yawRad) * EYE_OFFSET; // rough approx of horizontal shift
        float leftOffZ = (float) -Math.sin(yawRad) * EYE_OFFSET; 
        Matrix.setLookAtM(viewMatrix, 0, 
                camX - leftOffX, camY, camZ - leftOffZ, 
                targetX - leftOffX, targetY, targetZ - leftOffZ, 
                upX, upY, upZ);
        Matrix.multiplyMM(vPMatrix, 0, leftProjectionMatrix, 0, viewMatrix, 0);
        drawScene(vPMatrix, leftProjectionMatrix);
        drawUI();
        vignette.draw(); // Lens vignette for left eye

        // Draw Right Eye
        GLES20.glViewport(width / 2, 0, width / 2, height);
        // compute right eye offset
        float rightOffX = (float) -Math.cos(yawRad) * EYE_OFFSET; 
        float rightOffZ = (float) Math.sin(yawRad) * EYE_OFFSET;
        Matrix.setLookAtM(viewMatrix, 0, 
                camX - rightOffX, camY, camZ - rightOffZ, 
                targetX - rightOffX, targetY, targetZ - rightOffZ, 
                upX, upY, upZ);
        Matrix.multiplyMM(vPMatrix, 0, rightProjectionMatrix, 0, viewMatrix, 0);
        drawScene(vPMatrix, rightProjectionMatrix);
        drawUI();
        vignette.draw(); // Lens vignette for right eye

        // Draw black divider line between left and right eyes
        GLES20.glViewport(0, 0, width, height);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(width / 2 - 1, 0, 3, height);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0.93f, 0.84f, 0.64f, 1.0f);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    private void drawUI() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST); // UI renders entirely on top!

        // Draw HUD Text at top left
        Matrix.setIdentityM(uiModelMatrix, 0);
        Matrix.translateM(uiModelMatrix, 0, 20f, 20f, 0f); // 20px padding offset from Top-Left corner
        Matrix.scaleM(uiModelMatrix, 0, 512f, 512f, 1f);
        Matrix.multiplyMM(uiMVPMatrix, 0, uiProjectionMatrix, 0, uiModelMatrix, 0);
        inventoryOverlay.draw(uiMVPMatrix);

        // Draw GL Health Bar at bottom center
        Matrix.setIdentityM(uiModelMatrix, 0);
        float eyeWidth = width / 2f;
        Matrix.translateM(uiModelMatrix, 0, eyeWidth / 2f - 80f, height - 60f, 0f);
        Matrix.multiplyMM(uiMVPMatrix, 0, uiProjectionMatrix, 0, uiModelMatrix, 0);
        healthBarGL.draw(uiMVPMatrix);

        // Draw standard Crosshair precisely at center of each eye viewport
        Matrix.setIdentityM(uiModelMatrix, 0);
        float cx = eyeWidth / 2f;  // center X within this eye's viewport
        float cy = height / 2f;     // center Y
        Matrix.translateM(uiModelMatrix, 0, cx, cy, 0f);
        Matrix.multiplyMM(uiMVPMatrix, 0, uiProjectionMatrix, 0, uiModelMatrix, 0);
        crosshair.draw(uiMVPMatrix);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /** Compute a simple normal matrix from model matrix (transpose of inverse of upper-left 3x3) */
    private void computeNormalMatrix(float[] model, float[] out) {
        // For uniform scaling, the model matrix itself works. For safety, copy it.
        System.arraycopy(model, 0, out, 0, 16);
        // Zero out translation
        out[12] = 0; out[13] = 0; out[14] = 0;
    }

    private void drawScene(float[] projectionAndViewMatrix, float[] projectionMatrix) {
        float time = (float)(SystemClock.elapsedRealtime() % 100000L) / 1000f;

        // === SKY DOME (rendered first, behind everything) ===
        // Strip camera translation from view matrix for sky
        System.arraycopy(viewMatrix, 0, skyViewMatrix, 0, 16);
        skyViewMatrix[12] = 0; skyViewMatrix[13] = 0; skyViewMatrix[14] = 0;
        Matrix.multiplyMM(skyVPMatrix, 0, projectionMatrix, 0, skyViewMatrix, 0);
        skyDome.draw(skyVPMatrix);

        // === FLOOR ===
        GLES20.glUseProgram(shaderProgram);
        Matrix.setIdentityM(modelMatrix, 0);
        computeNormalMatrix(modelMatrix, normalMatrix);

        // Set light direction uniform
        int lightHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightDir");

        Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
        floor.draw(scratchMatrix, modelMatrix, normalMatrix);

        // === OBJECTS ===
        for (int i = 0; i < objects.length; i++) {
            if (objects[i].isCollected) continue;

            float ox = objects[i].x;
            float oy = objects[i].y;
            float oz = objects[i].z;

            // Animation: gentle bob + slow rotation
            float bob = (float)Math.sin(time * 1.5f + i * 1.7f) * 0.05f;
            float rotY = (time * 30f + i * 90f) % 360f;

            Matrix.setIdentityM(modelMatrix, 0);

            if (objects[i].type == GameObject.Type.WATER) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.35f + bob, oz);
                Matrix.rotateM(modelMatrix, 0, rotY, 0, 1, 0);
                Matrix.scaleM(modelMatrix, 0, 0.35f, 0.5f, 0.35f);
                computeNormalMatrix(modelMatrix, normalMatrix);
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                waterModel.draw(scratchMatrix, modelMatrix, normalMatrix);

            } else if (objects[i].type == GameObject.Type.FOOD) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.25f + bob, oz);
                Matrix.rotateM(modelMatrix, 0, rotY * 0.5f, 0, 1, 0);
                Matrix.scaleM(modelMatrix, 0, 0.5f, 0.45f, 0.5f);
                computeNormalMatrix(modelMatrix, normalMatrix);
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                foodModel.draw(scratchMatrix, modelMatrix, normalMatrix);

            } else if (objects[i].type == GameObject.Type.WOOD) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.15f + bob, oz);
                Matrix.rotateM(modelMatrix, 0, rotY * 0.3f, 0, 1, 0);
                Matrix.scaleM(modelMatrix, 0, 0.9f, 0.9f, 0.9f);
                computeNormalMatrix(modelMatrix, normalMatrix);
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                woodModel.draw(scratchMatrix, modelMatrix, normalMatrix);

            } else if (objects[i].type == GameObject.Type.MATCHBOX) {
                Matrix.translateM(modelMatrix, 0, ox, oy + 0.1f + bob, oz);
                Matrix.rotateM(modelMatrix, 0, rotY * 0.7f, 0, 1, 0);
                Matrix.scaleM(modelMatrix, 0, 0.35f, 0.35f, 0.35f);
                computeNormalMatrix(modelMatrix, normalMatrix);
                Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
                matchboxModel.draw(scratchMatrix, modelMatrix, normalMatrix);
            }
        }

        // === CACTI (static scenery) ===
        for (float[] cPos : cactusPositions) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, cPos[0], cPos[1], cPos[2]);
            // Vary scale slightly per cactus for natural look
            float scale = 0.7f + (float)(Math.sin(cPos[0] * 0.5f + cPos[2] * 0.3f) * 0.5 + 0.5) * 0.6f;
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
            // Vary rotation
            Matrix.rotateM(modelMatrix, 0, cPos[0] * 17f + cPos[2] * 31f, 0, 1, 0);
            computeNormalMatrix(modelMatrix, normalMatrix);
            Matrix.multiplyMM(scratchMatrix, 0, projectionAndViewMatrix, 0, modelMatrix, 0);
            cactusModel.draw(scratchMatrix, modelMatrix, normalMatrix);
        }

        // === SAND/WIND PARTICLES (after scene, with blending) ===
        sandParticles.draw(projectionAndViewMatrix, time, camX, camY, camZ);
    }
}
