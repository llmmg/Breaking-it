package com.lancelot.prototype;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;

/**
 * Created by Lancelot on 27.10.2016.
 */

public class HarmonicsData implements Runnable {

    public List getSpectre() {
        return spectre;
    }

    private List spectre;
    private AudioDispatcher myDispatcher;

    public HarmonicsData() {
        spectre = new ArrayList<Float>();
    }


    @Override
    public void run() {
        Log.d("test:", "testLib2");
        final SpectralPeakProcessor spectralPeakFollower;
        spectralPeakFollower = new SpectralPeakProcessor(1024, 0, 22050);
//        final AudioDispatcher dispatcher2 = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        myDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
//        dispatcher2.addAudioProcessor(spectralPeakFollower);
        myDispatcher.addAudioProcessor(spectralPeakFollower);

//        dispatcher2.addAudioProcessor(new AudioProcessor() {
        myDispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                Log.d("test:", "process");
                //TODO: replace runOnUiThread by simple thread beacause: Data wont be displayed in real time
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("test:", "run");
                        //TODO: Get/sort the three values with the highest magnitudes
                        //for a maybe feature to make user able to choose which harmonic to play

                        float[] magnitudes = spectralPeakFollower.getMagnitudes();
                        float[] frequencies = spectralPeakFollower.getFrequencyEstimates();

                        //min freq value
                        double min = magnitudes[0];
                        double tmpMin = min;

                        //index of min value
                        int minId = 0;

                        //max freq value
                        double max = 0;
                        double tmp = 0;

                        //index of max value
                        int maxInd = 0;
                        for (int i = 0; i < frequencies.length; i++) {
                            max = Math.max(magnitudes[i], max);
                            if (max != tmp) {
                                maxInd = i;
                                tmp = max;
                            }
                            //is the min value usefull? maybe for later data process...
                            min = Math.min(magnitudes[i], min);
                            if (min != tmpMin) {
                                minId = i;
                                tmpMin = min;
                            }
                        }

                        //store data (harmonic)
                        spectre.add(frequencies[maxInd]);

                        //!\\\ frequencies aren't the harmonique frequencies????
                        // ===> the frequencies with the higest magnitude are the harmoniques
                        Log.d("FREQUENCE:", "" + frequencies[maxInd]);
                    }
                });
                t.run();
                return true;
            }

            @Override
            public void processingFinished() {
                Log.d("test", "processingFinished");

            }
        });


//        new Thread(dispatcher2, "Audio Dispatcher2").start();
        new Thread(myDispatcher, "Audio Dispatcher2").start();
//        return dispatcher2;
    }

    public void stopProcessing()
    {
        myDispatcher.stop();
    }
}
