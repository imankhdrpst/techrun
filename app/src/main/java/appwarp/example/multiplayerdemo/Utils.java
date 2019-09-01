package appwarp.example.multiplayerdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Random;


public class Utils {

	private static String myUserName = "";
	private static boolean meAsStarter;

	private static HashMap<Integer, Integer> snakes = new HashMap<>(), ladders = new HashMap<>();

	public static float getPercentFromValue(float number, float amount){
		float percent = (number/amount)*100;
		return percent;
	}
	
	public static float getValueFromPercent(float percent, float amount){
		float value = (percent/100)*amount;
		return value;
	}
	
	public static void showToastAlert(Activity ctx, String alertMessage){
		Toast.makeText(ctx, alertMessage, Toast.LENGTH_SHORT).show();
	}
	
	public static void showToastOnUIThread(final Activity ctx, final String alertMessage){
		ctx.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ctx, alertMessage, Toast.LENGTH_SHORT).show();
				
			}
		});
	}

	public static String getMyUserName() {
		return myUserName;
	}

	public static void setMyUserName(String myUserName) {
		Utils.myUserName = myUserName;
	}

    public static void setMeAsStarter(boolean meAsStarter) {
        Utils.meAsStarter = meAsStarter;
    }

    public static boolean getMeAsStarter() {
        return meAsStarter;
    }



	public static void generateLadders() {
		int size = 2;
		for (int i = 0; i < 3; i++) {
			int from, to;
			from = (new Random()).nextInt(10) + (i + 2) * 10;
			to = (new Random()).nextInt(10) * size + from;
			while (true) {
				if (ladders.containsKey(from) || ladders.containsValue(from) || snakes.containsKey(from) || snakes.containsValue(from)) {
					from = (new Random()).nextInt(10) + (i + 2) * 10;
				} else if (ladders.containsKey(to) || ladders.containsValue(to) || snakes.containsKey(to) || snakes.containsValue(to)) {
					to = (new Random()).nextInt(10) * size + from;
				} else if (to >= 100) {
					to = (new Random()).nextInt(10) * size + from;
				} else if (Math.abs(from - to) < 10) {
					to = (new Random()).nextInt(10) * size + from;
				} else {
					break;
				}
			}
			ladders.put(from, to);
			size += 2;
		}
	}

	public static void generateSnakes() {
		int size = 2;
		for (int i = 0; i < 3; i++) {
			int from, to;
			from = (new Random()).nextInt(10) + (i + 2) * 10;
			to = (new Random()).nextInt(10) * size + from;
			while (snakes.containsKey(from) || snakes.containsValue(from)) {
				from = (new Random()).nextInt(10) + (i + 2) * 10;
			}
			while (true) {
				if (snakes.containsKey(to) || snakes.containsValue(to)) {
					to = (new Random()).nextInt(10) * size + from;
				} else if (to >= 100) {
					to = (new Random()).nextInt(10) * size + from;
				} else if (Math.abs(from - to) < 10) {
					to = (new Random()).nextInt(10) * size + from;
				} else {
					break;
				}
			}
			snakes.put(from, to);
			size += 2;
		}
	}

	public static HashMap<Integer, Integer> getSnakes() {
		return snakes;
	}

	public static HashMap<Integer, Integer> getLadders() {
		return ladders;
	}
}
