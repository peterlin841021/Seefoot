package ntue.prj;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import android.hardware.Sensor;
import android.hardware.SensorManager;


import java.util.ArrayList;

public class CheckService extends Service implements GoogleApiClient.OnConnectionFailedListener,GoogleApiClient.ConnectionCallbacks,LocationListener {
    private Toast hint;

    private GoogleApiClient client;
    private LocationRequest locationRequest;


    private SQLiteDatabase database;
    private String databaseName="TrafficLight",tableName="location";


    private Intent map;
    //Sensor 管理
    private SensorManager sm;
    //使用的Sensor
    private Sensor accelerometer;
    //Use sensor to detect shaking
    private ShakeDetector dectector;

    //Arrow
    private Toast arrow;
    private ImageView iv;
    //route mode
    private SharedPreferences sp;
    private int reciprocal;
    //store one intersection
    private LatLng intersection;
    private LatLng old;
    //Lock
    private boolean Locked;
    //face
    private LatLng two[];

    @Override
    public void onCreate() {
        init();
        super.onCreate();
    }

    private void init(){
        database = openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null);
        if (client == null) {
            client = new GoogleApiClient.Builder(this).
                    addApi(LocationServices.API).
                    addOnConnectionFailedListener(this).
                    addConnectionCallbacks(this).build();
            locationRequest = new LocationRequest();
            // 設定讀取位置資訊的間隔時間
            locationRequest.setInterval(5000);
            // 設定讀取位置資訊最快的間隔時間
            locationRequest.setFastestInterval(5000);
            // 設定優先讀取高精確度的位置資訊（GPS）
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
        client.connect();

        map = new Intent(this, Map.class);
        //避免重複開啟Map
        map.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        arrow=new Toast(this);
        iv=new ImageView(this);
        iv.setImageResource(R.drawable.arrow);
        arrow.setGravity(Gravity.TOP,0,0);
        arrow.setView(iv);
        arrow.setDuration(Toast.LENGTH_SHORT);
        sp=getSharedPreferences("RoadMode",0);
        two=new LatLng[2];
        two[0]=new LatLng(0,0);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        dectector = new ShakeDetector();
        dectector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake(int count) {
//                System.out.println("Check"+count);
                if (count == 2) {
                    startActivity(map);
                }
            }
        });
        sm.registerListener(dectector, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //結束Service
    @Override
    public void onDestroy()
    {
        client.disconnect();
        if(sm  != null) {
            sm.unregisterListener(dectector);
        }
        super.onDestroy();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        int errorCode = connectionResult.getErrorCode();
        if (errorCode == ConnectionResult.SERVICE_MISSING) {
            Toast.makeText(this,"裝置沒有安裝Google play service!",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                client, locationRequest, CheckService.this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private float getAngle(LatLng one,LatLng two){
        float angle=0;

        if(one.latitude - two.latitude == 0 && one.longitude - two.longitude == 0)
            angle=0;
        else if (two.latitude - one.latitude > 0 && two.longitude - one.longitude == 0)
            angle=1;
        else if (two.latitude - one.latitude < 0 && two.longitude - one.longitude == 0)
            angle=180;
        else if (two.latitude - one.latitude == 0 && two.longitude - one.longitude > 0)
            angle=90;
        else if (two.latitude - one.latitude == 0 && two.longitude - one.longitude < 0)
            angle=270;
        else if (two.latitude - one.latitude > 0 && two.longitude - one.longitude > 0)
            angle=45;
        else if (two.latitude - one.latitude < 0 && two.longitude - one.longitude < 0)
            angle=225;
        else if (two.latitude - one.latitude > 0 && two.longitude - one.longitude < 0)
            angle=315;
        else if (two.latitude - one.latitude < 0 && two.longitude - one.longitude > 0)
            angle=135;
        return angle;
    }

    private void arriveDest()
    {
        //arrive
        iv.setImageResource(R.drawable.dest);
        iv.setRotation(0);
        arrow.setGravity(Gravity.TOP,0,0);
        arrow.setView(iv);
        arrow.show();
        reciprocal++;
        if(reciprocal == 3)
        {
            Map.changerSwitch();
        }
    }

    private LatLng nearestPoint(ArrayList<LatLng> path,LatLng where){
        LatLng nearest=null;
        double distance=Double.MAX_VALUE;
        for (int i=0;i<path.size();i++)
        {
            if(distance > Math.sqrt(
                    Math.pow(where.longitude - path.get(i).longitude, 2) +
                            Math.pow(where.latitude - path.get(i).latitude, 2)))
            {
                distance=Math.sqrt(
                        Math.pow(where.longitude - path.get(i).longitude, 2) +
                                Math.pow(where.latitude - path.get(i).latitude, 2));
                nearest=path.get(i);
            }
        }
        return nearest;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        float angle1=0,angle2=0;
        double latitude,longitude;

        if (location != null)
        {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            //angle
            try {
                if (two[0].latitude == 0)
                {
                    two[0] = new LatLng(latitude, longitude);
                } else {
                    two[1] = new LatLng(latitude, longitude);
                   angle1=getAngle(two[0],two[1]);
                }
            }catch(NullPointerException e)
            {
                System.out.println("one of points is null , can't decide the direction.");
            }
            //navigation
            ArrayList<LatLng> path=null;
            if(Map.getOpti() != null)
                path=Map.getOpti().getLine();
            if(sp.getInt("mode",-1) == 0 && path != null)
            {
                if(Math.sqrt(Math.pow(longitude - path.get(path.size()-1).longitude, 2) + Math.pow(latitude - path.get(path.size()-1).latitude, 2)) <0.0001 )
                {
                    arriveDest();
                }else
                {
                    if(path.size() > 0)
                    {
                        LatLng temp=nearestPoint(path,new LatLng(latitude,longitude));
                        if(two[1] != null && two[1].latitude != 0)
                        {
                            angle2=getAngle(two[0],temp);
                            //clear user direction
                            two[0] = new LatLng(0, 0);
                            two[1] = new LatLng(0, 0);
                            if(reciprocal < 1)
                            {
                                //iv.setRotation((float)gps2d(temp.latitude,temp.longitude,two[1].latitude,two[1].longitude)-180);
                                iv.setRotation(angle2-angle1);
                                arrow.show();
                            }
                        }
                    }
                }
            }

            //newest location
            map.putExtra("la", location.getLatitude());
            map.putExtra("lo", location.getLongitude());
            //reset intersection
            if(intersection != null && Math.sqrt(Math.pow(longitude - intersection.longitude, 2) + Math.pow(latitude - intersection.latitude, 2)) > 0.0003)
            {
                intersection=null;
            }
            if (isDangerous(new LatLng(latitude,longitude)))
            {
                //intersection
                if(intersection == null)
                {
                    intersection=new LatLng(old.latitude,old.longitude);
                    Locked=false;

                }else if(old.longitude == intersection.longitude &&
                        old.latitude == intersection.latitude  )
                {
                    Locked=true;
                }else if(old.longitude != intersection.longitude)
                {
                    intersection=new LatLng(old.latitude,old.longitude);
                    Locked=false;
                }

                if(!Locked && sp.getInt("interrput",-1) == 1){
                    //Bump
                    startActivity(map);
                }else if(!Locked && sp.getInt("interrput",-1) == 0)
                {
                    Locked=true;
                    vibrateWithRing();
                }
            }
        }
    }

    private boolean isDangerous(LatLng where){
        boolean dangerous=false;
        Cursor cursor = database.rawQuery("select longitude,latitude from " + tableName, null);
        double distance;
        if (cursor.moveToFirst())
        {
            do {
                //小於安全距離，約30公尺
                distance = Math.sqrt(Math.pow(where.latitude - cursor.getDouble(cursor.getColumnIndex("latitude")), 2) + Math.pow(where.longitude - cursor.getDouble(cursor.getColumnIndex("longitude")), 2));
                if (distance < 0.0003) {
                    old = new LatLng(cursor.getDouble(cursor.getColumnIndex("latitude")), cursor.getDouble(cursor.getColumnIndex("longitude")));
                    dangerous = true;
                    break;
                } else
                    dangerous = false;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dangerous;
    }

    private void vibrateWithRing()
    {
        //vibrate
        Vibrator myVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(new long[]{10, 1000},-1);//no repeat
        //Ring
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 200);
    }

}

