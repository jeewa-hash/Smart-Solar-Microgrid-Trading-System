package com.smartsolar.microgrid.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.gson.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.smartsolar.microgrid.R;
import com.smartsolar.microgrid.api.ApiClient;
import com.smartsolar.microgrid.util.ApiUtils;
import androidx.activity.result.ActivityResultLauncher;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorActivity extends BaseActivity {

    private LinearLayout contentLayout;
    private TextView tvStatPending, tvStatApproved, tvStatToday;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) verifyQr(result.getContents());
                else toast("Scan cancelled");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (session.token().isEmpty() || !"GridOperator".equalsIgnoreCase(session.role())) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_operator);

        contentLayout  = findViewById(R.id.contentLayout);
        tvStatPending  = findViewById(R.id.tvStatPending);
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatToday    = findViewById(R.id.tvStatToday);

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> doLogout());

        buildActions();
        loadDashboard();
    }

    private void buildActions() {
        contentLayout.removeAllViews();
        addActionCard(contentLayout, "Scan Prosumer QR Code",
                android.R.drawable.ic_menu_camera, v -> startScan());
        addActionCard(contentLayout, "Pending Bookings",
                android.R.drawable.ic_menu_agenda, v -> showReservations("Pending"));
        addActionCard(contentLayout, "Approved Bookings",
                android.R.drawable.ic_menu_sort_by_size, v -> showReservations("Approved"));
        addActionCard(contentLayout, "Available Energy Slots",
                android.R.drawable.ic_menu_info_details, v -> showSlots(true));
        addActionCard(contentLayout, "Update Slot Availability",
                android.R.drawable.ic_menu_edit, v -> showSlots(false));
    }

    private void loadDashboard() {
        showLoading();
        ApiClient.get().operatorDashboard().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    JsonObject x = r.body();
                    tvStatPending.setText(String.valueOf(
                            x.has("pendingReservations") ? x.get("pendingReservations").getAsInt() : 0));
                    tvStatApproved.setText(String.valueOf(
                            x.has("approvedReservations") ? x.get("approvedReservations").getAsInt() : 0));
                    tvStatToday.setText(String.valueOf(
                            x.has("todayBookings") ? x.get("todayBookings").getAsInt() : 0));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); }
        });
    }

    // ── QR Scanner ────────────────────────────────────────
    private void startScan() {
        ScanOptions opts = new ScanOptions();
        opts.setPrompt("Scan Prosumer Transaction QR");
        opts.setBeepEnabled(true);
        opts.setOrientationLocked(false);
        opts.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanLauncher.launch(opts);
    }

    private void verifyQr(String qrToken) {
        JsonObject body = new JsonObject();
        body.addProperty("qrToken", qrToken);
        showLoading();
        ApiClient.get().verifyQr(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                JsonObject x   = r.body();
                JsonObject res = x.has("reservation") && x.get("reservation").isJsonObject()
                        ? x.getAsJsonObject("reservation") : new JsonObject();
                new AlertDialog.Builder(OperatorActivity.this)
                        .setTitle("✅ QR Verified")
                        .setMessage("📋 Transaction: " + ApiUtils.str(x, "transactionCode")
                                + "\n🔖 Reservation: " + ApiUtils.str(res, "reservationCode")
                                + "\n⚡ Energy: " + fmt(ApiUtils.num(res, "energyAmountKwh")) + " kWh"
                                + "\n🔖 Status: " + ApiUtils.str(res, "status"))
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Finalize Transfer", (d, w) -> completeQr(qrToken))
                        .show();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void completeQr(String qrToken) {
        JsonObject body = new JsonObject();
        body.addProperty("qrToken", qrToken);
        showLoading();
        ApiClient.get().completeQr(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                toast("✅ Energy transfer completed successfully!");
                loadDashboard();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Reservations ──────────────────────────────────────
    private void showReservations(String status) {
        showLoading();
        ApiClient.get().reservationsByStatus(status).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                JsonArray a = r.body().getAsJsonArray();
                if (a.size() == 0) { toast("No " + status.toLowerCase() + " reservations"); return; }

                String[] labels = new String[a.size()];
                for (int i = 0; i < a.size(); i++) {
                    JsonObject x = a.get(i).getAsJsonObject();
                    labels[i] = statusIcon(ApiUtils.str(x, "status"))
                            + " " + ApiUtils.str(x, "reservationCode")
                            + "  ⚡ " + fmt(ApiUtils.num(x, "energyAmountKwh")) + " kWh";
                }
                new AlertDialog.Builder(OperatorActivity.this)
                        .setTitle(status + " Reservations")
                        .setItems(labels, (d, w) -> reservationDetail(a.get(w).getAsJsonObject()))
                        .show();
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void reservationDetail(JsonObject x) {
        new AlertDialog.Builder(this)
                .setTitle(ApiUtils.str(x, "reservationCode"))
                .setMessage("👤 Prosumer: " + ApiUtils.str(x, "prosumerId")
                        + "\n🗺 Node: " + ApiUtils.str(x, "nodeId")
                        + "\n📅 Date: " + shortDate(ApiUtils.str(x, "reservationDate"))
                        + "\n⏱ " + ApiUtils.str(x, "startTime")
                        + " – " + ApiUtils.str(x, "endTime")
                        + "\n⚡ Energy: " + fmt(ApiUtils.num(x, "energyAmountKwh")) + " kWh"
                        + "\n🔖 Status: " + ApiUtils.str(x, "status"))
                .setPositiveButton("Close", null)
                .show();
    }

    // ── Energy Slots ──────────────────────────────────────
    private void showSlots(boolean availableOnly) {
        showLoading();
        ApiClient.get().operatorSlots("", availableOnly).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                JsonArray a = r.body().getAsJsonArray();
                if (a.size() == 0) { toast("No slots found"); return; }

                String[] labels = new String[a.size()];
                for (int i = 0; i < a.size(); i++) {
                    JsonObject x = a.get(i).getAsJsonObject();
                    labels[i] = "📅 " + shortDate(ApiUtils.str(x, "slotDate"))
                            + "  ⏱ " + ApiUtils.str(x, "startTime")
                            + "  ⚡ " + fmt(ApiUtils.num(x, "availableCapacityKwh")) + " kWh";
                }

                String title = availableOnly ? "Available Slots" : "Select Slot to Update";
                new AlertDialog.Builder(OperatorActivity.this)
                        .setTitle(title)
                        .setItems(labels, availableOnly ? null
                                : (d, w) -> capacityDialog(a.get(w).getAsJsonObject()))
                        .setPositiveButton("Close", null)
                        .show();
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void capacityDialog(JsonObject slot) {
        EditText et = new EditText(this);
        et.setHint("New capacity (kWh)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(fmt(ApiUtils.num(slot, "availableCapacityKwh")));

        new AlertDialog.Builder(this)
                .setTitle("Update Slot Availability")
                .setMessage("Slot ID: " + ApiUtils.str(slot, "id"))
                .setView(et)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try { updateSlot(ApiUtils.str(slot, "id"),
                            Double.parseDouble(et.getText().toString()));
                    } catch (Exception e) { toast("Enter a valid number"); }
                }).show();
    }

    private void updateSlot(String id, double capacity) {
        showLoading();
        ApiClient.get().updateAvailability(id, capacity).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) toast("✅ Slot availability updated");
                else toast(errorMsg(r));
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Helpers ────────────────────────────────────────────
    private String shortDate(String s) {
        return (s != null && s.length() >= 10) ? s.substring(0, 10) : s;
    }

    private String fmt(double d) {
        return d == (long) d ? String.valueOf((long) d) : String.valueOf(d);
    }

    private String statusIcon(String status) {
        if ("Approved".equalsIgnoreCase(status))  return "✅";
        if ("Pending".equalsIgnoreCase(status))   return "⏳";
        if ("Completed".equalsIgnoreCase(status)) return "🏁";
        if ("Cancelled".equalsIgnoreCase(status)) return "❌";
        return "•";
    }
}
