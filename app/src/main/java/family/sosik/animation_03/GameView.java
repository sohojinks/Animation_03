package family.sosik.animation_03;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;


public class GameView extends GLSurfaceView {

    private final GameRenderer gameRenderer;

    private PointF ClipCoords1 = null;
    private PointF ClipCoordsB = null;
    private PointF AdjClipCoords1 = new PointF();
    private PointF ClipCoords2 = null;
    private ScaleGestureDetector mScaleDetector;

    public GameView(Context context) {
        super(context);
        // Request an OpenGL ES 2.0 compatible context.
        setEGLContextClientVersion(2);
        gameRenderer = new GameRenderer(context);
        setRenderer(gameRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        //myGameView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public static PointF screenToWorld(float[] viewMatrix, float[] projMatrix, float screenX, float screenY, int[] view) {
        screenY = view[3] - screenY;
        float[] nearPos = unProject(viewMatrix, projMatrix, screenX, screenY, 0, view);
        float[] farPos = unProject(viewMatrix, projMatrix, screenX, screenY, 1, view);
        // The click occurred in somewhere on the line between the two points
        // nearPos and farPos. We want to find
        // where that line intersects the plane at z=0


        float distance = nearPos[2] / (nearPos[2] - farPos[2]); // Distance between nearPos and z=0
        float x = nearPos[0] + (farPos[0] - nearPos[0]) * distance;
        float y = nearPos[1] + (farPos[1] - nearPos[1]) * distance;
        //    Log.d("s2W", "s2W " +x + " " + y);
        return new PointF(x, y);
    }

    private static float[] unProject(float[] viewMatrix, float[] projMatrix, float screenX, float screenY, float depth, int[] view) {
        float[] position = {0, 0, 0, 0};

        //TODO: see if view is changing when screen rotates.

        GLU.gluUnProject(screenX, screenY, depth, viewMatrix, 0, projMatrix, 0,
                view, 0, position, 0);

        //Log.d("up2", "up2" +position[0] + " " + position[1]+ " " + position[2]);
        position[0] /= position[3];
        position[1] /= position[3];
        position[2] /= position[3];
        position[3] = 1;
        //  Log.d("up2", "up2" +position[0] + " " + position[1]+ " " + position[2]);
        return position;

    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mScaleDetector.onTouchEvent(e);

        long time = SystemClock.uptimeMillis();

        // get pointer index from the event object
        int pointerIndex = e.getActionIndex();

        // get pointer ID
        int pointerId = e.getPointerId(pointerIndex);

        // get masked (not specific to a pointer) action
        int maskedAction = e.getActionMasked();

        PointF tapXYPos = new PointF(); //store coordinates for initial tap
        PointF movePos = new PointF(); //store coordinates when holding a tap press on the screen
        switch (maskedAction) {
            case MotionEvent.ACTION_DOWN:
                gameRenderer.mSimpleHUD.touchEvent(e);
                tapXYPos.x = e.getRawX();
                tapXYPos.y = e.getRawY();
                //Get coords in space
                ClipCoords1 = screenToWorld(gameRenderer.mViewMatrix, gameRenderer.mProjectionMatrix, tapXYPos.x, tapXYPos.y, gameRenderer.ScreenView);

                //Matrix.translateM(mMVPMatrix, 0, .25f, 0, 0);
                Log.d("down", "@ " + ClipCoords1.x + " " + ClipCoords1.y);


                //calculate the nearest grid position touched on the playing field, taking into account offset caused by panning
                float xFact = (Math.round((ClipCoords1.x - gameRenderer.GameField_xPos) / .5) * .5f);
                float yFact = (Math.round((ClipCoords1.y - gameRenderer.GameField_yPos) / .5) * .5f);
                //float xFact2 = (Math.round((gameRenderer.movePos.x)/.5)*.5f);//players grid coords
                //float yFact2 = (Math.round((gameRenderer.movePos.y)/.5)*.5f);
                // Log.d("Tap", "Raw U " + ClipCoords1.x);
                // Log.d("Tap", "Raw V " + ClipCoords1.y);
                //  Log.d("Tap", "Adjusted U " + (ClipCoords1.x- gameRenderer.GameField_xPos));
                //  Log.d("Tap", "Adjusted V " + (ClipCoords1.y- gameRenderer.GameField_yPos));

                Log.d("Tap", "Nearest U .5 pos." + xFact);
                Log.d("Tap", "Nearest V .5 pos " + yFact);
                // Log.d("Locate", "@ " +Math.abs(xFact/.5) + " " + Math.abs(yFact/.5) + " " + gameRenderer.Terrain[(int)Math.abs(xFact/.5)][(int)Math.abs(yFact/.5)]);
                //Log.d("Player pos", "@ " +Math.abs(xFact2/.5) + " " + Math.abs(yFact2/.5));
                AdjClipCoords1.set(ClipCoords1);
                AdjClipCoords1.offset(-1 * gameRenderer.GameField_xPos, -1 * gameRenderer.GameField_yPos);
                //AdjClipCoords1.x = ClipCoords1.x- gameRenderer.GameField_xPos;
                //AdjClipCoords1.y = ClipCoords1.y- gameRenderer.GameField_yPos;
                // AdjClipCoords1.offset(-1 *gameRenderer.GameField_xPos,-1 *gameRenderer.GameField_yPos);
                Log.d("Tap", "Adjusted U " + AdjClipCoords1.x);
                Log.d("Tap", "Adjusted V " + AdjClipCoords1.y);
                Log.d("Trig ", "= " + gameRenderer.GetAngle(gameRenderer.playerPosition[0], AdjClipCoords1));

                //Log.d("Dist", "= " + ClipCoords1.x + " " + ClipCoords1.y);
                //hyponoose = Math.sqrt()
                float[] squareCoords = {
                        -0.25f + xFact, 0.25f + yFact, 0.0f, // top left
                        -0.25f + xFact, -0.25f + yFact, 0.0f, // bottom left
                        0.25f + xFact, -0.25f + yFact, 0.0f, // bottom right
                        0.25f + xFact, 0.25f + yFact, 0.0f}; // top right
                gameRenderer.SelectCursor.SetDrawLocation(squareCoords);
                //setPlayerDestination(xFact,yFact);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d("second touch", "@ " + time);
                break;
            case MotionEvent.ACTION_MOVE:
                movePos.x = e.getRawX();
                movePos.y = e.getRawY();
                float deltaX2, deltaY2;
                ClipCoords2 = screenToWorld(gameRenderer.mViewMatrix, gameRenderer.mProjectionMatrix, movePos.x, movePos.y, gameRenderer.ScreenView);

                float deltaX;
                if (movePos.x > tapXYPos.x) {
                    deltaX = tapXYPos.x - movePos.x;
                    if (Math.abs(deltaX) > 5) {
                        deltaX2 = ClipCoords1.x - ClipCoords2.x;
                        deltaX2 *= .5;
                        gameRenderer.GameField_xPos -= deltaX2; //swipe right
                    }
                } else {
                    deltaX = movePos.x - tapXYPos.x;
                    if (Math.abs(deltaX) > 5) {
                        deltaX2 = ClipCoords2.x - ClipCoords1.x;
                        deltaX2 *= .5;
                        gameRenderer.GameField_xPos += deltaX2; //swipe left}
                    }
                }
                tapXYPos.x = movePos.x;

                float deltaY;
                if (movePos.y > tapXYPos.y) {
                    deltaY = tapXYPos.y - movePos.y;
                    if (Math.abs(deltaY) > 5) {
                        deltaY2 = ClipCoords1.y - ClipCoords2.y;
                        deltaY2 *= .5;
                        gameRenderer.GameField_yPos -= deltaY2; //swipe right
                    }
                } else {
                    deltaY = movePos.y - tapXYPos.y;
                    if (Math.abs(deltaY) > 5) {
                        deltaY2 = ClipCoords2.y - ClipCoords1.y;
                        deltaY2 *= .5;
                        gameRenderer.GameField_yPos += deltaY2; //swipe left}
                    }
                }
                tapXYPos.y = movePos.y;
                ClipCoords1 = ClipCoords2;
                break;

            case MotionEvent.ACTION_UP:
                movePos.x = e.getRawX();
                movePos.y = e.getRawY();
                Log.d("up", "@ " + movePos.x + " " + movePos.y + " @ " + time);
                break;
                /*movePos.x = e.getRawX();
                deltaX = movePos.x - tapXYPos.x;

                if (Math.abs(deltaX) > 50)
                {
                    Log.d(DEBUG_TAG, "deltaX " +deltaX);
                    gameRenderer.GameField_xPos -= deltaX*.000001;
                }
                else
                {
                    deltaX=0;
                    gameRenderer.GameField_xPos = 0;// consider as something else - a screen tap for example
                }

                break;*/
            case MotionEvent.ACTION_POINTER_UP:
                Log.d("second touch up", "@ " + time);
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d("zscaleF", "@ " + detector.getScaleFactor());
            Log.d("zscale", "@ " + gameRenderer.eyeZ);
            gameRenderer.eyeZ /= (detector.getScaleFactor());
            //Log.d("zoom", "@ "+ gameRenderer.eyeZ);
            // // Don't let the object get too small or too large.
            // mScaleFactor  = Math.max(0.1f, Math.min(mScaleFactor  , .3f));
            ///  gameRenderer.eyeZ = 1 / mScaleFactor;
            if (gameRenderer.eyeZ < -11) gameRenderer.eyeZ = -10.9f;
            if (gameRenderer.eyeZ > -4) gameRenderer.eyeZ = -4.1f;
            gameRenderer.SetViewMatrix(); //reset view matrix to show view from the cameras new position
            invalidate();
            return true;
        }
    }

}
