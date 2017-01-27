package com.lancelot.prototype;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private double freqOfTone = 0; // hz
    private final int REQUEST_ACCESS_MIC = 1;

    Button buttonPlay;
    Button buttonRecorde;
    Button buttonStop;
    Button buttonUp;
    Button buttonDown;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Ask for microphone permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_ACCESS_MIC);
        }

        //Recorde button
        buttonRecorde = (Button) findViewById(R.id.btnRecorde);
        //Play sound button
        buttonPlay = (Button) findViewById(R.id.buttonPlay);
        //by default, buttonPlay is disabled
        buttonPlay.setEnabled(false);

        //Sound stop button
        buttonStop = (Button) findViewById(R.id.buttonStop);
        //by default, stop btn is disabled, it's enabled when play button is pressed
        buttonStop.setEnabled(false);

        //Buttons for offset
        //button up
        buttonUp = (Button) findViewById(R.id.btnUpOffset);
        //button down
        buttonDown = (Button) findViewById(R.id.btnDownOffset);

        //sound object used to play sound
        final Sound sound = new Sound(handler);

        //Play the computed frequency
        buttonPlay.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    //disable this button
                    buttonPlay.setEnabled(false);
                    //enable stop button
                    buttonStop.setEnabled(true);
                    //disable rec button
                    buttonRecorde.setEnabled(false);

                    //the frequency can't be change when sound is playing
                    buttonDown.setEnabled(false);
                    buttonUp.setEnabled(false);
                    //freqOfTone is initialised at 0.
                    if (freqOfTone > 0) {
                        sound.setFreqOfTone(freqOfTone);
                        //play sound
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

                //disable this button
                buttonStop.setEnabled(false);
                //enable rec button
                buttonRecorde.setEnabled(true);
                //enable play button
                buttonPlay.setEnabled(true);

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

                    //disable play button
                    buttonPlay.setEnabled(false);

                }//when button is released, stop recording and analyse data
                else if (event.getAction() == MotionEvent.ACTION_UP) {

                    //stop audio processing
                    harmonicsData.stopProcessing();

                    //store harmonic
                    freqOfTone = harmonicsData.signalEstimation();

                    TextView text = (TextView) findViewById(R.id.foundFreq);
                    text.setText("" + freqOfTone);

                    //clear old datas
                    harmonicsData.clear();

                    //Enable play button
                    if (freqOfTone > 0) {
                        buttonPlay.setEnabled(true);
                    }
                }
                return false;
            }
        });

        //increment frequency by 0.5
        buttonUp.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                freqOfTone += 0.5;

                TextView text = (TextView) findViewById(R.id.foundFreq);
                text.setText("" + freqOfTone);
            }
        });

        //decrement frequency by 0.5
        buttonDown.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                freqOfTone -= 0.5;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_MIC:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_LONG;
                    Toast.makeText(context, "Microphone permission granted!", duration).show();
                } else {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_LONG;
                    Toast.makeText(context, "Microphone permission refused! application closed!", duration).show();

                    //close application
                    this.finish();
                }
                return;
        }
    }

}