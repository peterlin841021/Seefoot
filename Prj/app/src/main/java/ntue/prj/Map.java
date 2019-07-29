package ntue.prj;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Map extends AppCompatActivity implements  GoogleMap.OnCameraChangeListener
        ,GoogleMap.OnMapLongClickListener
        ,LocationListener
        ,GoogleApiClient.OnConnectionFailedListener
        ,GoogleApiClient.ConnectionCallbacks
        ,CompoundButton.OnCheckedChangeListener
        ,DialogInterface.OnClickListener
{

    private static GoogleMap map;
    private static SQLiteDatabase database;
    private String databaseName = "TrafficLight", tableName = "location";
    private float zoomValue = 16;
    //Sensor 管理
    private SensorManager sm;
    //使用的Sensor
    private Sensor accelerometer;
    //Use sensor to detect shaking
    private ShakeDetector dectector;

    private DownloadJsonTask downloadJsonTask;

    private static SharedPreferences  sp;

    //Dialog
    private AlertDialog.Builder builder;
    //location
    private GoogleApiClient client;
    private LocationRequest locationRequest;

    //user's position
    private static LatLng where,dest;
    private boolean Destination;
    //
    //MAIN
    //
    private Intent service;
    private SharedPreferences show;
    private Toast msg;
    private ToggleButton interrput;
    private ToggleButton mode;
    private static ToggleButton switches;
    private static ArrayList<ArrayList<Road>> Road;
    private static Road optimization;
    private static Road connect;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        init();
    }
    //get csv file from Asset
    private void readCSVFile()
    {
        AssetManager am=getAssets();
        String line="";
        String []s;
        try{
            BufferedReader bf= new BufferedReader(new InputStreamReader(am.open("trafficlight.csv")));
            while( (line = bf.readLine()) != null ){
                StringBuilder sb=new StringBuilder("insert into "+tableName+"(longitude,latitude) values('");
                s=line.split(",");
                sb.append(s[1]+"',");
                sb.append("'"+s[2]);
                sb.append("');");
                database.execSQL(sb.toString());
            }
        }catch (IOException e){
            System.out.println(e);
        }
    }
    private void downloadCSV()
    {
        Thread t=new Thread(readData);
        t.start();
    }
    private Runnable readData = new Runnable()
    {
        public void run()
        {
            String serverIp;
            try
            {
                Socket socket;
                serverIp = "192.168.43.137";
                int port=5000;
                socket = new Socket(serverIp, port);
                InputStream is=socket.getInputStream();
                BufferedInputStream bis=new BufferedInputStream(is);
                //int len=socket.getReceiveBufferSize();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bis));
                String line="";
                String []s;
                while((line=bufferedReader.readLine()) != null)
                {
                    if(line.equals("Server accepted!"))
                        continue;
                    StringBuilder sb=new StringBuilder("insert into "+tableName+"(longitude,latitude) values('");
                    s=line.split(",");
                    sb.append(s[1]+"',");
                    sb.append("'"+s[2]);
                    sb.append("');");
                    database.execSQL(sb.toString());
                }
                bis.close();
                socket.close();
            }
            catch (IOException e)
            {
                System.out.println("NET:"+e);
            }
        }
    };

    //use database
    private void databaseOperation(){
        database=openOrCreateDatabase(databaseName, Context.MODE_PRIVATE,null);

        String createTable="create table if not exists "+tableName+"(_id integer primary key autoincrement,longitude double(8),latitude double(8))";
        database.execSQL(createTable);
        //downloadCSV();

    }

    public static void changerSwitch()
    {
        switches.setChecked(false);
    }
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean state)
    {
        if(compoundButton.getId() == R.id.interrupt){
            if(state){
                show.edit().putInt("interrput",0).commit();
                msg.cancel();
                msg=Toast.makeText(this,"Interrpution locked.",Toast.LENGTH_SHORT);
                msg.show();
            }else{
                show.edit().putInt("interrput",1).commit();
                msg.cancel();
                msg=Toast.makeText(this,"Interrpution unlock.",Toast.LENGTH_SHORT);
                msg.show();
            }
        }else if(compoundButton.getId() == R.id.mode){
            if(state){
                map.clear();
                show.edit().putInt("mode",0).commit();
                msg.cancel();
                msg=Toast.makeText(this,"avoidance mode.",Toast.LENGTH_SHORT);
                msg.show();
            }else{
                map.clear();
                show.edit().putInt("mode",1).commit();
                msg.cancel();
                msg=Toast.makeText(this,"all route mode.",Toast.LENGTH_SHORT);
                msg.show();
            }
        }else if(compoundButton.getId() == R.id.switches)
        {
            if(state)
            {
                if ((service == null))
                {
                    map.setOnMapLongClickListener(this);
                    interrput.setVisibility(View.VISIBLE);
                    mode.setVisibility(View.VISIBLE);
                    //show.edit().putInt("restart",1).commit();
                    service = new Intent(this, CheckService.class);
                    startService(service);
                    msg = Toast.makeText(this, "Start service", Toast.LENGTH_SHORT);
                    msg.show();
                    if(where != null){
                        CameraPosition camPos = CameraPosition
                                .builder(
                                        map.getCameraPosition() // current Camera
                                )
                                .target(where)
                                .zoom(19)
                                .build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
                    }
                }
            }else{
                if (service != null)
                {
                    map.setOnMapLongClickListener(null);
                    map.clear();
                    Destination=false;
                    stopService(service);
                    interrput.setVisibility(View.INVISIBLE);
                    mode.setVisibility(View.INVISIBLE);
                    service = null;
                    optimization=null;
                    msg = Toast.makeText(this, "Terminate service", Toast.LENGTH_SHORT);
                    msg.show();
                }
            }
        }

    }

    private void init()
    {
        //optimization or all
        sp=getSharedPreferences("RoadMode",0);
        //用google map標記目前位置和周遭路口
        //可拖拉、縮放地圖
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnCameraChangeListener(this);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        builder=new AlertDialog.Builder(this);

        //hint
        msg=new Toast(this);
        interrput=(ToggleButton)findViewById(R.id.interrupt);
        interrput.setOnCheckedChangeListener(this);
        interrput.setVisibility(View.INVISIBLE);
        mode=(ToggleButton)findViewById(R.id.mode);
        mode.setOnCheckedChangeListener(this);
        mode.setVisibility(View.INVISIBLE);
        switches=(ToggleButton)findViewById(R.id.switches);
        switches.setOnCheckedChangeListener(this);

        //SharePerference
        show=getSharedPreferences("RoadMode",0);
        show.edit().putInt("interrput",1).commit();
        show.edit().putInt("mode",1).commit();

        dectector = new ShakeDetector();
        dectector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake(int count) {
                if (count == 1) {
                    System.out.println("MAP:"+count);
                }
            }
        });
        database = openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null);
        //database
        databaseOperation();
        builder=new AlertDialog.Builder(this);
        //connect
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
        //使用者位置
        map.setMyLocationEnabled(true);
        Road=new ArrayList<>();
    }

    @Override
    protected void onResume() {
        if(where != null){
            CameraPosition camPos = CameraPosition
                    .builder(
                            map.getCameraPosition() // current Camera
                    )
                    .target(where)
                    .zoom(zoomValue)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
        }

        if (sm != null)
            sm.registerListener(dectector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (sm != null) {
            sm.unregisterListener(dectector);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        //滑掉Main即關閉service
        if(service != null)
            stopService(service);
        super.onDestroy();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        zoomValue = cameraPosition.zoom;
    }

    private void updateCameraBearing(GoogleMap googleMap, float bearing,LatLng latLng) {
        if ( googleMap == null) return;
        CameraPosition camPos=null;
        if(latLng != null){
             camPos = CameraPosition
                    .builder(
                            googleMap.getCameraPosition() // current Camera
                    ).target(latLng)
                    .zoom(zoomValue)
                    .bearing(bearing)
                    .build();
        }else{
             camPos = CameraPosition
                    .builder(
                            googleMap.getCameraPosition() // current Camera
                    )
                    .zoom(zoomValue)
                    .bearing(bearing)
                    .build();
        }
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }

    private LatLng nearestPoint(ArrayList<LatLng> path,LatLng where)
    {
        LatLng nearest=null;
        double distance=Double.MAX_VALUE;
        for (int i=0;i<path.size();i++)
        {
            if(distance > Math.sqrt(
                    Math.pow(where.longitude - path.get(i).longitude, 2) +
                            Math.pow(where.latitude - path.get(i).latitude, 2))){
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
        if(location != null)
        {
            double la=location.getLatitude(),lo=location.getLongitude();
            where=new LatLng(la,lo);
            LatLng p=null;
            double dis=0;
            if(optimization != null && where != null)
            {
                p = nearestPoint(optimization.getLine(), where);
                dis=Math.sqrt(Math.pow(p.longitude - where.longitude, 2) + Math.pow(p.latitude - where.latitude, 2));
            }

            //Redirect
            if(dis > 0.002)//200m
            {
                System.out.println("Distance:"+(dis*1000)+"m.");
                Intersection it=new Intersection();
                String url="";
                ArrayList<LatLng> dist=getPoint(where,dest);
                url = getDirectionAPIUrl(where,p, "walking");
                downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,dest),3);
                downloadJsonTask.execute(url);

                System.out.println("Modify!");
                for(LatLng lat : dist)
                {
                    url = getDirectionAPIUrl(where,lat, "walking");
                    downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,dest),2);
                    downloadJsonTask.execute(url);
                }
                //connect
                for(LatLng lat : dist)
                {
                    if(lat.latitude == dest.latitude && lat.longitude == dest.longitude)
                        continue;
                    url = getDirectionAPIUrl(lat,dest, "walking");
                    downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,dest),2);
                    downloadJsonTask.execute(url);
                }
            }else
            {
                System.out.println("Distance:"+(dis*1000)+"m.");
            }

            if(Destination)
            {
                updateCameraBearing(map,location.getBearing(),where);
            }else if(service != null)
            {
                updateCameraBearing(map,location.getBearing(),null);
                markerGenerator(where,new LatLng(0,0));
            }
        }
    }

    private static Road  compareRoad(Road oldRoad,Road newRoad)
    {
        int oldc=0,newc=0;
        for(int cc : oldRoad.getCount())
            oldc+=cc;
        for(int cc : newRoad.getCount())
            newc+=cc;
        if(newc < oldc)
        {
            return newRoad;
        }else if(newc ==  oldc && newRoad.getLength() < oldRoad.getLength())
            return newRoad;

        return oldRoad;
    }

    private ArrayList<LatLng> getPoint(LatLng s,LatLng d)
    {
        ArrayList<LatLng> array=new ArrayList<>();
        LatLng middle=new LatLng((s.latitude+d.latitude)/2,(s.longitude+d.longitude)/2);
        int compass[]=new int[]{-1,0,0,0,0,1,0,0,0,0};
        if((middle.latitude < s.latitude && middle.longitude < s.longitude) || (middle.latitude > s.latitude && middle.longitude > s.longitude))
        {
            compass[3]=1;
            compass[7]=1;
        }else if((middle.latitude < s.latitude && middle.longitude > s.longitude) || (middle.latitude > s.latitude && middle.longitude < s.longitude))
        {
            compass[1]=1;
            compass[9]=1;
        }else if((middle.latitude == s.latitude && middle.longitude > s.longitude) || (middle.latitude == s.latitude && middle.longitude < s.longitude))
        {
            compass[4]=1;
            compass[6]=1;
        }else if((middle.latitude < s.latitude && middle.longitude == s.longitude) || (middle.latitude > s.latitude && middle.longitude == s.longitude))
        {
            compass[2]=1;
            compass[8]=1;
        }

        double deglo=0.001,degla=0.001;//100m
        if(Math.sqrt(Math.pow(s.longitude - d.longitude, 2) + Math.pow(s.latitude - d.latitude, 2)) < 0.01 && Math.sqrt(Math.pow(s.longitude - d.longitude, 2) + Math.pow(s.latitude - d.latitude, 2)) > 0.001)
        {
            degla=0.001;
            deglo=degla;
        }else if(Math.sqrt(Math.pow(s.longitude - d.longitude, 2) + Math.pow(s.latitude - d.latitude, 2)) < 0.1)
        {
            degla=0.01;
            deglo=degla;
        }else if(Math.sqrt(Math.pow(s.longitude - d.longitude, 2) + Math.pow(s.latitude - d.latitude, 2)) < 1)
        {
            degla=0.1;
            deglo=degla;
        }

        for(int i=1;i<compass.length;i++)
        {
            if(compass[i]==0)
                switch(i)
                {
                    case 1:
                        array.add(new LatLng(middle.latitude+degla,middle.longitude-deglo));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 2 :
                        array.add(new LatLng(middle.latitude+degla,middle.longitude));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 3 :
                        array.add(new LatLng(middle.latitude+degla,middle.longitude+deglo));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 4 :
                        array.add(new LatLng(middle.latitude,middle.longitude-deglo));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 5 :
                        array.add(d);
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 6:
                        array.add(new LatLng(middle.latitude,middle.longitude+deglo));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 7 :
                        array.add(new LatLng(middle.latitude-degla,middle.longitude-deglo));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 8 :
                        array.add(new LatLng(middle.latitude-degla,middle.longitude));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                    case 9 :
                        array.add(new LatLng(middle.latitude-degla,middle.longitude+deglo));
                        //map.addMarker(new MarkerOptions().position().icon(BitmapDescriptorFactory.defaultMarker(30)));
                        break;
                }
        }
        return array;
    }
    @Override
    public void onMapLongClick(final LatLng latLng)
    {
        if(where != null)
        {
            //direction
            String url;
            dest=latLng;
            map.clear();
            markerGenerator(where,latLng);
            map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(180)));
            if(sp.getInt("mode",-1) == 0 )
            {
                builder.setTitle("目的地確認")
                        .setView(null)
                        .setPositiveButton("Yes",this)
                        .setNegativeButton("No",this)
                        .setCancelable(false)
                        .show();
            }else if(sp.getInt("mode",-1) == 1)
            {
                Intersection it=new Intersection();
                if(Math.sqrt(Math.pow(where.longitude - latLng.longitude, 2) + Math.pow(where.latitude - latLng.latitude, 2)) <= 0.001)
                {
                    url = getDirectionAPIUrl(where,latLng, "walking");
                    downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,latLng),1);
                    downloadJsonTask.execute(url);
                }else
                {
                    ArrayList<LatLng> dist=getPoint(where,latLng);
                    for(LatLng lat : dist)
                    {
                        url = getDirectionAPIUrl(where,lat, "walking");
                        downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,latLng),12);
                        downloadJsonTask.execute(url);
                    }
                    //connect
                    for(LatLng lat : dist)
                    {
                        if(lat.latitude == latLng.latitude && lat.longitude == latLng.longitude)
                            continue;
                        url = getDirectionAPIUrl(lat,latLng, "walking");
                        downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,latLng),12);
                        downloadJsonTask.execute(url);
                    }
                }
            }
        }
    }
    @Override
    public void onClick(DialogInterface dialogInterface, int check)
    {

        if(check == -1)
        {
            Intersection it=new Intersection();
            ArrayList<LatLng> dist=getPoint(where,dest);
            String url;
            if(Math.sqrt(Math.pow(where.longitude - dest.longitude, 2) + Math.pow(where.latitude - dest.latitude, 2)) <= 0.001)//100m
            {
                url = getDirectionAPIUrl(where,dest, "walking");
                downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,dest),1);
                downloadJsonTask.execute(url);
            }else
            {
                for(LatLng lat : dist)
                {
                    url = getDirectionAPIUrl(where,lat, "walking");
                    //map.addMarker(new MarkerOptions().position(lat).icon(BitmapDescriptorFactory.defaultMarker(30)));
                    downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,dest),12);
                    downloadJsonTask.execute(url);
                }
                //connect
                for(LatLng lat : dist)
                {
                    if(lat.latitude == dest.latitude && lat.longitude == dest.longitude)
                        continue;
                    url = getDirectionAPIUrl(lat,dest, "walking");
                    downloadJsonTask = new DownloadJsonTask(it.getRoadSet(database,where,dest),12);
                    downloadJsonTask.execute(url);
                }
            }
        }
    }

    //use google direction api with json format
    private String getDirectionAPIUrl(LatLng origin, LatLng destination, String mode) {
        String originUrl = "", destinationUrl = "", url = "";
        originUrl = "origin=" + origin.latitude + "," + origin.longitude;
        destinationUrl = "destination=" + destination.latitude + "," + destination.longitude;
        url = "https://maps.googleapis.com/maps/api/directions/json?"
                + originUrl
                + "&"
                + destinationUrl
                + "&mode=walking"
                + "&alternatives=true"
                + "&key=AIzaSyBqAf9ubtD6TNZ3095JdwubThymw2JgrHo";
//        System.out.println("URL:"+url);
        return url;
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                client, locationRequest, Map.this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        int errorCode = connectionResult.getErrorCode();
        if (errorCode == ConnectionResult.SERVICE_MISSING) {
            Toast.makeText(this,"裝置沒有安裝Google play service!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void mapSearch(View v){
        EditText edit=(EditText)findViewById(R.id.placeSearch);
        String location = edit.getText().toString();
        List<Address> addressList=null;
        if(location !=null && !location.equals("")){
            Geocoder geo=new Geocoder(this);
            try{
                addressList=geo.getFromLocationName(location,1);
                if(addressList.size() >0){
                    Address address =addressList.get(0);
                    LatLng lat=new LatLng(address.getLatitude(),address.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLng(lat));
                }
            }catch(IOException e){
                System.out.println("place sarch");
            }
        }
    }

    public void mapClear(View v)
    {
        map.clear();
        Destination=false;
        optimization=null;
    }

    private static void  markerGenerator(LatLng where,LatLng dest)
    {
        Intersection it=new Intersection();
        ArrayList<LatLng> intersection=it.getRoadSet(database,where,dest);
        for(LatLng latLng : intersection)
            map.addMarker(new MarkerOptions().position(latLng));
    }

    public static boolean singleLine(ArrayList<Road> r)
    {
        boolean finish=false;
        Polyline L = map.addPolyline(new PolylineOptions().addAll(r.get(0).getLine()));
        L.setWidth(15);
        L.setColor(Color.BLACK);
        optimization=r.get(0);
        finish=true;
        return finish;
    }

    public static boolean drawLine(ArrayList<Road> r)
    {
        boolean finish=false;
        Road.add(r);
        ArrayList<ArrayList<Road>> six=new ArrayList<>();
        //ArrayList<LatLng> points=new ArrayList<>();
        int colors[]={Color.BLACK,Color.BLUE,Color.GREEN,Color.LTGRAY,Color.RED,Color.YELLOW};

        if(Road.size() == 12)
        {
            if(sp.getInt("mode",-1) == 0)
            {
                /*ArrayList<LatLng> points=new ArrayList<>();
                for(int i=0;i<Road.size();i++)
                {
                    for (Road rr : Road.get(i))
                    {
                        for (LatLng Lat : rr.getLine())
                        {
                            points.add(Lat);
                        }
                    }
                }
                points=interPoints(points);*/
                //LatLng s=Road.get(0).get(0).getLine().get(0),d=Road.get(11).get(0).getLine().get(Road.get(11).get(0).getLine().size()-1);
                Road best=connectTwo(Road,where,dest);
                int c=0;
                for (int cc:best.getCount())
                    c+=cc;
                System.out.println("Best Count:"+c);
                System.out.println("Best Length:"+best.getLength());
                Polyline line = map.addPolyline(new PolylineOptions().addAll(best.getLine()));
                line.setWidth(15);
                line.setColor(colors[1]);
                optimization=best;
            }else
            {
                for(int i=0;i<6;i++)
                {
                    ArrayList<Road> a1=Road.get(i);
                    ArrayList<Road> a2=Road.get(i+6);
                    ArrayList<Road> a3=new ArrayList<>();

                    for(Road a: a1)
                        a3.add(a);
                    for(Road a: a2)
                        a3.add(a);

                    six.add(a3);
                }
                for(int j=0;j<six.size();j++)
                {
                    for(Road s: six.get(j))
                    {
                        Polyline line = map.addPolyline(new PolylineOptions().addAll(s.getLine()));
                        line.setWidth(15);
                        line.setColor(colors[j]);
                    }
                }
            }
            Road=new ArrayList<>();
            finish=true;
        }
        return finish;
    }

    public static void modifyRoute(ArrayList<Road> r)
    {
        Road.add(r);
        if(Road.size() == 12)
        {
            Road r1 = connectTwo(Road,where,dest);
            if(connect != null)
            {
                int index=optimization.getLine().indexOf(connect.getLine().get(connect.getLine().size()-1)),length=0;
                ArrayList <Integer> count=new ArrayList<>();
                for (int c:connect.getCount())
                    count.add(c);
                for (int c:optimization.getCount().subList(index,optimization.getCount().size()))
                    count.add(c);
                    //System.out.println("C/L:"+optimization.getCount().size()+"/"+optimization.getLine().size());

                ArrayList <LatLng> l=new ArrayList<>();
                length=connect.getLength();
                for(int i=index;i<optimization.getLine().size()-1;i++)
                    length+=optimization.getLength()/optimization.getLine().size();
                for (LatLng L:connect.getLine())
                    l.add(L);
                for (LatLng L:optimization.getLine().subList(index+1,optimization.getLine().size()))
                    l.add(L);
                Road old=new Road(l,length,count);
                r1=compareRoad(r1,old);

                map.clear();
                Polyline line = map.addPolyline(new PolylineOptions().addAll(r1.getLine()));
                line.setWidth(15);
                line.setColor(Color.BLUE);
                map.addMarker(new MarkerOptions().position(r1.getLine().get(r1.getLine().size()-1)).icon(BitmapDescriptorFactory.defaultMarker(180)));
                markerGenerator(r1.getLine().get(0),r1.getLine().get(r1.getLine().size()-1));
                optimization=r1;
            }else
            {
                System.out.println("Connect is null.");
            }
            Road=new ArrayList<>();//clear
        }
    }
    public static void setConnect(ArrayList<Road> r)
    {
        connect=r.get(0);
    }
    public static Road getOpti()
    {
        return optimization;
    }


    private static ArrayList<LatLng> interPoints(ArrayList<LatLng> road)
    {
        ArrayList<LatLng> points=new ArrayList<>();
        for(int i=0;i<road.size();i++)
        {
            for(int j=0;j<road.size();j++)
            {
                if(i == j)
                    continue;
                else
                {
                    if(road.get(i).latitude == road.get(j).latitude && road.get(i).longitude == road.get(j).longitude)
                        if(!points.contains(new LatLng(road.get(i).latitude,road.get(i).longitude)))
                            points.add(new LatLng(road.get(i).latitude,road.get(i).longitude));
                }
            }
        }
        return points;
    }
    private static Road findtheBestRoad(LatLng s,LatLng d,ArrayList<ArrayList<Road>> subRoad)
    {
        Road best;
        ArrayList<Road> start=new ArrayList<>();
        ArrayList<Road> end=new ArrayList<>();
        for (int i=0;i<subRoad.size();i++)
        {
            ArrayList<Road> eachRoad = subRoad.get(i);
            for (int j = 0; j < eachRoad.size(); j++)
            {
                Road eachSubRoad = eachRoad.get(j);
                LatLng head = eachSubRoad.getLine().get(0);
                LatLng tail= eachSubRoad.getLine().get( eachSubRoad.getLine().size()-1);
                if (head.latitude == s.latitude && head.longitude == s.longitude)
                    start.add(eachSubRoad);
                else if(Math.sqrt(Math.pow(tail.longitude - d.longitude, 2) + Math.pow(tail.latitude - d.latitude, 2)) < 0.0005
                /*tail.latitude == d.latitude && tail.longitude == d.longitude*/)
                    end.add(eachSubRoad);
            }
        }
        Road rs[]=new Road[start.size()];
        for (int i=0;i<start.size();i++)
            rs[i]=start.get(i);

        Road rd[]=new Road[end.size()];
        for (int i=0;i<end.size();i++)
            rd[i]=end.get(i);

        Road upper=null;
        Road bottom=null;
        for(int i=0;i<rs.length-1;i++)
            upper=compareRoad(rs[i],rs[i+1]);
        for(int i=0;i<rd.length-1;i++)
            bottom=compareRoad(rd[i],rd[i+1]);
        ArrayList<LatLng> L=new ArrayList<>();
        for (LatLng l:upper.getLine())
            L.add(l);
        for (LatLng l:bottom.getLine())
            L.add(l);
        int count=0;
        for (int c:upper.getCount())
            count+=c;
        for (int c:bottom.getCount())
            count+=c;
        ArrayList<Integer> c=new ArrayList<>();
        c.add(count);
        best=new Road(L,upper.getLength()+bottom.getLength(),c);
        return best;
    }
    private static Road[] getAllRoad(LatLng sour,LatLng dist,ArrayList<ArrayList<Road>> subRoad)
    {
        Road roads[],r[];
        int number=1,index=1;
        for (ArrayList<Road> rr : subRoad)
            for (Road rrr : rr)
                number++;
        r=new Road[number];
        for (ArrayList<Road> rr : subRoad)
            for (Road rrr : rr)
            {
                r[index]=rrr;
                index++;
            }

        ArrayList<Road> one;
        ArrayList< ArrayList<Road> > all=new ArrayList<>();
        ArrayList<Road> s=new ArrayList<>();
        ArrayList<Road> e=new ArrayList<>();
        for(int i=1;i<r.length;i++)//walk from sour
        {
            if(r[i].getLine().get(0).latitude ==sour.latitude && r[i].getLine().get(0).longitude == sour.longitude)
            {
                s.add(r[i]);
            }else
            {
                e.add(r[i]);
            }
        }

        for(int i=0;i<s.size();i++)
        {
            one=new ArrayList<>();
            Road start=s.get(i);
            for(int j=0;j<e.size();j++)
            {
                for (LatLng le :e.get(j).getLine())
                {
                    int len=0;
                    ArrayList<Integer> count=new ArrayList<>();
                    ArrayList<LatLng> lat=new ArrayList<>();
                    LatLng end=start.getLine().get(start.getLine().size()-1);
                    if(end.latitude == le.latitude && end.longitude == le.longitude)
                    //if(Math.sqrt(Math.pow(end.latitude - le.latitude, 2) + Math.pow(end.longitude- le.longitude, 2)) <= 0.0003)
                    {
                        for (int k=0;k<start.getCount().size();k++)
                            count.add(start.getCount().get(k));
                        for (int k=e.get(j).getLine().indexOf(le);k<e.get(j).getLine().size();k++)
                            count.add(e.get(j).getCount().get(k));
                        for (int k=0;k<start.getCount().size()-1;k++)
                            len+=start.getLength()/start.getLine().size();
                        for (int k=e.get(j).getLine().indexOf(le);k<e.get(j).getLine().size()-1;k++)
                            len+=e.get(j).getLength()/e.get(j).getLine().size();
                        for (LatLng l:start.getLine().subList(0,start.getLine().size()))
                            lat.add(l);
                        for (LatLng l:e.get(j).getLine().subList(e.get(j).getLine().indexOf(le),e.get(j).getLine().size()))
                            lat.add(l);
                        start=new Road(lat,len,count);
                        one.add(start);
                        break;
                    }
                }
            }
            if(one.size() > 0)
                all.add(one);
        }
        //all=new ArrayList<>();//walk back from dist
        s=new ArrayList<>();
        e=new ArrayList<>();
        for(int i=1;i<r.length;i++)
        {
            //if(r[i].getLine().get(r[i].getLine().size()-1).latitude ==last.latitude && r[i].getLine().get(r[i].getLine().size()-1).longitude == last.longitude)
            if(Math.sqrt(Math.pow(r[i].getLine().get(r[i].getLine().size()-1).latitude - dist.latitude, 2) + Math.pow(r[i].getLine().get(r[i].getLine().size()-1).longitude- dist.longitude, 2)) <= 0.0003)
            {
                e.add(r[i]);
            }else
            {
                s.add(r[i]);
            }
        }

        for(int i=0;i<e.size();i++)
        {
            one=new ArrayList<>();
            Road start=e.get(i);
            for(int j=0;j<s.size();j++)
            {
                for ( int l=s.get(j).getLine().size()-1;l>0;l--)
                {
                    LatLng ls=s.get(j).getLine().get(l);
                    int len=0;
                    ArrayList<Integer> count=new ArrayList<>();
                    ArrayList<LatLng> lat=new ArrayList<>();
                    LatLng end=start.getLine().get(0);
                    if(end.latitude == ls.latitude && end.longitude == ls.longitude)
                    {
                        for (int k=0;k<s.get(j).getLine().indexOf(ls);k++)
                            count.add(s.get(j).getCount().get(k));
                        for (int k=0;k<start.getCount().size();k++)
                            count.add(start.getCount().get(k));

                        for (int k=0;k<s.get(j).getLine().indexOf(ls)-1;k++)
                            len+=s.get(j).getLength()/s.get(j).getLine().size();
                        for (int k=0;k<start.getCount().size()-1;k++)
                            len+=start.getLength()/start.getLine().size();

                        for (LatLng latlng:s.get(j).getLine().subList(0,s.get(j).getLine().indexOf(ls)+1))
                            lat.add(latlng);
                        for (LatLng latlng:start.getLine().subList(0,start.getLine().size()))
                            lat.add(latlng);

                        start=new Road(lat,len,count);
                        one.add(start);
                        //System.out.println("connect");
                        break;
                    }
                }
            }
            if(one.size() > 0)
                all.add(one);
        }
        number=0;
        for (ArrayList<Road> rr : all)
            number++;
        roads=new Road[number];
        ArrayList<LatLng> latLng;
        ArrayList<Integer> count;
        int c=0,L=0;
        for (int i=0;i<all.size();i++)
        {
            latLng=new ArrayList<>();
            count=new ArrayList<>();
            c=0;
            L=0;
            for (Road rr : all.get(i))
            {
                for (LatLng l:rr.getLine())
                    latLng.add(l);

                    L+=rr.getLength();
                for (int co:rr.getCount())
                    c+=co;
            }
            count.add(c);
            roads[i]=new Road(latLng,L,count);
        }
        return roads;
    }
    private static ArrayList<ArrayList<Road>> getSubRoad( ArrayList<ArrayList<Road>> roads,ArrayList<LatLng> points)
    {
        ArrayList<ArrayList<Road>> subRoads = new ArrayList<>();
        for (int i = 0; i < roads.size(); i++)
        {
            ArrayList<Road> Roads = roads.get(i);
            ArrayList<Road> subRoad;
            for (int j = 0; j < Roads.size(); j++) //each road
            {
                subRoad = new ArrayList<>();
                Road eachRoad = Roads.get(j);
                ArrayList<Integer> indexes = new ArrayList<>();
                for (int k = 0; k < eachRoad.getLine().size(); k++) //each lat
                {
                    for (int L=0;L<points.size();L++) //each inter points
                    {
                        if (eachRoad.getLine().get(k).latitude == points.get(L).latitude && eachRoad.getLine().get(k).longitude == points.get(L).longitude)
                        {
                            indexes.add(k);
                            break;
                        }
                    }
                }

                int begin=0;
                Collections.sort(indexes);

                for(int k=0;k<indexes.size()-1;k++)
                {
                    ArrayList<Integer> c = new ArrayList<>();
                    ArrayList<LatLng> tmp = new ArrayList<>();
                    int Len ;
                    Road road=null;
                    if(indexes.get(k+1) - indexes.get(k) > 1)
                    {
                        for ( Integer count : eachRoad.getCount().subList(begin,indexes.get(k)+1) )
                            c.add(count);
                        Len=(eachRoad.getLength()/eachRoad.getLine().size()*eachRoad.getCount().subList(begin,indexes.get(k)).size() );//average dis
                        for (LatLng Lat : eachRoad.getLine().subList(begin,indexes.get(k)+1) )
                            tmp.add(Lat);
                        begin=indexes.get(k+1);
                        road=new Road(tmp,Len,c);
                    }else if(indexes.indexOf(begin) == indexes.size()-1)
                    {
                        for ( Integer count : eachRoad.getCount().subList(begin,begin+1) )
                            c.add(count);
                        Len=eachRoad.getLength()/eachRoad.getLine().size();//average dis
                        for (LatLng Lat : eachRoad.getLine().subList(begin,begin+1) )
                            tmp.add(Lat);
                        road=new Road(tmp,Len,c);
                    }else if(indexes.get(k+1) == indexes.indexOf(indexes.size()-1) && begin != indexes.size()-1)//整段都沒斷
                    {
                        for ( Integer count : eachRoad.getCount().subList(begin,indexes.size()) )
                            c.add(count);
                        Len=(eachRoad.getLength()/eachRoad.getLine().size() )*eachRoad.getCount().subList(begin,indexes.get(k)).size();//average dis
                        for (LatLng Lat : eachRoad.getLine().subList(begin,indexes.size()-1) )
                            tmp.add(Lat);
                        road=new Road(tmp,Len,c);
                    }
                    if(road != null)
                    {
                        subRoad.add(road);
                    }
                }
                subRoads.add(subRoad);
            }
        }
        return subRoads;
    }
    private static Road connectTwo(ArrayList<ArrayList<Road>> roads,LatLng s,LatLng d)
    {
        Road combine[];
        int number=0;
        for (int i=0;i<roads.size();i++)
        {
            for (int j = 0; j < roads.get(i).size(); j++)
                number++;
        }
        combine=new Road[number];
        int index=0;
        for (ArrayList<Road> r : roads )
            for (Road rr : r)
            {
                combine[index]=rr;
                index++;
            }
        ArrayList<Road> road=new ArrayList<>();
        for (int i=0;i<combine.length;i++)
        {
            for (int j = 0; j < combine.length; j++)
            {
                if(i!=j)
                {
                    LatLng start=combine[i].getLine().get(combine[i].getLine().size()-1);
                    LatLng end=combine[j].getLine().get(0);
                    if(start.latitude == end.latitude && start.longitude == end.longitude)
                    {
                        ArrayList<LatLng> lat=new ArrayList<>();
                        ArrayList<Integer> count=new ArrayList<>();
                        for (LatLng l:combine[i].getLine())
                            lat.add(l);
                        for (LatLng l:combine[j].getLine())
                            lat.add(l);
                        for (int c:combine[i].getCount())
                            count.add(c);
                        for (int c:combine[j].getCount())
                            count.add(c);
                        int l=combine[i].getLength()+combine[j].getLength();
                        road.add(new Road(lat,l,count));
                    }
                }
            }
        }
        Road best=road.get(0);
        for (int i=1;i<road.size();i++)
        {
           best=compareRoad(best,road.get(i));
        }
        System.out.println("Road size:"+road.size());
        return best;
    }
}