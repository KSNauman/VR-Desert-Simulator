package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A flat desert floor with rich vertex-colored sand variation.
 * Static Y = -0.5, no terrain undulation — player and objects stay above ground.
 * 40x40 grid for detailed per-vertex color: sunlit patches, shadow areas, gravel, ripples.
 */
public class Plane {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;

    private int mProgram;

    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;

    private final int vertexCount;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private final int colorStride = COLORS_PER_VERTEX * 4;

    public Plane(int programId) {
        this.mProgram = programId;

        int gridSize = 40;
        float halfExtent = 200.0f;
        float cellSize = (halfExtent * 2) / gridSize;
        float y = -0.5f; // Flat — no height variation

        int totalVerts = gridSize * gridSize * 6;
        vertexCount = totalVerts;

        float[] vertices = new float[totalVerts * 3];
        float[] colors = new float[totalVerts * 4];
        float[] normals = new float[totalVerts * 3];
        int vi = 0, ci = 0, ni = 0;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x0 = -halfExtent + col * cellSize;
                float z0 = -halfExtent + row * cellSize;
                float x1 = x0 + cellSize;
                float z1 = z0 + cellSize;

                // Triangle 1
                float[][] tri1 = {{x0,y,z0}, {x0,y,z1}, {x1,y,z1}};
                // Triangle 2
                float[][] tri2 = {{x0,y,z0}, {x1,y,z1}, {x1,y,z0}};

                for (float[] v : tri1) {
                    vertices[vi++] = v[0]; vertices[vi++] = v[1]; vertices[vi++] = v[2];
                    normals[ni++] = 0; normals[ni++] = 1; normals[ni++] = 0;
                    float[] c = sandColor(v[0], v[2]);
                    colors[ci++] = c[0]; colors[ci++] = c[1]; colors[ci++] = c[2]; colors[ci++] = 1f;
                }
                for (float[] v : tri2) {
                    vertices[vi++] = v[0]; vertices[vi++] = v[1]; vertices[vi++] = v[2];
                    normals[ni++] = 0; normals[ni++] = 1; normals[ni++] = 0;
                    float[] c = sandColor(v[0], v[2]);
                    colors[ci++] = c[0]; colors[ci++] = c[1]; colors[ci++] = c[2]; colors[ci++] = 1f;
                }
            }
        }

        vertexBuffer = makeBuf(vertices);
        colorBuffer = makeBuf(colors);
        normalBuffer = makeBuf(normals);
    }

    /**
     * Rich sand color variation based on position.
     * Sunlit patches, shadowed areas, gravel, ripple patterns, warm/cool shifts.
     */
    private float[] sandColor(float x, float z) {
        float baseR = 0.88f, baseG = 0.73f, baseB = 0.42f;

        // Large-scale color patches
        float noise1 = (float)(Math.sin(x * 0.03f + 0.5f) * Math.cos(z * 0.025f));
        baseR += noise1 * 0.1f;
        baseG += noise1 * 0.07f;
        baseB += noise1 * 0.04f;

        // Medium ripple patterns
        float ripple = (float)Math.sin(x * 0.07f + z * 0.04f + 1.3f);
        baseR += ripple * 0.05f;
        baseG += ripple * 0.04f;
        baseB += ripple * 0.02f;

        // Secondary ripple
        float ripple2 = (float)Math.cos(x * 0.05f - z * 0.08f + 2.7f);
        baseR += ripple2 * 0.04f;
        baseG += ripple2 * 0.03f;

        // Fine variation
        float fine = (float)(Math.sin(x * 0.25f + z * 0.15f) * 0.03f);
        baseR += fine;
        baseG += fine * 0.8f;
        baseB += fine * 0.5f;

        // Position-based warm/cool shift
        float noise2 = (float)(Math.sin(x * 0.13f + z * 0.17f) * 0.5 + 0.5);
        baseR += (noise2 - 0.5f) * 0.12f;
        baseG += (noise2 - 0.5f) * 0.08f;

        // Scattered dark gravel patches
        float gravel = (float)(Math.sin(x * 0.47f + z * 0.53f) *
                               Math.sin(x * 0.71f - z * 0.61f));
        if (gravel > 0.7f) {
            float darken = (gravel - 0.7f) * 1.5f;
            baseR -= darken * 0.25f;
            baseG -= darken * 0.2f;
            baseB -= darken * 0.1f;
        }

        // Reddish iron-sand tint in some areas
        float redShift = (float)(Math.sin(x * 0.31f - z * 0.23f + 1.5f) * 0.5 + 0.5) * 0.08f;
        baseR += redShift;
        baseB -= redShift * 0.5f;

        return new float[]{
            Math.max(0.15f, Math.min(1f, baseR)),
            Math.max(0.12f, Math.min(1f, baseG)),
            Math.max(0.08f, Math.min(1f, baseB))
        };
    }

    private FloatBuffer makeBuf(float[] d) {
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(d); fb.position(0);
        return fb;
    }

    public void draw(float[] mvpMatrix) {
        draw(mvpMatrix, null, null);
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] normalMatrix) {
        GLES20.glUseProgram(mProgram);

        int posH = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posH);
        GLES20.glVertexAttribPointer(posH, 3, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int colH = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(colH);
        GLES20.glVertexAttribPointer(colH, 4, GLES20.GL_FLOAT, false, colorStride, colorBuffer);

        int normH = GLES20.glGetAttribLocation(mProgram, "aNormal");
        if (normH >= 0) {
            GLES20.glEnableVertexAttribArray(normH);
            GLES20.glVertexAttribPointer(normH, 3, GLES20.GL_FLOAT, false, 12, normalBuffer);
        }

        int mvpH = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpH, 1, false, mvpMatrix, 0);

        if (modelMatrix != null) {
            int mh = GLES20.glGetUniformLocation(mProgram, "uModelMatrix");
            if (mh >= 0) GLES20.glUniformMatrix4fv(mh, 1, false, modelMatrix, 0);
        }
        if (normalMatrix != null) {
            int nh = GLES20.glGetUniformLocation(mProgram, "uNormalMatrix");
            if (nh >= 0) GLES20.glUniformMatrix4fv(nh, 1, false, normalMatrix, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(posH);
        GLES20.glDisableVertexAttribArray(colH);
        if (normH >= 0) GLES20.glDisableVertexAttribArray(normH);
    }
}
