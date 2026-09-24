package com.smartsolar.microgrid.ui;

import android.os.Bundle;
import android.widget.*;
import com.google.gson.*;
import com.google.android.material.textfield.TextInputLayout;
import com.smartsolar.microgrid.api.*;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.*;

public class RegisterActivity extends BaseActivity {
 TextInputLayout nic, name, email, phone, address, username, password;

 @Override protected void onCreate(Bundle b) {
  super.onCreate(b);
  setup("Prosumer Registration");
  ScrollView s = new ScrollView(this);
  LinearLayout box = new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.addView(t("Create your Prosumer account", 25, 0xff123f33));
  box.addView(t("NIC is the primary identifier.", 14, 0xff5b6b66));
  nic = input("NIC");
  name = input("Full name");
  email = input("Email");
  phone = input("Phone");
  address = input("Address");
  username = input("Username");
  password = input("Password");
  box.addView(nic); box.addView(name); box.addView(email);
  box.addView(phone); box.addView(address); box.addView(username); box.addView(password);
  Button submit = btn("REGISTER");
  submit.setOnClickListener(v -> register());
  box.addView(submit);
  Button back = btn("BACK TO LOGIN");
  back.setOnClickListener(v -> finish());
  box.addView(back);
  s.addView(box);
  root.addView(s, new LinearLayout.LayoutParams(-1, 0, 1));
 }

 void register() {
  if (val(nic).isEmpty() || val(name).isEmpty() || val(username).isEmpty() || val(password).isEmpty()) {
   toast("NIC, name, username and password are required");
   return;
  }
  JsonObject j = new JsonObject();
  j.addProperty("NIC", val(nic));
  j.addProperty("FullName", val(name));
  j.addProperty("Email", val(email));
  j.addProperty("Phone", val(phone));
  j.addProperty("Address", val(address));
  j.addProperty("Username", val(username));
  j.addProperty("Password", val(password));
  // Capture NIC value before entering anonymous class
  final String nicValue = val(nic);
  loading.show(RegisterActivity.this);
  ApiClient.get().register(j).enqueue(new Callback<JsonObject>() {
   public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
    loading.hide();
    if (!r.isSuccessful()) { toast(ApiUtils.error(r)); return; }
    getSharedPreferences("login_meta", 0).edit().putString("nic", nicValue).apply();
    toast("Registration submitted. Backoffice approval is required before login.");
    finish();
   }
   public void onFailure(Call<JsonObject> c, Throwable t) { loading.hide(); fail(t); }
  });
 }
}
