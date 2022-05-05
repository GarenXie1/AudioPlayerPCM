package com.garen.audioplayerpcm;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AudioPlayPcm implements Runnable{

    // Audio Player config.
    private static final int AudioPlayerSampleRate = 8000;
    private static final int AudioPlayerFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AudioPlayerChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AudioPlayerStreamType = AudioManager.STREAM_RING;
    private static final int AudioPlayerMode = AudioTrack.MODE_STREAM;
    private static final String playDir = "/storage/emulated/0/audioFile/";
    private static final String playFile = "data.pcm";
    private int minBufferSize = 0;

    private static String TAG = "AudioPlayPcm";
    private AudioTrack mAudioTrack = null;
    private boolean isPlaying = false;

    @Override
    public void run() {

        FileInputStream playFis = null;

        // 4. Starts playing an AudioTrack.
        if(mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            mAudioTrack.play();
        }

        // 实例化 FileInputStream, 用于读取 PCM 文件.
        try {
            playFis = new FileInputStream(playDir + playFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // 创建 字节数组空间，用于存储从文件读取出来的音频数据.
        byte[] bytes = new byte[minBufferSize + 100];
        int len = 0;


        // recieveUDPCommand
        recieveUDPCommand();


        while (isPlaying){

            // 从文件中 读取 minBufferSize 音频数据
            try {
                len = playFis.read(bytes,0,minBufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(len == -1){
                Log.w(TAG,"No more audio data can be read on PCM file.");
                break;
            }

            // 把 音频数据 写入 AudioTrack 中.
            mAudioTrack.write(bytes,0,len);

        }

        // 释放资源
        try {
            playFis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void recieveUDPCommand(){

        InetAddress address = null;
        DatagramSocket ds = null;
        try {
            // 创建接收端的 Socket对象(DatagramSocket)
            address = InetAddress.getByName("192.168.1.139");
            ds = new DatagramSocket(10086,address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // 创建一个数据包，用于接收数据
        byte[] bytes = new byte[1024];
        DatagramPacket dp = new DatagramPacket(bytes,bytes.length);

        // 调用 DatagramSocket对象的方法接收数据
        // blocks until a datagram is received.
        try {
            ds.receive(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 解析数据包，并把数据在控制台显示
        byte[] datas = dp.getData();
        int len = dp.getLength();

        String str = new String(datas,0,len);
        Log.i(TAG, "UDP recieve --> " + str);

        // 关闭接收端
        ds.close();

        // 设置 isPlaying
        if(str.equals("start")){
            isPlaying = true;
        }
    }


    public void startPlaying(){
        // 1. 获取 minimum 播放音频数据缓冲段大小
        minBufferSize = AudioTrack.getMinBufferSize(AudioPlayerSampleRate,AudioPlayerChannelConfig,AudioPlayerFormat);
        Log.i(TAG, "bufferSize --> " + minBufferSize);

        // 2. 初始化 AudioTrack 音频播放器
        mAudioTrack = new AudioTrack(AudioPlayerStreamType, AudioPlayerSampleRate, AudioPlayerChannelConfig,
                AudioPlayerFormat, minBufferSize, AudioPlayerMode);

        // 3. 判断 AudioTrack instance 实例化对象是否初始化成功.
        if(mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED){
            Log.e(TAG, "init AudioTrack Failed.");
        }

        // 开始播放线程.
        new Thread(this).start();
    }


    public void stopPlaying(){

        // getPlayState --> Returns the playback state of the AudioTrack instance.
        if (mAudioTrack != null && mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {

                // 7. Flushes the audio data currently queued for playback
                mAudioTrack.flush();

                // 8. Stops playing the audio data.
                // (MODE_STREAM) audio will stop playing after the last buffer that was written has been played.
                mAudioTrack.stop();
            }

            // 9. Releases the native AudioTrack resources.
            mAudioTrack.release();
            mAudioTrack = null;
        }

        // 设置 isPlaying
        isPlaying = false;
    }
}
