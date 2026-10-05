package com.smartsenergy.microgrid.prosumer;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.utils.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsActivity extends BaseActivity {

    private LinearLayout bookingsContainer;
    private EditText etSearch;
    private Spinner spStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        bookingsContainer = findViewById(R.id.bookingsContainer);
        etSearch = findViewById(R.id.etSearch);
        spStatus = findViewById(R.id.spStatus);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_dropdown_item, 
            new String[]{"All", "Pending", "Approved", "Completed", "Cancelled"});
        spStatus.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadBookings(); }
        });

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { loadBookings(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        
        setupBottomNav(R.id.nav_bookings);
        loadBookings();
    }

    private void loadBookings() {
        String query = etSearch.getText().toString();
        String status = spStatus.getSelectedItem().toString();
        if ("All".equals(status)) status = "";

        // showLoading(); // Silent load since it reloads on keystroke
        ApiClient.get().myReservations(session.nic(), query, status).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                // hideLoading();
                if (!r.isSuccessful() || r.body() == null) { return; }
                JsonArray a = r.body().getAsJsonArray();
                bookingsContainer.removeAllViews();
                
                if (a.size() == 0) {
                    TextView tv = new TextView(BookingsActivity.this);
                    tv.setText("No bookings found.");
                    tv.setTextColor(Color.GRAY);
                    bookingsContainer.addView(tv);
                    return;
                }

                for (int i = 0; i < a.size(); i++) {
                    addBookingCard(a.get(i).getAsJsonObject());
                }
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) { /* hideLoading(); */ }
        });
    }

    private void addBookingCard(JsonObject r) {
        String status = ApiUtils.str(r, "status");

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setCardElevation(3f);
        card.setRadius(16f);
        card.setUseCompatPadding(true);
        card.setOnClickListener(v -> showBookingDetails(r));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 32, 32, 32);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(ApiUtils.str(r, "reservationCode") + "  ·  " + status);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor("Pending".equals(status) ? Color.parseColor("#FFA000") : 
                             "Approved".equals(status) ? Color.parseColor("#4CAF50") : Color.GRAY);
        box.addView(tvTitle);

        TextView tvAmount = new TextView(this);
        tvAmount.setText(fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh");
        tvAmount.setTextSize(14f);
        tvAmount.setTextColor(Color.DKGRAY);
        tvAmount.setPadding(0, 8, 0, 0);
        box.addView(tvAmount);

        card.addView(box);
        bookingsContainer.addView(card);
    }
    
    private void showBookingDetails(JsonObject r) {
        String status = ApiUtils.str(r, "status");
                
        View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_booking_details, null);
        
        TextView tvCode = view.findViewById(R.id.tvCode);
        tvCode.setText(ApiUtils.str(r, "reservationCode"));
        
        TextView tvNode = view.findViewById(R.id.tvNode);
        tvNode.setText(ApiUtils.str(r, "nodeName"));
        
        TextView tvStatus = view.findViewById(R.id.tvStatus);
        tvStatus.setText(status);
        if ("Pending".equals(status)) {
            tvStatus.setTextColor(Color.parseColor("#FFA000"));
        } else if ("Approved".equals(status)) {
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if ("Cancelled".equals(status)) {
            tvStatus.setTextColor(Color.parseColor("#D32F2F"));
        }
        
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        tvAmount.setText(fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh");
        
        TextView tvPrice = view.findViewById(R.id.tvPrice);
        tvPrice.setText("Rs. " + fmt(ApiUtils.num(r, "totalPriceLkr")));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        com.google.android.material.button.MaterialButton btnAction = view.findViewById(R.id.btnAction);

        if ("Pending".equals(status)) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText("Cancel Booking");
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            btnAction.setOnClickListener(v -> {
                cancelBooking(ApiUtils.str(r, "id"));
                dialog.dismiss();
            });
        } else if ("Approved".equals(status)) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText("View QR");
            btnAction.setOnClickListener(v -> {
                generateQr(ApiUtils.str(r, "id"));
                dialog.dismiss();
            });
        }
    }
    
    private void cancelBooking(String id) {
        showLoading();
        ApiClient.get().cancelReservation(id, session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) { toast("Booking cancelled"); loadBookings(); }
                else toast(errorMsg(r));
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void generateQr(String resId) {
        showLoading();
        ApiClient.get().generateQr(resId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                JsonObject q = r.body();
                new MaterialAlertDialogBuilder(BookingsActivity.this)
                        .setTitle("Transaction QR Code")
                        .setMessage(ApiUtils.str(q, "qrData")) // Simple display for now
                        .setPositiveButton("OK", null)
                        .show();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private String fmt(double d) {
        if (d == (long) d) return String.format(java.util.Locale.US, "%d", (long) d);
        else return String.format(java.util.Locale.US, "%.1f", d);
    }
}
