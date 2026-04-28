package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A saguaro-style cactus with a main trunk and two arms.
 * Built from hexagonal prisms in dark green with lighter ridges.
 */
public class Cactus {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;
    private int mProgram;
    private final int vertexCount;

    private static final int SIDES = 8;

    public Cactus(int programId) {
        this.mProgram = programId;

        // Main trunk + left arm + right arm + 3 top caps
        int vertsPerCylinder = SIDES * 6; // body
        int vertsPerCap = SIDES * 3;      // top cap
        int partsCount = 3; // trunk, left arm, right arm
        int totalVerts = partsCount * (vertsPerCylinder + vertsPerCap);
        vertexCount = totalVerts;

        float[] verts = new float[totalVerts * 3];
        float[] cols = new float[totalVerts * 4];
        float[] norms = new float[totalVerts * 3];
        int[] idx = {0, 0, 0}; // vi, ci, ni packed

        // Main trunk: tall, centered
        buildCylinder(verts, cols, norms, idx,
            0f, 0f, 0f,         // base position
            0.18f,               // radius
            3.5f,                // height
            0.18f, 0.52f, 0.15f, // dark green
            0.25f, 0.60f, 0.20f  // lighter ridge green
        );

        // Left arm: branches out at Y=1.8, goes up
        // Arm is a shorter cylinder offset to the left and tilted via position
        buildCylinder(verts, cols, norms, idx,
            -0.45f, 1.8f, 0f,   // base position (offset left)
            0.12f,               // radius (thinner)
            1.4f,                // height
            0.16f, 0.48f, 0.13f, // slightly different green
            0.22f, 0.56f, 0.18f
        );

        // Right arm: branches out at Y=2.3, goes up
        buildCylinder(verts, cols, norms, idx,
            0.40f, 2.3f, 0f,    // base position (offset right)
            0.11f,               // radius (thinner)
            1.0f,                // height
            0.20f, 0.50f, 0.14f,
            0.26f, 0.58f, 0.19f
        );

        vertexBuffer = makeBuf(verts);
        colorBuffer = makeBuf(cols);
        normalBuffer = makeBuf(norms);
    }

    private void buildCylinder(float[] verts, float[] cols, float[] norms, int[] idx,
                                float cx, float cy, float cz,
                                float radius, float height,
                                float r1, float g1, float b1,
                                float r2, float g2, float b2) {
        int vi = idx[0], ci = idx[1], ni = idx[2];

        for (int i = 0; i < SIDES; i++) {
            float a0 = (float)(2 * Math.PI * i / SIDES);
            float a1 = (float)(2 * Math.PI * (i + 1) / SIDES);
            float x0 = radius * (float)Math.cos(a0);
            float z0 = radius * (float)Math.sin(a0);
            float x1 = radius * (float)Math.cos(a1);
            float z1 = radius * (float)Math.sin(a1);
            float nx = (float)Math.cos((a0 + a1) / 2);
            float nz = (float)Math.sin((a0 + a1) / 2);

            // Alternate colors for ridge effect
            float cr, cg, cb;
            if (i % 2 == 0) { cr = r1; cg = g1; cb = b1; }
            else { cr = r2; cg = g2; cb = b2; }

            // Side face: 2 triangles
            float[][] sv = {
                {cx+x0, cy, cz+z0}, {cx+x1, cy, cz+z1}, {cx+x0, cy+height, cz+z0},
                {cx+x0, cy+height, cz+z0}, {cx+x1, cy, cz+z1}, {cx+x1, cy+height, cz+z1}
            };
            for (float[] v : sv) {
                verts[vi++]=v[0]; verts[vi++]=v[1]; verts[vi++]=v[2];
                norms[ni++]=nx; norms[ni++]=0; norms[ni++]=nz;
                cols[ci++]=cr; cols[ci++]=cg; cols[ci++]=cb; cols[ci++]=1f;
            }

            // Top cap
            verts[vi++]=cx; verts[vi++]=cy+height; verts[vi++]=cz;
            verts[vi++]=cx+x0; verts[vi++]=cy+height; verts[vi++]=cz+z0;
            verts[vi++]=cx+x1; verts[vi++]=cy+height; verts[vi++]=cz+z1;
            // Cap is slightly darker green on top
            float topR = cr * 0.7f, topG = cg * 0.85f, topB = cb * 0.7f;
            for (int j = 0; j < 3; j++) {
                norms[ni++]=0; norms[ni++]=1; norms[ni++]=0;
                cols[ci++]=topR; cols[ci++]=topG; cols[ci++]=topB; cols[ci++]=1f;
            }
        }

        idx[0] = vi; idx[1] = ci; idx[2] = ni;
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] normalMatrix) {
        GLES20.glUseProgram(mProgram);
        int p = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(p);
        GLES20.glVertexAttribPointer(p, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
        int c = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(c);
        GLES20.glVertexAttribPointer(c, 4, GLES20.GL_FLOAT, false, 16, colorBuffer);
        int n = GLES20.glGetAttribLocation(mProgram, "aNormal");
        if (n >= 0) { GLES20.glEnableVertexAttribArray(n); GLES20.glVertexAttribPointer(n, 3, GLES20.GL_FLOAT, false, 12, normalBuffer); }
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);
        int mh = GLES20.glGetUniformLocation(mProgram, "uModelMatrix");
        if (mh >= 0) GLES20.glUniformMatrix4fv(mh, 1, false, modelMatrix, 0);
        int nh = GLES20.glGetUniformLocation(mProgram, "uNormalMatrix");
        if (nh >= 0) GLES20.glUniformMatrix4fv(nh, 1, false, normalMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(p);
        GLES20.glDisableVertexAttribArray(c);
        if (n >= 0) GLES20.glDisableVertexAttribArray(n);
    }

    private FloatBuffer makeBuf(float[] d) {
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(d); fb.position(0);
        return fb;
    }
}
