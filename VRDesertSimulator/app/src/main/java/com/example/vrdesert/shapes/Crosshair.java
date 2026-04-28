package com.example.vrdesert.shapes;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Circular reticle crosshair with center dot and animated gaze-progress arc.
 * Replaces the old simple "+" lines.
 */
public class Crosshair {

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 uColor;" +
            "void main() {" +
            "  gl_FragColor = uColor;" + // Dynamic coloration
            "}";

    private FloatBuffer ringBuffer;
    private FloatBuffer dotBuffer;
    private FloatBuffer progressBuffer;
    private int mProgram;
    private boolean isTargeting = false;
    private float gazeProgress = 0f;

    private static final int RING_SEGMENTS = 32;
    private static final float RING_RADIUS = 18.0f;
    private static final float DOT_RADIUS = 3.0f;
    private static final int DOT_SEGMENTS = 12;

    public void setTargeting(boolean targeting) {
        this.isTargeting = targeting;
    }

    public void setGazeProgress(float progress) {
        this.gazeProgress = Math.max(0f, Math.min(1f, progress));
        rebuildProgress();
    }

    public Crosshair() {
        // Ring — circle made of line segments
        float[] ringCoords = new float[RING_SEGMENTS * 4]; // pairs of x,y for GL_LINES
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a0 = (float)(2 * Math.PI * i / RING_SEGMENTS);
            float a1 = (float)(2 * Math.PI * (i + 1) / RING_SEGMENTS);
            ringCoords[i * 4]     = RING_RADIUS * (float)Math.cos(a0);
            ringCoords[i * 4 + 1] = RING_RADIUS * (float)Math.sin(a0);
            ringCoords[i * 4 + 2] = RING_RADIUS * (float)Math.cos(a1);
            ringCoords[i * 4 + 3] = RING_RADIUS * (float)Math.sin(a1);
        }
        ringBuffer = makeBuf(ringCoords);

        // Center dot — small filled circle via triangle fan emulated as triangles
        float[] dotCoords = new float[DOT_SEGMENTS * 6]; // triangles
        for (int i = 0; i < DOT_SEGMENTS; i++) {
            float a0 = (float)(2 * Math.PI * i / DOT_SEGMENTS);
            float a1 = (float)(2 * Math.PI * (i + 1) / DOT_SEGMENTS);
            dotCoords[i * 6]     = 0;
            dotCoords[i * 6 + 1] = 0;
            dotCoords[i * 6 + 2] = DOT_RADIUS * (float)Math.cos(a0);
            dotCoords[i * 6 + 3] = DOT_RADIUS * (float)Math.sin(a0);
            dotCoords[i * 6 + 4] = DOT_RADIUS * (float)Math.cos(a1);
            dotCoords[i * 6 + 5] = DOT_RADIUS * (float)Math.sin(a1);
        }
        dotBuffer = makeBuf(dotCoords);

        // Progress arc (initially empty)
        progressBuffer = makeBuf(new float[RING_SEGMENTS * 4]);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    private void rebuildProgress() {
        if (gazeProgress <= 0.01f) return;
        int activeSegments = Math.max(1, (int)(RING_SEGMENTS * gazeProgress));
        float outerRadius = RING_RADIUS + 6.0f;
        float[] coords = new float[activeSegments * 4];
        for (int i = 0; i < activeSegments; i++) {
            float a0 = (float)(2 * Math.PI * i / RING_SEGMENTS) - (float)(Math.PI / 2);
            float a1 = (float)(2 * Math.PI * (i + 1) / RING_SEGMENTS) - (float)(Math.PI / 2);
            coords[i * 4]     = outerRadius * (float)Math.cos(a0);
            coords[i * 4 + 1] = outerRadius * (float)Math.sin(a0);
            coords[i * 4 + 2] = outerRadius * (float)Math.cos(a1);
            coords[i * 4 + 3] = outerRadius * (float)Math.sin(a1);
        }
        progressBuffer = makeBuf(coords);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        int colorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Ring
        GLES20.glLineWidth(2.5f);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, ringBuffer);
        if (isTargeting) {
            GLES20.glUniform4f(colorHandle, 0.3f, 1.0f, 0.3f, 0.9f);
        } else {
            GLES20.glUniform4f(colorHandle, 1.0f, 1.0f, 1.0f, 0.6f);
        }
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, RING_SEGMENTS * 2);

        // Center dot
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, dotBuffer);
        if (isTargeting) {
            GLES20.glUniform4f(colorHandle, 0.3f, 1.0f, 0.3f, 1.0f);
        } else {
            GLES20.glUniform4f(colorHandle, 1.0f, 1.0f, 1.0f, 0.8f);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, DOT_SEGMENTS * 3);

        // Progress arc
        if (gazeProgress > 0.01f && isTargeting) {
            int activeSegments = Math.max(1, (int)(RING_SEGMENTS * gazeProgress));
            GLES20.glLineWidth(4.0f);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, progressBuffer);
            GLES20.glUniform4f(colorHandle, 1.0f, 0.85f, 0.2f, 0.95f); // Gold progress
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, activeSegments * 2);
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private FloatBuffer makeBuf(float[] d) {
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(d); fb.position(0);
        return fb;
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
