package appwarp.example.multiplayerdemo;

import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import java.util.Random;

public class Dice extends Sprite implements ITouchArea {
    private DiceClickListener listener = null;
    public Dice(float pX, float pY, ITextureRegion pTextureRegion, VertexBufferObjectManager pVertexBufferObjectManager, DiceClickListener listener) {
        super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
        this.listener = listener;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionUp() && isVisible()) {
            if (listener != null) {
                listener.onDiceClicked((new Random()).nextInt(6) + 1);
            }
        }
        return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
    }
}
