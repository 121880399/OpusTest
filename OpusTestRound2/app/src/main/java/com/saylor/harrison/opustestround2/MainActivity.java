package com.saylor.harrison.opustestround2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.saylor.harrison.opustestround2.audio.FileUploader;
import com.saylor.harrison.opustestround2.ogg.OpusApplication;
import com.saylor.harrison.opustestround2.ogg.OpusEncoder;
import com.saylor.harrison.opustestround2.ogg.OpusException;
import com.saylor.harrison.opustestround2.ogg.OpusSignal;

import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusInfo;
import org.gagravarr.opus.OpusTags;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 48000;
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
        final File file = new File(cacheDir, "output.ogg");
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //InputStream stream = getResources().openRawResource(R.raw.where_is_the_pool);
                //byte[] array = IOUtils.toByteArray(stream);
              new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
                        audioRecord.release();
                        audioRecord = null;
                        if (audioRecord == null) {
                            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
                        }
                        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusApplication.OPUS_APPLICATION_AUDIO);
                        encoder.setBitrate(64000);
                        encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
                        encoder.setComplexity(0);

                        FileOutputStream fileOut = new FileOutputStream(file);
                        OpusInfo info = new OpusInfo();
                        info.setNumChannels(1);
                        info.setSampleRate(48000);
                        OpusTags tags = new OpusTags();
                        OpusFile file = new OpusFile(fileOut, info, tags);
                        int packetSamples = 960;
                        byte[] inBuf = new byte[packetSamples * 2];
                        byte[] data_packet = new byte[packetSamples * 2];
                        audioRecord.startRecording();
                        isStop = false;
                        while (!isStop) {
                            int bytesRead = audioRecord.read(inBuf, 0, inBuf.length);
                            Log.e("OpusTest","bytesRead:"+bytesRead);
                            short[] pcm = BytesToShorts(inBuf, 0, bytesRead);
                            Log.e("OpusTest","pcm:"+pcm.length);
                            int bytesEncoded = encoder.encode(pcm, 0, packetSamples, data_packet, 0, data_packet.length);
                            Log.e("OpusTest","bytesEncoded:"+bytesEncoded);
                            byte[] packet = new byte[bytesEncoded];
                            System.arraycopy(data_packet, 0, packet, 0, bytesEncoded);
                            OpusAudioData data = new OpusAudioData(packet);
                            file.writeAudioData(data);
                        }
                        audioRecord.stop();
                        audioRecord.release();
                        file.close();
                        fileOut.flush();
                        fileOut.close();
                    }catch (Exception e){
                        e.printStackTrace();
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
        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(file.getPath());
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            Log.e("OpusTest","onError what:"+what + " extra:"+extra);
                            return false;
                        }
                    });
                    mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                        @Override
                        public boolean onInfo(MediaPlayer mp, int what, int extra) {
                            Log.e("OpusTest","onInfo what:"+what + " extra:"+extra);
                            return false;
                        }
                    });
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.btn_translate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputStream inputStream = MainActivity.this.getResources().openRawResource(R.raw.test);

                    OpusEncoder encoder = new OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
                    encoder.setBitrate(64000);
                    encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
                    encoder.setComplexity(10);

                    FileOutputStream fileOut = new FileOutputStream(file);
                    OpusInfo info = new OpusInfo();
                    info.setNumChannels(2);
                    info.setSampleRate(48000);
                    OpusTags tags = new OpusTags();
                    //tags.setVendor("Concentus");
                    //tags.addComment("title", "A test!");
                    OpusFile file = new OpusFile(fileOut, info, tags);
                    int packetSamples = 960;
                    byte[] inBuf = new byte[packetSamples * 2 *2];
                    byte[] data_packet = new byte[1275];
                    long start = System.currentTimeMillis();
                    while (inputStream.available() >= inBuf.length) {
                        int bytesRead = inputStream.read(inBuf, 0, inBuf.length);
                        short[] pcm = BytesToShorts(inBuf, 0, inBuf.length);
                        int bytesEncoded = encoder.encode(pcm, 0, packetSamples, data_packet, 0, 1275);
                        byte[] packet = new byte[bytesEncoded];
                        System.arraycopy(data_packet, 0, packet, 0, bytesEncoded);
                        OpusAudioData data = new OpusAudioData(packet);
                        file.writeAudioData(data);
                    }
                    file.close();

                    long end = System.currentTimeMillis();
                    System.out.println("Time was " + (end - start) + "ms");
                    inputStream.close();
                    //fileOut.close();
                    System.out.println("Done!");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (OpusException e) {
                    System.out.println(e.getMessage());
                }
            }
        });
    }

    public static short[] BytesToShorts(byte[] input) {
        return BytesToShorts(input, 0, input.length);
    }

    /// <summary>
    /// Converts interleaved byte samples (such as what you get from a capture device)
    /// into linear short samples (that are much easier to work with)
    /// </summary>
    /// <param name="input"></param>
    /// <returns></returns>
    public static short[] BytesToShorts(byte[] input, int offset, int length) {
        short[] processedValues = new short[length / 2];
        for (int c = 0; c < processedValues.length; c++) {
            short a = (short) (((int) input[(c * 2) + offset]) & 0xFF);
            short b = (short) (((int) input[(c * 2) + 1 + offset]) << 8);
            processedValues[c] = (short) (a | b);
        }

        return processedValues;
    }

    /// <summary>
    /// Converts linear short samples into interleaved byte samples, for writing to a file, waveout device, etc.
    /// </summary>
    /// <param name="input"></param>
    /// <returns></returns>
    public static byte[] ShortsToBytes(short[] input) {
        return ShortsToBytes(input, 0, input.length);
    }

    /// <summary>
    /// Converts linear short samples into interleaved byte samples, for writing to a file, waveout device, etc.
    /// </summary>
    /// <param name="input"></param>
    /// <returns></returns>
    public static byte[] ShortsToBytes(short[] input, int offset, int length) {
        byte[] processedValues = new byte[length * 2];
        for (int c = 0; c < length; c++) {
            processedValues[c * 2] = (byte) (input[c + offset] & 0xFF);
            processedValues[c * 2 + 1] = (byte) ((input[c + offset] >> 8) & 0xFF);
        }

        return processedValues;
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

    private void backCode(){
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
//        fileUploader = new FileUploader(new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS, true), file);
//        fileUploader.prepare();
//        audioRecord.release();
//        audioRecord = null;
//        if(audioRecord == null){
//            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
//        }
//        audioRecord.startRecording();
//        byte[] buffer = new byte[SpeechConfiguration.FRAME_SIZE*2];
//        isStop = false;
//        while (true) {
//            if(isStop){
//                audioRecord.stop();
//                audioRecord.release();
//                fileUploader.close();
//                break;
//            }
//            audioRecord.read(buffer, 0, buffer.length);
//            fileUploader.onHasData(buffer);
    }
}
