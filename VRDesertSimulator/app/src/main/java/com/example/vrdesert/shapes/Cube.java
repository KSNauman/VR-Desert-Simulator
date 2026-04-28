package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

public class Cube {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;
    
    private int mProgram;

    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;

    // A simple 1x1x1 cube centered on origin
    static float cubeCoords[] = {
        // Front face
        -0.5f, -0.5f,  0.5f,
         0.5f, -0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        // Back face
        -0.5f, -0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
        // Left face
        -0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f,  0.5f,
        -0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,
        -0.5f, -0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,
        // Right face
         0.5f, -0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
         0.5f, -0.5f,  0.5f,
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f, -0.5f,
         0.5f,  0.5f,  0.5f,
        // Top face
        -0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f,  0.5f,
         0.5f,  0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        // Bottom face
        -0.5f, -0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f,  0.5f,
        -0.5f, -0.5f,  0.5f,
         0.5f, -0.5f, -0.5f,
         0.5f, -0.5f,  0.5f
    };

    // Face normals for all 36 vertices
    static float[] cubeNormals = generateNormals();

    static float[] generateNormals() {
        float[] normals = new float[108]; // 36 vertices * 3 components
        float[][] faceNormals = {
            {0, 0, 1},   // front
            {0, 0, -1},  // back
            {-1, 0, 0},  // left
            {1, 0, 0},   // right
            {0, 1, 0},   // top
            {0, -1, 0}   // bottom
        };
        for (int face = 0; face < 6; face++) {
            for (int vert = 0; vert < 6; vert++) {
                int idx = (face * 6 + vert) * 3;
                normals[idx]     = faceNormals[face][0];
                normals[idx + 1] = faceNormals[face][1];
                normals[idx + 2] = faceNormals[face][2];
            }
        }
        return normals;
    }

    // Generate uniform colors (no more baked face darkness — real lighting handles it)
    static float[] generateColors(float r, float g, float b) {
        float[] colors = new float[144]; // 36 vertices * 4 color components
        for(int i = 0; i < 36; i++) {
            colors[i*4] = r;
            colors[i*4+1] = g;
            colors[i*4+2] = b;
            colors[i*4+3] = 1.0f;
        }
        return colors;
    }

    private final int vertexCount = 36;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private final int colorStride = COLORS_PER_VERTEX * 4;

    public Cube(int programId, float r, float g, float b) {
        this.mProgram = programId;

        ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);

        float[] generated = generateColors(r, g, b);
        ByteBuffer cb = ByteBuffer.allocateDirect(generated.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(generated);
        colorBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(cubeNormals.length * 4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(cubeNormals);
        normalBuffer.position(0);
    }

    public void draw(float[] mvpMatrix) {
        draw(mvpMatrix, null, null);
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] normalMatrix) {
        GLES20.glUseProgram(mProgram);

        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glVertexAttribPointer(mColorHandle, COLORS_PER_VERTEX,
                GLES20.GL_FLOAT, false, colorStride, colorBuffer);

        int normalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
        if (normalHandle >= 0) {
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glVertexAttribPointer(normalHandle, 3,
                    GLES20.GL_FLOAT, false, 3 * 4, normalBuffer);
        }

        int mVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mVPMatrixHandle, 1, false, mvpMatrix, 0);

        if (modelMatrix != null) {
            int mModelHandle = GLES20.glGetUniformLocation(mProgram, "uModelMatrix");
            if (mModelHandle >= 0)
                GLES20.glUniformMatrix4fv(mModelHandle, 1, false, modelMatrix, 0);
        }
        if (normalMatrix != null) {
            int mNormalHandle = GLES20.glGetUniformLocation(mProgram, "uNormalMatrix");
            if (mNormalHandle >= 0)
                GLES20.glUniformMatrix4fv(mNormalHandle, 1, false, normalMatrix, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
        if (normalHandle >= 0) GLES20.glDisableVertexAttribArray(normalHandle);
    }
}
