package jsk.mpd.adapter;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jsk.mpd.R;

import static jsk.mpd.utils.ContentUriUtil.checkFileType;
import static jsk.mpd.utils.ContentUriUtil.checkFileTypeAudio;
import static jsk.mpd.utils.ContentUriUtil.checkFileTypeVideo;

/**
 * Created by ALi on 2018/1/5.
 */

public class FileListAdapter extends BaseAdapter implements View.OnClickListener{

    public static final String TAG = "FileExplorerActivity";
    Context context;
    ArrayList<String> list;
    private LayoutInflater inflater;
    private Callback mCallback;

    public interface Callback {
        public void click(View v);
    }

    public FileListAdapter(Context context, ArrayList list, Callback mCallback) {
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
        fileHolder holder;
        if(convertView == null) {
            //Log.i(TAG, "1, position: "+position+", convertview: "+convertView+", parent: "+parent);
            holder = new fileHolder();
            convertView = inflater.inflate(R.layout.item_filelistview, null);
            holder.full = (LinearLayout)convertView.findViewById(R.id.item_fullfile);
            holder.image = (ImageView) convertView.findViewById(R.id.item_file_image);
            holder.path = (TextView)convertView.findViewById(R.id.item_file_path);
            holder.size = (TextView)convertView.findViewById(R.id.item_file_size);
            holder.time = (TextView)convertView.findViewById(R.id.item_file_time);
            convertView.setTag(R.id.holder, holder);
        } else {
            //Log.i(TAG, "2, position: "+position+", convertview: "+convertView+", parent: "+parent);
            holder = (fileHolder)convertView.getTag(R.id.holder);
        }

        holder.full.setTag(position);
        holder.full.setOnClickListener(this);
        //Log.i(TAG, "full: "+holder.full);
        String path = list.get(position);
        File file = new File(path);
        if(file.isDirectory()) {
            holder.isFile = false;
            holder.size.setText("");
            holder.time.setText("");
        }
        else {
            holder.isFile = true;
            long len = file.length();
            long time = file.lastModified();
            holder.size.setText(getPrintSize(len));
            holder.time.setText(mstodate(time));
        }

        final String[] split = path.split("/");
        if(path.equals("/storage/emulated/0"))
            path = "emulated/0";
        else
            path = split[split.length-1];

        holder.path.setText(path);
        holder.path.setTag(position);
        holder.path.setOnClickListener(this);
        if(holder.isFile) {
            if(true == checkFileTypeAudio(path)) {
                holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_audiotrack_black_24dp, null));
            } else if(true == checkFileTypeVideo(path)) {
                holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_movie_black_24dp, null));
            } else {
                holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_not_interested_black_24dp, null));
            }
        } else {
            holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_black_24dp, null));
        }
        holder.image.setTag(position);
        holder.image.setOnClickListener(this);

        return convertView;
    }

    protected class fileHolder {
        LinearLayout full;
        ImageView image;
        TextView path;
        TextView size;
        TextView time;
        boolean isFile;
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick, v: "+v);
        mCallback.click(v);

    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown, keyCode: "+keyCode+", event: "+event);
        return super.onKeyDown(keyCode, event);
    }*/

    public static String getPrintSize(long size) {
        //如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }
        //如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        //因为还没有到达要使用另一个单位的时候
        //接下去以此类推
        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "GB";
        }
    }

    public static String mstodate(long time) {
        SimpleDateFormat format =  new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

        Long t=new Long(time);

        String d = format.format(t);

        //Date date=format.parse(d);
        return d;
    }
}
