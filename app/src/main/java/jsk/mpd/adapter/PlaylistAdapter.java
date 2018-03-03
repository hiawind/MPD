package jsk.mpd.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

import jsk.mpd.R;

/**
 * Created by ALi on 2017/12/26.
 */

public class PlaylistAdapter extends BaseAdapter implements View.OnClickListener{

    Context context;
    ArrayList<String> list;
    private LayoutInflater inflater;
    private Callback mCallback;

    public interface Callback {
        public void click(View v);
    }

    public PlaylistAdapter(Context context, ArrayList list, Callback mCallback) {
        this.context = context;
        this.list = list;
        inflater = LayoutInflater.from(context);
        this.mCallback = mCallback;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        if(convertView == null) {
            holder = new Holder();
            convertView = inflater.inflate(R.layout.item_listview, null);
            holder.name = (TextView)convertView.findViewById(R.id.item_name);
            holder.id = (TextView)convertView.findViewById(R.id.item_id);
            convertView.setTag(holder);
        } else {
            holder = (Holder)convertView.getTag();
        }

        holder.id.setText(Integer.toString(position+1) + ".");
        String path = list.get(position);
        final String[] split = path.split("/");
        path = split[split.length-1];
        holder.name.setText(path);
        holder.name.setTag(position);
        holder.name.setOnClickListener(this);
        /*(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, list.get(position), Toast.LENGTH_SHORT).show();
            }
        });*/

        return convertView;
    }

    protected class Holder {
        TextView id;
        TextView name;
    }

    @Override
    public void onClick(View v) {
        mCallback.click(v);
    }
}
