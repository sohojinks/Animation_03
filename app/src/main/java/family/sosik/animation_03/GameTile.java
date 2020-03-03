package family.sosik.animation_03;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GameTile {
    private static final int COORDS_PER_TEXTURE = 2;
    private static final int COORDS_PER_VERTEX = 3;
    private static int textureStride = COORDS_PER_TEXTURE * 4;
    private final short[] drawOrder = {0, 1, 2, 0, 2, 3};
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private Context mContext;
    private int[] textures = new int[1];
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private FloatBuffer textureBuffer;
    private int mPositionHandle;
    private int mMVPMatrixHandle;


    public GameTile(float[] squareCoords, float[] textureSize, Context context) {
        mContext = context;
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
        bb = ByteBuffer.allocateDirect(textureSize.length * 4);
        bb.order(ByteOrder.nativeOrder());
        textureBuffer = bb.asFloatBuffer();
        textureBuffer.put(textureSize);
        textureBuffer.position(0);
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

    }


    public void SetDrawLocation(float[] squareCoords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }

    public void draw(float[] mvpMatrix, float Texture_PosU, float Texture_PosV, float Overlay_PosU, float Overlay_PosV, int texture, int mProgram, int maPositionHandle, int maTextureHandle) {
//maybe readd glvertexattribpointer statements?

        GLES20.glVertexAttribPointer(maPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        GLES20.glVertexAttribPointer(maTextureHandle, COORDS_PER_TEXTURE,
                GLES20.GL_FLOAT, false,
                textureStride, textureBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        // GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //int fsColor = GLES20.glGetUniformLocation(mProgram, "vColor");
        int fsTexture = GLES20.glGetUniformLocation(mProgram, "aTextureCoord");
        //int fsTexture2 = GLES20.glGetUniformLocation(mProgram, "TexCoordIn2");
        int fsTexture_PosU = GLES20.glGetUniformLocation(mProgram, "Texture_PosU");
        int fsTexture_PosV = GLES20.glGetUniformLocation(mProgram, "Texture_PosV");
        int fsOverlay_PosU = GLES20.glGetUniformLocation(mProgram, "Overlay_PosU");
        int fsOverlay_PosV = GLES20.glGetUniformLocation(mProgram, "Overlay_PosV");
        GLES20.glUniform1i(fsTexture, 0);

        GLES20.glUniform1f(fsTexture_PosU, Texture_PosU);
        GLES20.glUniform1f(fsTexture_PosV, Texture_PosV);
        GLES20.glUniform1f(fsOverlay_PosU, Overlay_PosU);
        GLES20.glUniform1f(fsOverlay_PosV, Overlay_PosV);

        //float color[] = {0.9f, 0.2f, 0.2f, 0.8f};
        // GLES20.glUniform4fv(fsColor, 1, color, 0);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GameRenderer.checkGlError("glGetUniformLocation");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GameRenderer.checkGlError("glUniformMatrix4fv");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }
}