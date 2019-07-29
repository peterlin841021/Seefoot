package ntue.prj;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by user1607281 on 2017/5/29.
 */

public class Intersection
{
    public ArrayList<LatLng> getRoadSet(SQLiteDatabase database, LatLng where, LatLng dest)
    {
        ArrayList<LatLng> set=new ArrayList<>();
        Cursor c = database.rawQuery("select * from location", null);
        LatLng middle=null;
        double radius=0;
        if (c.moveToFirst())
        {
            double d = 0.0, middleLa=0.0, middleLo=0.0;
            if (dest.latitude != 0 && dest.longitude != 0)
            {
                radius = Math.sqrt(Math.pow(where.longitude - dest.longitude, 2) + Math.pow(where.latitude - dest.latitude, 2));//diameter
                //Middle
                middleLa = (where.latitude + dest.latitude) / 2;
                middleLo = (where.longitude + dest.longitude) / 2;
                middle=new LatLng(middleLa,middleLo);
            } else {
                radius = 0.0005;
            }

            do {
                //半徑內路口
                if(middle == null)
                {
                    d = Math.sqrt(Math.pow(where.longitude - c.getDouble(c.getColumnIndex("longitude")), 2) + Math.pow(where.latitude - c.getDouble(c.getColumnIndex("latitude")), 2));
                }else{
                    d = Math.sqrt(Math.pow(middle.longitude - c.getDouble(c.getColumnIndex("longitude")), 2) + Math.pow(middle.latitude - c.getDouble(c.getColumnIndex("latitude")), 2));
                }
                if (d < radius)
                {
                    set.add(new LatLng(c.getDouble(c.getColumnIndex("latitude")), c.getDouble(c.getColumnIndex("longitude"))));
                }
            } while (c.moveToNext());
        }
        c.close();
        return set;
    }
}
