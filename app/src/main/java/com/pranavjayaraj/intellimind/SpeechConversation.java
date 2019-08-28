package com.pranavjayaraj.intellimind;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pranavjayaraj.intellimind.Database.DatabaseHandler;
import com.pranavjayaraj.intellimind.Recent.RecentAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpeechConversation extends AppCompatActivity implements VoiceView.OnRecordListener {
    private static String TAG = "SpeechConversation";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private VoiceView mStartStopBtn;

    private CloudSpeechService mCloudSpeechService;
    private VoiceRecorder mVoiceRecorder;
    private boolean mIsRecording = false;

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    private String mSavedText;
    CustomAutoCompleteView search;
    private Handler mHandler;

    // adapter for auto-complete
    ArrayAdapter<String> myAdapter;

    // for database operations
    DatabaseHandler databaseH;

    // just to add some initial value
    String[] item = new String[] {"Please search..."};

    ImageButton imageButton;

    RecyclerView recyclerView;

    RecentAdapter recentAdapter;

    ArrayList<String> arrPackage = new ArrayList<String>();

    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("USER",MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.speechlayout);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        editor = sharedPreferences.edit();
        search = (CustomAutoCompleteView) findViewById(R.id.search);
        imageButton = (ImageButton) findViewById(R.id.search_icon);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertSearchData();
                SaveToRecent();
            }
        });
        initViews();
        try{

            // instantiate database handler
            databaseH = new DatabaseHandler(SpeechConversation.this);

            // put sample data to database
            insertSampleData();

            // autocompletetextview is in activity_main.xml
            search = (CustomAutoCompleteView) findViewById(R.id.search);

            // add the listener so it will tries to suggest while the user types
            search.addTextChangedListener(new CustomAutoCompleteTextChangedListener(this));

            // set our adapter
            myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, item);
            search.setAdapter(myAdapter);

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ReadRecent();
    }

    // this function is used in CustomAutoCompleteTextChangedListener.java
    public String[] getItemsFromDb(){

        // add items on the array dynamically
        List<MyObject> products = databaseH.read();
        int rowCount = products.size();

        String[] item = new String[rowCount];
        int x = 0;

        for (MyObject record : products) {

            item[x] = record.objectName;
            x++;
        }

        return item;
    }

    public void SaveToRecent(){
        Gson gson = new Gson();
        String json = sharedPreferences.getString("Set", "");
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        if (!json.isEmpty())
        {
            arrPackage = gson.fromJson(json, type);
            if(arrPackage!=null) {
                if (arrPackage.size() >= 5) {
                    arrPackage.remove(0);
                }
        }
        }
        arrPackage.add(search.getText().toString());
        String json2 = gson.toJson(arrPackage);
        editor.putString("Set",json2);
        editor.commit();
       Display();
    }

    public void ReadRecent()
    {
        // Read the reverse queue from sharedpref
        Gson gson = new Gson();
        String json = sharedPreferences.getString("Set", "");
        if (json.isEmpty()) {
        }
        else {
            Type type = new TypeToken<ArrayList<String>>() {
            }.getType();
            arrPackage = gson.fromJson(json, type);
           Display();
        }
    }

    public void insertSampleData(){

        // CREATE
        databaseH.create( new MyObject("who is the CEO of Microsoft") );
        databaseH.create( new MyObject("who is the CEO of Google") );
        databaseH.create( new MyObject("Who is the Prime minister of India") );
        databaseH.create( new MyObject("Who is the President of America") );

    }

    public void insertSearchData()
    {
        databaseH.create(new MyObject(search.getText().toString()));
    }

    @Override
    public void onStart() {
        super.onStart();
        // Prepare Cloud Speech API
        bindService(new Intent(this, CloudSpeechService.class), mServiceConnection,
                BIND_AUTO_CREATE);
        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onStop() {

        // Stop listening to voice
        stopVoiceRecorder();

        // Stop Cloud Speech API
        if (mCloudSpeechService != null) {
            mCloudSpeechService.removeListener(mCloudSpeechServiceListener);
            unbindService(mServiceConnection);
            mCloudSpeechService = null;
        }

        super.onStop();
    }

    private void initViews() {

        mSavedText = "Hello";
        mStartStopBtn = (VoiceView) findViewById(R.id.recordButton);
        mStartStopBtn.setOnRecordListener(this);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private final CloudSpeechService.Listener mCloudSpeechServiceListener = new CloudSpeechService.Listener() {
        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            if (isFinal) {
                mVoiceRecorder.dismiss();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFinal&&(!search.getText().equals(""))) {
                        Log.d(TAG, "Final Response : " + text);
                        insertSearchData();
                        SaveToRecent();
                        search.setText("");
                        stopVoiceRecorder();
                        mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);
                        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.off);//Create MediaPlayer object with MP3 file under res/raw folder
                        mPlayer.start();//Start playing the music
                    } else {
                        if (text.toLowerCase().contains("search"))
                        {
                            insertSearchData();
                            SaveToRecent();
                            search.setText("");
                            stopVoiceRecorder();
                            mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);
                            mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.off);//Create MediaPlayer object with MP3 file under res/raw folder
                            mPlayer.start();//Start playing the music
                        }
                        else if(text.toLowerCase().contains("clear"))
                        {
                            search.setText("");
                        }
                        else {
                            search.setText(text);
                        }
                    }
                }
            });
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mCloudSpeechService = CloudSpeechService.from(binder);
            mCloudSpeechService.addListener(mCloudSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCloudSpeechService = null;
        }

    };

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mCloudSpeechService != null) {
                mCloudSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(final byte[] buffer, int size) {
            if (mCloudSpeechService != null) {
                mCloudSpeechService.recognize(buffer, size);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int amplitude = (buffer[0] & 0xff) << 8 | buffer[1];
                    double amplitudeDb3 = 20 * Math.log10((double)Math.abs(amplitude) / 32768);
                    float radius2 = (float) Math.log10(Math.max(1, amplitudeDb3)) * dp2px(SpeechConversation.this, 20);
                    mStartStopBtn.animateRadius(radius2 * 10);
                }
            });
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (mCloudSpeechService != null) {
                mCloudSpeechService.finishRecognizing();
            }
        }

    };

    @Override
    public void onRecordStart() {
        startStopRecording();
    }

    @Override
    public void onRecordFinish() {
        mPlayer.stop();
        mPlayer.release();
        startStopRecording();
    }

    private void startStopRecording() {

        Log.d(TAG, "# startStopRecording # : " + mIsRecording);
        if (mIsRecording) {
            mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);
            mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.off);//Create MediaPlayer object with MP3 file under res/raw folder
            mPlayer.start();//Start playing the music
            stopVoiceRecorder();
        } else {
            mStartStopBtn.changePlayButtonState(VoiceView.STATE_RECORDING);
            mPlayer = MediaPlayer.create(getApplicationContext(),R.raw.on);//Create MediaPlayer object with MP3 file under res/raw folder
            mPlayer.start();//Start playing the music
            startVoiceRecorder();
        }
    }
    private void startVoiceRecorder() {

        Log.d(TAG, "# startVoiceRecorder #");
        mIsRecording = true;
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    MediaPlayer mPlayer;

    private void stopVoiceRecorder() {
        Log.d(TAG, "# stopVoiceRecorder #");
        mIsRecording = false;
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showPermissionMessageDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("This app needs to record audio and recognize your speech")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
            }
        }).create();

        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public static int dp2px(Context context, int dp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context
                .getResources().getDisplayMetrics());
        return px;
    }

    void Display()
    {
        recentAdapter = new RecentAdapter(arrPackage);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recentAdapter);
        recentAdapter.notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }
}
