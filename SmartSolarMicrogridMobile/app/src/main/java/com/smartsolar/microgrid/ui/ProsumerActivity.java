package com.smartsolar.microgrid.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.gson.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import android.graphics.*;
import com.smartsolar.microgrid.R;
import com.smartsolar.microgrid.api.ApiClient;
import com.smartsolar.microgrid.db.LocalDbHelper;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProsumerActivity extends BaseActivity {

    private LinearLayout contentLayout;
    private TextView tvStatActive, tvStatPending, tvStatCompleted, tvUsername;
    private LocalDbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (session.token().isEmpty() || !"Prosumer".equalsIgnoreCase(session.role())) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_prosumer);

        contentLayout    = findViewById(R.id.contentLayout);
        tvStatActive     = findViewById(R.id.tvStatActive);
        tvStatPending    = findViewById(R.id.tvStatPending);
        tvStatCompleted  = findViewById(R.id.tvStatCompleted);
        tvUsername       = findViewById(R.id.tvUsername);
        db               = new LocalDbHelper(this);

        tvUsername.setText(session.username() + " · " + session.nic());

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> doLogout());

        buildActions();
        loadDashboard();
    }

    private void buildActions() {
        contentLayout.removeAllViews();
        addActionCard(contentLayout, "Book Energy Slot",
                android.R.drawable.ic_menu_add, v -> book());
        addActionCard(contentLayout, "My Bookings & Search",
                android.R.drawable.ic_menu_agenda, v -> filterBookings());
        addActionCard(contentLayout, "Booking History",
                android.R.drawable.ic_menu_recent_history, v -> loadBookings("", "Completed"));
        addActionCard(contentLayout, "Nearby Grid Nodes on Map",
                android.R.drawable.ic_menu_mapmode, v ->
                        startActivity(new Intent(this, MapActivity.class)));
        addActionCard(contentLayout, "My Profile",
                android.R.drawable.ic_menu_my_calendar, v -> profile());
        addActionCard(contentLayout, "View / Generate QR Code",
                android.R.drawable.ic_menu_share, v -> filterBookings());
        addActionCard(contentLayout, "Request Account Deactivation",
                android.R.drawable.ic_delete, v -> deactivate());
    }

    private void loadDashboard() {
        showLoading();
        ApiClient.get().prosumerDashboard(session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    JsonObject x = r.body();
                    tvStatActive.setText(String.valueOf(
                            x.has("activeReservations") ? x.get("activeReservations").getAsInt() : 0));
                    tvStatPending.setText(String.valueOf(
                            x.has("pendingReservations") ? x.get("pendingReservations").getAsInt() : 0));
                    tvStatCompleted.setText(String.valueOf(
                            x.has("historyCount") ? x.get("historyCount").getAsInt() : 0));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); }
        });
    }

    // ── Book a slot ────────────────────────────────────────
    private void book() {
        showLoading();
        ApiClient.get().slots("", true).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                JsonArray a = r.body().getAsJsonArray();
                if (a.size() == 0) { toast("No available slots found."); return; }

                String[] labels = new String[a.size()];
                for (int i = 0; i < a.size(); i++) {
                    JsonObject s = a.get(i).getAsJsonObject();
                    labels[i] = "📅 " + shortDate(ApiUtils.str(s, "slotDate"))
                            + "  ⏱ " + ApiUtils.str(s, "startTime") + "–" + ApiUtils.str(s, "endTime")
                            + "  ⚡ " + fmt(ApiUtils.num(s, "availableCapacityKwh")) + " kWh";
                }
                new AlertDialog.Builder(ProsumerActivity.this)
                        .setTitle("Available Energy Slots")
                        .setItems(labels, (d, w) -> energyDialog(a.get(w).getAsJsonObject()))
                        .show();
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void energyDialog(JsonObject slot) {
        EditText et = new EditText(this);
        et.setHint("Energy amount (kWh)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle("Reserve Slot")
                .setMessage("📅 " + shortDate(ApiUtils.str(slot, "slotDate"))
                        + "\n⏱ " + ApiUtils.str(slot, "startTime")
                        + " – " + ApiUtils.str(slot, "endTime")
                        + "\n⚡ Available: " + fmt(ApiUtils.num(slot, "availableCapacityKwh")) + " kWh")
                .setView(et)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (d, w) ->
                        createReservation(ApiUtils.str(slot, "id"), et.getText().toString()))
                .show();
    }

    private void createReservation(String slotId, String amount) {
        try {
            double k = Double.parseDouble(amount);
            if (k <= 0) { toast("Enter a valid energy amount"); return; }
            JsonObject body = new JsonObject();
            body.addProperty("energySlotId", slotId);
            body.addProperty("energyAmountKwh", k);
            showLoading();
            ApiClient.get().createReservation(session.nic(), body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                    hideLoading();
                    if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                    showSummary("✅ Booking Created", r.body());
                    loadDashboard();
                }
                @Override
                public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
            });
        } catch (Exception e) { toast("Enter a valid number"); }
    }

    // ── Filter / search bookings ───────────────────────────
    private void filterBookings() {
        EditText search = new EditText(this);
        search.setHint("Search code or node ID (optional)");
        Spinner spinner = new Spinner(this);
        String[] opts = {"All", "Pending", "Approved", "Cancelled", "Completed"};
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opts));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 16, 32, 0);
        box.addView(search);
        box.addView(spinner);

        new AlertDialog.Builder(this)
                .setTitle("Search & Filter Bookings")
                .setView(box)
                .setNegativeButton("Close", null)
                .setPositiveButton("Search", (d, w) -> {
                    String st = spinner.getSelectedItem().toString();
                    loadBookings(search.getText().toString().trim(),
                            "All".equals(st) ? "" : st);
                }).show();
    }

    private void loadBookings(String searchText, String statusFilter) {
        showLoading();
        ApiClient.get().myReservations(session.nic(), searchText, statusFilter)
                .enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                        hideLoading();
                        if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                        JsonArray a = r.body().getAsJsonArray();
                        if (a.size() == 0) { toast("No bookings found"); return; }

                        String[] labels = new String[a.size()];
                        for (int i = 0; i < a.size(); i++) {
                            JsonObject x = a.get(i).getAsJsonObject();
                            labels[i] = statusIcon(ApiUtils.str(x, "status"))
                                    + " " + ApiUtils.str(x, "reservationCode")
                                    + "  ·  " + shortDate(ApiUtils.str(x, "reservationDate"));
                        }
                        new AlertDialog.Builder(ProsumerActivity.this)
                                .setTitle("My Bookings")
                                .setItems(labels, (d, w) ->
                                        bookingActions(a.get(w).getAsJsonObject()))
                                .show();
                    }
                    @Override
                    public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
                });
    }

    private void bookingActions(JsonObject r) {
        String status = ApiUtils.str(r, "status");
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(ApiUtils.str(r, "reservationCode")
                        + "  " + statusIcon(status) + " " + status)
                .setMessage("📅 " + shortDate(ApiUtils.str(r, "reservationDate"))
                        + "\n⏱ " + ApiUtils.str(r, "startTime")
                        + " – " + ApiUtils.str(r, "endTime")
                        + "\n⚡ " + fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh");

        if ("Approved".equalsIgnoreCase(status))
            b.setNeutralButton("Get QR", (d, w) -> generateQr(ApiUtils.str(r, "id")));
        if ("Pending".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
            b.setPositiveButton("Modify", (d, w) -> modifyDialog(r));
            b.setNegativeButton("Cancel Booking", (d, w) -> confirmCancel(ApiUtils.str(r, "id")));
        } else {
            b.setPositiveButton("Close", null);
        }
        b.show();
    }

    private void modifyDialog(JsonObject old) {
        EditText etSlot = new EditText(this); etSlot.setHint("New Slot ID");
        EditText etAmt  = new EditText(this); etAmt.setHint("New Energy Amount (kWh)");
        etAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 16, 32, 0);
        box.addView(etSlot); box.addView(etAmt);

        new AlertDialog.Builder(this)
                .setTitle("Modify Reservation")
                .setMessage("Note: Modification requires at least 12 hours notice.")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (d, w) -> {
                    try {
                        JsonObject j = new JsonObject();
                        j.addProperty("energySlotId", etSlot.getText().toString().trim());
                        j.addProperty("energyAmountKwh", Double.parseDouble(etAmt.getText().toString()));
                        updateReservation(ApiUtils.str(old, "id"), j);
                    } catch (Exception e) { toast("Enter valid values"); }
                }).show();
    }

    private void updateReservation(String id, JsonObject body) {
        showLoading();
        ApiClient.get().updateReservation(id, session.nic(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                showSummary("✅ Booking Updated", r.body());
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void confirmCancel(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking?")
                .setMessage("Cancellation requires at least 12 hours notice.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, Cancel", (d, w) -> cancelReservation(id))
                .show();
    }

    private void cancelReservation(String id) {
        showLoading();
        ApiClient.get().cancelReservation(id, session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                showSummary("Booking Cancelled", r.body());
                loadDashboard();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── QR ─────────────────────────────────────────────────
    private void generateQr(String reservationId) {
        showLoading();
        ApiClient.get().generateQr(reservationId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                showQr(r.body());
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void showQr(JsonObject q) {
        String token = ApiUtils.str(q, "qrToken");
        ImageView img = new ImageView(this);
        img.setPadding(16, 16, 16, 16);
        try {
            BitMatrix m = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 700, 700);
            Bitmap bmp  = Bitmap.createBitmap(700, 700, Bitmap.Config.RGB_565);
            for (int x = 0; x < 700; x++)
                for (int y = 0; y < 700; y++)
                    bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
            img.setImageBitmap(bmp);
        } catch (Exception e) { toast("QR generation error"); return; }

        new AlertDialog.Builder(this)
                .setTitle("Transaction QR Code")
                .setMessage("📋 " + ApiUtils.str(q, "transactionCode")
                        + "\n\nShow this QR to the Grid Operator at the charging station.")
                .setView(img)
                .setPositiveButton("Done", null)
                .show();
    }

    // ── Profile ────────────────────────────────────────────
    private void profile() {
        showLoading();
        ApiClient.get().getProsumer(session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                JsonObject x = r.body();
                db.saveUser(session.nic(), ApiUtils.str(x, "fullName"),
                        ApiUtils.str(x, "email"), ApiUtils.str(x, "phone"),
                        ApiUtils.str(x, "address"), session.username(), session.role());

                EditText etName = makeField(ApiUtils.str(x, "fullName"));
                EditText etEmail = makeField(ApiUtils.str(x, "email"));
                EditText etPhone = makeField(ApiUtils.str(x, "phone"));
                EditText etAddr  = makeField(ApiUtils.str(x, "address"));

                LinearLayout box = new LinearLayout(ProsumerActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(32, 8, 32, 0);
                addLabeled(box, "Full Name", etName);
                addLabeled(box, "Email", etEmail);
                addLabeled(box, "Phone", etPhone);
                addLabeled(box, "Address", etAddr);

                new AlertDialog.Builder(ProsumerActivity.this)
                        .setTitle("My Profile  ·  NIC: " + session.nic())
                        .setView(box)
                        .setNegativeButton("Close", null)
                        .setPositiveButton("Save Changes", (d, w) -> {
                            JsonObject j = new JsonObject();
                            j.addProperty("fullName", etName.getText().toString());
                            j.addProperty("email",    etEmail.getText().toString());
                            j.addProperty("phone",    etPhone.getText().toString());
                            j.addProperty("address",  etAddr.getText().toString());
                            updateProfile(j);
                        }).show();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void updateProfile(JsonObject body) {
        showLoading();
        ApiClient.get().updateProsumer(session.nic(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                toast("✅ Profile updated successfully");
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Deactivation ───────────────────────────────────────
    private void deactivate() {
        new AlertDialog.Builder(this)
                .setTitle("Request Account Deactivation")
                .setMessage("This will send a deactivation request to Backoffice for review.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Request", (d, w) -> {
                    showLoading();
                    ApiClient.get().requestDeactivation(session.nic())
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                                    hideLoading();
                                    if (r.isSuccessful()) toast("Deactivation request submitted.");
                                    else toast(errorMsg(r));
                                }
                                @Override
                                public void onFailure(Call<JsonObject> c, Throwable t) {
                                    hideLoading(); fail(t);
                                }
                            });
                }).show();
    }

    // ── UI utility helpers ─────────────────────────────────
    private void showSummary(String title, JsonObject r) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("📋 " + ApiUtils.str(r, "reservationCode")
                        + "\n🔖 Status: " + ApiUtils.str(r, "status")
                        + "\n📅 Date: " + shortDate(ApiUtils.str(r, "reservationDate"))
                        + "\n⚡ Energy: " + fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh")
                .setPositiveButton("OK", null)
                .show();
    }

    private EditText makeField(String value) {
        EditText et = new EditText(this);
        et.setText(value);
        return et;
    }

    private void addLabeled(LinearLayout parent, String label, EditText et) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF1B6B4A);
        tv.setTextSize(12f);
        tv.setPadding(0, 12, 0, 2);
        parent.addView(tv);
        parent.addView(et);
    }

    private String shortDate(String s) {
        return (s != null && s.length() >= 10) ? s.substring(0, 10) : s;
    }

    private String fmt(double d) {
        return d == (long) d ? String.valueOf((long) d) : String.valueOf(d);
    }

    private String statusIcon(String status) {
        if ("Approved".equalsIgnoreCase(status))   return "✅";
        if ("Pending".equalsIgnoreCase(status))    return "⏳";
        if ("Completed".equalsIgnoreCase(status))  return "🏁";
        if ("Cancelled".equalsIgnoreCase(status))  return "❌";
        return "•";
    }
}
