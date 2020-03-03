package family.sosik.animation_03;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_DEPTH_TEST;


/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class GameRenderer implements GLSurfaceView.Renderer, Observer {

    private static final String TAG = "GameRenderer";
    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec2 aTextureCoord;" +
                    "varying vec2 vTextureCoord;" +
                    "void main() {" +
                    " gl_Position = uMVPMatrix * aPosition;" +
                    " vTextureCoord = aTextureCoord;" +
                    "}";
    private final String mFragmentShader =
            "precision mediump float;" +
                    //"uniform vec4 vColor;" +
                    "uniform sampler2D aTextureCoord;" +

                    //"uniform sampler2D aTextureCoord2;" +
                    "uniform float Texture_PosU;" +             //Main texture horizontal offset
                    "uniform float Texture_PosV;" +             //Main texture vertical offset
                    "uniform float Overlay_PosU;" +                   //Overlay texture horizontal offset
                    "uniform float Overlay_PosV;" +                   //Overlay texture vertical offset
                    "varying vec2 vTextureCoord;" +
                    "vec4 underlay;" +
                    "vec4 overlay;" +
                    "void main() {" +
                    //";" +
                    " if (Overlay_PosU >= 0.0) {;" +
                    "   underlay=texture2D(aTextureCoord, vec2(vTextureCoord.x + Texture_PosU ,vTextureCoord.y + Texture_PosV ));" +
                    "   overlay=texture2D(aTextureCoord, vec2(vTextureCoord.x + Overlay_PosU,vTextureCoord.y + Overlay_PosV));" +

                    // " underlay.rgb *= underlay.a;" +
                    //   " overlay.rgb *= overlay.a;" +
                    //" composite.rgb = overlay.rgb + (underlay.rgb *(1-overlay.a));"+
                    //" composite.a = overlay.a + (underlay.a *(1-overlay.a));"+
                    //" gl_FragColor = clamp(mix(overlay, underlay,0.8),0.0,1.0);" +
                    "   if (overlay.a > 0.2) gl_FragColor = overlay;" +
                    "   else gl_FragColor = underlay;" +  //*vColor;" +
                    " } else {" +
                    "   gl_FragColor = texture2D(aTextureCoord, vec2(vTextureCoord.x + Texture_PosU ,vTextureCoord.y + Texture_PosV ));" +
                    //" gl_FragColor = underlay;" +
                    " }" +
                    "}";
    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    public float[] mProjectionMatrix = new float[16];
    /**
     * Store the view matrix. This can be thought of as our camera.
     * This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    public float[] mViewMatrix = new float[16];
    public float eyeZ = -8.0f;
    public int[] ScreenView = new int[4]; //the current view, {x, y, width, height}
    public float GameField_xPos = 3f;
    public float GameField_yPos = -2f;
    public GameTile SelectCursor;
    public HUD mSimpleHUD;
    public PointF[] playerPosition = new PointF[1];
    float scaleFactor = 0.5f;
    float frame = 0;
    private Context mContext;
    /**
     * Allocate storage for the final combined (Model/View/Projection)
     * matrix. This will be passed into
     * the shader program. World space
     */
    private float[] mMVPMatrix = new float[16];
    /**
     * Store the model matrix. This matrix is used to move models from
     * object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];
    private float[] mPlayerMatrix = new float[16];
    private float[] mTranslationMatrix = new float[16];
    private GameTile Player1;
    private float time;   // time value (initialize it)
    private float frameTime = 0;               // frame animation time
    private int player_LocoMode = 0;
    //TODO: use these float buggers in the renderer rather than for each tile object
    private FloatBuffer mTileVertices;
    private FloatBuffer m4x4TexCoords;
    private FloatBuffer m32x32TexCoords;
    private int mProgramID;
    private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int[] textures = new int[2];
    private double mLastTime;
    private int mFrames;
    private int mFPS;
    //spatial abstracts
    //Todo
    private float mAspectRatio;
    private int MapSize = 16;
    private int[][] Terrain; //matrix of integers, each reperesnting a different terrain type
    // Todo
    private int[][] CollisionMap;
    //display objects
    private GameTile[][] PlayingField;
    private float[] texture32x32 = { //square coordinates of a tile texture from a 32x32 tilemap
            0f, 0.03125f,
            0f, 0.0f,
            0.03125f, 0f,
            0.03125f, 0.03125f,
    };
    private float[] texture4x4 = { //square coordinates of a tile texture from a 4x4 tilemap
            0f, 0.25f,
            0f, 0.0f,
            0.25f, 0f,
            0.25f, 0.25f,
    };
    private float[] tileVertices = { //square coordinates of a tile
            -0.25f, 0.25f, 0.0f, // top left
            -0.25f, -0.25f, 0.0f, // bottom left
            0.25f, -0.25f, 0.0f, // bottom right
            0.25f, 0.25f, 0.0f // top right
    };
    private int mLocoFrame;
    private int mWidth;
    private int mHeight;

    public GameRenderer(Context GameContext) {
        mContext = GameContext;


        Matrix.setIdentityM(mTranslationMatrix, 0);

        mLastTime = System.currentTimeMillis();
    }

    public static int createGLProgram(int vertexShader, int fragmentShader) {
        int mProgramID = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramID, vertexShader);
        GLES20.glAttachShader(mProgramID, fragmentShader);
        GLES20.glLinkProgram(mProgramID);
        return mProgramID;
    }

    public static int loadShader(int type, String shaderCode) {

        int shaderHandle = GLES20.glCreateShader(type);     // Load in the vertex shader.
        if (shaderHandle != 0) {
            GLES20.glShaderSource(shaderHandle, shaderCode);// Pass in the shader source.
            GLES20.glCompileShader(shaderHandle);           // Compile the shader.


            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {

            Log.d("Shader Compiler", "Error creating shader. " + shaderCode);
            throw new RuntimeException("Error creating shader.");
        }
        return shaderHandle;
    }


    //TODO
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GL_DEPTH_TEST);
        GLES20.glUseProgram(mProgramID);
        checkGlError("glUseProgram");


        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0); //setup Model View
        Matrix.setIdentityM(mTranslationMatrix, 0); //reset translation matrix
        Matrix.translateM(mTranslationMatrix, 0, GameField_xPos, GameField_yPos, 0); //shift translation matrix by amount that's been panned around the screen

        Matrix.multiplyMM(mModelMatrix, 0, mMVPMatrix, 0, mTranslationMatrix, 0);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        drawField(MapSize);
        SelectCursor.draw(mModelMatrix, 30 * 0.03125f, 26 * 0.03125f, -1, 0, textures[0], mProgramID, maPositionHandle, maTextureHandle);
        drawCharacter();

        //   drawCharacter();
        drawUI();

        GLES20.glDisable(GLES20.GL_BLEND);

    }

    private void drawCharacter() {
        //Player1.draw(mModelMatrix, .25f * frame, 0, -1, 0 ,textures[1],mProgramID,maPositionHandle, maTextureHandle);
        float elapsedTime = (System.nanoTime() - time) / 1000000000.0f;
        time = System.nanoTime();
        frameTime += elapsedTime;             // count frame time
        if (frameTime >= .1f) {   // if 1/60th of a second passed (for 60fps)
            // step to next frame
            if (player_LocoMode == 5) {//idle
            } else if (player_LocoMode == 2) {//down{
                playerPosition[0].y += .001;
            } else if (player_LocoMode == 4) {//left
                playerPosition[0].x += .001;
            } else if (player_LocoMode == 6) {//right
                playerPosition[0].x -= .001;
            } else if (player_LocoMode == 0) {//up
                playerPosition[0].y -= .001;
            }


            mLocoFrame++;
            if (mLocoFrame > 3)            // if end of sequence
                mLocoFrame = 0;               // restart sequence
            frameTime = 0;
        }

        Matrix.setIdentityM(mTranslationMatrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, playerPosition[0].x + GameField_xPos, playerPosition[0].y + GameField_yPos, 0);

        Matrix.multiplyMM(mModelMatrix, 0, mMVPMatrix, 0, mTranslationMatrix, 0);
        Player1.draw(mModelMatrix, .25f * mLocoFrame, 0, -1, 0, textures[1], mProgramID, maPositionHandle, maTextureHandle);


    }

    //  This method is called when the surface is first created. It will also be
    // called if we lose our surface context and it is later recreated by the system.
    // the GL10 instance passed in is referred to as glUnused. We don’t use this when
    // drawing using OpenGL ES 2; instead, we use the static methods of the class GLES20.
    // The GL10 parameter is only there because the same interface is used for OpenGL ES 1.x.
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        int vertexShader = GameRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                mVertexShader);
        int fragmentShader = GameRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShader);

        mProgramID = createGLProgram(vertexShader, fragmentShader);
        if (mProgramID == 0) {
            return;
        }


        maPositionHandle = GLES20.glGetAttribLocation(mProgramID, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgramID, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramID, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        // Set the background clear color
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        // Set the OpenGL viewport
        SetViewMatrix();
        //load playing field
        loadField();
        //load textures
        loadTexture(R.drawable.terrain, mContext, 0);
        loadTexture(R.drawable.player1, mContext, 1);
        playerPosition[0] = new PointF();
        playerPosition[0].x = 0;
        playerPosition[0].y = 0;
        setupHud();

    }

    @Override
    public void update(Observable arg0, Object arg1) {
        Log.d("HUD", " " + arg0 + " " + arg1);
        // Check if observed object is hud
        if (arg0 == mSimpleHUD) {

            new AlertDialog.Builder(mContext)
                    .setTitle("HUD")
                    .setMessage("Element \"" + arg1 + "\" selected")
                    .setPositiveButton("OK", null)
                    .show();

        }

    }

    public void SetViewMatrix() {
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;


        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }

    /**
     * Creates a simple HUD with some text elements
     */
    private void setupHud() {
        mSimpleHUD = new HUD(mContext);

        // Add some elements to our hud
        HUDElement hudElement;
        mSimpleHUD.addHudElement("fpscounter", "", new Point(25, 25));
        hudElement = mSimpleHUD.addHudElement("menu", "Menu", new Point(256, 120));
        hudElement.getTextPaint().setTextAlign(Paint.Align.CENTER);
        hudElement.getTextPaint().setTextSize(48);
        hudElement = mSimpleHUD.addHudElement("newgame", "New Game", new Point(256, 200));
        hudElement.getTextPaint().setTextAlign(Paint.Align.CENTER);
        hudElement.getTextPaint().setTextSize(48);
        hudElement = mSimpleHUD.addHudElement("settings", "Settings", new Point(256, 260));
        hudElement.getTextPaint().setTextAlign(Paint.Align.CENTER);
        hudElement.getTextPaint().setTextSize(48);
        hudElement = mSimpleHUD.addHudElement("about", "About", new Point(256, 320));
        hudElement.getTextPaint().setTextAlign(Paint.Align.CENTER);
        hudElement.getTextPaint().setTextSize(48);
        //   hudElement = mSimpleHUD.addHudElement("quit", "Quit", new Point(256, 500));
        //     hudElement.getTextPaint().setTextAlign(Paint.Align.CENTER);
        //     hudElement.getTextPaint().setTextSize(48);

        // Add our renderer as an observer to the HUD, so we can react to selected elements
        mSimpleHUD.addObserver(this);
    }

    private void loadField() {

        PlayingField = new GameTile[MapSize][MapSize];
        Terrain = new int[MapSize][MapSize];
        CollisionMap = new int[MapSize][MapSize];
        GenerateTerrain(Terrain, MapSize);

        Player1 = new GameTile(tileVertices, texture4x4, mContext);

        SelectCursor = new GameTile(tileVertices, texture32x32, mContext);

        for (int y = 0; y < MapSize; y++) {
            for (int x = 0; x < MapSize; x++) {
                PlayingField[x][y] = new GameTile(tileVertices, texture32x32, mContext);
                tileVertices[0] -= .5;
                tileVertices[3] -= .5;
                tileVertices[6] -= .5;
                tileVertices[9] -= .5;
            }
            tileVertices[0] = -0.25f;
            tileVertices[3] = -0.25f;
            tileVertices[6] = .25f;
            tileVertices[9] = .25f;
            tileVertices[1] += .5;
            tileVertices[4] += .5;
            tileVertices[7] += .5;
            tileVertices[10] += .5;
        }


    }

    private void GenerateTerrain(int[][] TerrainMap, int MapSize) {
        int startX = (int) (Math.random() * MapSize);
        int startY = (int) (Math.random() * MapSize);
        float jitter = .5f;
        float[][] PerlinMap;
        PerlinMap = new float[MapSize][MapSize];
        PerlinMap[0][0] = (float) Math.random();
        for (int y = 0; y < MapSize; y++) {
            for (int x = 0; x < MapSize; x++) {
                //Log.d("GENER x y", x + " + " + y);
                if (x > 0)
                    PerlinMap[x][y] = PerlinMap[x - 1][y] + ((float) Math.random() * jitter - jitter / 2);
                else if (y > 0)
                    PerlinMap[x][y] = ((PerlinMap[x][y - 1] + PerlinMap[x + 1][y - 1]) / 2) + ((float) Math.random() * jitter - jitter / 2);//(float)Math.random()*PerlinMap[x-1][y]*jitter-((PerlinMap[x-1][y])/2)*jitter;
                // if (y > 0) PerlinMap[x][y] = PerlinMap[x][y-1] +  ((float)Math.random()*jitter - jitter/2);//(float)Math.random()*PerlinMap[x][y-1]*jitter-((PerlinMap[x][y-1])/2)*jitter;
                if (x > 0 && y > 0 && x < MapSize - 1)
                    PerlinMap[x][y] = ((PerlinMap[x - 1][y - 1] + PerlinMap[x][y - 1] + PerlinMap[x + 1][y - 1]) / 3) + ((float) Math.random() * jitter - jitter / 2);
                PerlinMap[x][y] = Math.max(0, Math.min(1, PerlinMap[x][y]));

                if (PerlinMap[x][y] <= .25) TerrainMap[x][y] = 1;   //water
                else if (PerlinMap[x][y] <= .5) TerrainMap[x][y] = 2;
                else if (PerlinMap[x][y] <= .75) TerrainMap[x][y] = 3;
                else TerrainMap[x][y] = 4;
            }
            Log.d("Generate terrain", "# " + TerrainMap[0][y] + " " + TerrainMap[1][y] + " " + TerrainMap[2][y] + " " + TerrainMap[3][y] + " " + TerrainMap[4][y] + " " + TerrainMap[5][y] + " " + TerrainMap[6][y] + " " + TerrainMap[7][y] + " " + TerrainMap[8][y] + " " + TerrainMap[9][y] + " " + TerrainMap[10][y] + " " + TerrainMap[11][y] + " " + TerrainMap[12][y] + " " + TerrainMap[13][y] + " " + TerrainMap[14][y] + " " + TerrainMap[15][y]);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {

        ScreenView[0] = 0;
        ScreenView[1] = 0;
        ScreenView[2] = width;
        ScreenView[3] = height;
        mWidth = width;
        mHeight = height;
        GLES20.glViewport(ScreenView[0], ScreenView[1], ScreenView[2], ScreenView[3]);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 3.0f;
        final float far = 11.0f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    private void drawUI() {
        //draw UI frame overlay


        mSimpleHUD.render();
        mFrames++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastTime >= 1000) {
            mFPS = mFrames;
            mFrames = 0;
            mLastTime = currentTime;
            mSimpleHUD.getHudElement("fpscounter").setText(mFPS + " fps");
            mSimpleHUD.update();
        }
    }

    private float GetDistance(PointF P1, PointF P2) {
        return (float) Math.sqrt(Math.pow(P2.x - P1.x, 2) + Math.pow(P2.y - P1.y, 2));
    }

    private float GetDistance(PointF P1, float P2x, float P2y) {
        return (float) Math.sqrt(Math.pow(P2x - P1.x, 2) + Math.pow(P2y - P1.y, 2));
    }

    public float GetDistance(float P1x, float P1y, float P2x, float P2y) {
        return (float) Math.sqrt(Math.pow(P2x - P1x, 2) + Math.pow(P2y - P1y, 2));
    }

    public float GetAngle(PointF P1, PointF P2) {
        float hypoteneuse = GetDistance(P1, P2);
        float adjacent = GetDistance(P1, P2.x, P1.y);
        float opposite = GetDistance(P2, P2.x, P1.y);
        if (P1.y <= P2.y && P1.x <= P2.x)//uupleft
            return (float) (90 - Math.toDegrees(Math.asin(opposite / hypoteneuse))) + 90;
        if (P1.y >= P2.y && P1.x <= P2.x)//downleft
            return (float) Math.toDegrees(Math.asin(opposite / hypoteneuse)) + 180;
        if (P1.y >= P2.y && P1.x >= P2.x)//down right
            return (float) (90 - Math.toDegrees(Math.asin(opposite / hypoteneuse))) + 270;
        else
            return (float) Math.toDegrees(Math.asin(opposite / hypoteneuse));
    }

    private void drawField(int MapSize) {
        for (int x = 0; x < MapSize; x++)
            for (int y = 0; y < MapSize; y++)
                if (Terrain[x][y] == 1)
                    PlayingField[x][y].draw(mModelMatrix, (30 * 0.03125f), (26 * 0.03125f), -1, 0, textures[0], mProgramID, maPositionHandle, maTextureHandle);
                else if (Terrain[x][y] == 2)
                    PlayingField[x][y].draw(mModelMatrix, (30 * 0.03125f), (25 * 0.03125f), 26 * 0.03125f, (11 * 0.03125f), textures[0], mProgramID, maPositionHandle, maTextureHandle);
                else if (Terrain[x][y] == 3)
                    PlayingField[x][y].draw(mModelMatrix, (30 * 0.03125f), (24 * 0.03125f), 25 * 0.03125f, (11 * 0.03125f), textures[0], mProgramID, maPositionHandle, maTextureHandle);
                else
                    PlayingField[x][y].draw(mModelMatrix, (30 * 0.03125f), (23 * 0.03125f), 24 * 0.03125f, (11 * 0.03125f), textures[0], mProgramID, maPositionHandle, maTextureHandle);

    }

    private void loadTexture(int texture, Context context, int texID) {
        InputStream imagestream = context.getResources().
                openRawResource(texture);
        Bitmap bitmap = null;
        Bitmap temp;

        android.graphics.Matrix flip = new android.graphics.Matrix();
        flip.postScale(-1f, -1f);
        try {
            temp = BitmapFactory.decodeStream(imagestream);
            bitmap = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), flip, true);
        } catch (Exception e) {//TODO
        } finally {
            try {
                imagestream.close();
                imagestream = null;
            } catch (IOException e) {//TODO
            }
        }
        GLES20.glGenTextures(1, textures, texID);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[texID]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }
}