package com.lancelot.prototype;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * ---------DEPRECATED------------
 * - The Sound class replace this test class
 * - as Sound class work better.
 * -
 *
 * Created by Lancelot on 17.11.2016.
 * <p>
 * This is a test class.
 * It should be able to generate a sin sound as long as it's running.
 */

public class TestSoundPlay implements Runnable {

    private double freq;
    private AudioTrack mAudioTrack;


    @Override
    public void run() {
        play(freq);
    }

    public void stop() {
        mAudioTrack.stop();
        mAudioTrack.flush();
        mAudioTrack.release();
    }

    public void setFreq(double frequency) {
        this.freq = frequency;
    }

    public void play(double freq) {
        playSound(freq);
    }

    private void playSound(double frequency) {
        // AudioTrack definition
        int mBufferSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_8BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSize, AudioTrack.MODE_STATIC);

        // Sine wave
        double[] mSound = new double[4410];
//        short[] mBuffer = new short[duration];
        short[] mBuffer = new short[44100*2];
        for (int i = 0; i < mSound.length; i++) {
            mSound[i] = Math.sin((2.0 * Math.PI * i / (44100 / frequency)));
            mBuffer[i] = (short) (mSound[i] * Short.MAX_VALUE);
        }


        //TODO: find right begin/end
        Double test=mBuffer.length/frequency;
        Integer size=test.intValue();

        mAudioTrack.write(mBuffer, 0, mSound.length);
//        mAudioTrack.setLoopPoints(0,size, -1);
        mAudioTrack.setLoopPoints(0,size, -1);
        mAudioTrack.play();



//        mAudioTrack.stop();
//        mAudioTrack.release();

    }
}
