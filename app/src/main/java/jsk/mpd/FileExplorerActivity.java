package jsk.mpd;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jsk.mpd.adapter.FileListAdapter;
import jsk.mpd.adapter.PlaylistAdapter;

import static jsk.mpd.utils.ContentUriUtil.checkFileType;
import static jsk.mpd.utils.ContentUriUtil.getPath;

public class FileExplorerActivity extends Activity implements
        View.OnClickListener,
        FileListAdapter.Callback{

    public static final String TAG = "FileExplorerActivity";

    private TextView pathTextView;
    private ListView fileListView;
    private ArrayList<String> fileList;
    private FileListAdapter filelistAdapter;
    private String currentPath;
    private String currentPlayfile;
    private int currentFirstPos;
    private int currentSelPos;

    private static String storageRootPath = "/storage";
    private static final int PERMISSION_REQUEST_CODE = 321;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_file_explorer);

        initView();
        checkExternalStorage();
        Log.i(TAG, "onCreate");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);

            Log.i(TAG, "permission, i: " + i);

            if(i != PackageManager.PERMISSION_GRANTED) {
                //showDialogTipUserRequestPermission();
                Log.i(TAG, "OnCreate, permission denied, need to request");
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.i(TAG, "onRequestPermissionsResult, code: " + requestCode);

        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if(!b) {
                        Log.i(TAG, "onRequestPermissionsResult, !b");
                        showDialogTipUserGoToAppSetting();
                    } else {
                        Log.i(TAG, "onRequestPermissionsResult, finish");
                        //finish();
                    }
                    //storageRootPath = "/mnt/media_rw";
                } else {
                    AToast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();

                }

                scanDirectory(storageRootPath, 0, 0);
            }
        }
    }

    private void showDialogTipUserGoToAppSetting() {

    }

    public void initView() {
        pathTextView = (TextView)findViewById(R.id.PathText);
        fileListView = (ListView)findViewById(R.id.filelist);
        fileListView.setItemsCanFocus(true);

        if(fileList == null) {
            fileList = new ArrayList<String>();
        }

        scanDirectory(storageRootPath, 0, 0);
        if(fileList.size() == 0) {
            Log.e(TAG, "initListView, list is null");

        }

        if(filelistAdapter == null)
            filelistAdapter = new FileListAdapter(getBaseContext(), fileList, this);

        fileListView.setAdapter(filelistAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.i(TAG, "onRestart");

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume, currentPath: " + currentPath+", firstpos: "+currentFirstPos+", selPos: "+currentSelPos);
        if(fileList == null || fileList.size() == 0) {
            scanDirectory(currentPath, currentFirstPos, currentSelPos);
            currentFirstPos = 0;
            currentSelPos = 0;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i(TAG, "onPause");

    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy");
    }

    public void onClick(View v) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.i(TAG, "onKeyDown, keyCode: " + keyCode);
        Log.i(TAG, "onKeyDown, currentpath: " + currentPath);
        Log.i(TAG, "onKeyDown, currentplayfile: " + currentPlayfile);

        if(keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "onKeyDown, KEYCODE_BACK: ");

            String path = "";
            final String[] split = currentPath.split("/");
            if (split.length > 2) {
                if (currentPath.equals("/storage/emulated/0"))
                    path = storageRootPath;
                else {
                    for (int i = 1; i < split.length - 1; i++) {
                        Log.i(TAG, "onKeyDown, split " + i + ": " + split[i]);
                        path += "/" + split[i];
                    }
                }

                scanDirectory(path, currentFirstPos, currentSelPos);
                currentFirstPos = 0;
                currentSelPos = 0;
                return true;
            }
        } else if(keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            int iPos = fileListView.getSelectedItemPosition();
            Log.i(TAG, "key up or down, pos: "+iPos);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void click(View v) {
        int pos = (int) v.getTag();
        Log.i(TAG, "click, currentpath: " + currentPath+", pos: "+pos);
        try {
            //if (fileList != null && fileList.size() > 0)
            {
                String path = fileList.get(pos);
                Log.i(TAG, "click, pos: " + pos + ", path: " + path);
                currentFirstPos = fileListView.getFirstVisiblePosition();
                currentSelPos = pos;
                File file = new File(path);
                if((!file.isDirectory()) && (false == checkFileType(path)))
                    AToast.makeText(this, "Seems Not to Supported", Toast.LENGTH_SHORT).show();

                scanDirectory(path, 0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean scanDirectory(String path, int firstPos, int selPos) {
        String pfileName;

        Log.i(TAG, "scanDirectory, path: "+path);
        Log.i(TAG, "scanDirectory, currentplayfile: " + currentPlayfile);

        //filelistAdapter = null;
        //filelistAdapter = new FileListAdapter(getBaseContext(), fileList, this);

        //fileListView.setAdapter(filelistAdapter);

        File file = new File(path);
        if (file.isDirectory()) {
            currentPath = path;
        }
        pathTextView.setText(currentPath);
        fileList.clear();
        searchFile(file, firstPos, selPos);

        return true;
    }

    private void searchFile(File file, int firstPos, int selPos) {
        String pFileName;

        Log.i(TAG, "searchFile");

        try {
            if (file != null && file.exists()) {
                if (file.isDirectory()) {
                    Log.i(TAG, "searchFile, file is dir");
                    File[] listFile = file.listFiles();

                    if (listFile != null) {
                        for (int i = 0; i < listFile.length; i++) {
                            pFileName = listFile[i].getAbsolutePath();
                            //Log.i(TAG, "scan file, i: " + i + ", path: " + pFileName);
                            if(currentPath.equals(storageRootPath) && (pFileName.contains("emulated") || pFileName.contains("self")))
                                continue;

                            fileList.add(pFileName);
                        }

                        // sort playlist
                        if(fileList.size() > 0) {
                            Log.i(TAG, "searchFile, sort");
                            Collections.sort(fileList, new PlayerActivity.ComparatorString());
                        }
                    } else {
                        Log.i(TAG, "listFile is NULL");
                    }

                    if(currentPath.equals(storageRootPath) && checkExternalStorage() == true) {
                        fileList.add(Environment.getExternalStorageDirectory().getAbsolutePath());
                    }

                    if(null != filelistAdapter)
                        filelistAdapter.notifyDataSetChanged();

                    Log.i(TAG, "searchFile, select item, firstpos: " + firstPos);
                    //fileListView.setSelection(firstPos);
                    fileListView.setSelection(selPos);
                    //currentFirstPos = 0;

                } else {
                    currentPlayfile = file.getAbsolutePath();
                    currentFirstPos = fileListView.getFirstVisiblePosition();
                    //currentSelPos = selPos;
                    Log.i(TAG, "searchPlayFile, file is not directory: " + currentPlayfile);
                    // start player activity

                    playFile(currentPlayfile);
                }
            } else {
                Log.i(TAG, "file is error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception");
        }

    }

    private void playFile(String path) {
        //Uri uri = Uri.parse(path);
        Intent intent = new Intent(FileExplorerActivity.this, PlayerActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("uri", path);
        intent.putExtra("uriInfo", bundle);
        startActivity(intent);
    }

    private boolean checkExternalStorage() {
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;

        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)) {
            Log.i(TAG, "checkExternalStorage, MEDIA_MOUNTED");
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.i(TAG, "checkExternalStorage, MEDIA_MOUNTED_READ_ONLY");
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            Log.i(TAG, "checkExternalStorage, No MEDIA_MOUNTED");
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        return mExternalStorageAvailable;
    }
}
