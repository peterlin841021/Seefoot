package ntue.prj;

import android.graphics.Color;
import android.os.AsyncTask;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by user1607281 on 2017/5/29.
 */

public class ParseJsonTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>>
{
    private JSONObject object = null;
    private List<List<HashMap<String, String>>> route = null;
    private static ArrayList<Road> roads;
    private ArrayList<Integer> distances;
    private ArrayList<LatLng> intersetion;
    private int mode;
    ParseJsonTask(ArrayList<LatLng> it,int mode)
    {
        roads=new ArrayList<>();
        this.mode=mode;
        intersetion=it;
    }
    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... json)
    {
        try {
            object = new JSONObject(json[0]);
            DirectionsJSONParser parser = new DirectionsJSONParser();
            route = parser.parseJson(object);
            distances=parser.distances();
        } catch (JSONException e) {
            System.out.println("ParseJsonTask:" + e);
        }
        return route;
    }

    private ArrayList<Integer> getIntersectionCount(ArrayList<LatLng> roadSet,ArrayList<LatLng> path)
    {
        ArrayList<Integer> count=new ArrayList<>();
        LatLng middle;
        double distance,r;

        for (int i=0;i<path.size()-1;i++)//road
        {
            int c=0;
            middle=new LatLng((path.get(i).latitude+path.get(i+1).latitude)/2,(path.get(i).longitude+path.get(i+1).longitude)/2);
            distance=Math.sqrt(Math.pow((path.get(i).latitude-middle.latitude),2)+Math.pow((path.get(i).longitude-middle.longitude),2));
            for (int j=0;j<roadSet.size();j++)//intersection
            {
                r=Math.sqrt(Math.pow((middle.latitude-roadSet.get(j).latitude),2)+Math.pow((middle.longitude-roadSet.get(j).longitude),2));
                if (r <= distance)
                {
                    c++;
                }
                /*
                r=Math.sqrt(Math.pow((path.get(i).latitude-roadSet.get(j).latitude),2)+Math.pow((path.get(i).longitude-roadSet.get(j).longitude),2));
                if (r <=0.0003)
                {
                    c++;
                }*/
            }
            count.add(c);
        }
        return count;
    }

    //解析完json後...
    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> lists)
    {
        ArrayList<LatLng> points ;
        List<HashMap<String, String>> path;
        HashMap<String, String> point;
        double lat , lon ;
        LatLng position;

        ArrayList<LatLng> roadSet=intersetion;
        //System.out.println("START");
        for (int i = 0; i < lists.size(); i++)
        {
            points = new ArrayList<>();

            path = lists.get(i);
            for (int j = 0; j < path.size(); j++)
            {
                point = path.get(j);
                lat = Double.parseDouble(point.get("lat"));
                lon = Double.parseDouble(point.get("lon"));
                position = new LatLng(lat, lon);
                points.add(position);
            }
            Road r=new Road(points,distances.get(i),getIntersectionCount(roadSet,points));
            roads.add(r);
            //System.out.println(i+1);
        }
        //System.out.println("END");
        //mode 0 : optimization
        if(mode == 12)
            Map.drawLine(roads);
        else if(mode == 1)
            Map.singleLine(roads);
        else if(mode == 2)
            Map.modifyRoute(roads);
        else if(mode == 3)
            Map.setConnect(roads);
        roads=new ArrayList<>();
        super.onPostExecute(lists);
    }
}
