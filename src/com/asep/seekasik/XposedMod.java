package com.asep.seekasik;

import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;

public class XposedMod implements IXposedHookLoadPackage {
	
	private static XSharedPreferences PREFS = new XSharedPreferences("com.asep.seekasik");
	
	private static final String PREF_MASTER_POWER = "master_power";
    private static final String PREF_RANDOM_SEEK = "random_seek";
    private static final String PREF_TIME_TO_SEEK = "time_to_seek";
    private static final String PREF_RANDOM_BY_DURATION = "random_by_duration";
    private static final String PREF_RANDOM_DURATION = "random_duration";
    private static final String PREF_RANDOM_PERCENTAGE = "random_percentage";
    
    private boolean masterPower;
    private boolean randomSeek;
    private boolean randomByDuration;
    private long timeToSeek;
    private long duration;
    private int tempCalc;
    private String durationInterval = "";
    private String percentageInterval = "";
    private String[] interval = { "", "" };
    private Random rand = new Random();
	
	public Object mLocalDevicePlayback;	
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.google.android.music")) {
			return;
		}
		
		loadPref();
        		
		findAndHookMethod("com.google.android.music.playback.MusicPlaybackService", lpparam.classLoader, "next", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {			
				super.afterHookedMethod(param);
				
				if (!masterPower) {
					return;
				}
					
				try {
					mLocalDevicePlayback = param.thisObject;
					Thread.sleep(500);					
					
					PREFS.reload();
					loadPref();
					calcTime();
					
					if (duration > timeToSeek) {
						callMethod(mLocalDevicePlayback, "seek", timeToSeek);
					}
			        
				} catch (Throwable e) {
					XposedBridge.log(e.getMessage());
				}				
			}
		});
	}
	
	public void loadPref() {
		masterPower = PREFS.getBoolean(PREF_MASTER_POWER, true);
        randomSeek = PREFS.getBoolean(PREF_RANDOM_SEEK, false);
        randomByDuration = PREFS.getBoolean(PREF_RANDOM_BY_DURATION, true);
        durationInterval = PREFS.getString(PREF_RANDOM_DURATION, "30-90");
        percentageInterval = PREFS.getString(PREF_RANDOM_PERCENTAGE, "10-20");
	}
	
	public void calcTime() {
		duration = (Long)callMethod(mLocalDevicePlayback, "duration");
		timeToSeek = Long.parseLong(PREFS.getString(PREF_TIME_TO_SEEK, "45")) * 1000;
		if (randomSeek)  {
			try {
				interval = randomByDuration ? durationInterval.split("-") : percentageInterval.split("-");
				tempCalc = rand.nextInt(Integer.parseInt(interval[1]) - Integer.parseInt(interval[0]) + 1) + Integer.parseInt(interval[0]);
				timeToSeek = (long) (randomByDuration ? tempCalc * 1000 : ((double)tempCalc/100) * duration);
			} 							
			catch (Exception e) {
				XposedBridge.log(e.getMessage());
				return;
			}
		}
	}
}