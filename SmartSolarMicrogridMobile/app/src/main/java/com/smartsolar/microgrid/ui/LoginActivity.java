package com.smartsolar.microgrid.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartsolar.microgrid.R;
import com.smartsolar.microgrid.api.ApiClient;
import com.smartsolar.microgrid.util.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextView tvServerSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auto-skip login if token exists
        if (!session.token().isEmpty()) {
            openRole(session.role());
            return;
        }

        setContentView(R.layout.activity_login);

        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tvServerSettings = findViewById(R.id.tvServerSettings);

        MaterialButton btnLogin    = findViewById(R.id.btnLogin);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        updateServerSettingsLabel();
        tvServerSettings.setOnClickListener(v -> showServerSettingsDialog());
    }

    private void updateServerSettingsLabel() {
        if (tvServerSettings != null) {
            tvServerSettings.setText("⚙ Server: " + session.getBaseUrl());
        }
    }

    private void showServerSettingsDialog() {
        final EditText input = new EditText(this);
        input.setText(session.getBaseUrl());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Configure Server URL")
                .setMessage("Emulator default: http://10.0.2.2:5000/api/\nPhysical device: http://<PC-LAN-IP>:5000/api/")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUrl = input.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        session.setBaseUrl(newUrl);
                        ApiClient.reset();
                        updateServerSettingsLabel();
                        toast("Server URL updated: " + session.getBaseUrl());
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset Default", (dialog, which) -> {
                    session.setBaseUrl("http://10.0.2.2:5000/api/");
                    ApiClient.reset();
                    updateServerSettingsLabel();
                    toast("Reset to default URL: " + session.getBaseUrl());
                })
                .show();
    }

    private void login() {
        String u  = val(tilUsername);
        String pw = val(tilPassword);

        if (u.isEmpty() || pw.isEmpty()) {
            toast("Enter username and password");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("username", u);
        body.addProperty("password", pw);

        showLoading();

        ApiClient.get().login(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) {
                    toast(errorMsg(r));
                    return;
                }
                JsonObject x   = r.body();
                String token   = ApiUtils.str(x, "token");
                String role    = ApiUtils.str(x, "role");
                String username = ApiUtils.str(x, "username");
                String nic     = resolveNic(x, token);

                session.save(token, role, username, nic);
                openRole(role);
            }

            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }

    /** Pull NIC from response JSON or decode it from the JWT payload */
    private String resolveNic(JsonObject x, String token) {
        String n = ApiUtils.str(x, "nic");
        if (!n.isEmpty()) return n;

        try {
            String[] parts = token.split("[.]");
            if (parts.length > 1) {
                String raw = new String(
                        android.util.Base64.decode(parts[1],
                                android.util.Base64.URL_SAFE
                                        | android.util.Base64.NO_WRAP
                                        | android.util.Base64.NO_PADDING),
                        java.nio.charset.StandardCharsets.UTF_8);

                JsonObject p = new JsonParser().parse(raw).getAsJsonObject();
                if (p.has("nic"))    return p.get("nic").getAsString();
                if (p.has("nameid")) return p.get("nameid").getAsString();
                String claim = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";
                if (p.has(claim))    return p.get(claim).getAsString();
            }
        } catch (Exception ignored) {}

        return getSharedPreferences("login_meta", 0).getString("nic", "");
    }

    private void openRole(String role) {
        if ("Prosumer".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, ProsumerActivity.class));
            finish();
        } else if ("GridOperator".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, OperatorActivity.class));
            finish();
        } else {
            toast("Web Backoffice accounts cannot sign in on the mobile app.");
        }
    }
}