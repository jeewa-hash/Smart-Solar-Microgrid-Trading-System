package com.smartsolar.microgrid.ui;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.smartsolar.microgrid.R;
import com.smartsolar.microgrid.api.ApiClient;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout tilNic, tilName, tilEmail, tilPhone, tilAddress, tilUsername, tilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilNic      = findViewById(R.id.tilNic);
        tilName     = findViewById(R.id.tilName);
        tilEmail    = findViewById(R.id.tilEmail);
        tilPhone    = findViewById(R.id.tilPhone);
        tilAddress  = findViewById(R.id.tilAddress);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);

        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        MaterialButton btnBack     = findViewById(R.id.btnBack);

        btnRegister.setOnClickListener(v -> register());
        btnBack.setOnClickListener(v -> finish());
    }

    private void register() {
        String nic  = val(tilNic);
        String name = val(tilName);
        String user = val(tilUsername);
        String pass = val(tilPassword);

        if (nic.isEmpty() || name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            toast("NIC, name, username and password are required");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("NIC",      nic);
        body.addProperty("FullName", name);
        body.addProperty("Email",    val(tilEmail));
        body.addProperty("Phone",    val(tilPhone));
        body.addProperty("Address",  val(tilAddress));
        body.addProperty("Username", user);
        body.addProperty("Password", pass);

        showLoading();

        ApiClient.get().register(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) {
                    toast(errorMsg(r));
                    return;
                }
                // Cache NIC for post-approval login hint
                getSharedPreferences("login_meta", 0)
                        .edit().putString("nic", nic).apply();
                toast("Registration submitted! Backoffice approval is required before you can sign in.");
                finish();
            }

            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }
}
