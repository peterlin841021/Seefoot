package ntue.prj;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import java.util.ArrayList;

public class Leading extends AppCompatActivity {

    private ViewPager mViewPager;
    private ArrayList viewList;
    private RadioGroup radiogp;
    private Button startbt,skipbt;
    private static final int PERMISSION_RESULT = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leading);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        radiogp=(RadioGroup)findViewById(R.id.radiogroup);

        final LayoutInflater mInflater = getLayoutInflater().from(this);

        View v1 = mInflater.inflate(R.layout.lead1,null);
        View v2 = mInflater.inflate(R.layout.lead2, null);
        View v3 = mInflater.inflate(R.layout.lead3, null);
        View v4 = mInflater.inflate(R.layout.lead4, null);

        startbt=(Button)findViewById(R.id.OK);
        startbt.setVisibility(View.INVISIBLE);
        skipbt=(Button)findViewById(R.id.skip);
        viewList = new ArrayList<View>();
        viewList.add(v1);
        viewList.add(v2);
        viewList.add(v3);
        viewList.add(v4);
        mViewPager.setAdapter(new MyViewPagerAdapter(viewList));
        mViewPager.setCurrentItem(0);
        requestForPermission();
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }
            @Override
            public void onPageSelected(int position) {
                switch (position){
                    case 0:
                        radiogp.check(R.id.page1);
                        startbt.setVisibility(View.INVISIBLE);
                        skipbt.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        radiogp.check(R.id.page2);
                        startbt.setVisibility(View.INVISIBLE);
                        skipbt.setVisibility(View.INVISIBLE);
                        break;
                    case 2:
                        radiogp.check(R.id.page3);
                        startbt.setVisibility(View.INVISIBLE);
                        skipbt.setVisibility(View.INVISIBLE);
                        break;
                    case 3:
                        radiogp.check(R.id.page4);
                        startbt.setVisibility(View.VISIBLE);
                        skipbt.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        startbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it=new Intent(Leading.this,Map.class);
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(it);
                finish();
            }
        });
        skipbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it=new Intent(Leading.this,Map.class);
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(it);
                finish();
            }
        });
    }
    private void requestForPermission()
    {
        //request for access
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                )
        {
        }else
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.INTERNET,Manifest.permission.ACCESS_NETWORK_STATE},
                    PERMISSION_RESULT);
        }
    }
}
