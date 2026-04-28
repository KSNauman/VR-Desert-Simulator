package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A pure OpenGL rendered health bar for the VR HUD.
 * Gradient from red to yellow to green based on health percentage.
 * Includes a white outline frame and smooth fill animation.
 */
public class HealthBarGL {

    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 _vColor;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  _vColor = vColor;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 _vColor;" +
        "void main() {" +
        "  gl_FragColor = _vColor;" +
        "}";

    private int mProgram;
    private FloatBuffer bgVertexBuffer;
    private FloatBuffer bgColorBuffer;
    private FloatBuffer fillVertexBuffer;
    private FloatBuffer fillColorBuffer;
    private FloatBuffer borderVertexBuffer;
    private FloatBuffer borderColorBuffer;

    private float barWidth = 160f;
    private float barHeight = 16f;
    private float currentFill = 0.3f;
    private float targetFill = 0.3f;

    public HealthBarGL() {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);

        // Background (dark)
        float[] bgVerts = {0,0, barWidth,0, 0,barHeight, 0,barHeight, barWidth,0, barWidth,barHeight};
        bgVertexBuffer = makeBuf(bgVerts);
        float[] bgCols = new float[24];
        for (int i = 0; i < 6; i++) {
            bgCols[i*4]=0.1f; bgCols[i*4+1]=0.1f; bgCols[i*4+2]=0.1f; bgCols[i*4+3]=0.7f;
        }
        bgColorBuffer = makeBuf(bgCols);

        // Border (white outline) — 4 lines = 8 vertices
        float[] borderVerts = {
            0,0, barWidth,0,
            barWidth,0, barWidth,barHeight,
            barWidth,barHeight, 0,barHeight,
            0,barHeight, 0,0
        };
        borderVertexBuffer = makeBuf(borderVerts);
        float[] borderCols = new float[32];
        for (int i = 0; i < 8; i++) {
            borderCols[i*4]=1f; borderCols[i*4+1]=1f; borderCols[i*4+2]=1f; borderCols[i*4+3]=0.8f;
        }
        borderColorBuffer = makeBuf(borderCols);

        rebuildFill();
    }

    public void setHealth(float healthPercent) {
        targetFill = Math.max(0f, Math.min(1f, healthPercent / 100f));
    }

    public void update() {
        // Smooth lerp toward target
        currentFill += (targetFill - currentFill) * 0.08f;
        rebuildFill();
    }

    private void rebuildFill() {
        float fw = barWidth * currentFill;
        float p = 2f; // padding
        float[] fv = {p,p, fw-p,p, p,barHeight-p, p,barHeight-p, fw-p,p, fw-p,barHeight-p};
        fillVertexBuffer = makeBuf(fv);

        // Color gradient based on fill level
        float r, g;
        if (currentFill < 0.5f) {
            r = 1f; g = currentFill * 2f;
        } else {
            r = 1f - (currentFill - 0.5f) * 2f; g = 1f;
        }
        float[] fc = new float[24];
        for (int i = 0; i < 6; i++) {
            // Left vertices slightly darker
            float darken = (i == 0 || i == 2 || i == 3) ? 0.8f : 1f;
            fc[i*4]=r*darken; fc[i*4+1]=g*darken; fc[i*4+2]=0f; fc[i*4+3]=0.9f;
        }
        fillColorBuffer = makeBuf(fc);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int posH = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int colH = GLES20.glGetAttribLocation(mProgram, "vColor");
        int mvpH = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpH, 1, false, mvpMatrix, 0);

        // Draw background
        GLES20.glEnableVertexAttribArray(posH);
        GLES20.glVertexAttribPointer(posH, 2, GLES20.GL_FLOAT, false, 0, bgVertexBuffer);
        GLES20.glEnableVertexAttribArray(colH);
        GLES20.glVertexAttribPointer(colH, 4, GLES20.GL_FLOAT, false, 0, bgColorBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        // Draw fill
        if (currentFill > 0.01f) {
            GLES20.glVertexAttribPointer(posH, 2, GLES20.GL_FLOAT, false, 0, fillVertexBuffer);
            GLES20.glVertexAttribPointer(colH, 4, GLES20.GL_FLOAT, false, 0, fillColorBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }

        // Draw border
        GLES20.glLineWidth(2.0f);
        GLES20.glVertexAttribPointer(posH, 2, GLES20.GL_FLOAT, false, 0, borderVertexBuffer);
        GLES20.glVertexAttribPointer(colH, 4, GLES20.GL_FLOAT, false, 0, borderColorBuffer);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8);

        GLES20.glDisableVertexAttribArray(posH);
        GLES20.glDisableVertexAttribArray(colH);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private FloatBuffer makeBuf(float[] d) {
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(d); fb.position(0);
        return fb;
    }

    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
}
