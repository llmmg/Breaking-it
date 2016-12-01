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
 * <p>
 * This class can find harmonic frequency of a sound
 */

public class HarmonicsData implements Runnable {


    private ArrayList<Float> spectre;
    private AudioDispatcher myDispatcher;

    /**
     * Constructor
     */
    public HarmonicsData() {
        spectre = new ArrayList<Float>();
    }

    /**
     * spectre getter
     *
     * @return Harmonics frequency List's
     */
    public List getSpectre() {
        return spectre;
    }

    /**
     * Clear value stored in spectre list
     */
    public void clear() {
        spectre.clear();
    }

    //TODO: we may found many value for one sound => need to get most recurents values with range
    //Solution: => do an histogram sorted by CLASS rather than value (so we have a 'range')
    //          =>Round value to 0.5 closest value (and to have a class of amplitude 4)

    /**
     * Get the most recurrent value in spectre list
     * =>Etablish an histogram and extract most recursive value
     * ==>Values are rounded to 0.5 to group them by class
     *
     * @return most recurrent frequency == harmonic frequency
     */
    public float signalEstimation() {
        //map used as histogram
        Map<Float, Integer> histogram = new HashMap<>();
        for (Float key : spectre) {
            key = (float) (Math.round(key * 2) / 2.0);
            Integer val = histogram.get(key);
            //put 1 if map.get(key) return null else increment
            if (val == null) {
                histogram.put(key, 1);
            } else {
                histogram.put(key, histogram.get(key) + 1);
            }
//            map.put(key, val == null ? 1 : val + 1);
        }
        Map.Entry<Float, Integer> max = null;

        for (Map.Entry<Float, Integer> e : histogram.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }
        return max.getKey();
    }


    @Override
    public void run() {
        final SpectralPeakProcessor spectralPeakFollower;
        //TODO: check microphone quality
        spectralPeakFollower = new SpectralPeakProcessor(1024, 0, 22050);
        myDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        myDispatcher.addAudioProcessor(spectralPeakFollower);

        myDispatcher.addAudioProcessor(new AudioProcessor() {
            /**
             * Compute spectral frequency
             * An array of frequencies and one of magnitude is generated from spectralPeakProcessor object.
             * Then the value with the highest magnitude == harmonic frequency
             * index of two arrays is shared => magnitudes[i] is the magnitude of frequencies[i]...)
             * @param audioEvent
             * @return
             */
            @Override
            public boolean process(AudioEvent audioEvent) {
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

    /**
     * Stop AudioDispatcher processing (sound recordind)
     */
    public void stopProcessing() {
        myDispatcher.stop();
    }
}
