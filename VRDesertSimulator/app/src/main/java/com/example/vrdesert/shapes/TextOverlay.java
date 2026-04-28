package com.example.vrdesert.shapes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TextOverlay {

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 aTexCoordinate;" +
            "varying vec2 vTexCoordinate;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  vTexCoordinate = aTexCoordinate;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D uTexture;" +
            "varying vec2 vTexCoordinate;" +
            "void main() {" +
            "  gl_FragColor = texture2D(uTexture, vTexCoordinate);" +
            "}";

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private int mProgram;
    private int[] textureHandle = new int[1];

    private static final int COORDS_PER_VERTEX = 2;
    // Standard 1x1 quad mapped relative to Top Left
    private float[] quadCoords = {
            0.0f, 0.0f, // top left
            0.0f, 1.0f, // bottom left
            1.0f, 1.0f, // bottom right
            0.0f, 0.0f, // top left
            1.0f, 1.0f, // bottom right
            1.0f, 0.0f  // top right
    };

    private float[] textureCoords = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Paint textPaint;
    private Paint titlePaint;
    private Paint bgPaint;
    private Paint borderPaint;

    public TextOverlay() {
        ByteBuffer bb = ByteBuffer.allocateDirect(quadCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(quadCoords);
        vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        textureBuffer = tb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        // Pre-allocate Canvas and Bitmap structure natively, drastically increasing vertical scale for Multi-line text!
        textBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);

        // Item text paint
        textPaint = new Paint();
        textPaint.setTextSize(44);
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(4.0f, 1.0f, 1.0f, Color.BLACK);

        // Title paint (golden)
        titlePaint = new Paint();
        titlePaint.setTextSize(48);
        titlePaint.setColor(Color.rgb(255, 200, 60));
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);
        titlePaint.setShadowLayer(5.0f, 2.0f, 2.0f, Color.BLACK);

        // Background panel paint
        bgPaint = new Paint();
        bgPaint.setColor(Color.argb(140, 10, 8, 5));
        bgPaint.setAntiAlias(true);

        // Border paint
        borderPaint = new Paint();
        borderPaint.setColor(Color.argb(100, 255, 200, 60));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setAntiAlias(true);

        updateText("");
    }

    public void updateText(String text) {
        textBitmap.eraseColor(Color.TRANSPARENT);
        // Only drawing the White Text with its drop-shadow directly onto the 100% transparent Canvas!
        if (!text.isEmpty()) {
            // Android Canvas drawText doesn't natively do multiline mapping. We split and increment Y offset!
            String[] lines = text.split("\n");
            int lineCount = lines.length;
            int panelHeight = 30 + lineCount * 50 + 10;
            int panelWidth = 340;

            // Draw dark background panel with rounded corners
            RectF panelRect = new RectF(8, 8, panelWidth, panelHeight);
            textCanvas.drawRoundRect(panelRect, 16, 16, bgPaint);
            textCanvas.drawRoundRect(panelRect, 16, 16, borderPaint);

            // Draw text lines
            int yPos = 50;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Paint p = (i == 0) ? titlePaint : textPaint;
                textCanvas.drawText(line, 24, yPos, p);
                yPos += 50;
            }
        }

        // Upload to OpenGL pipeline
        if (textureHandle[0] == 0) {
            GLES20.glGenTextures(1, textureHandle, 0);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textBitmap, 0);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int texCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoordinate");
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        int texHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glUniform1i(texHandle, 0);

        int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
