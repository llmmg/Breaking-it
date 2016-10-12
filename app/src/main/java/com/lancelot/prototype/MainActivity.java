package com.lancelot.prototype;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.Console;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetector;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {

    private final int duration = 3; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 440; // hz

    private final byte generatedSnd[] = new byte[2 * numSamples];

    public static final String EXTRA_MESSAGE = "com.example.lancelot.applicationtest.MESSAGE";

    Handler handler = new Handler();

    Button buttonFreq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonFreq = (Button) findViewById(R.id.buttonPlay);

        buttonFreq.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    EditText editText = (EditText) findViewById(R.id.textFreq);
                    String value = editText.getText().toString();
                    if (!value.isEmpty()) {
                        double newFrequency = Double.parseDouble(value);

                        //read text value and set the frequency with it
                        freqOfTone = newFrequency;
                    }
                    generateSound();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //-------------TEST----------------
        //test of TarsosDSP library
        //testLib();
        testLib2();
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
                        TextView text = (TextView) findViewById(R.id.textView1);
                        text.setText("" + pitchInHz);
                    }
                });
            }
        };
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
        dispatcher.addAudioProcessor(p);
        new Thread(dispatcher, "Audio Dispatcher").start();

    }

    public void testLib2() {
        final SpectralPeakProcessor spectralPeakFollower;
        spectralPeakFollower = new SpectralPeakProcessor(1024, 0, 22050);
        AudioDispatcher dispatcher2 = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        dispatcher2.addAudioProcessor(spectralPeakFollower);
        dispatcher2.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        float[] magnitudes = spectralPeakFollower.getMagnitudes();
                        float[] frequencies = spectralPeakFollower.getFrequencyEstimates();

                        double min=magnitudes[0];
                        double tmpMin=min;
                        int minId=0;
                        double max = 0;
                        double tmp = 0;
                        int maxInd = 0;
                        for (int i = 0; i < frequencies.length; i++) {
                            max = Math.max(magnitudes[i], max);
                            if (max != tmp) {
                                maxInd = i;
                                tmp = max;
                            }
                            min=Math.min(magnitudes[i],min);
                            if(min!=tmpMin)
                            {
                                minId=i;
                                tmpMin=min;
                            }

                        }
                        //!\\\ frequencies aren't the harmonique frequencies????
                        // ===> the frequencies with the higest magnitude are the harmoniques
                        TextView text = (TextView) findViewById(R.id.textView1);
                        text.setText("" + frequencies[maxInd]+" Magnitude max/min: "+max+"/"+min);
                        Log.d("FREQUENCE:", "" + frequencies[maxInd]);

                    }
                });

                return true;
            }

            @Override
            public void processingFinished() {

            }
        });

        new Thread(dispatcher2, "Audio Dispatcher").start();
//        dispatcher2.run();

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


    //======> A COMPRENDRE
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
