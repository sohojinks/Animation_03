package family.sosik.animation_03;
/*
 * Copyright � 2014 by Sascha Willems - http://www.saschawillems.de
 *
 * This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0)
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Observable;

/**
 * Implements a very basic fullscreen HUD for displaying GUI-Elements on top of a 3D scene
 *
 * @author Sascha Willems
 */
public class HUD extends Observable {
    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = aTextureCoord;\n" +
                    "}\n";

    private final String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}";

    private int mProgramID;

    private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private Context mContext;
    private float mAspectRatio = 1.0f;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private int[] mTexture = new int[1];
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexCoordBuffer;
    private ArrayList<HUDElement> mHudElements = new ArrayList<HUDElement>();

    /**
     * Constructor for GLSimpleHUD
     * Note : Must be called from a valid OpenGL render context
     */
    public HUD(Context context) {
        mContext = context;
        // Create a bitmap for our android canvas that'll get passed to OpenGL ES as a texture later on
        mBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_4444);
        // Create canvas that's used to draw on the bitmap
        mCanvas = new Canvas(mBitmap);
        int vertexShader = GameRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                mVertexShader);
        int fragmentShader = GameRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShader);

        mProgramID = GameRenderer.createGLProgram(vertexShader, fragmentShader);
        if (mProgramID == 0) {
            return;
        }


        maPositionHandle = GLES20.glGetAttribLocation(mProgramID, "aPosition");
        GameRenderer.checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgramID, "aTextureCoord");
        GameRenderer.checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramID, "uMVPMatrix");
        GameRenderer.checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        // Generate buffers for vertices and texture coordinates
        float[] mVertices = {0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0};
        float[] mTexCoords = {0, 1, 1, 1, 0, 0, 1, 0};
        mVertexBuffer = ByteBuffer.allocateDirect(mVertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(mVertices).position(0);
        mTexCoordBuffer = ByteBuffer.allocateDirect(mTexCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoordBuffer.put(mTexCoords).position(0);

        // Generate OpenGL ES texture
        GLES20.glGenTextures(1, mTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
    }

    /**
     * Updates the HUD-Texture and uploads it to OpenGL ES
     * As this is pretty costly, it's not advised to do this every frame
     * But most HUD/GUI elements are static and can be updated at a lower or fixed frame rate
     */
    public void update() {
        mBitmap.eraseColor(Color.TRANSPARENT);

        // Scale depending on viewport ratio
        mCanvas.save();
        mCanvas.scale(1.0f, mAspectRatio);

        // Render elements
        for (HUDElement hudElement : mHudElements) {
            mCanvas.drawText(hudElement.getText(), hudElement.getPosition().x, hudElement.getPosition().y, hudElement.getTextPaint());
        }

        mCanvas.restore();

        // Upload our bitmap to OpenGL. We use texSubImage this time as it's faster
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mBitmap);
    }

    /**
     * Render the HUD as a fullscreen quad in orthogonal mode
     */
    public void render() {

        // Get shader attrib and uniform handles
        // For a complete HUD you should create and assign it's own shader


        // Setup orthogonal mode
        float[] mProjMatrix = new float[16];
        GLES20.glUseProgram(mProgramID);
        Matrix.orthoM(mProjMatrix, 0, 0, 1, 0, 1, -64.0f, 64.0f);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mProjMatrix, 0);

        // Change depth func so our HUD is always rendered atop
        GLES20.glDepthFunc(GLES20.GL_ALWAYS);
        // Disable depth writes
        GLES20.glDepthMask(false);

        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        // GLES20.glEnable(GLES20.GL_BLEND);

        // Render fullscreen quad
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexBuffer.capacity() / 3);

        GLES20.glGetError();

        // Restore depth func an depth write
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);

    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
    }


    /**
     * Checks if a motion event is triggered at the position of a hud element
     *
     * @param event
     */
    public void touchEvent(MotionEvent event) {
        // Since our hud texture has different dimensions than the screen, we need to scale the event coordinates
        // to match our virtual hud coordinates

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Point hitPoint = new Point((int) (512.0f / size.x * event.getX()), (int) (512.0f / size.y * event.getY()));

        for (HUDElement hudElement : mHudElements) {
            if (hudElement.pointInElement(hitPoint)) {
                setChanged();
                notifyObservers(hudElement.getName());
            }
        }
    }

    /**
     * Adds a new text element to the hud
     *
     * @param name     Unique identifier of the element
     * @param text     Initial text (caption) of this element
     * @param position Initial on-screen position of this element
     * @return The added hud element
     */
    public HUDElement addHudElement(String name, String text, Point position) {
        HUDElement hudElement = new HUDElement(name, text, position);
        mHudElements.add(hudElement);
        return hudElement;
    }

    /**
     * @param name Unique identifier of the hud element to return
     * @return hud element with the given identifier, or null if no element with that name is found
     */
    public HUDElement getHudElement(String name) {
        HUDElement hudElement = null;
        for (HUDElement element : mHudElements) {
            if (element.getName().equals(name)) {
                return element;
            }
        }
        return hudElement;
    }

}
