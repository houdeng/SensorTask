package com.example.sensortask;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

public class AutoUpdateService extends Service {

    private MainActivity mActivity;

    private GetDataBinder mBinder = new GetDataBinder();

    private static String str = "{\"method\":\"thing.service.property.set\",\"id\":\"674238177\",\"params\":{\"PowerSwitch\":2},\"version\":\"1.0.0\"}\n";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 根据13.5节的知识来理解
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 5000;        //5 秒钟
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        mActivity.publishMessage(str);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    class GetDataBinder extends Binder{
        public void getData(MainActivity activity){
            mActivity = activity;
        }
    }
}
