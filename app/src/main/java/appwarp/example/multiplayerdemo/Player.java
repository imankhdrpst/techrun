package appwarp.example.multiplayerdemo;

import org.andengine.entity.Entity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

public class Player extends Entity {
    private float pX, pY;
    private int currentLocationIndex = 1;
    private String userName = "";

    public Player(float pX, float pY, ITextureRegion pTextureRegion, VertexBufferObjectManager vertexBufferObjectManager) {
        super(pX, pY);

        this.pX = pX;
        this.pY  = pY;
        Sprite player = new Sprite(0, 0, pTextureRegion, vertexBufferObjectManager);
        attachChild(player);
    }

//    @Override
//    public void onAttached() {
//
//        super.onAttached();
//
//    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getpX() {
        return pX;
    }

    public void setpX(float pX) {
        this.pX = pX;
    }

    public float getpY() {
        return pY;
    }

    public void setpY(float pY) {
        this.pY = pY;
    }

    public int getCurrentLocationIndex() {
        return currentLocationIndex;
    }

    public void setCurrentLocationIndex(int currentLocationIndex) {
        this.currentLocationIndex = currentLocationIndex;
    }
}
