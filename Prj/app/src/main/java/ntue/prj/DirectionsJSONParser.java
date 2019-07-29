package ntue.prj;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by user1607281 on 2017/5/29.
 */

public class DirectionsJSONParser {
    private List<List<HashMap<String, String>>> route = new ArrayList<>();
    private List<HashMap<String, String>> path;
    private List<LatLng> point;
    private HashMap<String, String> laANDlo;
    private JSONArray jsonRoute, jsonLeg, jsonStep;
    private String polyline = "";

    private ArrayList<Integer> distance=new ArrayList<>();

    public ArrayList<Integer> distances()
    {
        return distance;
    }

    public List<List<HashMap<String, String>>> parseJson(JSONObject object) {
        try {
            jsonRoute = object.getJSONArray("routes");
            for (int i = 0; i < jsonRoute.length(); i++)
            {
                jsonLeg = ((JSONObject) (jsonRoute.get(i))).getJSONArray("legs");
                for (int j = 0; j < jsonLeg.length(); j++)
                {
                    distance.add(Integer.parseInt( ( (JSONObject)((JSONObject) (jsonLeg.get(j))).get("distance") ).get("value").toString()) );
                    jsonStep = ((JSONObject) (jsonLeg.get(j))).getJSONArray("steps");
                    path = new ArrayList<>();
                    for (int k = 0; k < jsonStep.length(); k++)
                    {
                        polyline = (String) (((JSONObject) ((JSONObject) (jsonStep.get(k))).get("polyline")).get("points"));
                        point = polylinePoint(polyline);
                        for (int l = 0; l < point.size(); l++)
                        {
                            laANDlo = new HashMap<>();
                            laANDlo.put("lat", Double.toString(((LatLng) point.get(l)).latitude));
                            laANDlo.put("lon", Double.toString(((LatLng) point.get(l)).longitude));
                            path.add(laANDlo);
                        }
                    }
                    route.add(path);
                }
            }
        } catch (JSONException e) {
            System.out.println("DirectionsJSONParser:" + e);
        }
        return route;
    }

    //decomposition polyline
    private List<LatLng> polylinePoint(String polyline) {
        List<LatLng> point = new ArrayList<>();
        int index = 0, len = polyline.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = polyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = polyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            point.add(p);
        }
        return point;
    }
}
