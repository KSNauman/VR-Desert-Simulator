package com.example.vrdesert;

import android.content.Context;
import android.widget.Toast;

/**
 * Manages game state: health, win condition.
 * Health updates are pushed to the VR renderer's GL health bar via a listener.
 */
public class GameManager {

    public interface HealthListener {
        void onHealthChanged(int health);
    }

    private int health = 30;
    private final int MAX_HEALTH = 100;
    private boolean gameLocked = false;
    private Context context;
    private HealthListener healthListener;

    public GameManager(Context context) {
        this.context = context;
    }

    public void setHealthListener(HealthListener listener) {
        this.healthListener = listener;
        // Push initial value
        if (healthListener != null) {
            healthListener.onHealthChanged(health);
        }
    }

    public boolean isGameLocked() {
        return gameLocked;
    }

    public int getHealth() {
        return health;
    }

    public void processItemCollection(GameObject.Type type, BackpackManager backpack) {
        if (gameLocked) return;

        // Health-giving items
        if (type == GameObject.Type.WATER || type == GameObject.Type.FOOD) {
            health = Math.min(health + 20, MAX_HEALTH);
            notifyHealthChanged();
        }

        checkWinCondition(backpack);
    }

    private void notifyHealthChanged() {
        if (healthListener != null) {
            healthListener.onHealthChanged(health);
        }
    }

    private void checkWinCondition(BackpackManager backpack) {
        // Condition: Enough health, plus one Wood and one Matchbox
        if (health >= 80 && backpack.getWoodCount() > 0 && backpack.getMatchboxCount() > 0) {
            gameLocked = true; // Lock game loop
            
            Toast.makeText(context, "You lit a fire! The smoke alerted rescuers.", Toast.LENGTH_LONG).show();
            
            // Simulating a delay for the final event outcome using a handler
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(context, "You were rescued. Congratulations!", Toast.LENGTH_LONG).show();
            }, 4000);
        }
    }
}
