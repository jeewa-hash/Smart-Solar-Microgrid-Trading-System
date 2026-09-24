package com.smartsolar.microgrid.ui;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.gson.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smartsolar.microgrid.api.*;
import com.smartsolar.microgrid.db.LocalDbHelper;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.*;
import java.text.*;
import java.util.*;

public class ProsumerActivity extends BaseActivity {
 LinearLayout content; TextView stats; LocalDbHelper db;

 @Override protected void onCreate(Bundle b) {
  super.onCreate(b);
  if (session.token().isEmpty() || !"Prosumer".equalsIgnoreCase(session.role())) {
   startActivity(new Intent(this, LoginActivity.class)); finish(); return;
  }
  setup("Prosumer Mobile");
  db = new LocalDbHelper(this);
  ScrollView sv = new ScrollView(this);
  content = new LinearLayout(this);
  content.setOrientation(LinearLayout.VERTICAL);
  sv.addView(content);
  root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
  loadDashboard();
 }

 void loadDashboard() {
  content.removeAllViews();
  content.addView(t("My Solar Trading Dashboard", 26, 0xff123f33));
  stats = t("Loading\u2026", 15, 0xff40534c);
  content.addView(stats);
  add("BOOK ENERGY SLOT", v -> book());
  add("MY BOOKINGS / SEARCH / FILTER", v -> filterBookings());
  add("BOOKING HISTORY", v -> loadBookings("", "Completed"));
  add("NEARBY GRID NODES", v -> startActivity(new Intent(this, MapActivity.class)));
  add("MY PROFILE", v -> profile());
  add("GENERATE / VIEW QR", v -> qrChooser());
  add("REQUEST ACCOUNT DEACTIVATION", v -> deactivate());
  add("LOG OUT", v -> logout());
  ApiClient.get().prosumerDashboard(session.nic()).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    if (r.isSuccessful() && r.body() != null) {
     JsonObject x = r.body();
     stats.setText("Active: " + x.get("activeReservations").getAsInt()
      + "    Pending: " + x.get("pendingReservations").getAsInt()
      + "    Completed: " + x.get("historyCount").getAsInt());
    } else stats.setText("Dashboard unavailable: " + ApiUtils.error(r));
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { stats.setText("Dashboard unavailable"); }
  });
 }

 void add(String text, View.OnClickListener l) { Button b = btn(text); b.setOnClickListener(l); content.addView(b); }

 void book() {
  loading.show(ProsumerActivity.this);
  ApiClient.get().slots("", true).enqueue(new Callback<JsonElement>() {
   public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
    loading.hide();
    if (!r.isSuccessful() || r.body() == null) { toast(ApiUtils.error(r)); return; }
    JsonArray a = r.body().getAsJsonArray();
    if (a.size() == 0) { toast("No available slots found."); return; }
    String[] labels = new String[a.size()];
    for (int i = 0; i < a.size(); i++) {
     JsonObject s = a.get(i).getAsJsonObject();
     labels[i] = ApiUtils.str(s, "id") + " | " + date(s, "slotDate")
      + " | " + ApiUtils.str(s, "startTime") + "-" + ApiUtils.str(s, "endTime")
      + " | " + ApiUtils.num(s, "availableCapacityKwh") + " kWh";
    }
    new AlertDialog.Builder(ProsumerActivity.this)
     .setTitle("Select available slot")
     .setItems(labels, (d, w) -> energyDialog(a.get(w).getAsJsonObject()))
     .show();
   }
   public void onFailure(Call<JsonElement> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void energyDialog(JsonObject slot) {
  final EditText e = new EditText(this);
  e.setHint("Energy amount (kWh)");
  e.setInputType(2 | 8192);
  new AlertDialog.Builder(this)
   .setTitle("Reserve slot")
   .setMessage("Date: " + date(slot, "slotDate")
    + "\nTime: " + ApiUtils.str(slot, "startTime") + " - " + ApiUtils.str(slot, "endTime")
    + "\nAvailable: " + ApiUtils.num(slot, "availableCapacityKwh") + " kWh")
   .setView(e)
   .setNegativeButton("CANCEL", null)
   .setPositiveButton("CONFIRM", (d, w) -> createReservation(ApiUtils.str(slot, "id"), e.getText().toString()))
   .show();
 }

 void createReservation(String slot, String amount) {
  try {
   double k = Double.parseDouble(amount);
   if (k <= 0) { toast("Enter a valid amount"); return; }
   JsonObject j = new JsonObject();
   j.addProperty("energySlotId", slot);
   j.addProperty("energyAmountKwh", k);
   loading.show(ProsumerActivity.this);
   ApiClient.get().createReservation(session.nic(), j).enqueue(new Callback<JsonObject>() {
    public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
     loading.hide();
     if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
     showSummary("Booking request created", r.body());
     loadDashboard();
    }
    public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
   });
  } catch (Exception e) { toast("Enter a valid number"); }
 }

 void bookings() { filterBookings(); }

 void filterBookings() {
  final EditText search = new EditText(this);
  search.setHint("Search reservation code / node ID (optional)");
  final Spinner sp = new Spinner(this);
  String[] opts = {"All", "Pending", "Approved", "Cancelled", "Completed"};
  sp.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, opts));
  LinearLayout box = new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.addView(search);
  box.addView(sp);
  new AlertDialog.Builder(this)
   .setTitle("Search & Filter Bookings")
   .setView(box)
   .setNegativeButton("CLOSE", null)
   .setPositiveButton("SEARCH", (d, w) -> {
    String st = sp.getSelectedItem().toString();
    loadBookings(search.getText().toString().trim(), "All".equals(st) ? "" : st);
   }).show();
 }

 void loadBookings(String searchText, String statusFilter) {
  loading.show(ProsumerActivity.this);
  ApiClient.get().myReservations(session.nic(), searchText, statusFilter).enqueue(new Callback<JsonElement>() {
   public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
    loading.hide();
    if (!r.isSuccessful() || r.body() == null) { toast(ApiUtils.error(r)); return; }
    JsonArray a = r.body().getAsJsonArray();
    if (a.size() == 0) { toast("No bookings found"); return; }
    String[] labels = new String[a.size()];
    for (int i = 0; i < a.size(); i++) {
     JsonObject x = a.get(i).getAsJsonObject();
     labels[i] = ApiUtils.str(x, "reservationCode") + " | "
      + ApiUtils.str(x, "status") + " | " + date(x, "reservationDate");
    }
    new AlertDialog.Builder(ProsumerActivity.this)
     .setTitle("Bookings")
     .setItems(labels, (d, w) -> bookingActions(a.get(w).getAsJsonObject()))
     .show();
   }
   public void onFailure(Call<JsonElement> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void bookingActions(JsonObject r) {
  String status = ApiUtils.str(r, "status");
  AlertDialog.Builder b = new AlertDialog.Builder(this)
   .setTitle(ApiUtils.str(r, "reservationCode"))
   .setMessage("Date: " + date(r, "reservationDate")
    + "\nTime: " + ApiUtils.str(r, "startTime") + " - " + ApiUtils.str(r, "endTime")
    + "\nEnergy: " + ApiUtils.num(r, "energyAmountKwh") + " kWh"
    + "\nStatus: " + status);
  if ("Approved".equalsIgnoreCase(status))
   b.setNeutralButton("GET QR", (d, w) -> generateQr(ApiUtils.str(r, "id")));
  if ("Pending".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
   b.setPositiveButton("MODIFY", (d, w) -> modifyDialog(r));
   b.setNegativeButton("CANCEL", (d, w) -> cancel(ApiUtils.str(r, "id")));
  } else b.setPositiveButton("OK", null);
  b.show();
 }

 void modifyDialog(JsonObject old) {
  final EditText slot = new EditText(this);
  slot.setHint("New Energy Slot ID");
  final EditText amt = new EditText(this);
  amt.setHint("New Energy Amount kWh");
  LinearLayout box = new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.addView(slot); box.addView(amt);
  new AlertDialog.Builder(this)
   .setTitle("Modify reservation")
   .setMessage("Modification requires at least 12 hours notice.")
   .setView(box)
   .setNegativeButton("CANCEL", null)
   .setPositiveButton("UPDATE", (d, w) -> {
    try {
     JsonObject j = new JsonObject();
     j.addProperty("energySlotId", slot.getText().toString().trim());
     j.addProperty("energyAmountKwh", Double.parseDouble(amt.getText().toString()));
     updateReservation(ApiUtils.str(old, "id"), j);
    } catch (Exception e) { toast("Enter valid values"); }
   }).show();
 }

 void updateReservation(String id, JsonObject j) {
  loading.show(ProsumerActivity.this);
  ApiClient.get().updateReservation(id, session.nic(), j).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    showSummary("Booking updated", r.body());
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void cancel(String id) {
  new AlertDialog.Builder(this)
   .setTitle("Cancel booking?")
   .setMessage("Cancellation requires at least 12 hours notice.")
   .setNegativeButton("NO", null)
   .setPositiveButton("YES", (d, w) -> {
    loading.show(ProsumerActivity.this);
    ApiClient.get().cancelReservation(id, session.nic()).enqueue(new Callback<JsonObject>() {
     public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
      loading.hide();
      if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
      showSummary("Booking cancelled", r.body());
      loadDashboard();
     }
     public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
    });
   }).show();
 }

 void generateQr(String id) {
  loading.show(ProsumerActivity.this);
  ApiClient.get().generateQr(id).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    showQr(r.body());
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void qrChooser() { bookings(); }

 void showQr(JsonObject q) {
  String token = ApiUtils.str(q, "qrToken");
  ImageView img = new ImageView(this);
  try {
   BitMatrix m = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 800, 800);
   Bitmap bmp = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565);
   for (int x = 0; x < 800; x++)
    for (int y = 0; y < 800; y++)
     bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
   img.setImageBitmap(bmp);
  } catch (Exception e) { toast("QR display error"); return; }
  new AlertDialog.Builder(this)
   .setTitle("Transaction QR")
   .setMessage("Transaction: " + ApiUtils.str(q, "transactionCode")
    + "\nShow this QR to the Grid Operator at the station.")
   .setView(img)
   .setPositiveButton("DONE", null).show();
 }

 void profile() {
  loading.show(ProsumerActivity.this);
  ApiClient.get().getProsumer(session.nic()).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    JsonObject x = r.body();
    db.saveUser(session.nic(), ApiUtils.str(x, "fullName"), ApiUtils.str(x, "email"),
     ApiUtils.str(x, "phone"), ApiUtils.str(x, "address"), session.username(), session.role());
    LinearLayout box = new LinearLayout(ProsumerActivity.this);
    box.setOrientation(LinearLayout.VERTICAL);
    EditText n = field(x, "fullName");
    EditText e = field(x, "email");
    EditText p = field(x, "phone");
    EditText a = field(x, "address");
    box.addView(n); box.addView(e); box.addView(p); box.addView(a);
    new AlertDialog.Builder(ProsumerActivity.this)
     .setTitle("My Profile\nNIC: " + session.nic())
     .setView(box)
     .setNegativeButton("CLOSE", null)
     .setPositiveButton("SAVE", (d, w) -> {
      JsonObject j = new JsonObject();
      j.addProperty("fullName", n.getText().toString());
      j.addProperty("email", e.getText().toString());
      j.addProperty("phone", p.getText().toString());
      j.addProperty("address", a.getText().toString());
      updateProfile(j);
     }).show();
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 EditText field(JsonObject x, String k) {
  EditText e = new EditText(this);
  e.setHint(k);
  e.setText(ApiUtils.str(x, k));
  return e;
 }

 void updateProfile(JsonObject j) {
  loading.show(ProsumerActivity.this);
  ApiClient.get().updateProsumer(session.nic(), j).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    toast("Profile updated");
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }

 void deactivate() {
  new AlertDialog.Builder(this)
   .setTitle("Request account deactivation")
   .setMessage("This sends a deactivation request to Backoffice.")
   .setNegativeButton("CANCEL", null)
   .setPositiveButton("REQUEST", (d, w) -> {
    loading.show(ProsumerActivity.this);
    ApiClient.get().requestDeactivation(session.nic()).enqueue(new Callback<JsonObject>() {
     public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
      loading.hide();
      if (r.isSuccessful()) toast("Deactivation request submitted");
      else toast(ApiUtils.error(r));
     }
     public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
    });
   }).show();
 }

 void showSummary(String title, JsonObject r) {
  new AlertDialog.Builder(this)
   .setTitle(title)
   .setMessage("Reservation: " + ApiUtils.str(r, "reservationCode")
    + "\nStatus: " + ApiUtils.str(r, "status")
    + "\nDate: " + date(r, "reservationDate")
    + "\nEnergy: " + ApiUtils.num(r, "energyAmountKwh") + " kWh")
   .setPositiveButton("OK", null).show();
 }

 String date(JsonObject o, String k) {
  String s = ApiUtils.str(o, k);
  return s.length() >= 10 ? s.substring(0, 10) : s;
 }
}
