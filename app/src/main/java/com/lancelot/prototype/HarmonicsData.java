package com.lancelot.prototype;

import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private double computedHarmonic;

    private ArrayList<Float> spectre;
    private AudioDispatcher myDispatcher;

    public HarmonicsData() {
        spectre = new ArrayList<Float>();
    }

    /**
     * Clear value
     */
    public void clear() {
        spectre.clear();
    }

    //TODO: we may found many value for one sound => need to get most recurents values with range
    //Solution: => do an histogram sorted by CLASS rather than value (so we have a 'range')
    //          =>Round value to 0.5 closest value (and to have a class of amplitude 4)
    public float signalEstimation() {
        Map<Float, Integer> map = new HashMap<>();
        for (Float key : spectre) {
            key=(float)(Math.round(key*2)/2.0);
            Integer val = map.get(key);
            //put 1 if map.get(key) return null else increment
            if (val == null) {
                map.put(key, 1);
            } else {
                map.put(key, map.get(key) + 1);
            }
//            map.put(key, val == null ? 1 : val + 1);
        }

        Map.Entry<Float, Integer> max=null;

        for (Map.Entry<Float, Integer> e : map.entrySet()) {
            if (max==null || e.getValue() > max.getValue())
                max = e;
        }
        return max.getKey();
    }

    @Override
    public void run() {
        final SpectralPeakProcessor spectralPeakFollower;
        spectralPeakFollower = new SpectralPeakProcessor(1024, 0, 22050);
        myDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        myDispatcher.addAudioProcessor(spectralPeakFollower);

        myDispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                //TODO: replace runOnUiThread by simple thread beacause: Data wont be displayed in real time
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
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

                        // ===> the frequencies with the higest magnitude are the harmoniques
//                        Log.d("FREQUENCE:", "" + frequencies[maxInd]);
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

        new Thread(myDispatcher, "Audio Dispatcher2").start();
    }

    public void stopProcessing() {
        myDispatcher.stop();
    }
}
