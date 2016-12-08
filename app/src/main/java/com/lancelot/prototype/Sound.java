package com.lancelot.prototype;

/**
 * Created by Lancelot on 17.11.2016.
 */

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Object that play sine generated sound
 */
public class Sound implements Runnable{

    private final int duration = 2; // seconds
    private final int sampleRate = 44100;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 0; // hz

    private final byte generatedSnd[] = new byte[2 * numSamples];

    private AudioTrack audioTrack;

    Handler handler;

    public Sound(Handler h)
    {
        this.handler=h;
    }

    public void setFreqOfTone(double freq)
    {
        this.freqOfTone=freq;
    }

    //Play the sound
    public void generateSound() {
        // Use a new tread as this can take a while
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                genTone();
                handler.post(new Runnable() {

                    public void run() {
                        playSound();
                    }
                });
            }
        });
        thread.start();
    }

    //Create the sinusoidal sound
    private void genTone() {
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            //y(t)=ASin(2PI*f*t)
            //i===t, (sampleRate/freqOfTone)==T freqOfTone==oscillations/sec,
//          //sampleRate== measures/sec
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            // (2^15 => precision est 2^16/2 pour couvrir l'apmplitude/2 (valeurs négatives))
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    private void playSound() {
        //16bits=> precisions d'un échantillon
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);

        //set loop points
        audioTrack.setLoopPoints(0,numSamples,-1);

        audioTrack.play();
    }

    @Override
    public void run() {
        generateSound();
    }


    public void stop(){
        audioTrack.stop();
        audioTrack.flush();
        audioTrack.release();
    }
}
