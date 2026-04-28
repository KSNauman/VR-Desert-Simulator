package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A bundle of 3 hexagonal logs for wood items.
 * Two logs on bottom, one on top in triangular arrangement.
 * Dark bark-brown body with lighter tan end caps.
 */
public class WoodLog {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;

    private int mProgram;
    private final int vertexCount;
    private static final int COORDS_PER_VERTEX = 3;
    private static final int COLORS_PER_VERTEX = 4;

    public WoodLog(int programId) {
        this.mProgram = programId;
        int sides = 6;
        // Each log: sides*6 (body) + sides*3*2 (caps) = 6*6 + 6*3*2 = 36+36 = 72 vertices per log
        // 3 logs = 216 vertices
        int vertsPerLog = sides * 6 + sides * 3 * 2;
        int totalVertices = vertsPerLog * 3;
        vertexCount = totalVertices;

        float[] verts = new float[totalVertices * 3];
        float[] cols = new float[totalVertices * 4];
        float[] norms = new float[totalVertices * 3];

        // Log positions (laid on side along Z axis): 2 bottom, 1 top
        float r = 0.12f;
        float halfLen = 0.45f;
        float[][] logCenters = {
            {-r * 1.1f, 0f, 0f},        // bottom left
            { r * 1.1f, 0f, 0f},        // bottom right
            { 0f, r * 1.8f, 0f}          // top center
        };

        // Bark color and end cap color
        float bkR = 0.30f, bkG = 0.18f, bkB = 0.08f;
        float ecR = 0.65f, ecG = 0.50f, ecB = 0.30f;

        int vi = 0, ci = 0, ni = 0;
        for (float[] center : logCenters) {
            float cx = center[0], cy = center[1], cz = center[2];
            for (int i = 0; i < sides; i++) {
                float a0 = (float)(2 * Math.PI * i / sides);
                float a1 = (float)(2 * Math.PI * (i + 1) / sides);
                float x0 = r * (float)Math.cos(a0);
                float y0 = r * (float)Math.sin(a0);
                float x1 = r * (float)Math.cos(a1);
                float y1 = r * (float)Math.sin(a1);
                float nx = (float)Math.cos((a0 + a1) / 2);
                float ny = (float)Math.sin((a0 + a1) / 2);

                // Side face (along Z)
                float[][] sv = {
                    {cx+x0, cy+y0, -halfLen}, {cx+x1, cy+y1, -halfLen}, {cx+x0, cy+y0, halfLen},
                    {cx+x0, cy+y0, halfLen}, {cx+x1, cy+y1, -halfLen}, {cx+x1, cy+y1, halfLen}
                };
                for (float[] v : sv) {
                    verts[vi++]=v[0]; verts[vi++]=v[1]; verts[vi++]=v[2];
                    norms[ni++]=nx; norms[ni++]=ny; norms[ni++]=0;
                    cols[ci++]=bkR; cols[ci++]=bkG; cols[ci++]=bkB; cols[ci++]=1f;
                }
                // Front cap (z+)
                verts[vi++]=cx; verts[vi++]=cy; verts[vi++]=halfLen;
                verts[vi++]=cx+x0; verts[vi++]=cy+y0; verts[vi++]=halfLen;
                verts[vi++]=cx+x1; verts[vi++]=cy+y1; verts[vi++]=halfLen;
                for (int j=0;j<3;j++){norms[ni++]=0;norms[ni++]=0;norms[ni++]=1;}
                for (int j=0;j<3;j++){cols[ci++]=ecR;cols[ci++]=ecG;cols[ci++]=ecB;cols[ci++]=1f;}
                // Back cap (z-)
                verts[vi++]=cx; verts[vi++]=cy; verts[vi++]=-halfLen;
                verts[vi++]=cx+x1; verts[vi++]=cy+y1; verts[vi++]=-halfLen;
                verts[vi++]=cx+x0; verts[vi++]=cy+y0; verts[vi++]=-halfLen;
                for (int j=0;j<3;j++){norms[ni++]=0;norms[ni++]=0;norms[ni++]=-1;}
                for (int j=0;j<3;j++){cols[ci++]=ecR*0.8f;cols[ci++]=ecG*0.8f;cols[ci++]=ecB*0.8f;cols[ci++]=1f;}
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
