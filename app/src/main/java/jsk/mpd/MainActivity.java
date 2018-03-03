package jsk.mpd;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int PLAY_REQUEST_CODE = 0;
    private static final int READ_REQUEST_CODE = 42;
    private static final int PERMISSION_REQUEST_CODE = 321;

    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private AlertDialog dialog;

    private TextView enter_text;
    private TextView main_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        enter_text = (TextView)findViewById(R.id.enter_text);
        main_text = (TextView)findViewById(R.id.main_text);
        enter_text.setOnClickListener(this);
        main_text.setOnClickListener(this);

        Log.i(TAG, "onCreate exit");
    }

    private void showDialogTipUserRequestPermission() {

        new AlertDialog.Builder(this)
                .setTitle("Need External Storage Permission")
                .setMessage("Allow MediaPlayerDemo to Access External Storage")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
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

        Log.i(TAG, "onResume");
        //handleDataFormPlayer();
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

        switch(v.getId()) {
            case R.id.enter_text:
                performFileSearch();
                break;

            case R.id.main_text:
                Intent intent = new Intent(MainActivity.this, FileExplorerActivity.class);
                startActivity(intent);
                break;

            default:
                break;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(TAG, "onActivityResult, requestCode: "+ requestCode );

        switch(requestCode) {
            case READ_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("uri", uri.toString());
                        intent.putExtra("uriInfo", bundle);
                        startActivityForResult(intent, PLAY_REQUEST_CODE);
                    }
                }
                break;

            case PLAY_REQUEST_CODE:
                //performFileSearch();
                break;

            default:
                break;
        }
    }

            @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void performFileSearch() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void handleDataFormPlayer() {
        String name = this.getIntent().getStringExtra("name");
        Log.i(TAG, "get name: " + name);
        if(name == "fromPlayer") {
            //performFileSearch();
        }
    }
}
