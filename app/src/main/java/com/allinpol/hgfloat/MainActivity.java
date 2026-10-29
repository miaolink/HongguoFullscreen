package com.allinpol.hgfloat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final int REQ_OVERLAY = 1001;
    private TextView tvStatus;
    private TextView tvLog;
    private CheckBox cbBoot;
    private CheckBox cbSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvLog = (TextView) findViewById(R.id.tvLog);
        cbBoot = (CheckBox) findViewById(R.id.cbBoot);
        cbSkip = (CheckBox) findViewById(R.id.cbSkip);
        Button btnStart = (Button) findViewById(R.id.btnStart);
        Button btnStop = (Button) findViewById(R.id.btnStop);
        Button btnSu = (Button) findViewById(R.id.btnSu);
        Button btnSuSilent = (Button) findViewById(R.id.btnSuSilent);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnSu.setOnClickListener(this);
        btnSuSilent.setOnClickListener(this);
        cbBoot.setOnCheckedChangeListener(null);
        cbBoot.setChecked(Prefs.isAutoStartBoot(this));
        cbBoot.setOnCheckedChangeListener(this);
        cbSkip.setOnCheckedChangeListener(null);
        cbSkip.setChecked(Prefs.isAutoSkipPrompt(this));
        cbSkip.setOnCheckedChangeListener(this);
        DebugLog.i("MainActivity.onCreate on-device inject");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.cbBoot) {
            Prefs.setAutoStartBoot(this, isChecked);
            DebugLog.i("auto_start_boot=" + isChecked);
            Toast.makeText(this,
                    isChecked ? "已开启开机自启" : "已关闭开机自启",
                    Toast.LENGTH_SHORT).show();
        } else if (id == R.id.cbSkip) {
            Prefs.setAutoSkipPrompt(this, isChecked);
            DebugLog.i("auto_skip_prompt=" + isChecked);
            Toast.makeText(this,
                    isChecked ? "已开启自动跳过" : "已关闭自动跳过",
                    Toast.LENGTH_SHORT).show();
            notifyFloatReloadPrefs();
        }
        refreshStatus();
    }

    private void notifyFloatReloadPrefs() {
        Intent i = new Intent(this, FloatService.class);
        i.setAction("reload_prefs");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception e) {
            DebugLog.e("reload_prefs fail", e);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnStart) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.need_overlay, Toast.LENGTH_LONG).show();
                Intent i = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_OVERLAY);
                return;
            }
            startFloat();
        } else if (id == R.id.btnStop) {
            stopService(new Intent(this, FloatService.class));
            tvStatus.setText(R.string.float_off);
            DebugLog.i("stop float requested");
        } else if (id == R.id.btnSu) {
            if (SuManager.open(this)) {
                Toast.makeText(this, "已打开授权管理", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未找到授权管理界面", Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.btnSuSilent) {
            String msg = SuSilent.disablePrompt();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            refreshStatus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY && Settings.canDrawOverlays(this)) {
            startFloat();
        }
        refreshStatus();
    }

    private void startFloat() {
        String a11y = HotkeyEnabler.ensureEnabled(this);
        Toast.makeText(this, a11y, Toast.LENGTH_SHORT).show();
        BootReceiver.startFloat(this);
        DebugLog.i("startFloat service requested; " + a11y);
        tvStatus.setText(R.string.float_on);
        moveTaskToBack(true);
    }

    private void refreshStatus() {
        DebugLog.init(this);
        boolean overlay = Settings.canDrawOverlays(this);
        boolean boot = Prefs.isAutoStartBoot(this);
        boolean skip = Prefs.isAutoSkipPrompt(this);
        boolean f9 = HotkeyEnabler.isEnabled(this);
        StringBuilder sb = new StringBuilder();
        sb.append(getString(overlay ? R.string.status_overlay_on : R.string.status_overlay_off));
        sb.append('\n').append(getString(boot ? R.string.status_boot_on : R.string.status_boot_off));
        sb.append('\n').append(getString(skip ? R.string.status_skip_on : R.string.status_skip_off));
        sb.append('\n').append(getString(f9 ? R.string.status_f9_on : R.string.status_f9_off));
        tvStatus.setText(sb.toString());
        String logTail = DebugLog.readTail(400);
        if (tvLog != null) {
            tvLog.setText(logTail.isEmpty() ? getString(R.string.log_empty) : logTail);
        }
    }
}
