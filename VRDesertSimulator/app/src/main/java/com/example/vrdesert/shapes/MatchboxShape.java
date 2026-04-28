package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A realistic matchbox shape — flat, wide, thin rectangular box
 * with a darker striker strip on one side and a lighter inner drawer detail.
 */
public class MatchboxShape {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;

    private int mProgram;
    private final int vertexCount;

    public MatchboxShape(int programId) {
        this.mProgram = programId;

        // Main box: 36 vertices + striker strip: 6 + drawer end: 6 = 48
        int totalVerts = 36 + 6 + 6;
        vertexCount = totalVerts;

        float[] verts = new float[totalVerts * 3];
        float[] cols = new float[totalVerts * 4];
        float[] norms = new float[totalVerts * 3];
        int vi = 0, ci = 0, ni = 0;

        // Matchbox proportions: wide, thin, medium depth
        float hw = 0.5f;   // half width (X)
        float hh = 0.15f;  // half height (Y) — thin!
        float hd = 0.3f;   // half depth (Z)

        // Red body color
        float bR = 0.72f, bG = 0.12f, bB = 0.10f;

        // Build 6 faces of the main box
        float[][][] faces = {
            {{-hw,-hh,hd},{hw,-hh,hd},{-hw,hh,hd},{-hw,hh,hd},{hw,-hh,hd},{hw,hh,hd}},       // front
            {{-hw,-hh,-hd},{-hw,hh,-hd},{hw,-hh,-hd},{hw,-hh,-hd},{-hw,hh,-hd},{hw,hh,-hd}},   // back
            {{-hw,-hh,-hd},{-hw,-hh,hd},{-hw,hh,-hd},{-hw,hh,-hd},{-hw,-hh,hd},{-hw,hh,hd}},   // left
            {{hw,-hh,-hd},{hw,hh,-hd},{hw,-hh,hd},{hw,-hh,hd},{hw,hh,-hd},{hw,hh,hd}},         // right
            {{-hw,hh,-hd},{-hw,hh,hd},{hw,hh,-hd},{hw,hh,-hd},{-hw,hh,hd},{hw,hh,hd}},         // top
            {{-hw,-hh,-hd},{hw,-hh,-hd},{-hw,-hh,hd},{-hw,-hh,hd},{hw,-hh,-hd},{hw,-hh,hd}}     // bottom
        };
        float[][] fn = {{0,0,1},{0,0,-1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        float[][] fc = {
            {bR,bG,bB}, {bR*0.8f,bG*0.8f,bB*0.8f}, {bR*0.85f,bG*0.85f,bB*0.85f},
            {bR*0.9f,bG*0.9f,bB*0.9f}, {bR*1.1f,bG*0.15f,bB*0.12f}, {bR*0.6f,bG*0.6f,bB*0.6f}
        };

        for (int f = 0; f < 6; f++) {
            for (int v = 0; v < 6; v++) {
                verts[vi++]=faces[f][v][0]; verts[vi++]=faces[f][v][1]; verts[vi++]=faces[f][v][2];
                norms[ni++]=fn[f][0]; norms[ni++]=fn[f][1]; norms[ni++]=fn[f][2];
                cols[ci++]=Math.min(1f,fc[f][0]); cols[ci++]=Math.min(1f,fc[f][1]); cols[ci++]=Math.min(1f,fc[f][2]); cols[ci++]=1f;
            }
        }

        // Striker strip on right side face — darker rough band
        float sR = 0.20f, sG = 0.12f, sB = 0.08f;
        float sd = 0.01f; // slight protrusion
        float sh = hh * 0.5f; // half height of strip
        float[][] strikerVerts = {
            {hw+sd, -sh, -hd*0.8f}, {hw+sd, sh, -hd*0.8f}, {hw+sd, -sh, hd*0.8f},
            {hw+sd, -sh, hd*0.8f}, {hw+sd, sh, -hd*0.8f}, {hw+sd, sh, hd*0.8f}
        };
        for (float[] v : strikerVerts) {
            verts[vi++]=v[0]; verts[vi++]=v[1]; verts[vi++]=v[2];
            norms[ni++]=1; norms[ni++]=0; norms[ni++]=0;
            cols[ci++]=sR; cols[ci++]=sG; cols[ci++]=sB; cols[ci++]=1f;
        }

        // Drawer detail — lighter colored band on front bottom edge
        float dR = 0.85f, dG = 0.75f, dB = 0.55f;
        float dOff = 0.005f;
        float dH = hh * 0.4f;
        float[][] drawerVerts = {
            {-hw*0.9f, -hh, hd+dOff}, {hw*0.9f, -hh, hd+dOff}, {-hw*0.9f, -hh+dH, hd+dOff},
            {-hw*0.9f, -hh+dH, hd+dOff}, {hw*0.9f, -hh, hd+dOff}, {hw*0.9f, -hh+dH, hd+dOff}
        };
        for (float[] v : drawerVerts) {
            verts[vi++]=v[0]; verts[vi++]=v[1]; verts[vi++]=v[2];
            norms[ni++]=0; norms[ni++]=0; norms[ni++]=1;
            cols[ci++]=dR; cols[ci++]=dG; cols[ci++]=dB; cols[ci++]=1f;
        }

        vertexBuffer = makeBuf(verts);
        colorBuffer = makeBuf(cols);
        normalBuffer = makeBuf(norms);
    }

    private FloatBuffer makeBuf(float[] d) {
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(d); fb.position(0);
        return fb;
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
}
