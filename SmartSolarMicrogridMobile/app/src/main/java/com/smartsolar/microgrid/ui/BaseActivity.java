package com.smartsolar.microgrid.ui;

import android.app.*;import android.content.*;import androidx.appcompat.app.AppCompatActivity;import android.graphics.Color;import android.os.Bundle;import android.view.*;import android.widget.*;import com.google.android.material.button.MaterialButton;import com.google.android.material.textfield.TextInputEditText;import com.google.android.material.textfield.TextInputLayout;import com.smartsolar.microgrid.util.SessionManager;import retrofit2.*;

public abstract class BaseActivity extends AppCompatActivity {
 protected LinearLayout root; protected SessionManager session; protected LocalLoading loading;
 @Override protected void onCreate(Bundle b){super.onCreate(b);SessionManager.init(this);session=SessionManager.getInstance();loading=new LocalLoading();}
 protected void setup(String title){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,18,20,18);root.setBackgroundColor(Color.rgb(245,248,247)); TextView bar=t(title,22,Color.WHITE);bar.setPadding(18,22,18,22);bar.setBackgroundColor(Color.rgb(20,107,82));root.addView(bar,new LinearLayout.LayoutParams(-1,-2));setContentView(root);}
 protected TextView t(String x,float sp,int c){TextView v=new TextView(this);v.setText(x);v.setTextSize(sp);v.setTextColor(c);v.setPadding(4,8,4,8);return v;}
 protected TextInputLayout input(String hint){TextInputLayout l=new TextInputLayout(this);l.setHint(hint);l.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);TextInputEditText e=new TextInputEditText(this);l.addView(e);l.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));((LinearLayout.LayoutParams)l.getLayoutParams()).setMargins(0,0,0,10);return l;}
 protected String val(TextInputLayout l){return ((TextInputEditText)l.getEditText()).getText().toString().trim();}
 protected MaterialButton btn(String text){MaterialButton b=new MaterialButton(this);b.setText(text);b.setAllCaps(false);b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return b;}
 protected void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
 protected void fail(Throwable t){toast(t.getMessage()==null?"Request failed":t.getMessage());}
 protected void logout(){session.clear();startActivity(new Intent(this,LoginActivity.class));finishAffinity();}
 protected static class LocalLoading{ProgressDialog p;void show(Context c){p=ProgressDialog.show(c,"Please wait","Connecting to server…",true,false);}void hide(){if(p!=null&&p.isShowing())p.dismiss();}}
}
