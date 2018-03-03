package jsk.mpd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jsk.mpd.adapter.PlaylistAdapter;

import static jsk.mpd.utils.ContentUriUtil.checkFileType;
import static jsk.mpd.utils.ContentUriUtil.getPath;
import static android.media.MediaPlayer.MEDIA_ERROR_IO;
import static android.media.MediaPlayer.MEDIA_ERROR_MALFORMED;
import static android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED;
import static android.media.MediaPlayer.MEDIA_ERROR_TIMED_OUT;
import static android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
import static android.media.MediaPlayer.MEDIA_ERROR_UNSUPPORTED;

public class PlayerActivity extends Activity implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener,
        View.OnClickListener,
        View.OnHoverListener,
        AdapterView.OnItemClickListener,
        PlaylistAdapter.Callback{

    public static final String TAG = "PlayerActivity";

    private static final int READ_REQUEST_CODE = 42;
    public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
    public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
    public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
    public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
    public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
    public static final int MEDIA_TRACK_TYPE_METADATA = 5;

    public static final int MEDIA_PROGRESS_UPDATE = 0;
    public static final int MEDIA_ERROR_SYSTEM = -2147483648;
    public static final int GAP_TIME = 8000;
    public static final int SEEK_GAP_TIME = 1500;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;
    //private ProgressBar progressBar;
    private ImageView image_play;
    private ImageView image_replay;
    private ImageView image_open;
    private ImageView image_prev;
    private ImageView image_next;
    private ImageView image_list;
    private TextView videoTimeTextView;
    private SeekBar seekBar;
    private RadioGroup radioGroup;
    private TextView fileNameText;
    private ListView listView;
    private RelativeLayout relativeLayout1;

    private String videoTimeString;
    //private boolean seekBarAutoFlag = false;
    private Uri currentUri = null;
    private String currentPath = null;
    private String currentDir = null;
    private List<String> audioList;
    //private List<MediaPlayer.TrackInfo> audioTracks;
    private int[] trackid;
    private ArrayList<String> playList;
    private PlaylistAdapter playlistAdapter;
    private int currentPlaylistIndex;
    private String currentPlayPath;
    static private Handler mHandler;
    private boolean isCreating = false;
    private long currentTime = 0;
    private long currentSeekTime = 0;
    private int seekPos = -1;
    private boolean getKeyBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_player);

        initView();
        mHandler = new Handler();
        handleDataFormMain();
    }


    public void initView() {
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new SurfaceCallback());

        //progressBar = (ProgressBar)findViewById(R.id.progressBar);
        image_play = (ImageView) findViewById(R.id.image_play);
        image_replay = (ImageView)findViewById(R.id.image_replay);
        image_open = (ImageView)findViewById(R.id.image_open);
        image_list = (ImageView)findViewById(R.id.image_list);
        image_prev = (ImageView)findViewById(R.id.image_prev);
        image_next = (ImageView)findViewById(R.id.image_next);
        videoTimeTextView = (TextView)findViewById(R.id.textView_showTime);
        seekBar = (SeekBar)findViewById(R.id.seekbar);
        radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        fileNameText = (TextView)findViewById(R.id.fileName);
        listView = (ListView)findViewById(R.id.playlistView1);
        relativeLayout1 = (RelativeLayout)findViewById(R.id.relativelayout1);

        image_list.setOnClickListener(this);
        image_next.setOnClickListener(this);
        image_open.setOnClickListener(this);
        image_prev.setOnClickListener(this);
        image_play.setOnClickListener(this);
        image_replay.setOnClickListener(this);
        listView.setOnItemClickListener(this);
        surfaceView.setOnClickListener(this);
        surfaceView.setOnHoverListener(this);
        listView.setOnHoverListener(this);
        audioList = new ArrayList<>();

        radioGroup.setFocusable(true);
        radioGroup.setFocusableInTouchMode(true);
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        playerRelease();
        if(null != mHandler) {
            mHandler.removeCallbacks(runnableProgress);
            mHandler.removeCallbacks(runnableNextPlay);
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public void onClick(View v) {
        Log.i(TAG, "id: "+v.getId());
        int listIndex = 0;
        switch(v.getId()) {
            case R.id.image_play:
                if(mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        image_play.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_white_48dp, null));
                    } else {
                        mediaPlayer.start();
                        image_play.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_48dp, null));
                    }
                }
                break;

            case R.id.image_replay:
                if(null == mediaPlayer) {
                    playerInit();
                }
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(0);
                } else {
                    if(currentUri != null) {
                        mediaPlayer.reset();
                        try {
                            mediaPlayer.setDataSource(getApplicationContext(), currentUri);
                            mediaPlayer.prepareAsync();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

            case R.id.image_open:
                if(mHandler == null)
                    mHandler = new Handler();

                Intent intent = new Intent(PlayerActivity.this, FileExplorerActivity.class);
                startActivity(intent);
                finish();
                //performFileSearch();
                break;

            case R.id.image_list:
                if( (playList == null || playList.size() == 0) && isCreating == false) {
                    isCreating = true;
                    AToast.makeText(this, "Creating Playlist", Toast.LENGTH_SHORT).show();
                    //mHandler.postDelayed(runableCreatePlaylist, 2000);
                } else {

                    if (listView.getVisibility() == View.VISIBLE)
                        listView.setVisibility(View.INVISIBLE);
                    else
                        listView.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.image_next:
                if(isCreating == false && playList != null) {
                    if (playList.size() > 1) {
                        listIndex = currentPlaylistIndex + 1;

                        if (listIndex >= playList.size())
                            listIndex = 0;

                        playListFilePlay(listIndex, false);
                    }
                }
                break;

            case R.id.image_prev:
                if(isCreating == false && playList != null) {
                    if (playList.size() > 1) {
                        if (currentPlaylistIndex > 0)
                            listIndex = currentPlaylistIndex - 1;
                        else
                            listIndex = playList.size() - 1;

                        playListFilePlay(listIndex, false);
                    }
                }
                break;

            case R.id.surfaceView:

                break;

            default:
                break;
        }

        currentTime = System.currentTimeMillis();
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
        int what = event.getAction();
        //Log.i(TAG, "onHover, what: " + what + ", getkeyback: " + getKeyBack);
        switch(what) {
            case MotionEvent.ACTION_HOVER_ENTER:
                //getKeyBack = false;
                break;

            case MotionEvent.ACTION_HOVER_MOVE:
                if(getKeyBack == false) {
                    if (relativeLayout1.getVisibility() == View.INVISIBLE) {
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        relativeLayout1.setVisibility(View.VISIBLE);
                    }

                    if (radioGroup.getVisibility() == View.INVISIBLE) {
                        radioGroup.setVisibility(View.VISIBLE);
                    }

                    if (fileNameText.getVisibility() == View.INVISIBLE) {
                        fileNameText.setVisibility(View.VISIBLE);
                    }
                    currentTime = System.currentTimeMillis();
                }
                break;

            case MotionEvent.ACTION_HOVER_EXIT:
                getKeyBack = false;
                break;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.i(TAG, "onKeyDown, keyCode: " + keyCode);

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "onKeyDown, KEYCODE_BACK.");
            if (listView.getVisibility() == View.VISIBLE) {
                listView.setVisibility(View.INVISIBLE);
                return true;
            }
            relativeLayout1.setVisibility(View.INVISIBLE);
            radioGroup.setVisibility(View.INVISIBLE);
            fileNameText.setVisibility(View.INVISIBLE);
            getKeyBack = true;
        } else {
            if (relativeLayout1.getVisibility() == View.INVISIBLE) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                relativeLayout1.setVisibility(View.VISIBLE);
            }

            if (radioGroup.getVisibility() == View.INVISIBLE) {
                radioGroup.setVisibility(View.VISIBLE);
            }

            if (fileNameText.getVisibility() == View.INVISIBLE) {
                fileNameText.setVisibility(View.VISIBLE);
            }
            currentTime = System.currentTimeMillis();

            int listIndex = 0;
            int progress = 0;
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                if (isCreating == false && playList != null) {
                    if (playList.size() > 1) {
                        listIndex = currentPlaylistIndex + 1;

                        if (listIndex >= playList.size())
                            listIndex = 0;

                        playListFilePlay(listIndex, false);
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                if (isCreating == false && playList != null) {
                    if (playList.size() > 1) {
                        if (currentPlaylistIndex > 0)
                            listIndex = currentPlaylistIndex - 1;
                        else
                            listIndex = playList.size() - 1;

                        playListFilePlay(listIndex, false);
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    progress = seekBar.getProgress();
                    Log.i(TAG, "key right, progress: " + progress);
                    progress += 5000;
                    if(progress < mediaPlayer.getDuration()) {
                        videoTimeTextView.setText(getShowTime(progress) + "/" + videoTimeString);
                        seekBar.setProgress(progress);
                        currentSeekTime = System.currentTimeMillis();
                        seekPos = progress;
                        //mediaPlayer.seekTo(progress);
                    }
                }

            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    progress = seekBar.getProgress();
                    Log.i(TAG, "key right, progress: " + progress);
                    if(progress > 5000)
                        progress -= 5000;
                    else
                        progress = 0;

                    videoTimeTextView.setText(getShowTime(progress) + "/" + videoTimeString);
                    seekBar.setProgress(progress);
                    currentSeekTime = System.currentTimeMillis();
                    seekPos = progress;
                }
            } else if(keyCode == KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK) {
                int count = audioList.size();
                int idx = radioGroup.getCheckedRadioButtonId();
                Log.i(TAG, "count: "+count+", idx: "+idx);

                radioGroup.requestFocus();
                //radioGroup.check(idx);
                int i = 0;
                for(i = 0; i < radioGroup.getChildCount(); i++) {
                    if(i > idx) {
                        break;
                    }
                }

                if(i == radioGroup.getChildCount()) {
                    i = 0;
                }
                RadioButton rb = (RadioButton) radioGroup.getChildAt(i);
                if(rb != null && trackid.length > 0 && mediaPlayer != null) {
                    rb.setChecked(true);

                    AToast.makeText(PlayerActivity.this, rb.getText().toString(), Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "index: " + i + ", track id: " + trackid[i]);
                    mediaPlayer.selectTrack(trackid[i]);
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i(TAG, "dispatchKeyEvent, event: "+event);
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if(action == KeyEvent.ACTION_DOWN) {

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (relativeLayout1.getVisibility() == View.INVISIBLE) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    relativeLayout1.setVisibility(View.VISIBLE);
                }

                if (radioGroup.getVisibility() == View.INVISIBLE) {
                    radioGroup.setVisibility(View.VISIBLE);
                }

                if (fileNameText.getVisibility() == View.INVISIBLE) {
                    fileNameText.setVisibility(View.VISIBLE);
                }
                currentTime = System.currentTimeMillis();

                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        image_play.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_white_48dp, null));
                    } else {
                        mediaPlayer.start();
                        image_play.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_48dp, null));
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {

        Log.i(TAG, "onItemClick, pos: "+position+", id: "+id);

        //listView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(resultData != null) {
                uriFilePlay(resultData.getData());
            }
        }
    }

    @Override
    public void click(View v) {
        int pos = (int)v.getTag();
        Log.i(TAG, "click, pos: "+pos);
        playListFilePlay(pos, false);
        listView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        getAudioTrack();
        mediaPlayer.start();
        image_play.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_48dp, null));
        mediaPlayer.setDisplay(surfaceHolder);
        mediaPlayer.setScreenOnWhilePlaying(true);
        surfaceHolder.setKeepScreenOn(true);
        seekBar.setVisibility(View.VISIBLE);

        if(Build.VERSION.SDK_INT <=23)
        {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int dHeight = dm.heightPixels;
            int dWidth = dm.widthPixels;
            int mpHeight = mediaPlayer.getVideoHeight();
            int mpWidth = mediaPlayer.getVideoWidth();
            Log.i(TAG, "video wh: (" + mpWidth + ", " + mpHeight + "), screen wh: (" + dWidth + ", " + dHeight + ")");
            if (mpWidth != 0 && mpHeight != 0) {
                double fH = new BigDecimal((float) dHeight / mpHeight).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                double fW = new BigDecimal((float) dWidth / mpWidth).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                Log.i(TAG, "fW,fH: (" + fW + ", " + fH + ")");

                int rH, rW;
                if ( (fH != fW) && (fH > 1) && (fW > 1) )
                {
                    double fR = (fH > fW) ? fW : fH;

                    if (fW < fH) {
                        rW = dWidth;
                        rH = (int) (fR * mpHeight);
                        Log.i(TAG, "fW < fH, wh: (" + dWidth + ", " + rH + ")");
                    } else {
                        rW = (int) (fR * mpWidth);
                        rH = dHeight;
                        Log.i(TAG, "fW > fH, wh: (" + rW + ", " + dHeight + ")");
                    }
                } else {
                    rW = dWidth;
                    rH = dHeight;
                }
                Log.i(TAG, "rW, rH: ("+rW+", "+rH+")");
                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                //params.addRule(RelativeLayout.CENTER_IN_PARENT);
                params.height = rH;
                params.width = rW;
                //surfaceView.getHolder().setFixedSize(rW, rH);
                surfaceView.setLayoutParams(params);
            }
        }

        seekBar.setProgress(0);
        seekBar.setMax(mediaPlayer.getDuration());

        videoTimeString = getShowTime(mediaPlayer.getDuration());
        videoTimeTextView.setText("00:00:00/" + videoTimeString);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 0) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                    }

                    videoTimeTextView.setText(getShowTime(progress) + "/" + videoTimeString);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // seekBarAutoFlag = true;
        mHandler.postDelayed(runnableProgress, 1000);
        if( (playList == null || playList.size() == 0) && isCreating == false) {
            isCreating = true;
            mHandler.postDelayed(runableCreatePlaylist, 2000);
        }
        relativeLayout1.setVisibility(View.VISIBLE);
        radioGroup.setVisibility(View.VISIBLE);
        fileNameText.setVisibility(View.VISIBLE);
        currentTime = System.currentTimeMillis();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        Log.v(TAG, "onError, what: " + what + ", extra: " + extra);
        String err = "Error: ";
        boolean ret = true;

        switch(what) {
            case MEDIA_ERROR_UNKNOWN:
                err += "MEDIA_ERROR_UNKNOWN ";
                break;

            case MEDIA_ERROR_SERVER_DIED:
                err += "MEDIA_ERROR_SERVER_DIED ";
                break;

            default:
                break;
        }

        /*
         * MEDIA_ERROR_SYSTEM (-2147483648) - low-level system error.
        * */
        switch(extra) {
            case MEDIA_ERROR_IO:
                err += "MEDIA_ERROR_IO";
                break;
            case MEDIA_ERROR_MALFORMED:
                err += "MEDIA_ERROR_MALFORMED";
                break;
            case MEDIA_ERROR_UNSUPPORTED:
                err += "MEDIA_ERROR_UNSUPPORTED";
                break;
            case MEDIA_ERROR_TIMED_OUT:
                err += "MEDIA_ERROR_TIMED_OUT";
                break;
            case MEDIA_ERROR_SYSTEM:
                err += "MEDIA_ERROR_SYSTEM";
                break;
            default:
                break;
        }

        Log.i(TAG, "err: " + err);
        AToast.makeText(this, err, Toast.LENGTH_SHORT).show();

        //mHandler.postDelayed(runnableNextPlay, 5000);

        return ret;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onCompletion");
        videoTimeTextView.setText(videoTimeString + "/" + videoTimeString);
        playerRelease();
        mHandler.removeCallbacks(runnableProgress);
        mHandler.removeCallbacks(runnableNextPlay);

        int listIndex = 0;
        if (isCreating == false) {
            if (playList != null) {
                if (playList.size() > 1) {
                    listIndex = currentPlaylistIndex + 1;

                    if (listIndex >= playList.size())
                        listIndex = 0;
                }
                playListFilePlay(listIndex, false);
            } else {
                this.finish();
            }
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int what) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onBufferingUpdate");
    }

    private void handleDataFormMain() {
        Bundle bundle = this.getIntent().getBundleExtra("uriInfo");
        String uriPath = bundle.getString("uri");
        Log.i(TAG, "get uriPath: " + uriPath);

        filelistFilePlay(uriPath);
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, "surfaceChanged, format: "+format+", w: "+width+", h: "+height);
            holder.setFixedSize(width, height);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            playerInit();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            playerRelease();
            mHandler.removeCallbacks(runnableProgress);
            mHandler.removeCallbacks(runnableNextPlay);
            //seekBarAutoFlag = false;
        }
    }

    protected void initListView() {
        if(playList == null) {
            playList = new ArrayList<String>();
        }

        initPlaylistData();
        if(playList.size() == 0) {
            Log.e(TAG, "initListView, list is null");

        }

        if(playlistAdapter == null)
            playlistAdapter = new PlaylistAdapter(getBaseContext(), playList, this);

        listView.setAdapter(playlistAdapter);
        listView.setVisibility(View.INVISIBLE);
    }

    public void performFileSearch() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void playerInit() {
        if(mediaPlayer == null)
            mediaPlayer = new MediaPlayer();
        if(Build.VERSION.SDK_INT <= 23) {
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);

        //performFileSearch();
    }

    public void playerRelease() {
        if(null != mediaPlayer) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void uriFilePlay(Uri data) {
        currentUri = data;
        Log.i(TAG, "uri: " + currentUri.toString());
        if(mediaPlayer == null) {
            playerInit();
        }
        if(Build.VERSION.SDK_INT <= 23) {
            mediaPlayer.reset();
        }

        String path = getPath(getBaseContext(), currentUri);
        currentPlayPath = path;
        fileNameText.setText(path);
        listView.setVisibility(View.INVISIBLE);
        if(playList != null) {
            playList.clear();
        }

        try {
            mediaPlayer.setDataSource(getApplicationContext(), currentUri);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playListFilePlay(int index, boolean error) {
        String path = playList.get(index);
        radioGroup.removeAllViews();

        Log.i(TAG, "path: "+path);
        //Uri uri = pathToUri(path);
        Uri uri = Uri.parse(path);
        Log.i(TAG, "uri: " + uri.toString());
        if(null == mediaPlayer) {
            playerInit();
        }

        if(mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        mediaPlayer.reset();
        currentUri = uri;
        fileNameText.setText(path);
        currentPlaylistIndex = index;
        final String[] split = path.split("/");
        String tmppath = split[split.length-1];
        if(error == false)
            AToast.makeText(this, tmppath, Toast.LENGTH_LONG).show();
        try {
            //mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void filelistFilePlay(String path) {
        currentPath = path;
        Log.i(TAG, "path: " + path);
        if(mediaPlayer == null) {
            playerInit();
        }
        if(Build.VERSION.SDK_INT <= 23) {
            mediaPlayer.reset();
        }

        currentPlayPath = path;
        fileNameText.setText(path);
        listView.setVisibility(View.INVISIBLE);
        if(playList != null) {
            playList.clear();
        }

        try {
            mediaPlayer.setDataSource(currentPath);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchPlayFile(File file) {
        String pFileName;

        try {
            if (file != null && file.exists()) {
                if (file.isDirectory()) {
                    Log.i(TAG, "searchPlayFile, file is dir: " + file.getAbsolutePath());
                    File[] listFile = file.listFiles();

                    if (listFile != null) {
                        for (int i = 0; i < listFile.length; i++) {
                            searchPlayFile(listFile[i]);
                        }
                    } else {
                        Log.i(TAG, "listFile is NULL");
                    }

                } else if (file.isFile()) {
                    pFileName = file.getAbsolutePath();
                    Log.i(TAG, "searchPlayFile, file: " + pFileName);
                    if (checkFileType(pFileName) == true) {
                        Log.i(TAG, "searchPlayFile， search file: " + pFileName);
                        playList.add(pFileName);
                    }
                } else {
                    Log.e(TAG, "file is nothing: " + file.getPath());
                }
            } else {
                Log.e(TAG, "searchPlayFile， file is null or not exist: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception");
        }

        // sort playlist
        if(playList.size() > 0) {
            Collections.sort(playList, new ComparatorString());
        }

        currentPlaylistIndex = playList.indexOf(currentPlayPath);
    }

    private void searchPlayFileExceptDir(File file) {
        String pFileName;

        try {
            if (file != null && file.exists()) {
                if (file.isDirectory()) {
                    Log.i(TAG, "searchPlayFile, file: " + file.getAbsolutePath());
                    File[] listFile = file.listFiles();

                    if (listFile != null) {
                        for (int i = 0; i < listFile.length; i++) {
                            if(listFile[i].isDirectory()) {
                                Log.i(TAG, "searchPlayFile, file is directory: " + listFile[i].getAbsolutePath());
                                continue;
                            }
                            pFileName = listFile[i].getAbsolutePath();
                            if (checkFileType(pFileName) == true) {
                                Log.i(TAG, "searchPlayFile， search file: " + pFileName);
                                playList.add(pFileName);
                            }
                        }
                    } else {
                        Log.i(TAG, "listFile is NULL");
                    }

                } else {
                    Log.i(TAG, "searchPlayFile, file is not directory: " + file.getAbsolutePath());
                }
            } else {
                Log.e(TAG, "searchPlayFile， file is null or not exist: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception");
        }

        // sort playlist
        if(playList.size() > 0) {
            Collections.sort(playList, new ComparatorString());
        }

        currentPlaylistIndex = playList.indexOf(currentPlayPath);
    }

    protected boolean initPlaylistData() {
        String pfileName;
        String path = currentPath;

        Log.i(TAG, "initPlaylistData, path: "+path);
        currentPlayPath = path;
        final String[] split = path.split("/");
        path = path.replace(split[split.length-1], "");
        Log.i(TAG, "initPlaylistData, path: "+path);
        Log.i(TAG, "initPlaylistData, currentDir: "+currentDir);
        if(path == currentDir)
            return false;
        else {
            currentDir = path;
        }
        Log.i(TAG, "initPlaylistData, currentDir: "+currentDir);

        File file = new File(path);
        playList.clear();
        searchPlayFileExceptDir(file);

        return true;
    }

   Runnable runnableProgress = new Runnable() {
        @Override
        public void run ()
        {
            //Log.i(TAG, "set progress");
            if (mediaPlayer != null && mediaPlayer.isPlaying() && relativeLayout1.getVisibility() == View.VISIBLE) {
                if(seekPos >= 0 && System.currentTimeMillis() - currentSeekTime > SEEK_GAP_TIME) {
                    mediaPlayer.seekTo(seekPos);
                    seekPos = -1;
                }

                if(seekPos == -1) {
                    videoTimeTextView.setText(getShowTime(mediaPlayer.getCurrentPosition()) + "/" + videoTimeString);
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                }
            }

            if(System.currentTimeMillis() - currentTime > GAP_TIME && mediaPlayer.isPlaying()) {
                if(relativeLayout1.getVisibility() == View.VISIBLE)
                    relativeLayout1.setVisibility(View.INVISIBLE);

                if(listView.getVisibility() == View.VISIBLE)
                    listView.setVisibility(View.INVISIBLE);

                if(radioGroup.getVisibility() == View.VISIBLE)
                    radioGroup.setVisibility(View.INVISIBLE);

                if(fileNameText.getVisibility() == View.VISIBLE)
                    fileNameText.setVisibility(View.INVISIBLE);
            }

            mHandler.postDelayed(this, 1000);
        }

    };

    Runnable runableCreatePlaylist = new Runnable() {
        @Override
        public void run ()
        {
            Log.i(TAG, "runableCreatePlaylist");

            synchronized (this) {
                initListView();
            }
            isCreating = false;

            if (listView.getVisibility() == View.VISIBLE)
                listView.setVisibility(View.INVISIBLE);
            //else
                //listView.setVisibility(View.VISIBLE);
        }

    };

    Runnable runnableNextPlay = new Runnable() {
        @Override
        public void run() {
            //Log.i(TAG, "error, runnable setnext");
            playerRelease();
            mHandler.removeCallbacks(runnableProgress);
            mHandler.removeCallbacks(runnableNextPlay);

            int listIndex = 0;
            if (playList.size() > 1) {
                listIndex = currentPlaylistIndex + 1;

                if (listIndex >= playList.size())
                    listIndex = 0;
            }
            playListFilePlay(listIndex, false);
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SimpleDateFormat")
    public String getShowTime(long milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        SimpleDateFormat dateFormat;

        if(milliseconds / 60000 > 60) {
            dateFormat = new SimpleDateFormat("hh:mm:ss");
        } else {
            dateFormat = new SimpleDateFormat("mm:ss");
        }

        return dateFormat.format(calendar.getTime());
    }

    private void getAudioTrack() {
        MediaPlayer.TrackInfo[] trackInfos = mediaPlayer.getTrackInfo();
        int index = 0;
        String lang = "null";
        for(int i = 0; i < trackInfos.length; i++) {
            lang = trackInfos[i].getLanguage();

            if (trackInfos[i].getTrackType() == MEDIA_TRACK_TYPE_AUDIO) {
                index++;
            }
        }

        trackid = null;
        audioList.clear();
        //audioTracks.clear();
        radioGroup.removeAllViews();

        trackid = new int[index];
        index = 0;
        for(int i = 0; i < trackInfos.length; i++) {
            if (trackInfos[i].getTrackType() == MEDIA_TRACK_TYPE_AUDIO) {
                Log.i(TAG, "find audiotrack: i: " + i + ", index: " + index);
                lang = trackInfos[i].getLanguage();
                Log.i(TAG, "track language: "+lang);
                lang = "audio"+i+": "+lang;

                audioList.add(lang);
                //audioTracks.add(trackInfos[i]);
                trackid[index] = i;
                index++;
            }
        }
        if(index > 1) {
            addAudioView(radioGroup);
            radioGroup.clearCheck();
            int trackSel = mediaPlayer.getSelectedTrack(MEDIA_TRACK_TYPE_AUDIO);
            for (int i = 0; i < index; i++) {
                Log.i(TAG, "i: " + i + ", trackSel: " + trackSel + ", track id: " + trackid[i]);
                if (trackSel == trackid[i]) {
                    radioGroup.check(i);
                    //RadioButton rBt = (RadioButton)findViewById(radioGroup.getCheckedRadioButtonId());
                    //rBt.setTextColor(getResources().getColor(R.color.whitecolor, null));
                }
            }
        }
    }

    public void addAudioView(RadioGroup radioGroup) {
        int index = 0;

        if(audioList.size() == 0) {
            Log.e(TAG, "audio list is null");
            return;
        }
        for(String ss:audioList) {
            RadioButton button = new RadioButton(this);
            setRadioBtnAttr(button, ss, index);

            radioGroup.addView(button);
            index++;
        }
    }

    private void setRadioBtnAttr(final RadioButton btn, String content, final int id) {
        if(btn == null) {
            return;
        }

        btn.setBackgroundResource(R.drawable.radio_group_selector);
        btn.setId(id);
        btn.setText(content);
        btn.setTextColor(getResources().getColor(R.color.whitecolor, null));
        btn.setButtonDrawable(R.drawable.ic_music_box_outline_white_18dp);
        btn.setGravity(Gravity.CENTER);
        btn.setPaddingRelative(8, 6, 8, 6);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AToast.makeText(PlayerActivity.this, btn.getText().toString(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "index: "+id+", track id: "+trackid[id]);
                mediaPlayer.selectTrack(trackid[id]);
                //btn.setTextColor(getResources().getColor(R.color.whitecolor, null));
            }
        });

        //LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, DensityUtilHelps.Dp2)
        //btn.setLayoutParams(rlp);
    }

    private void uiDisplay() {

    }

    public static final class ComparatorString implements Comparator<String> {
        @Override
        public int compare(String object1, String object2) {
            int result = 0;
            if(object1.compareToIgnoreCase(object2) > 0) {
                result = 1;
            }

            if(object1.compareToIgnoreCase(object2) < 0) {
                result = -1;
            }

            return result;
        }
    }
}
