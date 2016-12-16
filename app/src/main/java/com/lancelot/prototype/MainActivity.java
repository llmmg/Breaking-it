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

    private double freqOfTone = 0; // hz

    Button buttonFreq;
    Button buttonRecorde;
    Button buttonStop;
    Button buttonUp;
    Button buttonDown;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Recorde button
        buttonRecorde = (Button) findViewById(R.id.btnRecorde);
        //Play sound button
        buttonFreq = (Button) findViewById(R.id.buttonPlay);
        //Sound stop button
        buttonStop = (Button) findViewById(R.id.buttonStop);

        //Buttons for offset
        //button up
        buttonUp= (Button) findViewById(R.id.btnUpOffset);
        //button down
        buttonDown= (Button) findViewById(R.id.btnDownOffset);

        //sound object used to play sound
        final Sound sound = new Sound(handler);

        //Play the computed frequency
        buttonFreq.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    //the frequency can't be change when sound is playing
                    buttonDown.setEnabled(false);
                    buttonUp.setEnabled(false);
                    //freqOfTone is initialised at 0.
                    if (freqOfTone > 0) {
                        sound.setFreqOfTone(freqOfTone);
//                        sound.generateSound();
                        new Thread(sound).start();

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

        //Stop audio
        buttonStop.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                sound.stop();

                //enable buttons to change frequency
                buttonDown.setEnabled(true);
                buttonUp.setEnabled(true);
            }
        });


        //"Tool" Object to get harmonics frequencies
        final HarmonicsData harmonicsData = new HarmonicsData();

        buttonRecorde.setOnTouchListener(new Button.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //When button is down, keep recording data
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new Thread(harmonicsData).start();

                }//when button is released, stop recording and analyse data
                else if (event.getAction() == MotionEvent.ACTION_UP) {

                    //stop audio processing
                    harmonicsData.stopProcessing();

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

        //increment frequency by 0.5
        buttonUp.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                freqOfTone+=0.5;

                TextView text = (TextView) findViewById(R.id.foundFreq);
                text.setText("" + freqOfTone);
            }
        });

        //decrement frequency by 0.5
        buttonDown.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                freqOfTone-=0.5;
                TextView text = (TextView) findViewById(R.id.foundFreq);
                text.setText("" + freqOfTone);
            }
        });
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

}