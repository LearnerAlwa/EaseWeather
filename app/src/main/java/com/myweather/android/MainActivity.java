package com.myweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.myweather.android.util.HttpUtil;
import com.myweather.android.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;

import static org.litepal.LitePalApplication.getContext;


public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;
    private Button startLBS;//用于点击进行点位信息输出
    private TextView CityID_show;//向服务器请求回来的城市ID展示
    public double LongitudeId;//经度
    public double LatitudeId;//纬度
    public String CityID;//根据经纬度得出的城市ID
    private Button button;
    private Button fresh;
    private ImageView imageView;
    private TextView tq_city;
    private TextView tq_update_time;
    private TextView tq_tmp;
    private TextView tq_cond;
    private TextView tq_wind;
    private TextView tq_tfanwei;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    String weatherId=null;
    String tianqi = null,wendu = null;
    String cityname=null,update=null;
    String windrange=null,tmprange=null;
    String code=null;
    private SharedPreferences sharedPreferences;

    public static final int UPDATE_TEXT=1;
    private Handler handler=new Handler(){
        public void  handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_TEXT:
                    tq_city.setText(cityname);
                    tq_update_time.setText(update);
                    tq_tmp.setText(wendu);
                    tq_cond.setText(tianqi);
                    break;
                default:
                    break;
            }
        }
    };
    private Handler handler1=new Handler(){
        public void  handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_TEXT:
                    tq_wind.setText(windrange);
                    tq_tfanwei.setText(tmprange);
                    adapter.notifyDataSetChanged();
                    int imgID = getResources().getIdentifier(code, "mipmap",
                            "com.myweather.android");
                    System.out.println(imgID);
                    if (imgID != 0) {
                        Drawable drawable = getResources().getDrawable(imgID);
                        imageView.setImageDrawable(drawable);
                    }else {
                        imageView.setImageResource(R.mipmap.p100);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private Handler handler2 = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            this.update();
            handler2.postDelayed(this, 1000 * 120);// 间隔120秒
        }
        void update() {
            //刷新msg的内容
            getnowinfo(weatherId);
            getforeinfo(weatherId);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //        定位监听器，一旦调用requestLocation()函数，就会触发MyLocationListener()
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        setContentView(R.layout.activity_main);
        startLBS=findViewById(R.id.my_local);
        CityID_show=findViewById(R.id.loc_txt);

        button=findViewById(R.id.location_button);
        fresh=findViewById(R.id.refresh);
        imageView=findViewById(R.id.wea_cond_img);
        tq_city=findViewById(R.id.city_txt);
        tq_update_time=findViewById(R.id.update_time_txt);
        tq_tmp=findViewById(R.id.temperature_txt) ;
        tq_cond=findViewById(R.id.cond_txt) ;
        tq_wind=findViewById(R.id.wind_txt);
        tq_tfanwei=findViewById(R.id.temperature_range_txt);
        listView = findViewById(R.id.list_view_w);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        sharedPreferences=getSharedPreferences("data",MODE_PRIVATE);
        boolean s = sharedPreferences.getBoolean("plus", false);
        if(s==false)
        {
            sharedPreferences.edit().putString("code","CN101010100").apply();
            sharedPreferences.edit().putBoolean("plus",true).apply();
        }
        weatherId=getInitialCity();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//监听按钮
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent=new Intent("android.intent.action.ACTION_START");
                        startActivityForResult(intent,1);
                    }
                }).start();
            }
        });
        tq_city.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent=new Intent("android.intent.action.ACTION_START");
                        startActivityForResult(intent,2);
                    }
                }).start();
            }
        });
        handler2.postDelayed(runnable, 1000 * 60);
        //        点击定位按钮监听器
        startLBS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //所有权限都开启之后，进行定位
                requestLocation();
                CityID_show.setText(CityID);
            }
        });
        fresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//监听按钮
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getnowinfo(weatherId);
                        getforeinfo(weatherId);
                    }
                }).start();
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        getnowinfo(weatherId);
        getforeinfo(weatherId);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    weatherId = data.getStringExtra("extra_return");
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    weatherId = data.getStringExtra("extra_return");
                    sharedPreferences.edit().putString("code",weatherId).apply();
                }
            default:
        }
    }

    public static void closeStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll().penaltyLog().build());

    }
    private void getnowinfo(final String weaid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil httpUtil=new HttpUtil();
                String response=httpUtil.sendHttpRequest("https://free-api.heweather.net/s6/weather/now?location="+weaid+"&key=b3294a97512a4955adfda76eac0d2132");
                showNow(response);
            }
        }).start();

    }
    private void getforeinfo(final String weaid) {
        closeStrictMode();
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil httpUtil=new HttpUtil();
                String requestfore=httpUtil.sendHttpRequest("https://free-api.heweather.net/s6/weather/forecast?location="+weaid+"&key=b3294a97512a4955adfda76eac0d2132");
                showForecast(requestfore);
            }
        }).start();
    }
    private void showNow(String s){

        try {
            JSONObject jsonObject=new JSONObject(s);
            String s1=jsonObject.getString("HeWeather6");
            JSONArray jsonArray=new JSONArray(s1);
            JSONObject jsonObject0=jsonArray.getJSONObject(0);
            String hebasic=null,heupdate=null,henow=null;
            hebasic=jsonObject0.getString("basic");
            heupdate=jsonObject0.getString("update");
            henow=jsonObject0.getString("now");
            JSONObject jsonObject1=new JSONObject(hebasic);
            JSONObject jsonObject2=new JSONObject(heupdate);
            JSONObject jsonObject3=new JSONObject(henow);
            cityname=jsonObject1.getString("location");//城市名
            update=jsonObject2.getString("loc");//更新时间
            wendu=jsonObject3.getString("tmp");//温度
            tianqi=jsonObject3.getString("cond_txt");//天气状况
            code="p"+jsonObject3.getString("cond_code");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("AAA", "error");
        }

        Message message=new Message();
        message.what=UPDATE_TEXT;
        handler.sendMessage(message);

    }
    private void showForecast(String s){

        try {
            JSONObject jsonObject=new JSONObject(s);
            String s1=jsonObject.getString("HeWeather6");
            JSONArray jsonArray=new JSONArray(s1);
            JSONObject jsonObject0=jsonArray.getJSONObject(0);
            String s2=jsonObject0.getString("daily_forecast");
            JSONArray jsonArray0=new JSONArray(s2);
            JSONObject jsonObject1=jsonArray0.getJSONObject(0);
            windrange=jsonObject1.getString("wind_dir")+jsonObject1.getString("wind_sc")+"级";
            tmprange=jsonObject1.getString("tmp_min")+"℃~"+jsonObject1.getString("tmp_max")+"℃";
            JSONObject jsonObject2=jsonArray0.getJSONObject(1);
            dataList.clear();
            dataList.add(jsonObject2.getString("date")+"                    "+jsonObject2.getString("cond_txt_n")+"                   "+jsonObject2.getString("tmp_max")+"℃/"+jsonObject2.getString("tmp_min")+"℃");
            JSONObject jsonObject3=jsonArray0.getJSONObject(2);
            dataList.add(jsonObject3.getString("date")+"                    "+jsonObject3.getString("cond_txt_n")+"                 "+jsonObject3.getString("tmp_max")+"℃/"+jsonObject3.getString("tmp_min")+"℃");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("AAA", "error");
        }

        Message message=new Message();
        message.what=UPDATE_TEXT;
        handler1.sendMessage(message);
    }
    private String getInitialCity(){
        String CityId=sharedPreferences.getString("code","");
        return CityId;
    }

    //    定位函数
    private void requestLocation()
    {
        initLocation();//定位初始化
        mLocationClient.start();//开始定位
    }
    //    定位初始化
    private void initLocation()
    {
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(5000);//每隔5s刷新一下
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);//开启GPS定位
        mLocationClient.setLocOption(option);
    }


    //    请求权限小窗口
    @Override
    public void onRequestPermissionsResult(int requestCode,String [] permissions, int [] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                if (grantResults.length>0)
                {
                    for (int result:grantResults)
                    {
                        if (result != PackageManager.PERMISSION_GRANTED)
                        {
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();//权限同意之后进行定位
                }else
                {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    //    定位结果信息赋值
    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation location)
        {
            LongitudeId=location.getLongitude();//经度获取
            LatitudeId=location.getLatitude();//纬度获取
            String cityUrl1="https://search.heweather.net/find?location="+LongitudeId+","+LatitudeId+"&key=8e669fb35db1436496ad76e9aec7ba60";
            Log.d("AAA", cityUrl1);
            requestCityInfo();//根据经纬度，请求服务器
        }

    }

    //    用经纬度向服务器请求获取城市json
    public void requestCityInfo()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil httpUtil=new HttpUtil();
                String response=httpUtil.sendHttpRequest("https://search.heweather.net/find?location="+LongitudeId+","+LatitudeId+"&key=8e669fb35db1436496ad76e9aec7ba60");
                String cityUrl="https://search.heweather.net/find?location="+LongitudeId+","+LatitudeId+"&key=8e669fb35db1436496ad76e9aec7ba60";
                Log.d("AAA", cityUrl);
                showLoc(response);
            }
        }).start();

    }
    private void showLoc(String s){

        try {
            JSONObject jsonObject=new JSONObject(s);
            String s1=jsonObject.getString("HeWeather6");
            JSONArray jsonArray=new JSONArray(s1);
            JSONObject jsonObject0=jsonArray.getJSONObject(0);
            String hebasic=null;
            hebasic=jsonObject0.getString("basic");
            JSONObject jsonObject1=new JSONObject(hebasic);
            CityID=jsonObject1.getString("location");//城市名
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("AAA", "error");

        }

    }

}


