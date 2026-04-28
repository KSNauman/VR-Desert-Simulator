package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * Circular vignette overlay for VR box lens matching.
 * Darkens the edges of each eye viewport to match the round lens shape,
 * reducing visible distortion at corners and improving immersion.
 */
public class Vignette {

    private final String vertexShaderCode =
        "attribute vec2 vPosition;" +
        "varying vec2 vUV;" +
        "void main() {" +
        "  gl_Position = vec4(vPosition, 0.0, 1.0);" +
        // Map from clip space (-1,1) to UV (0,1)
        "  vUV = vPosition * 0.5 + 0.5;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec2 vUV;" +
        "void main() {" +
        // Distance from center (0.5, 0.5)
        "  vec2 center = vec2(0.5, 0.5);" +
        // Squared light dark frame at edges
        "  float edgeX = smoothstep(0.0, 0.06, vUV.x) * smoothstep(0.0, 0.06, 1.0 - vUV.x);" +
        "  float edgeY = smoothstep(0.0, 0.06, vUV.y) * smoothstep(0.0, 0.06, 1.0 - vUV.y);" +
        "  float vignette = (1.0 - edgeX * edgeY) * 0.5;" +
        "  gl_FragColor = vec4(0.0, 0.0, 0.0, vignette);" +
        "}";

    private FloatBuffer vertexBuffer;
    private int mProgram;

    public Vignette() {
        // Full-screen quad in clip space
        float[] quad = {
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
            -1f,  1f,
             1f, -1f,
             1f,  1f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(quad.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(quad);
        vertexBuffer.position(0);

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);
    }

    /**
     * Draw the vignette overlay. Call once per eye AFTER all other rendering.
     * The viewport must already be set to the current eye's half-screen.
     */
    public void draw() {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
}
