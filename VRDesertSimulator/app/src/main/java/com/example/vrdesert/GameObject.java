package com.example.vrdesert;

public class GameObject {
    public enum Type { WATER, FOOD, WOOD, MATCHBOX }
    
    public float x;
    public float y;
    public float z;
    public boolean isCollected;
    public Type type;

    public GameObject(float x, float y, float z, Type type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.isCollected = false;
    }
}
