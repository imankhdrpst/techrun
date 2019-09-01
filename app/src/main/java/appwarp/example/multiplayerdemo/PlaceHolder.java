package appwarp.example.multiplayerdemo;

import org.andengine.entity.Entity;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

public class PlaceHolder extends Entity implements IOnAreaTouchListener {
    public final Text caption;
    public float pX, pY;
    public int index = 0;

    public PlaceHolder(int index, float pX, float pY, Font font, String cap, VertexBufferObjectManager vertexBufferObjectManager) {
        super(pX, pY);

        this.pX = pX;
        this.pY  = pY;
        this.index = index;
        caption = new Text(0 , 0, font, cap, vertexBufferObjectManager);
//                final Sprite button = new Sprite(touchX[i], touchY[j], this.mOTextureRegion, this.getVertexBufferObjectManager());
//                button.setScale(GRID_WIDTH/ CAMERA_WIDTH);
//                scene.registerTouchArea(button);
        attachChild(caption);
    }

    @Override
    public void onAttached() {

        super.onAttached();

    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, ITouchArea pTouchArea, float pTouchAreaLocalX, float pTouchAreaLocalY) {
        return false;
    }
}
