package com.smartsolar.microgrid.ui;
import com.smartsolar.microgrid.util.ApiUtils;
import android.app.AlertDialog;
import android.view.View;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import com.google.gson.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.smartsolar.microgrid.api.*;
import retrofit2.*;

public class OperatorActivity extends BaseActivity {
 LinearLayout content; TextView stats;

 @Override protected void onCreate(Bundle b) {
  super.onCreate(b);
  if (session.token().isEmpty() || !"GridOperator".equalsIgnoreCase(session.role())) {
   startActivity(new Intent(this, LoginActivity.class)); finish(); return;
  }
  setup("Grid Operator Mobile");
  ScrollView sv = new ScrollView(this);
  content = new LinearLayout(this);
  content.setOrientation(LinearLayout.VERTICAL);
  sv.addView(content);
  root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
  dashboard();
 }

 void dashboard() {
  content.removeAllViews();
  content.addView(t("Operations Dashboard", 26, 0xff123f33));
  stats = t("Loading\u2026", 15, 0xff40534c);
  content.addView(stats);
  add("SCAN PROSUMER QR", v -> scan());
  add("PENDING BOOKINGS", v -> showReservations("Pending"));
  add("APPROVED BOOKINGS", v -> showReservations("Approved"));
  add("AVAILABLE ENERGY SLOTS", v -> slots());
  add("UPDATE SLOT AVAILABILITY", v -> availability());
  add("LOG OUT", v -> logout());
  ApiClient.get().operatorDashboard().enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    if (r.isSuccessful() && r.body() != null) {
     JsonObject x = r.body();
     stats.setText("Pending: " + ApiUtils.num(x, "pendingReservations")
      + "    Approved: " + ApiUtils.num(x, "approvedReservations")
      + "    Today: " + ApiUtils.num(x, "todayBookings")
      + "    Available slots: " + ApiUtils.num(x, "availableSlots"));
    } else stats.setText(ApiUtils.error(r));
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { stats.setText("Dashboard unavailable"); }
  });
 }

 void add(String s, View.OnClickListener l) { Button b = btn(s); b.setOnClickListener(l); content.addView(b); }

 void scan() {
  ScanOptions o = new ScanOptions();
  o.setPrompt("Scan Prosumer transaction QR");
  o.setBeepEnabled(true);
  o.setOrientationLocked(false);
  o.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
  scanLauncher.launch(o);
 }

 private final androidx.activity.result.ActivityResultLauncher<ScanOptions> scanLauncher =
  registerForActivityResult(new ScanContract(), result -> {
   if (result.getContents() != null) verify(result.getContents());
   else toast("Scan cancelled");
  });

 void verify(String token) {
  JsonObject j = new JsonObject();
  j.addProperty("qrToken", token);
  loading.show(OperatorActivity.this);
  ApiClient.get().verifyQr(j).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    JsonObject x = r.body();
    JsonObject res = x.has("reservation") && x.get("reservation").isJsonObject()
     ? x.getAsJsonObject("reservation") : new JsonObject();
    new AlertDialog.Builder(OperatorActivity.this)
     .setTitle("QR VERIFIED \u2713")
     .setMessage("Transaction: " + ApiUtils.str(x, "transactionCode")
      + "\nReservation: " + ApiUtils.str(res, "reservationCode")
      + "\nEnergy: " + ApiUtils.num(res, "energyAmountKwh") + " kWh"
      + "\nStatus: " + ApiUtils.str(res, "status"))
     .setNegativeButton("CANCEL", null)
     .setPositiveButton("FINALIZE TRANSFER", (d, w) -> complete(token))
     .show();
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void complete(String token) {
  JsonObject j = new JsonObject();
  j.addProperty("qrToken", token);
  loading.show(OperatorActivity.this);
  ApiClient.get().completeQr(j).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    toast("Energy transfer completed successfully.");
    dashboard();
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void showReservations(String status) {
  loading.show(OperatorActivity.this);
  ApiClient.get().reservationsByStatus(status).enqueue(new Callback<JsonElement>() {
   public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
    loading.hide();
    if (!r.isSuccessful() || r.body() == null) { toast(ApiUtils.error(r)); return; }
    JsonArray a = r.body().getAsJsonArray();
    if (a.size() == 0) { toast("No " + status.toLowerCase() + " reservations."); return; }
    String[] labels = new String[a.size()];
    for (int i = 0; i < a.size(); i++) {
     JsonObject x = a.get(i).getAsJsonObject();
     labels[i] = ApiUtils.str(x, "reservationCode") + " | "
      + ApiUtils.num(x, "energyAmountKwh") + " kWh | " + ApiUtils.str(x, "status");
    }
    new AlertDialog.Builder(OperatorActivity.this)
     .setTitle(status + " Reservations")
     .setItems(labels, (d, w) -> details(a.get(w).getAsJsonObject()))
     .show();
   }
   public void onFailure(Call<JsonElement> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void details(JsonObject x) {
  new AlertDialog.Builder(this)
   .setTitle(ApiUtils.str(x, "reservationCode"))
   .setMessage("Prosumer: " + ApiUtils.str(x, "prosumerId")
    + "\nNode: " + ApiUtils.str(x, "nodeId")
    + "\nSlot: " + ApiUtils.str(x, "energySlotId")
    + "\nDate: " + ApiUtils.str(x, "reservationDate")
    + "\nTime: " + ApiUtils.str(x, "startTime") + " - " + ApiUtils.str(x, "endTime")
    + "\nEnergy: " + ApiUtils.num(x, "energyAmountKwh") + " kWh"
    + "\nStatus: " + ApiUtils.str(x, "status"))
   .setPositiveButton("OK", null).show();
 }

 void slots() {
  loading.show(OperatorActivity.this);
  ApiClient.get().operatorSlots("", true).enqueue(new Callback<JsonElement>() {
   public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
    loading.hide();
    if (!r.isSuccessful() || r.body() == null) { toast(ApiUtils.error(r)); return; }
    JsonArray a = r.body().getAsJsonArray();
    String[] l = new String[a.size()];
    for (int i = 0; i < a.size(); i++) {
     JsonObject x = a.get(i).getAsJsonObject();
     l[i] = ApiUtils.str(x, "id") + " | " + ApiUtils.str(x, "slotDate")
      + " | " + ApiUtils.str(x, "startTime")
      + " | " + ApiUtils.num(x, "availableCapacityKwh") + " kWh";
    }
    new AlertDialog.Builder(OperatorActivity.this).setTitle("Available slots").setItems(l, null).show();
   }
   public void onFailure(Call<JsonElement> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void availability() {
  loading.show(OperatorActivity.this);
  ApiClient.get().operatorSlots("", false).enqueue(new Callback<JsonElement>() {
   public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
    loading.hide();
    if (!r.isSuccessful() || r.body() == null) { toast(ApiUtils.error(r)); return; }
    JsonArray a = r.body().getAsJsonArray();
    if (a.size() == 0) { toast("No slots found"); return; }
    String[] l = new String[a.size()];
    for (int i = 0; i < a.size(); i++) {
     JsonObject x = a.get(i).getAsJsonObject();
     l[i] = ApiUtils.str(x, "id") + " | " + ApiUtils.str(x, "status")
      + " | " + ApiUtils.num(x, "availableCapacityKwh") + " kWh";
    }
    new AlertDialog.Builder(OperatorActivity.this)
     .setTitle("Select slot")
     .setItems(l, (d, w) -> capacityDialog(a.get(w).getAsJsonObject()))
     .show();
   }
   public void onFailure(Call<JsonElement> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void capacityDialog(JsonObject x) {
  EditText e = new EditText(this);
  e.setHint("Available capacity kWh");
  e.setText(String.valueOf(ApiUtils.num(x, "availableCapacityKwh")));
  new AlertDialog.Builder(this)
   .setTitle("Update availability")
   .setMessage("Slot: " + ApiUtils.str(x, "id"))
   .setView(e)
   .setNegativeButton("CANCEL", null)
   .setPositiveButton("SAVE", (d, w) -> {
    try { update(ApiUtils.str(x, "id"), Double.parseDouble(e.getText().toString())); }
    catch (Exception ex) { toast("Enter a valid number"); }
   }).show();
 }

 void update(String id, double k) {
  loading.show(OperatorActivity.this);
  ApiClient.get().updateAvailability(id, k).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (r.isSuccessful()) toast("Availability updated");
    else toast(ApiUtils.error(r));
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }
}
