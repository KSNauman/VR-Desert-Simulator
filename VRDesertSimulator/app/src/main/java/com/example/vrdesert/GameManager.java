package com.example.vrdesert;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.Toast;

public class GameManager {
    private int health = 30;
    private final int MAX_HEALTH = 100;
    private boolean gameLocked = false;
    private Context context;
    private ProgressBar healthBarLeft;
    private ProgressBar healthBarRight;

    public GameManager(Context context, ProgressBar healthBarLeft, ProgressBar healthBarRight) {
        this.context = context;
        this.healthBarLeft = healthBarLeft;
        this.healthBarRight = healthBarRight;
        updateHealthBar();
    }

    public boolean isGameLocked() {
        return gameLocked;
    }

    public void processItemCollection(GameObject.Type type, BackpackManager backpack) {
        if (gameLocked) return;

        // Health-giving items
        if (type == GameObject.Type.WATER || type == GameObject.Type.FOOD) {
            health = Math.min(health + 20, MAX_HEALTH);
            updateHealthBar();
        }

        checkWinCondition(backpack);
    }

    private void updateHealthBar() {
        if (healthBarLeft != null && healthBarRight != null) {
            // Must run on UI Thread in reality, since Backpack update handles GL, we rely on Activity Handler safely wrapping this
            healthBarLeft.post(() -> {
                healthBarLeft.setProgress(health);
                healthBarRight.setProgress(health);
            });
        }
    }

    private void checkWinCondition(BackpackManager backpack) {
        // Condition: Enough health, plus one Wood and one Matchbox
        if (health >= 80 && backpack.getWoodCount() > 0 && backpack.getMatchboxCount() > 0) {
            gameLocked = true; // Lock game loop
            
            Toast.makeText(context, "You lit a fire! The smoke alerted rescuers.", Toast.LENGTH_LONG).show();
            
            // Simulating a delay for the final event outcome
            if (healthBarLeft != null) {
                healthBarLeft.postDelayed(() -> {
                    Toast.makeText(context, "You were rescued. Congratulations!", Toast.LENGTH_LONG).show();
                }, 4000);
            }
        }
    }
}
