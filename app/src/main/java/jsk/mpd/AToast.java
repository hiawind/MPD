package jsk.mpd;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by ALi on 2018/2/24.
 */

public class AToast {
    private Toast mToast;

    private AToast(Context context, CharSequence text, int duration) {
        View v = LayoutInflater.from(context).inflate(R.layout.eplay_toast, null);
        TextView textView = (TextView)v.findViewById(R.id.toast_text);
        textView.setText(text);
        mToast = new Toast(context);
        mToast.setDuration(duration);
        mToast.setView(v);
    }

    public static AToast makeText(Context context, CharSequence text, int duration) {
        AToast aToast =  new AToast(context, text, duration);
        aToast.setGravity(Gravity.BOTTOM, 0, 120);
        return aToast;
    }

    public void show() {
        if(mToast != null) {
            mToast.show();
        }
    }

    public void setGravity(int gravity, int x, int y) {
        if(mToast != null) {
            mToast.setGravity(gravity, x, y);
        }
    }
}
