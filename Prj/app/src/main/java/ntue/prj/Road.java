package ntue.prj;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by user1607281 on 2017/5/29.
 */

public class Road {
    private ArrayList<LatLng> line;
    private int length;
    private ArrayList<Integer> count;
    Road(ArrayList<LatLng> line,int length,ArrayList<Integer> count){
        this.line=line;
        this.length=length;
        this.count=count;
    }
    public ArrayList<LatLng> getLine(){
        return line;
    }
    public int getLength(){
        return length;
    }
    public ArrayList<Integer> getCount(){
        return count;
    }
}