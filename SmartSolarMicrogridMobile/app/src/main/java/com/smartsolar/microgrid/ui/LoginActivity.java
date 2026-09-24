package com.smartsolar.microgrid.ui;

import android.content.*;
import android.os.*;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.google.gson.*;
import com.google.android.material.textfield.TextInputLayout;
import com.smartsolar.microgrid.api.*;
import com.smartsolar.microgrid.util.ApiUtils;
import com.smartsolar.microgrid.db.LocalDbHelper;
import retrofit2.*;

public class LoginActivity extends BaseActivity {

    TextInputLayout user, pass;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        if (!session.token().isEmpty()) {
            openRole(session.role());
            return;
        }

        setup("Smart Solar Microgrid");

        ScrollView s = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 28, 0, 0);

        box.addView(t("Welcome back", 30, 0xff123f33));
        box.addView(t("Sign in to continue", 16, 0xff5b6b66));

        user = input("Username");
        pass = input("Password");
        pass.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        box.addView(user);
        box.addView(pass);

        MaterialButtonWrap(box, "LOGIN", v -> login());
        MaterialButtonWrap(box, "CREATE PROSUMER ACCOUNT",
                v -> startActivity(new Intent(this, RegisterActivity.class)));

        s.addView(box);
        root.addView(s, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    void MaterialButtonWrap(LinearLayout p, String text, View.OnClickListener l) {
        android.widget.Button b = btn(text);
        b.setOnClickListener(l);
        p.addView(b);
    }

    void login() {
        String u = val(user), pw = val(pass);

        if (u.isEmpty() || pw.isEmpty()) {
            toast("Enter username and password");
            return;
        }

        JsonObject j = new JsonObject();
        j.addProperty("username", u);
        j.addProperty("password", pw);

        loading.show(this);

        ApiClient.get().login(j).enqueue(new Callback<JsonObject>() {
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                loading.hide();

                if (!r.isSuccessful() || r.body() == null) {
                    toast(ApiUtils.error(r));
                    return;
                }

                JsonObject x = r.body();
                String token = ApiUtils.str(x, "token");
                String role = ApiUtils.str(x, "role");
                String nic = getNicFromTokenOrUsername(x, role);

                session.save(token, role, ApiUtils.str(x, "username"), nic);
                openRole(role);
            }

            public void onFailure(Call<JsonObject> c, Throwable t) {
                loading.hide();
                fail(t);
            }
        });
    }

    String getNicFromTokenOrUsername(JsonObject x, String role) {
        String n = ApiUtils.str(x, "nic");
        if (!n.isEmpty()) return n;

        String token = ApiUtils.str(x, "token");

        try {
            String[] parts = token.split("[.]");

            if (parts.length > 1) {
                String raw = new String(
                        android.util.Base64.decode(
                                parts[1],
                                android.util.Base64.URL_SAFE
                                        | android.util.Base64.NO_WRAP
                                        | android.util.Base64.NO_PADDING),
                        java.nio.charset.StandardCharsets.UTF_8);

                JsonObject p = new JsonParser().parse(raw).getAsJsonObject();

                if (p.has("nic"))
                    return p.get("nic").getAsString();

                if (p.has("nameid"))
                    return p.get("nameid").getAsString();

                String nameClaim =
                        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";

                if (p.has(nameClaim))
                    return p.get(nameClaim).getAsString();
            }
        } catch (Exception ignored) {
        }

        return getSharedPreferences("login_meta", 0).getString("nic", "");
    }

    void openRole(String role) {
        if ("Prosumer".equalsIgnoreCase(role))
            startActivity(new Intent(this, ProsumerActivity.class));
        else if ("GridOperator".equalsIgnoreCase(role))
            startActivity(new Intent(this, OperatorActivity.class));
        else
            toast("Web Backoffice accounts are not supported in this mobile app.");

        if (!"Backoffice".equalsIgnoreCase(role))
            finish();
    }
}