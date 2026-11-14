package com.feedblocker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText scrolls, minutes;
    private CheckBox fb, lite, liteFull;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences p = Prefs.get(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(48, 64, 48, 48);

        TextView title = new TextView(this);
        title.setText("Feed Blocker");
        title.setTextSize(26);
        l.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, 16, 0, 24);
        l.addView(status);

        Button open = new Button(this);
        open.setText("Turn on in Accessibility settings");
        open.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        l.addView(open);

        fb = check(l, "Block Facebook", p.getBoolean(Prefs.BLOCK_FB, true));
        lite = check(l, "Block Facebook Lite", p.getBoolean(Prefs.BLOCK_LITE, true));
        liteFull = check(l, "Block FB Lite completely (use Messenger for messages)", p.getBoolean(Prefs.LITE_FULL, false));
        scrolls = number(l, "Scrolls allowed per minute (0 = none)", p.getInt(Prefs.SCROLL_LIMIT, 0));
        minutes = number(l, "Max minutes per visit (0 = no limit)", p.getInt(Prefs.SESSION_MIN, 0));

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> {
            p.edit().putBoolean(Prefs.BLOCK_FB, fb.isChecked())
                    .putBoolean(Prefs.BLOCK_LITE, lite.isChecked())
                    .putBoolean(Prefs.LITE_FULL, liteFull.isChecked())
                    .putInt(Prefs.SCROLL_LIMIT, read(scrolls, 0))
                    .putInt(Prefs.SESSION_MIN, read(minutes, 0)).apply();
            p.edit().putLong(Prefs.LOCKED_UNTIL, 0).apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        l.addView(save);

        TextView note = new TextView(this);
        note.setPadding(0, 24, 0, 0);
        note.setText("0 = any feed scroll kicks you out. FB Lite chats are always allowed: scroll and message freely there. "
                + "The Messenger app is a separate app and is never affected.");
        l.addView(note);

        ScrollView s = new ScrollView(this);
        s.addView(l);
        setContentView(s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String on = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean enabled = on != null && on.contains(getPackageName());
        status.setText(enabled ? "✅ Running" : "❌ Off: tap the button below, then Installed apps → Feed Blocker → On");
    }

    private CheckBox check(LinearLayout l, String text, boolean val) {
        CheckBox c = new CheckBox(this);
        c.setText(text);
        c.setChecked(val);
        l.addView(c);
        return c;
    }

    private EditText number(LinearLayout l, String label, int val) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setPadding(0, 24, 0, 0);
        l.addView(t);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(val));
        l.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private int read(EditText e, int def) {
        try { return Math.max(0, Integer.parseInt(e.getText().toString().trim())); }
        catch (NumberFormatException ex) { return def; }
    }
}
