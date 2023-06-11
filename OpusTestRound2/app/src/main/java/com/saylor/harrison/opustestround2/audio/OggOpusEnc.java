/**
 * © Copyright IBM Corporation 2015
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.saylor.harrison.opustestround2.audio;

import android.os.Environment;
import android.util.Log;

import com.saylor.harrison.opustestround2.conf.SpeechConfiguration;
import com.saylor.harrison.opustestround2.opus.JNAOpus;
import com.saylor.harrison.opustestround2.opus.OggOpus;
import com.saylor.harrison.opustestround2.opus.OpusWriter;
import com.sun.jna.ptr.PointerByReference;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Ogg Opus Encoder
 */
public class OggOpusEnc extends OpusWriter implements ISpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    /** Data writer */
    private OpusWriter writer = null;
    /** Opus encoder reference */
    private PointerByReference opusEncoder;

    /**
     * Constructor
     */
    public OggOpusEnc() {
    }

    /**
     * For WebSocketClient
     * @param uploader
     * @throws IOException
     */

    @Override
    public void initEncoderWithUploader(IChunkUploader uploader) throws IOException {
        writer = new OpusWriter(uploader);
        IntBuffer error = IntBuffer.allocate(4);
        this.opusEncoder = JNAOpus.INSTANCE.opus_encoder_create(
                SpeechConfiguration.SAMPLE_RATE,
                SpeechConfiguration.AUDIO_CHANNELS,
                JNAOpus.OPUS_APPLICATION_AUDIO,
                error);
    }

    /**
     * When the encode begins
     */
    @Override
    public void onStart() {
        writer.writeHeader("encoder=Lavc56.20.100 libopus");
    }

    /**
     * Encode raw audio data into Opus format then call OpusWriter to write the Ogg packet
     *
     * @param rawAudio
     * @return
     * @throws IOException
     */
    @Override
    public int encodeAndWrite(byte[] rawAudio) throws IOException {
        int uploadedAudioSize = 0;
        ByteArrayInputStream ios = new ByteArrayInputStream(rawAudio);
        byte[] data = new byte[SpeechConfiguration.FRAME_SIZE * 2];
        int bufferSize, read;
        while ((read = ios.read(data)) > 0) {
            bufferSize = read;
            byte[] pcmBuffer = Arrays.copyOfRange(data, 0, read);
            ShortBuffer shortBuffer = ShortBuffer.allocate(bufferSize);
            for (int i = 0; i < read; i += 2) {
                int b1 = pcmBuffer[i] & 0xff;
                int b2 = pcmBuffer[i + 1] << 8;
                shortBuffer.put((short) (b1 | b2));
            }
            shortBuffer.flip();

            ByteBuffer opusBuffer = ByteBuffer.allocate(bufferSize);
            int opus_encoded = JNAOpus.INSTANCE.opus_encode(this.opusEncoder, shortBuffer, SpeechConfiguration.FRAME_SIZE,
                    opusBuffer, bufferSize);
            if (opus_encoded > 0 && opus_encoded <= opusBuffer.capacity()) {
                opusBuffer.rewind();
                byte[] opusData = new byte[opus_encoded];
                opusBuffer.get(opusData, 0, opusData.length);
                uploadedAudioSize += opusData.length;
                writer.writePacket(opusData, 0, opusData.length);
                System.out.println("This is where I'd write some data. " + uploadedAudioSize + " to be specific.");
            } else {
                Log.e("OpusTest", "opus_encoded:" + opus_encoded);
            }
        }
        ios.close();
        return uploadedAudioSize;
    }


    public int encodeAndWriteBack(byte[] rawAudio) throws IOException {
        //初始化一个变量用于记录已上传的音频数据大小。
        int uploadedAudioSize = 0;
        //创建一个ByteArrayInputStream对象，用于从原始音频数据中读取数据。
        ByteArrayInputStream ios = new ByteArrayInputStream(rawAudio);
        //创建一个缓冲区，用于存储每次从输入流中读取的数据。
        byte[] data = new byte[SpeechConfiguration.FRAME_SIZE * 2];
        //定义两个变量，bufferSize表示缓冲区大小，read表示每次从输入流中实际读取的数据量。
        int bufferSize, read;
        //循环读取输入流中的数据，每次读取的数据存储在data数组中，并记录实际读取的数据量到read变量中。
        while ((read = ios.read(data)) > 0) {
            bufferSize = read;
            //创建一个大小为read的byte[]数组，用于存储实际读取的PCM数据。
            byte[] pcmBuffer = new byte[read];
            //将从输入流中读取的数据复制到pcmBuffer数组中。
            System.arraycopy(data, 0, pcmBuffer, 0, read);
            //创建一个ShortBuffer对象，用于存储转换后的PCM数据。
            ShortBuffer shortBuffer = ShortBuffer.allocate(bufferSize);
            //循环遍历实际读取的 PCM 数据，每次增加2个字节（一个 short 类型的大小）。
            for (int i = 0; i < read; i += 2) {
                //获取 PCM 数据中的第一个字节。
                int b1 = pcmBuffer[i] & 0xff;
                //获取 PCM 数据中的第二个字节，并左移8位。
                int b2 = pcmBuffer[i + 1] << 8;
                //将两个字节合并为一个 short 类型，并将其添加到 shortBuffer 中。
                shortBuffer.put((short) (b1 | b2));
            }
            //翻转shortBuffer，准备从头开始读取数据。
            shortBuffer.flip();
            //创建一个ByteBuffer对象，用于存储编码后的Opus数据。
            ByteBuffer opusBuffer = ByteBuffer.allocate(bufferSize);
            //调用Opus编码器对PCM数据进行编码，并将编码后的数据存储到opusBuffer中，返回编码后的数据长度。
            int opus_encoded = JNAOpus.INSTANCE.opus_encode(this.opusEncoder, shortBuffer, SpeechConfiguration.FRAME_SIZE,
                    opusBuffer, bufferSize);
            // 检查是否成功编码，如果编码长度大于0，则说明编码成功。
            if (opus_encoded > 0) {
                //设置opusBuffer的读取位置为编码后的数据长度。
                opusBuffer.position(opus_encoded);
                //翻转opusBuffer，准备从头开始读取数据。
                opusBuffer.flip();
                //创建一个大小为opusBuffer剩余数据长度的byte[]数组，用于存储编码后的Opus数据。
                byte[] opusData = new byte[opusBuffer.remaining()];
                //将 opusBuffer 中的数据读取到 opusData 数组中。
                opusBuffer.get(opusData, 0, opusData.length);
                //将成功编码后的 Opus 数据长度添加到已上传音频大小中。
                uploadedAudioSize += opusData.length;
                System.out.println("This is where I'd write some data. " + uploadedAudioSize + " to be specific.");
                writer.writePacket(opusData, 0, opusData.length);
            } else {
                Log.e("OpusTest", "opus_encoded:" + opus_encoded);
            }
        }
        ios.close();
        return uploadedAudioSize;
    }


    /**
     * Close writer
     */
    @Override
    public void close() {
        try {
            writer.close();
            JNAOpus.INSTANCE.opus_encoder_destroy(this.opusEncoder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
