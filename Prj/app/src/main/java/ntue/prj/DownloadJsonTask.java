package ntue.prj;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by user1607281 on 2017/5/29.
 */

public class DownloadJsonTask extends AsyncTask<String, Void, String>
{
    private ArrayList<LatLng> intersetion;
    private int mode;
    DownloadJsonTask(ArrayList<LatLng> it,int mode)
    {
        intersetion=it;
        this.mode=mode;
    }
    @Override
    protected String doInBackground(String... url)
    {
        String data = "";
        try {
            data = downloadJSON(url[0]);
        } catch (IOException e)
        {
            System.out.println("DownloadJsonTask:" + e);
        }
        return data;
    }

    //讀完json後...
    @Override
    protected void onPostExecute(String s)
    {
        ParseJsonTask pjk = new ParseJsonTask(intersetion,mode);
        pjk.execute(s);
        super.onPostExecute(s);
    }
    //get json file
    private String downloadJSON(String jsonUrl) throws IOException
    {
        InputStream is = null;
        HttpURLConnection huc = null;
        String data = "", line;
        BufferedReader bf;
        StringBuffer sb;
        try {
            URL url = new URL(jsonUrl);
            huc = (HttpURLConnection) url.openConnection();
            huc.connect();
            is = huc.getInputStream();
            bf = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();
            while ((line = bf.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            bf.close();
        } catch (Exception e) {
            System.out.println("downloadJSON:" + e);
        } finally {
            is.close();
            huc.disconnect();
        }
        return data;
    }
}