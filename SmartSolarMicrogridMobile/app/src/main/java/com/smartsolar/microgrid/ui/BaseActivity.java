package com.smartsolar.microgrid.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartsolar.microgrid.R;
import com.smartsolar.microgrid.util.SessionManager;
import retrofit2.Response;

public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager session;
    protected ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager.init(this);
        session = SessionManager.getInstance();
    }

    // ── Loading dialog ──────────────────────────────────────
    protected void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Please wait…");
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
        }
        if (!loadingDialog.isShowing() && !isFinishing()) {
            loadingDialog.show();
        }
    }

    protected void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        hideLoading();
        super.onDestroy();
    }

    // ── Feedback helpers ────────────────────────────────────
    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void snack(View root, String msg) {
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.primary))
                .setTextColor(0xFFFFFFFF)
                .show();
    }

    protected void fail(Throwable t) {
        String msg = t.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = "Network request failed. Please check server connection.";
        }
        toast(msg);
    }

    // ── Text field helper ───────────────────────────────────
    protected String val(TextInputLayout til) {
        TextInputEditText et = (TextInputEditText) til.getEditText();
        return et == null ? "" : et.getText().toString().trim();
    }

    // ── Error body helper ───────────────────────────────────
    protected String errorMsg(Response<?> r) {
        if (r == null) return "Network error";
        if (r.errorBody() != null) {
            try { return r.errorBody().string(); } catch (Exception ignored) {}
        }
        return "Request failed (" + r.code() + ")";
    }

    // ── Dashboard action card builder ───────────────────────
    protected void addActionCard(LinearLayout container, String title,
                                  int iconRes, View.OnClickListener click) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_action_card, container, false);
        ((TextView) card.findViewById(R.id.title)).setText(title);
        ImageView icon = card.findViewById(R.id.icon);
        icon.setImageResource(iconRes);
        card.setOnClickListener(click);

        MaterialCardView mcv = (MaterialCardView) card;
        mcv.setClickable(true);
        mcv.setFocusable(true);
        container.addView(card);
    }

    // ── Logout helper ───────────────────────────────────────
    protected void doLogout() {
        session.clear();
        android.content.Intent i = new android.content.Intent(this, LoginActivity.class);
        i.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
