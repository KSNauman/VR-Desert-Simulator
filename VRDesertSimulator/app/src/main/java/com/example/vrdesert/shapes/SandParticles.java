package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;
import java.util.Random;

/**
 * Enhanced GPU sand/dust particle system with strong wind-blown effect.
 * Two layers: drifting dust motes + fast horizontal wind streaks.
 */
public class SandParticles {

    // --- DUST MOTES SHADER (soft round particles) ---
    private final String dustVertexShader =
        "uniform mat4 uMVPMatrix;" +
        "uniform float uTime;" +
        "uniform vec3 uCamPos;" +
        "attribute vec3 aPosition;" +
        "attribute float aPhase;" +
        "attribute float aSize;" +
        "varying float vAlpha;" +
        "void main() {" +
        "  vec3 pos = aPosition;" +
        // Wind drift — strong horizontal push + gentle vertical float
        "  float windSpeed = 2.5;" +
        "  pos.x += sin(uTime * 0.4 + aPhase * 6.28) * 1.2 + uTime * windSpeed * (0.5 + aPhase * 0.5);" +
        "  pos.y += sin(uTime * 0.8 + aPhase * 4.0) * 0.3;" +
        "  pos.z += cos(uTime * 0.35 + aPhase * 5.1) * 0.8;" +
        // Wrap particles around camera
        "  pos.x = uCamPos.x + mod(pos.x - uCamPos.x + 25.0, 50.0) - 25.0;" +
        "  pos.z = uCamPos.z + mod(pos.z - uCamPos.z + 25.0, 50.0) - 25.0;" +
        "  gl_Position = uMVPMatrix * vec4(pos, 1.0);" +
        "  float dist = gl_Position.w;" +
        "  gl_PointSize = max(3.0, aSize * 20.0 / dist);" +
        // Fade based on distance and pulsing life
        "  float distFade = clamp(1.0 - (dist - 2.0) / 35.0, 0.0, 1.0);" +
        "  float pulse = 0.6 + 0.4 * sin(uTime * 1.5 + aPhase * 6.28);" +
        "  vAlpha = distFade * pulse * 0.55;" +
        "}";

    private final String dustFragmentShader =
        "precision mediump float;" +
        "varying float vAlpha;" +
        "void main() {" +
        "  vec2 c = gl_PointCoord - vec2(0.5);" +
        "  float d = dot(c, c);" +
        "  if (d > 0.25) discard;" +
        "  float soft = 1.0 - smoothstep(0.1, 0.25, d);" +
        "  gl_FragColor = vec4(0.92, 0.82, 0.58, vAlpha * soft);" +
        "}";

    // --- WIND STREAKS SHADER (fast horizontal lines) ---
    private final String streakVertexShader =
        "uniform mat4 uMVPMatrix;" +
        "uniform float uTime;" +
        "uniform vec3 uCamPos;" +
        "attribute vec3 aPosition;" +
        "attribute float aPhase;" +
        "varying float vAlpha;" +
        "void main() {" +
        "  vec3 pos = aPosition;" +
        "  float speed = 6.0 + aPhase * 4.0;" +
        "  pos.x += uTime * speed;" +
        "  pos.y += sin(uTime * 0.3 + aPhase * 3.14) * 0.1;" +
        // Wrap around camera
        "  pos.x = uCamPos.x + mod(pos.x - uCamPos.x + 20.0, 40.0) - 20.0;" +
        "  pos.z = uCamPos.z + mod(pos.z - uCamPos.z + 20.0, 40.0) - 20.0;" +
        "  gl_Position = uMVPMatrix * vec4(pos, 1.0);" +
        "  float dist = gl_Position.w;" +
        "  vAlpha = clamp(1.0 - (dist - 1.0) / 25.0, 0.0, 0.35);" +
        "}";

    private final String streakFragmentShader =
        "precision mediump float;" +
        "varying float vAlpha;" +
        "void main() {" +
        "  gl_FragColor = vec4(0.88, 0.78, 0.55, vAlpha);" +
        "}";

    // Dust motes
    private FloatBuffer dustPosBuffer;
    private FloatBuffer dustPhaseBuffer;
    private FloatBuffer dustSizeBuffer;
    private int dustProgram;
    private int dustCount;

    // Wind streaks (rendered as GL_LINES)
    private FloatBuffer streakPosBuffer;
    private FloatBuffer streakPhaseBuffer;
    private int streakProgram;
    private int streakVertCount;

    public SandParticles(int count) {
        this.dustCount = count;
        Random rand = new Random(42);

        // === DUST MOTES ===
        float[] positions = new float[count * 3];
        float[] phases = new float[count];
        float[] sizes = new float[count];

        for (int i = 0; i < count; i++) {
            float angle = rand.nextFloat() * (float)(2 * Math.PI);
            float radius = 1f + rand.nextFloat() * 24f;
            positions[i * 3] = radius * (float)Math.cos(angle);
            positions[i * 3 + 1] = -0.5f + rand.nextFloat() * 5f;
            positions[i * 3 + 2] = radius * (float)Math.sin(angle);
            phases[i] = rand.nextFloat();
            sizes[i] = 0.5f + rand.nextFloat() * 1.5f;
        }

        dustPosBuffer = makeBuf(positions);
        dustPhaseBuffer = makeBuf(phases);
        dustSizeBuffer = makeBuf(sizes);

        dustProgram = buildProgram(dustVertexShader, dustFragmentShader);

        // === WIND STREAKS (pairs of points forming short horizontal lines) ===
        int streakCount = 60;
        streakVertCount = streakCount * 2; // 2 vertices per line
        float[] sPos = new float[streakVertCount * 3];
        float[] sPhase = new float[streakVertCount];

        for (int i = 0; i < streakCount; i++) {
            float sx = (rand.nextFloat() - 0.5f) * 40f;
            float sy = rand.nextFloat() * 3f;
            float sz = (rand.nextFloat() - 0.5f) * 40f;
            float streakLen = 0.8f + rand.nextFloat() * 1.5f;
            float p = rand.nextFloat();

            // Line start
            sPos[i*6]   = sx;
            sPos[i*6+1] = sy;
            sPos[i*6+2] = sz;
            sPhase[i*2] = p;

            // Line end (offset in X for horizontal streak)
            sPos[i*6+3] = sx + streakLen;
            sPos[i*6+4] = sy;
            sPos[i*6+5] = sz;
            sPhase[i*2+1] = p;
        }

        streakPosBuffer = makeBuf(sPos);
        streakPhaseBuffer = makeBuf(sPhase);

        streakProgram = buildProgram(streakVertexShader, streakFragmentShader);
    }

    public void draw(float[] mvpMatrix, float time, float camX, float camY, float camZ) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);

        // --- Draw dust motes ---
        GLES20.glUseProgram(dustProgram);

        int posH = GLES20.glGetAttribLocation(dustProgram, "aPosition");
        GLES20.glEnableVertexAttribArray(posH);
        GLES20.glVertexAttribPointer(posH, 3, GLES20.GL_FLOAT, false, 12, dustPosBuffer);

        int phaseH = GLES20.glGetAttribLocation(dustProgram, "aPhase");
        GLES20.glEnableVertexAttribArray(phaseH);
        GLES20.glVertexAttribPointer(phaseH, 1, GLES20.GL_FLOAT, false, 4, dustPhaseBuffer);

        int sizeH = GLES20.glGetAttribLocation(dustProgram, "aSize");
        GLES20.glEnableVertexAttribArray(sizeH);
        GLES20.glVertexAttribPointer(sizeH, 1, GLES20.GL_FLOAT, false, 4, dustSizeBuffer);

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(dustProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(dustProgram, "uTime"), time);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(dustProgram, "uCamPos"), camX, camY, camZ);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, dustCount);

        GLES20.glDisableVertexAttribArray(posH);
        GLES20.glDisableVertexAttribArray(phaseH);
        GLES20.glDisableVertexAttribArray(sizeH);

        // --- Draw wind streaks ---
        GLES20.glUseProgram(streakProgram);
        GLES20.glLineWidth(1.5f);

        int sposH = GLES20.glGetAttribLocation(streakProgram, "aPosition");
        GLES20.glEnableVertexAttribArray(sposH);
        GLES20.glVertexAttribPointer(sposH, 3, GLES20.GL_FLOAT, false, 12, streakPosBuffer);

        int spH = GLES20.glGetAttribLocation(streakProgram, "aPhase");
        GLES20.glEnableVertexAttribArray(spH);
        GLES20.glVertexAttribPointer(spH, 1, GLES20.GL_FLOAT, false, 4, streakPhaseBuffer);

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(streakProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(streakProgram, "uTime"), time);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(streakProgram, "uCamPos"), camX, camY, camZ);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, streakVertCount);

        GLES20.glDisableVertexAttribArray(sposH);
        GLES20.glDisableVertexAttribArray(spH);

        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    // Backwards compatible overload
    public void draw(float[] mvpMatrix, float time) {
        draw(mvpMatrix, time, 0f, 0f, 0f);
    }

    private int buildProgram(String vs, String fs) {
        int vsh = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int fsh = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vsh);
        GLES20.glAttachShader(prog, fsh);
        GLES20.glLinkProgram(prog);
        return prog;
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
