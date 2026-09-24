package com.smartsolar.microgrid.ui;
import android.app.AlertDialog;
import android.os.Bundle;import android.widget.*;import com.google.android.gms.maps.*;import com.google.android.gms.maps.model.*;import com.google.gson.*;import com.smartsolar.microgrid.api.*;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.*;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {
 GoogleMap map; JsonArray nodes=new JsonArray();
 @Override protected void onCreate(Bundle b){super.onCreate(b);setup("Nearby Grid Nodes");FrameLayout frame=new FrameLayout(this);frame.setId(1001);root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));SupportMapFragment f=SupportMapFragment.newInstance();f.getMapAsync(this);getSupportFragmentManager().beginTransaction().replace(1001,f).commit();load();}
 void load(){ApiClient.get().nodes(true).enqueue(new Callback<JsonElement>(){public void onResponse(Call<JsonElement> c,Response<JsonElement> r){if(r.isSuccessful()&&r.body()!=null){nodes=r.body().getAsJsonArray();if(map!=null)plot();}else toast(ApiUtils.error(r));}public void onFailure(Call<JsonElement> c,Throwable t){fail(t);}});}
 public void onMapReady(GoogleMap g){map=g;map.getUiSettings().setZoomControlsEnabled(true);if(nodes.size()>0)plot();}
 void plot(){map.clear();LatLng first=null;for(JsonElement e:nodes){JsonObject n=e.getAsJsonObject();double lat=ApiUtils.num(n,"latitude"),lng=ApiUtils.num(n,"longitude");LatLng p=new LatLng(lat,lng);if(first==null)first=p;Marker m=map.addMarker(new MarkerOptions().position(p).title(ApiUtils.str(n,"nodeName")).snippet(ApiUtils.str(n,"nodeCode")+" • "+ApiUtils.num(n,"capacityKw")+" kW • Battery slots: "+ApiUtils.str(n,"batterySlotAvailability")));if(m!=null)m.setTag(n);}if(first!=null)map.moveCamera(CameraUpdateFactory.newLatLngZoom(first,12));map.setOnMarkerClickListener(m->{JsonObject n=(JsonObject)m.getTag();if(n!=null)new AlertDialog.Builder(this).setTitle(ApiUtils.str(n,"nodeName")).setMessage("Code: "+ApiUtils.str(n,"nodeCode")+"\nCapacity: "+ApiUtils.num(n,"capacityKw")+" kW\nBattery slots: "+ApiUtils.str(n,"batterySlotAvailability")+"\nStatus: "+ApiUtils.str(n,"status")).setPositiveButton("OK",null).show();return false;});}
}
