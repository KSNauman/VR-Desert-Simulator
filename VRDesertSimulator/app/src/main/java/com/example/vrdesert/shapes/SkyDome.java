package com.example.vrdesert.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * Realistic desert sky dome inspired by Minecraft desert biome aesthetics.
 * Features:
 * - Warm golden-orange horizon blending into clear vivid blue
 * - A bright sun glow in the upper sky
 * - Subtle wispy cloud bands at mid elevation
 * - Smooth non-blocky hemisphere mesh
 */
public class SkyDome {

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;

    private int mProgram;

    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;

    private final int vertexCount;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private final int colorStride = COLORS_PER_VERTEX * 4;

    // Sun position (direction in spherical: elevation ~60°, azimuth ~45°)
    private static final float SUN_THETA = (float)(Math.PI * 0.35); // elevation
    private static final float SUN_PHI = (float)(Math.PI * 0.25);   // azimuth

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

    public SkyDome() {
        int lonSegments = 32;
        int latSegments = 16;
        float radius = 200.0f;

        int totalTriangles = lonSegments * latSegments * 2;
        int totalVertices = totalTriangles * 3;
        vertexCount = totalVertices;

        float[] vertices = new float[totalVertices * 3];
        float[] colors = new float[totalVertices * 4];
        int vi = 0;
        int ci = 0;

        for (int lat = 0; lat < latSegments; lat++) {
            float theta0 = (float) (lat * Math.PI / 2.0 / latSegments);
            float theta1 = (float) ((lat + 1) * Math.PI / 2.0 / latSegments);

            for (int lon = 0; lon < lonSegments; lon++) {
                float phi0 = (float) (lon * 2 * Math.PI / lonSegments);
                float phi1 = (float) ((lon + 1) * 2 * Math.PI / lonSegments);

                float[] p00 = spherePoint(radius, theta0, phi0);
                float[] p10 = spherePoint(radius, theta1, phi0);
                float[] p01 = spherePoint(radius, theta0, phi1);
                float[] p11 = spherePoint(radius, theta1, phi1);

                // Triangle 1
                float[][] tri1 = {p00, p10, p01};
                float[] t1Theta = {theta0, theta1, theta0};
                float[] t1Phi = {phi0, phi0, phi1};
                // Triangle 2
                float[][] tri2 = {p01, p10, p11};
                float[] t2Theta = {theta0, theta1, theta1};
                float[] t2Phi = {phi1, phi0, phi1};

                for (int t = 0; t < 3; t++) {
                    vertices[vi++] = tri1[t][0];
                    vertices[vi++] = tri1[t][1];
                    vertices[vi++] = tri1[t][2];
                    float[] c = skyColor(t1Theta[t], t1Phi[t]);
                    colors[ci++] = c[0]; colors[ci++] = c[1]; colors[ci++] = c[2]; colors[ci++] = c[3];
                }
                for (int t = 0; t < 3; t++) {
                    vertices[vi++] = tri2[t][0];
                    vertices[vi++] = tri2[t][1];
                    vertices[vi++] = tri2[t][2];
                    float[] c = skyColor(t2Theta[t], t2Phi[t]);
                    colors[ci++] = c[0]; colors[ci++] = c[1]; colors[ci++] = c[2]; colors[ci++] = c[3];
                }
            }
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);
    }

    private float[] spherePoint(float r, float theta, float phi) {
        float y = r * (float) Math.sin(theta);
        float xz = r * (float) Math.cos(theta);
        float x = xz * (float) Math.cos(phi);
        float z = xz * (float) Math.sin(phi);
        return new float[]{x, y, z};
    }

    /**
     * Minecraft desert-inspired sky color.
     * Horizon: warm golden-orange haze
     * Mid sky: vivid clear blue
     * Zenith: deep rich blue
     * Sun: bright white-yellow glow around sun position
     * Clouds: subtle lighter bands at mid elevations
     */
    private float[] skyColor(float theta, float phi) {
        // t: 0 = horizon, 1 = zenith
        float t = (float) (theta / (Math.PI / 2.0));
        t = Math.max(0f, Math.min(1f, t));

        // --- Base gradient (Minecraft desert-inspired) ---
        float r, g, b;

        if (t < 0.08f) {
            // Very low horizon: warm golden-orange haze
            float f = t / 0.08f;
            r = lerp(0.95f, 0.90f, f);
            g = lerp(0.80f, 0.72f, f);
            b = lerp(0.55f, 0.45f, f);
        } else if (t < 0.2f) {
            // Low sky: transition from golden to warm light blue
            float f = (t - 0.08f) / 0.12f;
            r = lerp(0.90f, 0.55f, f);
            g = lerp(0.72f, 0.70f, f);
            b = lerp(0.45f, 0.85f, f);
        } else if (t < 0.5f) {
            // Mid sky: vivid Minecraft-like blue
            float f = (t - 0.2f) / 0.3f;
            r = lerp(0.55f, 0.30f, f);
            g = lerp(0.70f, 0.55f, f);
            b = lerp(0.85f, 0.92f, f);
        } else {
            // Upper sky to zenith: deep rich blue
            float f = (t - 0.5f) / 0.5f;
            r = lerp(0.30f, 0.18f, f);
            g = lerp(0.55f, 0.35f, f);
            b = lerp(0.92f, 0.95f, f);
        }

        // --- Sun glow ---
        // Compute angular distance from sun position
        float sunX = (float)(Math.cos(SUN_THETA) * Math.cos(SUN_PHI));
        float sunY = (float)(Math.sin(SUN_THETA));
        float sunZ = (float)(Math.cos(SUN_THETA) * Math.sin(SUN_PHI));

        float ptX = (float)(Math.cos(theta) * Math.cos(phi));
        float ptY = (float)(Math.sin(theta));
        float ptZ = (float)(Math.cos(theta) * Math.sin(phi));

        float dot = sunX * ptX + sunY * ptY + sunZ * ptZ;
        float sunAngle = (float)Math.acos(Math.max(-1f, Math.min(1f, dot)));

        // Bright sun core (small radius)
        float sunCore = Math.max(0f, 1.0f - sunAngle / 0.15f);
        sunCore = sunCore * sunCore * sunCore; // Sharp falloff
        r += sunCore * 0.9f;
        g += sunCore * 0.85f;
        b += sunCore * 0.6f;

        // Soft sun halo (large radius)
        float sunHalo = Math.max(0f, 1.0f - sunAngle / 0.6f);
        sunHalo = sunHalo * sunHalo;
        r += sunHalo * 0.25f;
        g += sunHalo * 0.20f;
        b += sunHalo * 0.05f;

        // --- Wispy cloud bands at mid elevation ---
        if (t > 0.15f && t < 0.45f) {
            float cloudNoise = (float)(
                Math.sin(phi * 3.0f + 1.5f) * 0.5f +
                Math.sin(phi * 7.0f + theta * 5.0f) * 0.3f +
                Math.sin(phi * 13.0f - theta * 8.0f) * 0.2f
            );
            // Only show cloud where noise is positive (sparse)
            if (cloudNoise > 0.3f) {
                float cloudStrength = (cloudNoise - 0.3f) * 0.8f;
                // Fade clouds near edges of the band
                float bandFade = 1.0f - Math.abs(t - 0.3f) / 0.15f;
                bandFade = Math.max(0f, Math.min(1f, bandFade));
                cloudStrength *= bandFade;

                r = lerp(r, 0.95f, cloudStrength * 0.4f);
                g = lerp(g, 0.93f, cloudStrength * 0.4f);
                b = lerp(b, 0.90f, cloudStrength * 0.3f);
            }
        }

        // Clamp
        r = Math.max(0f, Math.min(1f, r));
        g = Math.max(0f, Math.min(1f, g));
        b = Math.max(0f, Math.min(1f, b));

        return new float[]{r, g, b, 1.0f};
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);
        GLES20.glDepthMask(false);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        int posHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int colorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false, colorStride, colorBuffer);

        int mvpHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(true);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
