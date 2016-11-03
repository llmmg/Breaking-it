package com.lancelot.prototype;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {

    private final int duration = 3; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 0; // hz

    private final byte generatedSnd[] = new byte[2 * numSamples];

    public static final String EXTRA_MESSAGE = "com.example.lancelot.applicationtest.MESSAGE";

    Handler handler = new Handler();

    Button buttonFreq;
    Button buttonRecorde;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonRecorde = (Button) findViewById(R.id.btnRecorde);
        buttonFreq = (Button) findViewById(R.id.buttonPlay);

        //Play the computed frequency
        buttonFreq.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (freqOfTone > 0) {
                        generateSound();
                    } else {
                        //print that nothing has been record yet
                        TextView harmonicText = (TextView) findViewById(R.id.foundFreq);
                        harmonicText.setText("Please, recorde something first!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //list used to display data
        final List list = Collections.synchronizedList(new ArrayList());

        final HarmonicsData harmonicsData = new HarmonicsData();

        buttonRecorde.setOnTouchListener(new Button.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //When button is down, keep recording data
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new Thread(harmonicsData).start();

                }//when button is released, stop recording and analyse data
                else if (event.getAction() == MotionEvent.ACTION_UP) {

                    harmonicsData.stopProcessing();

//                    list.clear();
//                    list.add(harmonicsData.getSpectre());

                    //store harmonic
                    freqOfTone = harmonicsData.signalEstimation();

                    TextView text = (TextView) findViewById(R.id.foundFreq);
                    text.setText("" + harmonicsData.signalEstimation());

                    //clear old datas
                    harmonicsData.clear();
                }
                return false;
            }
        });
    }

    /**
     * Detect actual sound frequency of the microphone
     */
    public void testLib() {
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                final float pitchInHz = result.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        TextView text = (TextView) findViewById(R.id.textView2);
//                        text.setText("" + pitchInHz);
                        Toast.makeText(MainActivity.this, "" + pitchInHz, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
        dispatcher.addAudioProcessor(p);
        new Thread(dispatcher, "Audio Dispatcher").start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();  // Always call the superclass

        // Stop method tracing that the activity started during onCreate()
        android.os.Debug.stopMethodTracing();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    void genTone() {
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

    void playSound() {
        //16bits=> precisions d'un échantillon
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

}