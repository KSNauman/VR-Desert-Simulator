package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * An 8-sided cylindrical canteen shape for water items.
 * Blue-green body with a lighter top cap (lid).
 */
public class WaterCanteen {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;

    private int mProgram;

    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;

    private final int vertexCount;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private final int colorStride = COLORS_PER_VERTEX * 4;

    public WaterCanteen(int programId) {
        this.mProgram = programId;

        int sides = 8;
        float radius = 0.5f;
        float halfHeight = 0.5f;

        // Each side face = 2 triangles = 6 vertices
        // Top cap = sides triangles = sides * 3 vertices
        // Bottom cap = sides triangles = sides * 3 vertices
        int totalVertices = sides * 6 + sides * 3 * 2;
        vertexCount = totalVertices;

        float[] vertices = new float[totalVertices * 3];
        float[] colors = new float[totalVertices * 4];
        float[] normals = new float[totalVertices * 3];
        int vi = 0, ci = 0, ni = 0;

        // Body color: blue-green
        float bR = 0.15f, bG = 0.45f, bB = 0.55f;
        // Top cap color: lighter lid
        float tR = 0.35f, tG = 0.65f, tB = 0.70f;
        // Bottom cap color: darker
        float btR = 0.10f, btG = 0.30f, btB = 0.40f;

        for (int i = 0; i < sides; i++) {
            float angle0 = (float) (2 * Math.PI * i / sides);
            float angle1 = (float) (2 * Math.PI * (i + 1) / sides);

            float x0 = radius * (float) Math.cos(angle0);
            float z0 = radius * (float) Math.sin(angle0);
            float x1 = radius * (float) Math.cos(angle1);
            float z1 = radius * (float) Math.sin(angle1);

            // Normal for this face
            float nx = (float) Math.cos((angle0 + angle1) / 2);
            float nz = (float) Math.sin((angle0 + angle1) / 2);

            // Side face: two triangles
            // Triangle 1: bottom-left, bottom-right, top-left
            float[][] sideVerts = {
                {x0, -halfHeight, z0}, {x1, -halfHeight, z1}, {x0, halfHeight, z0},
                {x0, halfHeight, z0}, {x1, -halfHeight, z1}, {x1, halfHeight, z1}
            };
            for (float[] v : sideVerts) {
                vertices[vi++] = v[0]; vertices[vi++] = v[1]; vertices[vi++] = v[2];
                normals[ni++] = nx; normals[ni++] = 0f; normals[ni++] = nz;
                colors[ci++] = bR; colors[ci++] = bG; colors[ci++] = bB; colors[ci++] = 1f;
            }

            // Top cap triangle: center, edge0, edge1
            vertices[vi++] = 0; vertices[vi++] = halfHeight; vertices[vi++] = 0;
            vertices[vi++] = x0; vertices[vi++] = halfHeight; vertices[vi++] = z0;
            vertices[vi++] = x1; vertices[vi++] = halfHeight; vertices[vi++] = z1;
            for (int j = 0; j < 3; j++) {
                normals[ni++] = 0; normals[ni++] = 1; normals[ni++] = 0;
                colors[ci++] = tR; colors[ci++] = tG; colors[ci++] = tB; colors[ci++] = 1f;
            }

            // Bottom cap triangle: center, edge1, edge0 (reversed winding)
            vertices[vi++] = 0; vertices[vi++] = -halfHeight; vertices[vi++] = 0;
            vertices[vi++] = x1; vertices[vi++] = -halfHeight; vertices[vi++] = z1;
            vertices[vi++] = x0; vertices[vi++] = -halfHeight; vertices[vi++] = z0;
            for (int j = 0; j < 3; j++) {
                normals[ni++] = 0; normals[ni++] = -1; normals[ni++] = 0;
                colors[ci++] = btR; colors[ci++] = btG; colors[ci++] = btB; colors[ci++] = 1f;
            }
        }

        vertexBuffer = createFloatBuffer(vertices);
        colorBuffer = createFloatBuffer(colors);
        normalBuffer = createFloatBuffer(normals);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] normalMatrix) {
        GLES20.glUseProgram(mProgram);

        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int colorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, colorStride, colorBuffer);

        int normalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
        if (normalHandle >= 0) {
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, normalBuffer);
        }

        int mvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        int modelHandle = GLES20.glGetUniformLocation(mProgram, "uModelMatrix");
        if (modelHandle >= 0) {
            GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0);
        }

        int normalMatHandle = GLES20.glGetUniformLocation(mProgram, "uNormalMatrix");
        if (normalMatHandle >= 0) {
            GLES20.glUniformMatrix4fv(normalMatHandle, 1, false, normalMatrix, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        if (normalHandle >= 0) GLES20.glDisableVertexAttribArray(normalHandle);
    }
}
