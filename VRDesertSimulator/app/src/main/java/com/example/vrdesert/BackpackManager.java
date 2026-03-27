package com.example.vrdesert;

public class BackpackManager {
    private int waterCount = 0;
    private int foodCount = 0;
    private int woodCount = 0;
    private int matchboxCount = 0;

    public void addWater() { waterCount++; }
    public void addFood() { foodCount++; }
    public void addWood() { woodCount++; }
    public void addMatchbox() { matchboxCount++; }

    public int getWoodCount() { return woodCount; }
    public int getMatchboxCount() { return matchboxCount; }

    public String getInventoryText() {
        StringBuilder sb = new StringBuilder("Backpack:\n");
        if (waterCount > 0) sb.append("Water: ").append(waterCount).append("\n");
        if (foodCount > 0) sb.append("Food: ").append(foodCount).append("\n");
        if (woodCount > 0) sb.append("Wood: ").append(woodCount).append("\n");
        if (matchboxCount > 0) sb.append("Matchbox: ").append(matchboxCount).append("\n");
        
        if (waterCount == 0 && foodCount == 0 && woodCount == 0 && matchboxCount == 0) {
            sb.append("Empty");
        }
        return sb.toString().trim();
    }
}
