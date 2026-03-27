package com.example.vrdesert;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.opengl.GLSurfaceView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements InteractionManager.InteractionListener {

    private GLSurfaceView glSurfaceView;
    private VRRenderer vrRenderer;
    private SensorHandler sensorHandler;
    private InteractionManager interactionManager;

    private BackpackManager backpackManager;
    private AudioEngine audioEngine;
    private GameManager gameManager;
    private MoveServer moveServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);

        // Require OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2);

        sensorHandler = new SensorHandler(this);
        interactionManager = new InteractionManager(this);

        vrRenderer = new VRRenderer(sensorHandler, interactionManager);
        glSurfaceView.setRenderer(vrRenderer);
        
        // Render only when data changes or animate continuously
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        backpackManager = new BackpackManager();
        audioEngine = new AudioEngine();
        audioEngine.startDesertWind(); // Kick off async background noise

        ProgressBar healthBarLeft = findViewById(R.id.healthBarLeft);
        ProgressBar healthBarRight = findViewById(R.id.healthBarRight);
        gameManager = new GameManager(this, healthBarLeft, healthBarRight);

        Button btnMove = findViewById(R.id.btnMove);
        btnMove.setOnClickListener(v -> vrRenderer.moveForward());

        // Spawn Native Web Controller Server targeting the Renderer on port 8080!
        moveServer = new MoveServer(vrRenderer);
        moveServer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        sensorHandler.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        sensorHandler.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioEngine != null) {
            audioEngine.stopAudio();
        }
        if (moveServer != null) {
            moveServer.stopServer();
        }
    }

    @Override
    public void onObjectCollected(int objectId, GameObject.Type type) {
        if (gameManager.isGameLocked()) return; // Stop executing collection behaviors if final win-state initiated

        audioEngine.playCollectionSound(); // Ping!
        Toast.makeText(this, "Collected " + type.name(), Toast.LENGTH_SHORT).show();
        
        // Dynamically boost backpack counts
        switch (type) {
            case WATER: backpackManager.addWater(); break;
            case FOOD: backpackManager.addFood(); break;
            case WOOD: backpackManager.addWood(); break;
            case MATCHBOX: backpackManager.addMatchbox(); break;
        }
        
        // Pass to logic threshold checker
        gameManager.processItemCollection(type, backpackManager);
        
        String invText = backpackManager.getInventoryText();
        vrRenderer.updateInventory(invText);
    }
}
