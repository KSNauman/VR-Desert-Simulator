package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A wooden supply crate for food items.
 * Main box body with darker plank slat strips on front/back faces.
 */
public class FoodCrate {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;
    private int mProgram;
    private final int vertexCount;

    public FoodCrate(int programId) {
        this.mProgram = programId;

        // Main body: 36 verts + 4 slats * 6 verts = 60
        int totalVerts = 60;
        vertexCount = totalVerts;
        float[] verts = new float[totalVerts * 3];
        float[] cols = new float[totalVerts * 4];
        float[] norms = new float[totalVerts * 3];
        int vi = 0, ci = 0, ni = 0;

        float hw = 0.5f, hh = 0.4f, hd = 0.5f;
        float mR = 0.55f, mG = 0.35f, mB = 0.15f;

        float[][][] faces = {
            {{-hw,-hh,hd},{hw,-hh,hd},{-hw,hh,hd},{-hw,hh,hd},{hw,-hh,hd},{hw,hh,hd}},
            {{-hw,-hh,-hd},{-hw,hh,-hd},{hw,-hh,-hd},{hw,-hh,-hd},{-hw,hh,-hd},{hw,hh,-hd}},
            {{-hw,-hh,-hd},{-hw,-hh,hd},{-hw,hh,-hd},{-hw,hh,-hd},{-hw,-hh,hd},{-hw,hh,hd}},
            {{hw,-hh,-hd},{hw,hh,-hd},{hw,-hh,hd},{hw,-hh,hd},{hw,hh,-hd},{hw,hh,hd}},
            {{-hw,hh,-hd},{-hw,hh,hd},{hw,hh,-hd},{hw,hh,-hd},{-hw,hh,hd},{hw,hh,hd}},
            {{-hw,-hh,-hd},{hw,-hh,-hd},{-hw,-hh,hd},{-hw,-hh,hd},{hw,-hh,-hd},{hw,-hh,hd}}
        };
        float[][] fn = {{0,0,1},{0,0,-1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        float[][] fc = {
            {mR,mG,mB}, {mR*0.8f,mG*0.8f,mB*0.8f}, {mR*0.85f,mG*0.85f,mB*0.85f},
            {mR*0.9f,mG*0.9f,mB*0.9f}, {mR*1.1f,mG*1.1f,mB*1.1f}, {mR*0.7f,mG*0.7f,mB*0.7f}
        };

        for (int f = 0; f < 6; f++) {
            for (int v = 0; v < 6; v++) {
                verts[vi++]=faces[f][v][0]; verts[vi++]=faces[f][v][1]; verts[vi++]=faces[f][v][2];
                norms[ni++]=fn[f][0]; norms[ni++]=fn[f][1]; norms[ni++]=fn[f][2];
                cols[ci++]=Math.min(1f,fc[f][0]); cols[ci++]=Math.min(1f,fc[f][1]);
                cols[ci++]=Math.min(1f,fc[f][2]); cols[ci++]=1f;
            }
        }

        // Plank slats on front and back
        float sR = 0.35f, sG = 0.22f, sB = 0.08f;
        float slatH = 0.06f;
        float[][] slatY = {{-hh*0.3f-slatH,-hh*0.3f+slatH},{hh*0.3f-slatH,hh*0.3f+slatH}};

        // Front slats
        for (float[] sy : slatY) {
            float sz = hd + 0.02f;
            float[][] sv = {
                {-hw,sy[0],sz},{hw,sy[0],sz},{-hw,sy[1],sz},
                {-hw,sy[1],sz},{hw,sy[0],sz},{hw,sy[1],sz}
            };
            for (float[] v : sv) {
                verts[vi++]=v[0]; verts[vi++]=v[1]; verts[vi++]=v[2];
                norms[ni++]=0; norms[ni++]=0; norms[ni++]=1;
                cols[ci++]=sR; cols[ci++]=sG; cols[ci++]=sB; cols[ci++]=1f;
            }
        }
        // Back slats
        for (float[] sy : slatY) {
            float sz = -(hd + 0.02f);
            float[][] sv = {
                {-hw,sy[0],sz},{-hw,sy[1],sz},{hw,sy[0],sz},
                {hw,sy[0],sz},{-hw,sy[1],sz},{hw,sy[1],sz}
            };
            for (float[] v : sv) {
                verts[vi++]=v[0]; verts[vi++]=v[1]; verts[vi++]=v[2];
                norms[ni++]=0; norms[ni++]=0; norms[ni++]=-1;
                cols[ci++]=sR; cols[ci++]=sG; cols[ci++]=sB; cols[ci++]=1f;
            }
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
