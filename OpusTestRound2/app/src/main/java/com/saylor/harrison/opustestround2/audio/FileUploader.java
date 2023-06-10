package com.saylor.harrison.opustestround2.audio;

import android.util.Log;

import com.saylor.harrison.opustestround2.ISpeechDelegate;
import com.saylor.harrison.opustestround2.conf.SpeechConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ================================================
 * 作    者：ZhouZhengyi
 * 创建日期：2023/6/8 10:38
 * 描    述：
 * 修订历史：
 * ================================================
 */
public class FileUploader implements IChunkUploader{

    private static final String TAG = "FileUploader";

    private ISpeechEncoder encoder = null;
    private SpeechConfiguration sConfig = null;
    private FileOutputStream mFileOutputStream;

    public FileUploader(SpeechConfiguration config, File file){
        this.sConfig = config;
        try {
            mFileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        if(sConfig.audioFormat.equals(SpeechConfiguration.AUDIO_FORMAT_DEFAULT)) {
            this.encoder = new RawEnc();
        }
        else if(sConfig.audioFormat.equals(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS)){
            this.encoder = new OggOpusEnc();
        }
    }

    @Override
    public int onHasData(byte[] buffer) {
        int uploadedAudioSize = 0;
        try {
            uploadedAudioSize = encoder.encodeAndWrite(buffer);
            Log.d(TAG, "onHasData: " + uploadedAudioSize + " " + buffer.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return uploadedAudioSize;
    }

    @Override
    public boolean isUploadPrepared() {
        return true;
    }

    @Override
    public void upload(byte[] data) {
        try {
            mFileOutputStream.write(data, 0, data.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if(mFileOutputStream!=null){
            try {
                mFileOutputStream.flush();
                mFileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void prepare() {
        try {
            this.encoder.initEncoderWithUploader(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.encoder.onStart();
    }

    @Override
    public void setDelegate(ISpeechDelegate delegate) {

    }

    @Override
    public void close() {
        encoder.close();
    }
}
