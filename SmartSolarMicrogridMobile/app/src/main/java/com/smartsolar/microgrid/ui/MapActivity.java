package com.smartsolar.microgrid.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartsolar.microgrid.R;
import com.smartsolar.microgrid.api.ApiClient;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private JsonArray nodes = new JsonArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Attach map fragment
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        loadNodes();
    }

    private void loadNodes() {
        showLoading();
        ApiClient.get().nodes(true).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    nodes = r.body().getAsJsonArray();
                    if (map != null) plotMarkers();
                } else {
                    toast(errorMsg(r));
                }
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        if (nodes.size() > 0) plotMarkers();
    }

    private void plotMarkers() {
        map.clear();
        LatLng first = null;

        for (JsonElement el : nodes) {
            JsonObject n = el.getAsJsonObject();
            double lat = ApiUtils.num(n, "latitude");
            double lng = ApiUtils.num(n, "longitude");
            LatLng pos = new LatLng(lat, lng);
            if (first == null) first = pos;

            Marker m = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(ApiUtils.str(n, "nodeName"))
                    .snippet(ApiUtils.str(n, "nodeCode") + " · "
                            + ApiUtils.num(n, "capacityKw") + " kW")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (m != null) m.setTag(n);
        }

        if (first != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 12f));
        }

        map.setOnMarkerClickListener(marker -> {
            JsonObject n = (JsonObject) marker.getTag();
            if (n != null) {
                new AlertDialog.Builder(MapActivity.this)
                        .setTitle("🌿 " + ApiUtils.str(n, "nodeName"))
                        .setMessage("📍 Code: " + ApiUtils.str(n, "nodeCode")
                                + "\n⚡ Capacity: " + ApiUtils.num(n, "capacityKw") + " kW"
                                + "\n🔋 Battery slots: " + ApiUtils.str(n, "batterySlotAvailability")
                                + "\n🔖 Status: " + ApiUtils.str(n, "status"))
                        .setPositiveButton("Close", null)
                        .show();
            }
            return false;
        });
    }
}
