package com.saylor.harrison.opustestround2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.saylor.harrison.opustestround2.audio.FileUploader;
import com.saylor.harrison.opustestround2.audio.IChunkUploader;
import com.saylor.harrison.opustestround2.audio.OggOpusEnc;
import com.saylor.harrison.opustestround2.audio.WebSocketUploader;
import com.saylor.harrison.opustestround2.conf.SpeechConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private FileUploader fileUploader;
    private AudioRecord audioRecord;

    private volatile boolean isStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        File cacheDir = MainActivity.this.getCacheDir();
        File file = new File(cacheDir, "test.ogg");
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        fileUploader = new FileUploader(new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS, true), file);
        fileUploader.prepare();
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //InputStream stream = getResources().openRawResource(R.raw.where_is_the_pool);
                //byte[] array = IOUtils.toByteArray(stream);
              new Thread(new Runnable() {
                @Override
                public void run() {
                    audioRecord.release();
                    audioRecord = null;
                    if(audioRecord == null){
                      audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
                    }
                  audioRecord.startRecording();
                  byte[] buffer = new byte[BUFFER_SIZE];
                  isStop = false;
                  while (true) {
                    if(isStop){
                        audioRecord.stop();
                        audioRecord.release();
                      fileUploader.close();
                      break;
                    }
                    audioRecord.read(buffer, 0, BUFFER_SIZE);
                    fileUploader.onHasData(buffer);
                  }
                }
              }).start();
            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStop = true;
            }
        });
        //MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.where_is_the_pool);
        //mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == 200) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 200);
                    return;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 200) {
            checkPermission();
        }
    }
}
