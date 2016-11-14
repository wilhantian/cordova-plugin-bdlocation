package org.apache.cordova.bdlocation;

import android.content.Context;
import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.location.LocationClient;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClientOption;

/**
 * This class echoes a string called from JavaScript.
 */
public class BDLocation extends CordovaPlugin implements BDLocationListener {
    private String TAG = "BDLocationPlugin";

    // private static int BD_PERMISSION_TYPE = 999;
    private String [] permissions = { 
        Manifest.permission.READ_PHONE_STATE, 
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION, 
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE 
    };

    private LocationClient mLocationClient = null;
    private CallbackContext mWatchCallback = null;
    private JSONArray mWatchArgs = null;
    private String mWatchAction = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView){
        Context context = this.cordova.getActivity().getApplicationContext();
        mLocationClient = new LocationClient(context);
        mLocationClient.registerLocationListener(this);
        System.err.println("init 定位插件");
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("watch")){
            this.watch(action, args, callbackContext);
            return true;
        }
        if (action.equals("stop")){
            this.stop(action, args, callbackContext);
            return true;
        }
        return false;
    }

    /**
     * 请求位置
     * args:
        0: mode [string](默认为高精度. "Battery_Saving":低功耗模式, "Device_Sensors":仅设备Gps模式, "Hight_Accuracy":高精度模式)
        1: coor [string](默认为bd09ll. 仅支持Android, "gcj02":返回国测局经纬度坐标系, "bd09":返回百度墨卡托坐标系, "bd09ll":返回百度经纬度坐标系)
        2: span [int](默认为0. 单位毫秒 0时仅定位一次. 当<1000(1s)时，定时定位无效)
        3: gps [boolean](默认为false. 是否使用GPS)
        4: ignoreKillProcess [boolean](默认为true. 设置是否在stop的时候杀死SERVICE进程)
        5: enableSimulateGps [boolean](默认为false. 设置是否允许模拟GPS true:允许； false:不允许)
     */ 
    private void watch(String action, JSONArray args, CallbackContext callbackContext) throws JSONException{
        mWatchCallback = callbackContext;//保存回调结果
        mWatchArgs = args;
        mWatchAction = action;

        if(!hasPermisssion()){
            this.cordova.requestPermissions(this, 0, permissions);
            return;
        }
        
        String mode = args.getString(0);
        String coor = args.getString(1);
        int span = args.getInt(2);
        boolean gps = args.getBoolean(3);
        boolean ignoreKillProcess = args.getBoolean(4);
        boolean enableSimulateGps = args.getBoolean(5);

        LocationClientOption option = new LocationClientOption();
        if(mode.equals("Battery_Saving")) option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        if(mode.equals("Device_Sensors")) option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        else option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType(coor);//可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setOpenGps(gps);//可选，默认false,设置是否使用gps
        option.setIgnoreKillProcess(ignoreKillProcess);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.setEnableSimulateGps(enableSimulateGps);//可选，默认为false 设置是否允许模拟GPS true:允许； false:不允许，
        option.setIsNeedAltitude(true);//GPS定位时需要高度结果
        mLocationClient.setLocOption(option);
        mLocationClient.start();

        // 保证持久回调
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void stop(String action, JSONArray args, CallbackContext callbackContext) throws JSONException{
        mLocationClient.stop();

        mWatchCallback = null;
        mWatchArgs = null;
        mWatchAction = null;
        
        callbackContext.success();
    }

    /**
     * 收到请求的回调
     * 百度BDLocation跟此类同名了 只能用全包名了
     * 
     {
         code: int, //定位结果 locType
         msg: string, //定位相关描述信息 locTypeDescription
         data:{ //data可能为空
             time: string, //server返回的当前定位时间
             latitude: double, // 纬度坐标 默认值Double.MIN_VALUE
             longitude: double,// 经度坐标 默认值Double.MIN_VALUE
             altitude: double, // 高度信息 仅在GPS模式下有效
             radius: float, // 精度:米
         }
     }
     */
    @Override
    public void onReceiveLocation(com.baidu.location.BDLocation location){
        if(mWatchCallback == null) return;

        try {
            JSONObject json = new JSONObject();
            json.put("code", location.getLocType());
            json.put("msg", location.getLocTypeDescription());

            // GPS定位结果
            if(location.getLocType() == com.baidu.location.BDLocation.TypeGpsLocation
                || location.getLocType() == com.baidu.location.BDLocation.TypeNetWorkLocation
                || location.getLocType() == com.baidu.location.BDLocation.TypeOffLineLocation
            ){
                JSONObject data = new JSONObject();
                data.put("time", location.getTime());
                data.put("latitude", location.getLatitude());
                data.put("longitude", location.getLongitude());
                data.put("altitude", location.getAltitude());
                data.put("radius", location.getRadius());
                json.put("data", data);

                PluginResult result = new PluginResult(PluginResult.Status.OK, json);
                result.setKeepCallback(true);//保持回调
                mWatchCallback.sendPluginResult(result);
                return;
            }
            mWatchCallback.error(json);
        }catch (JSONException e){
            LOG.e(TAG, e.toString());
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException
    {
        if(mWatchCallback != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    LOG.e(TAG, "相关权限被拒绝!");
                    PluginResult result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    mWatchCallback.sendPluginResult(result);
                    return;
                }
            }
            this.watch(mWatchAction, mWatchArgs, mWatchCallback);
        }
    }

    @Override
    public boolean hasPermisssion() {
        for(String p : permissions)
        {
            if(!this.cordova.hasPermission(p))
                return false;
        }
        return true;
    }

    @Override
    public void requestPermissions(int requestCode){
        this.cordova.requestPermissions(this, requestCode, permissions);
    }
}
