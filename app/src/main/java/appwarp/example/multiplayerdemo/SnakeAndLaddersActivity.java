package appwarp.example.multiplayerdemo;

/**
 * Created by per on 09/04/15.
 */

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.shephertz.app42.gaming.multiplayer.client.WarpClient;
import com.shephertz.app42.gaming.multiplayer.client.command.WarpResponseResultCode;
import com.shephertz.app42.gaming.multiplayer.client.events.ChatEvent;
import com.shephertz.app42.gaming.multiplayer.client.events.ConnectEvent;
import com.shephertz.app42.gaming.multiplayer.client.events.LiveRoomInfoEvent;
import com.shephertz.app42.gaming.multiplayer.client.events.LobbyData;
import com.shephertz.app42.gaming.multiplayer.client.events.MoveEvent;
import com.shephertz.app42.gaming.multiplayer.client.events.RoomData;
import com.shephertz.app42.gaming.multiplayer.client.events.RoomEvent;
import com.shephertz.app42.gaming.multiplayer.client.events.UpdateEvent;
import com.shephertz.app42.gaming.multiplayer.client.listener.ConnectionRequestListener;
import com.shephertz.app42.gaming.multiplayer.client.listener.NotifyListener;
import com.shephertz.app42.gaming.multiplayer.client.listener.RoomRequestListener;

import org.andengine.engine.Engine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.entity.modifier.MoveModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.IModifier;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SnakeAndLaddersActivity extends SimpleBaseGameActivity implements DiceClickListener, ConnectionRequestListener, RoomRequestListener, NotifyListener {

    final private float STROKE_WIDTH = 4;

    private WarpClient theClient;

    private PlaceHolder[][] placeHolders = new PlaceHolder[Constants.GRID_WIDTH][Constants.GRID_HEIGHT];


    private Scene scene;
    private BuildableBitmapTextureAtlas mBitmapTextureAtlas;
    private ITextureRegion mPlayerOneTexture;
    private ITextureRegion mPlayerTwoTexture;
    private ITexture fontTexture;
    private Font mFont;
    private List<Line> snakeLines = new ArrayList<>();
    private List<Line> ladderLines = new ArrayList<>();
    private HashMap<String, Player> players = new HashMap<>();
    private ITextureRegion mDiceTexture;
    private Font mFontDice;
    private ITexture fontTextureDice;
    private Dice dice;
    private Turn currentTurn = Turn.Idle;

    private String roomId;
    private HashMap<String, Object> currentTableProperties = new HashMap<>();
    private ProgressDialog progressDialog = null;


    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (theClient != null) {
            leaveGame(Utils.getMyUserName());
            theClient.leaveRoom(roomId);
            theClient.unsubscribeRoom(roomId);
            theClient.removeRoomRequestListener(this);
            theClient.removeNotificationListener(this);
            unloadResources();
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 128, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        this.mPlayerOneTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "pone.png");
        this.mPlayerTwoTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "ptwo.png");
        this.mDiceTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "dice.png");


        try {
            this.mBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 0));
            this.mBitmapTextureAtlas.load();
        } catch (ITextureAtlasBuilder.TextureAtlasBuilderException e) {
            Debug.e(e);
        }

        fontTexture = new BitmapTextureAtlas(this.getTextureManager(), 64, 64);
        fontTextureDice = new BitmapTextureAtlas(this.getTextureManager(), 256, 256);


        mFont = FontFactory.createFromAsset(this.getFontManager(), fontTexture, this.getAssets(), "font/sansmedium.ttf", 18f, true, Color.BLACK);
        mFontDice = FontFactory.createFromAsset(this.getFontManager(), fontTextureDice, this.getAssets(), "font/sansmedium.ttf", 56f, true, Color.BLACK);

        mFont.load();
        mFontDice.load();


    }

    @Override
    protected Scene onCreateScene() {
        scene = new Scene();
        final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();

        float lineX[] = new float[Constants.GRID_WIDTH];
        float lineY[] = new float[Constants.GRID_HEIGHT];

        float touchX[] = new float[Constants.GRID_WIDTH];
        float touchY[] = new float[Constants.GRID_HEIGHT];

        float midTouchX = Constants.CAMERA_WIDTH / Constants.GRID_WIDTH / 2;
        float midTouchY = Constants.CAMERA_HEIGHT / Constants.GRID_HEIGHT / 2;

        float halfTouchX = mPlayerOneTexture.getWidth() / 2;
        float halfTouchY = mPlayerOneTexture.getHeight() / 2;

        float paddingX = midTouchX - halfTouchX;
        float paddingY = midTouchY - halfTouchY;

        for (int i = 0; i < Constants.GRID_WIDTH; i++) {
            lineX[i] = Constants.CAMERA_WIDTH / Constants.GRID_WIDTH * i;
            touchX[i] = lineX[i] + paddingX;
        }

        for (int i = 0; i < Constants.GRID_HEIGHT; i++) {
            lineY[i] = Constants.CAMERA_HEIGHT / Constants.GRID_HEIGHT * i;
            touchY[i] = lineY[i] + paddingY;
        }

        scene.setBackground(new Background(0.85f, 0.85f, 0.85f));

        // draw the grid lines
        for (int i = 0; i < Constants.GRID_WIDTH; i++) {
            final Line line = new Line(lineX[i], 0, lineX[i], Constants.CAMERA_HEIGHT, STROKE_WIDTH, vertexBufferObjectManager);
            line.setColor(0.15f, 0.15f, 0.15f);
            scene.attachChild(line);
        }
        for (int i = 0; i < Constants.GRID_HEIGHT; i++) {
            final Line line = new Line(0, lineY[i], Constants.CAMERA_WIDTH, lineY[i], STROKE_WIDTH, vertexBufferObjectManager);
            line.setColor(0.15f, 0.15f, 0.15f);
            scene.attachChild(line);
        }

        // button sprites
        boolean reverse = false;
        int index = 0;
        for (int j = Constants.GRID_HEIGHT - 1; j >= 0; j--) {

            if (!reverse) {
                for (int i = 0; i < Constants.GRID_WIDTH; i++) {
                    index++;
                    PlaceHolder placeHolder = new PlaceHolder(index, touchX[i], touchY[j], mFont, String.valueOf(index), getVertexBufferObjectManager());
                    scene.attachChild(placeHolder);
                    placeHolders[i][j] = placeHolder;
                }
            } else {
                for (int i = Constants.GRID_WIDTH - 1; i >= 0; i--) {
                    index++;
                    PlaceHolder placeHolder = new PlaceHolder(index, touchX[i], touchY[j], mFont, String.valueOf(index), getVertexBufferObjectManager());
                    scene.attachChild(placeHolder);
                    placeHolders[i][j] = placeHolder;
                }
            }
            if (index % 10 == 0) reverse = !reverse;

        }

        scene.setTouchAreaBindingOnActionDownEnabled(true);

        Intent intent = getIntent();
        roomId = intent.getStringExtra("roomId");
        initRoom(roomId);

        return scene;
    }

    private void initRoom(String roomId) {
        if (theClient != null) {
            theClient.addRoomRequestListener(this);
            theClient.addNotificationListener(this);
            theClient.joinRoom(roomId);
        }
    }

    private void initBoard() {

        for (Map.Entry<Integer, Integer> snake : Utils.getSnakes().entrySet()) {

            final Line line = new Line(
                    getPlaceHolderByIndex(snake.getKey()).pX,
                    getPlaceHolderByIndex(snake.getKey()).pY,
                    getPlaceHolderByIndex(snake.getValue()).pX,
                    getPlaceHolderByIndex(snake.getValue()).pY, 9, getVertexBufferObjectManager());
            line.setColor(1f, 0f, 0f);
            snakeLines.add(line);
            scene.attachChild(line);
        }

        for (Map.Entry<Integer, Integer> snake : Utils.getLadders().entrySet()) {

            final Line line = new Line(
                    getPlaceHolderByIndex(snake.getKey()).pX,
                    getPlaceHolderByIndex(snake.getKey()).pY,
                    getPlaceHolderByIndex(snake.getValue()).pX,
                    getPlaceHolderByIndex(snake.getValue()).pY, 9, getVertexBufferObjectManager());
            line.setColor(0f, 0f, 1f);
            ladderLines.add(line);
            scene.attachChild(line);
        }

        dice = new Dice(Constants.CAMERA_WIDTH / 2 - mDiceTexture.getWidth() / 2, Constants.CAMERA_WIDTH / 2 - mDiceTexture.getHeight() / 2, mDiceTexture, getVertexBufferObjectManager(), this);
        scene.registerTouchArea(dice);
        scene.attachChild(dice);

        if (players.size() > 1) {
            toggleTurn();
        } else {
            waitForAnotherPlayer();
        }
    }

    private void waitForAnotherPlayer() {
        dice.setVisible(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog = ProgressDialog.show(SnakeAndLaddersActivity.this, "", "Waiting for another player");
            }
        });
    }

//    private void reset() {
//
//        for (Map.Entry<String, Player> entry : players.entrySet()) {
//            scene.detachChild(entry.getValue());
//        }
//        for (Line line : snakeLines) {
//            scene.detachChild(line);
//        }
//        for (Line line : ladderLines) {
//            scene.detachChild(line);
//        }
//
//        Utils.getSnakes().clear();
//        Utils.getLadders().clear();
//
//
//        snakeLines.clear();
//        ladderLines.clear();
//
//        if (Utils.getMeAsStarter()) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    progressDialog = ProgressDialog.show(SnakeAndLaddersActivity.this, "", "Pleaes wait...");
//                    progressDialog.setCancelable(true);
//                }
//            });
//
//            Utils.generateSnakes();
//            Utils.generateLadders();
//
//            HashMap<String, Object> properties = new HashMap<String, Object>();
//            properties.put("snakes", Utils.getSnakes());
//            properties.put("ladders", Utils.getLadders());
//            theClient.updateRoomProperties(roomId, properties, null);
//        }
//
//        HashMap<String, Player> playersCopy = new HashMap<>();
//        for(Map.Entry<String, Player> entry : players.entrySet())
//        {
//            playersCopy.put(entry.getKey(), entry.getValue());
//        }
//
//        players.clear();
//
//        for(Map.Entry<String, Player> entry : playersCopy.entrySet())
//        {
//            addPlayer(entry.getKey(), entry.getKey().equals(Utils.getMyUserName()));
//        }
//
//        initBoard();
//
//    }

    private void unloadResources() {
        fontTexture.unload();
        fontTextureDice.unload();
        mBitmapTextureAtlas.unload();
        scene.dispose();
    }

    public void leaveGame(String name) {

        if (name.length() > 0 && players.get(name) != null) {
            Player player = players.get(name);
            final Engine.EngineLock engineLock = this.mEngine.getEngineLock();
            engineLock.lock();
            scene.detachChild(player);
            player.dispose();
            player = null;
            players.remove(name);
            engineLock.unlock();
        }
    }

    private void movePlayerImmediately(final int from, final int to, final Player player) {
        final MoveModifier moveModifier = new MoveModifier(Constants.ANIM_SPEED, getPlaceHolderByIndex(from).pX, getPlaceHolderByIndex(to).pX, getPlaceHolderByIndex(from).pY, getPlaceHolderByIndex(to).pY);
        moveModifier.addModifierListener(new IEntityModifier.IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                player.setCurrentLocationIndex(to);
                toggleTurn();
            }
        });
        player.registerEntityModifier(moveModifier);
    }

    private void movePlayer(final int from, final int to, final Player player) {
        final int[] diff = {to - from};

        final MoveModifier moveModifier = new MoveModifier(Constants.ANIM_SPEED, getPlaceHolderByIndex(from).pX, getPlaceHolderByIndex(from + 1).pX, getPlaceHolderByIndex(from).pY, getPlaceHolderByIndex(from + 1).pY);
        moveModifier.addModifierListener(new IEntityModifier.IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                player.setCurrentLocationIndex(to);
                diff[0]--;
                if (diff[0] > 0) {
                    movePlayer(from + 1, to, player);
                } else {
                    if (Utils.getSnakes().containsValue(to)) {
                        for (Map.Entry<Integer, Integer> snake : Utils.getSnakes().entrySet()) {
                            if (snake.getValue() == to) {
                                movePlayerImmediately(player.getCurrentLocationIndex(), snake.getKey(), player);
                                break;
                            }
                        }
                    } else if (Utils.getLadders().containsKey(to)) {
                        movePlayerImmediately(player.getCurrentLocationIndex(), Utils.getLadders().get(to).intValue(), player);
                    } else {
                        toggleTurn();
                    }
                }
            }
        });
        player.registerEntityModifier(moveModifier);
    }

    private PlaceHolder getPlaceHolderByIndex(int index) {
        for (int i = 0; i < Constants.GRID_WIDTH; i++) {
            for (int j = 0; j < Constants.GRID_HEIGHT; j++) {
                if (placeHolders[i][j].index == index) {
                    return placeHolders[i][j];
                }
            }
        }
        return null;
    }

    public void addPlayer(String userName, boolean me) {
        if (players.containsKey(userName)) {
            return;
        }

        final Player player = new Player(getPlaceHolderByIndex(1).pX, getPlaceHolderByIndex(1).pY, me ? mPlayerOneTexture : mPlayerTwoTexture, getVertexBufferObjectManager());
        player.setUserName(userName);
        player.setCurrentLocationIndex(1);

        players.put(userName, player);

        scene.attachChild(player);

        if (players.size() > 1) {
            if (progressDialog != null) {
                progressDialog.cancel();
                progressDialog = null;
                toggleTurn();
            }
        }

    }


    @Override
    public EngineOptions onCreateEngineOptions() {
        try {
            theClient = WarpClient.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Camera camera = new Camera(0, 0, Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);

        EngineOptions options = new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED,
                new RatioResolutionPolicy(Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT), camera);
        return options;
    }


    @Override
    public void onDiceClicked(final int diceIndex) {
        dice.setVisible(false);

        final Player me = players.get(Utils.getMyUserName());

        if (me.getCurrentLocationIndex() + diceIndex == 100) {

            setWinner(true);

        } else if (me.getCurrentLocationIndex() + diceIndex > 100) {

            notAllowedMove();

        } else {


            final Text diceText = new Text(Constants.CAMERA_WIDTH / 2, 240, mFontDice, String.valueOf(diceIndex), getVertexBufferObjectManager());
            scene.attachChild(diceText);
            ScaleModifier modifier = new ScaleModifier(0.8f, 1.0f, 2.5f);
            modifier.addModifierListener(new IEntityModifier.IEntityModifierListener() {
                @Override
                public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

                }

                @Override
                public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                    scene.detachChild(diceText);
                    movePlayer(me.getCurrentLocationIndex(), me.getCurrentLocationIndex() + diceIndex, me);
                }
            });
            diceText.registerEntityModifier(modifier);
        }

        sendActionDice(diceIndex);

    }

    private void sendActionDice(int diceIndex) {
        try {
            JSONObject object = new JSONObject();
            object.put("dice", diceIndex);
            theClient.sendChat(object.toString());
        } catch (Exception e) {
            Log.d("sendUpdateEvent", e.getMessage());
        }
    }


    private void notAllowedMove() {
        final Text diceText = new Text(190, Constants.CAMERA_WIDTH / 2, mFontDice, "NOT ALLOWED", getVertexBufferObjectManager());
        scene.attachChild(diceText);
        ScaleModifier modifier = new ScaleModifier(0.8f, 1.0f, 2.5f);
        modifier.addModifierListener(new IEntityModifier.IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                scene.detachChild(diceText);
                toggleTurn();
            }
        });
        diceText.registerEntityModifier(modifier);
    }

    private void toggleTurn() {
        if (currentTurn == Turn.Idle) {
            if (Utils.getMeAsStarter()) {
                dice.setVisible(true);
                currentTurn = Turn.Me;
            } else {
                dice.setVisible(false);
                currentTurn = Turn.Other;
            }
        } else if (currentTurn == Turn.Me) {
            if (players.size() < 2) {
                currentTurn = Turn.Me;
                dice.setVisible(true);
            } else {
                currentTurn = Turn.Other;
                dice.setVisible(false);
            }
        } else if (currentTurn == Turn.Other) {
            if (players.size() < 2) {
                currentTurn = Turn.Me;
                dice.setVisible(true);
            } else {
                currentTurn = Turn.Me;
                dice.setVisible(true);
            }

        }

    }

    private void setWinner(final boolean meWinner) {
        final Text message = new Text(210, Constants.CAMERA_WIDTH / 2, mFontDice, meWinner ? "YOU WON!" : "YOU LOST!", getVertexBufferObjectManager());
        scene.attachChild(message);
        ScaleModifier modifier = new ScaleModifier(0.8f, 1.0f, 2.5f);
        modifier.addModifierListener(new IEntityModifier.IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                scene.detachChild(message);
//                if (meWinner)
//                    reset();
            }
        });
        message.registerEntityModifier(modifier);
    }


    @Override
    public void onConnectDone(ConnectEvent connectEvent) {

    }

    @Override
    public void onDisconnectDone(ConnectEvent connectEvent) {

    }

    @Override
    public void onInitUDPDone(byte b) {

    }

    @Override
    public void onSubscribeRoomDone(RoomEvent event) {
        if (event.getResult() == WarpResponseResultCode.SUCCESS) {
            theClient.getLiveRoomInfo(roomId);
        } else {
            Utils.showToastOnUIThread(this, "onSubscribeRoomDone: Failed " + event.getResult());
        }
    }

    @Override
    public void onUnSubscribeRoomDone(RoomEvent roomEvent) {

    }

    @Override
    public void onJoinRoomDone(RoomEvent event) {
        if (event.getResult() == WarpResponseResultCode.SUCCESS) {
            theClient.subscribeRoom(roomId);
        } else {
            Utils.showToastOnUIThread(this, "onJoinRoomDone: Failed " + event.getResult());
        }
    }

    @Override
    public void onLeaveRoomDone(RoomEvent roomEvent) {

    }

    @Override
    public void onGetLiveRoomInfoDone(LiveRoomInfoEvent event) {
        if (event.getResult() == WarpResponseResultCode.SUCCESS) {
            String[] joinedUser = event.getJoinedUsers();
            if (joinedUser != null) {
                for (int i = 0; i < joinedUser.length; i++) {
                    addPlayer(joinedUser[i], event.getData().getRoomOwner().equals(joinedUser[i]));
                }
            }
            currentTableProperties = event.getProperties();
            Utils.setMeAsStarter(event.getData().getRoomOwner().equals(Utils.getMyUserName()));
            Utils.getSnakes().clear();
            Utils.getLadders().clear();
            for (Map.Entry<String, Object> entry : currentTableProperties.entrySet()) {
                if (entry.getKey().equals("snakes")) {
                    String snakesStr = entry.getValue().toString();
                    snakesStr = snakesStr.replace('{', ' ').replace('}', ' ');
                    for (String pair : snakesStr.split(",")) {
                        pair = pair.trim();
                        Utils.getSnakes().put(Integer.parseInt(pair.split("=")[0]), Integer.parseInt(pair.split("=")[1]));
                    }
                } else if (entry.getKey().equals("ladders")) {
                    String laddersStr = entry.getValue().toString();
                    laddersStr = laddersStr.replace('{', ' ').replace('}', ' ');
                    for (String pair : laddersStr.split(",")) {
                        pair = pair.trim();
                        Utils.getLadders().put(Integer.parseInt(pair.split("=")[0]), Integer.parseInt(pair.split("=")[1]));
                    }
                }
            }

            initBoard();

        } else {
            Utils.showToastOnUIThread(this, "onGetLiveRoomInfoDone: Failed " + event.getResult());
        }
    }

    @Override
    public void onSetCustomRoomDataDone(LiveRoomInfoEvent liveRoomInfoEvent) {

    }

    @Override
    public void onUpdatePropertyDone(LiveRoomInfoEvent liveRoomInfoEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.cancel();
                    progressDialog = null;
                }
            }
        });
    }

    @Override
    public void onLockPropertiesDone(byte b) {

    }

    @Override
    public void onUnlockPropertiesDone(byte b) {

    }

    @Override
    public void onRoomCreated(RoomData roomData) {

    }

    @Override
    public void onRoomDestroyed(RoomData roomData) {

    }

    @Override
    public void onUserLeftRoom(RoomData roomData, String s) {
        leaveGame(s);
    }

    @Override
    public void onUserJoinedRoom(RoomData roomData, String s) {
        addPlayer(s, roomData.getRoomOwner().equals(s));
    }

    @Override
    public void onUserLeftLobby(LobbyData lobbyData, String s) {

    }

    @Override
    public void onUserJoinedLobby(LobbyData lobbyData, String s) {

    }

    @Override
    public void onChatReceived(ChatEvent chatEvent) {
        String sender = chatEvent.getSender();
        if (sender.equals(Utils.getMyUserName()) == false) {
            String message = chatEvent.getMessage();
            try {
                JSONObject object = new JSONObject(message);
                int diceIndex = Integer.parseInt(object.getString("dice"));
                Player player = players.get(chatEvent.getSender());
                if (player.getCurrentLocationIndex() + diceIndex > 100) {
                    toggleTurn();
                } else if (player.getCurrentLocationIndex() + diceIndex == 100) {
                    setWinner(sender.equals(Utils.getMyUserName()));
                } else {
                    movePlayer(player.getCurrentLocationIndex(), player.getCurrentLocationIndex() + diceIndex, player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPrivateChatReceived(String s, String s1) {

    }

    @Override
    public void onPrivateUpdateReceived(String s, byte[] bytes, boolean b) {

    }

    @Override
    public void onUpdatePeersReceived(UpdateEvent updateEvent) {

    }

    @Override
    public void onUserChangeRoomProperty(RoomData roomData, String userName, HashMap<String, Object> hashMap, HashMap<String, String> hashMap1) {
        if (userName.equals(Utils.getMyUserName())) {
            // just update the local property table.
            // no need to update UI as we have already done so.
            currentTableProperties = hashMap;
            return;
        }
        currentTableProperties = hashMap;
        Utils.setMeAsStarter(userName.equals(Utils.getMyUserName()));
        Utils.getSnakes().clear();
        Utils.getLadders().clear();
        for (Map.Entry<String, Object> entry : currentTableProperties.entrySet()) {
            if (entry.getKey().equals("snakes")) {
                String snakesStr = entry.getValue().toString();
                snakesStr = snakesStr.replace('{', ' ').replace('}', ' ');
                for (String pair : snakesStr.split(",")) {
                    pair = pair.trim();
                    Utils.getSnakes().put(Integer.parseInt(pair.split("=")[0]), Integer.parseInt(pair.split("=")[1]));
                }
            } else if (entry.getKey().equals("ladders")) {
                String laddersStr = entry.getValue().toString();
                laddersStr = laddersStr.replace('{', ' ').replace('}', ' ');
                for (String pair : laddersStr.split(",")) {
                    pair = pair.trim();
                    Utils.getLadders().put(Integer.parseInt(pair.split("=")[0]), Integer.parseInt(pair.split("=")[1]));
                }
            }
        }

        initBoard();
    }

    @Override
    public void onMoveCompleted(MoveEvent moveEvent) {

    }

    @Override
    public void onGameStarted(String s, String s1, String s2) {

    }

    @Override
    public void onGameStopped(String s, String s1) {

    }

    @Override
    public void onUserPaused(String s, boolean b, String s1) {

    }

    @Override
    public void onUserResumed(String s, boolean b, String s1) {

    }

    @Override
    public void onNextTurnRequest(String s) {

    }
}
