package com.lookingbus.chaithra.demo;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.transit.realtime.GtfsRealtime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class VehicalInfo511 extends AsyncTask<Double,Void,ArrayList<LocationModel>> {

    public static String VEHICLE_MONITORING_API = "http://api.511.org/transit/VehiclePositions?api_key=807ac98d-433b-4f02-9e62-e9eb8993f987&agency=SC";//CT

    ArrayList<LocationModel> locarrlist = new ArrayList<>();

    public ArrayList<LocationModel> getVehicleOnTrip(Double[] locarr) {
        URL url = null;
        try {
            url = new URL(VEHICLE_MONITORING_API);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        GtfsRealtime.FeedMessage feed = null;
        try {
            feed = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(feed != null){
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (entity.hasVehicle()) {
                    Double la = Double.valueOf(entity.getVehicle().getPosition().getLatitude());
                    Double lo = Double.valueOf(entity.getVehicle().getPosition().getLongitude());
//                    if(locarrlist.size() < 10) {
                        locarrlist.add(new LocationModel(la, lo));
                        Log.e("VehicalInfo511", "" + entity.getVehicle().getPosition().getLatitude() + " " + entity.getVehicle().getPosition().getLongitude());
//                    }else{
//                        break;
//                    }
                }
            }
        }else{
            Log.e("VehicalInfo511","feed : "+feed);
        }
        return locarrlist;
    }

    @Override
    protected ArrayList<LocationModel> doInBackground(Double[] objects) {
        getVehicleOnTrip(objects);
        return getVehicleOnTrip(objects);
    }

    @Override
    protected void onPostExecute(ArrayList<LocationModel> locarrlist) {
        super.onPostExecute(locarrlist);
        for (LocationModel locarrobj:locarrlist) {
            MapsActivity.map.addMarker(new MarkerOptions()
                    .position(new LatLng(locarrobj.getLat(),locarrobj.getLog()))
                    .title(""+locarrobj.getLat()+" "+locarrobj.getLog())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus)));
        }
//        MapsActivity.map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locarrlist.get(0).getLat(),locarrlist.get(0).getLog()), 13));
    }
}
