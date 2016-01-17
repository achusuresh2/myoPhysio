package com.example.hrishikeshsuresh.myophysio1;

import android.content.Intent;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import org.w3c.dom.Text;


public class MainActivity extends ActionBarActivity {

    private TextView instructionText;
    private TextView countdownText;
    CountDownTimer countDownTimer;
    private ImageView actionImage;
    Intent myoIntent;

    //States
    boolean monitoring = false;
    boolean starting = true;
    boolean active = false;
    int currentPose = 0;
    long timeRemaining = 0;
    int restDelay = 5000;

    //Poses
    String[] instructionsDisplay = {"fist",  "flex hand down", "flex hand up"};
    String[] instructions = {"FIST",  "WAVE_IN", "WAVE_OUT"};
    int[] neutralImages = {R.drawable.fist_neutral, R.drawable.flexdown_neutral, R.drawable.extendup_neutral};
    int[] goodImages = {R.drawable.fist_good, R.drawable.flexdown_good, R.drawable.extendup_good};
    int[] badImages = {R.drawable.fist_error, R.drawable.flexdown_error, R.drawable.extendup_error};
    Integer[] durations = {5000, 5000, 5000};

    //TTS
    TextToSpeech tts;

    //Myo Object
    Myo myoGlobal;


    private DeviceListener mListener = new AbstractDeviceListener() {
        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
            super.onAccelerometerData(myo, timestamp, accel);

        }

        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            super.onArmSync(myo, timestamp, arm, xDirection);
            //makeToast("Myo Synced!");

        }

        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            super.onArmUnsync(myo, timestamp);
            //makeToast("Myo Desynced!");
        }

        @Override
        public void onAttach(Myo myo, long timestamp) {
            super.onAttach(myo, timestamp);

        }

        @Override
        public void onConnect(Myo myo, long timestamp) {
            super.onConnect(myo, timestamp);
            myo.unlock(Myo.UnlockType.HOLD);
            makeToast("Myo Connected! Press back to start your session");
            myoGlobal = myo;

        }

        @Override
        public void onDetach(Myo myo, long timestamp) {
            super.onDetach(myo, timestamp);
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            super.onDisconnect(myo, timestamp);
            makeToast("Myo Disconnected!");
        }

        @Override
        public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
            super.onGyroscopeData(myo, timestamp, gyro);
        }

        @Override
        public void onLock(Myo myo, long timestamp) {
            super.onLock(myo, timestamp);
            //makeToast("Locked!");
            myo.unlock(Myo.UnlockType.HOLD);
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            super.onOrientationData(myo, timestamp, rotation);
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            super.onPose(myo, timestamp, pose);
            handlePose(pose);
        }


        @Override
        public void onRssi(Myo myo, long timestamp, int rssi) {
            super.onRssi(myo, timestamp, rssi);
        }

        @Override
        public void onUnlock(Myo myo, long timestamp) {
            super.onUnlock(myo, timestamp);
            //makeToast("Unlocked!");

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Activate the Myo
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            finish();
            return;
        }

        myoIntent = new Intent(this, ScanActivity.class);
        this.startActivity(myoIntent);

        hub.addListener(mListener);

        //Reference UI elements
        instructionText = (TextView) findViewById(R.id.instructionText);
        countdownText = (TextView) findViewById(R.id.countdownText);
        actionImage = (ImageView) findViewById(R.id.actionImage);

        //Text to speech
         tts = new TextToSpeech(this, null);

        waitForPose();

    }

    public void waitForPose() {
            instructionText.setText("Make " + instructionsDisplay[currentPose]);
            speak("Make " + instructionsDisplay[currentPose]);
            actionImage.setImageResource(neutralImages[currentPose]);
            timeRemaining = durations[currentPose];
            countdownText.setText(durations[currentPose] /1000 + " second(s) remaining");
            monitoring = true;

    }

    public void startPose() {
        instructionText.setText("Hold " + instructionsDisplay[currentPose]);
        active = true;
        actionImage.setImageResource(goodImages[currentPose]);
        countDownTimer =  new CountDownTimer(durations[currentPose], 1000) {
            @Override
            public void onTick(long l) {
                countdownText.setText(l/1000 + " second(s) remaining");
                timeRemaining = l;
                myoGlobal.vibrate(Myo.VibrationType.SHORT);
            }

            @Override
            public void onFinish() {
                monitoring = false;
                currentPose += 1;
                if (currentPose < instructionsDisplay.length) {
                    countdownText.setText("Done!");
                    restTimer();
                } else {
                    //Show completion image
                    instructionText.setText( "Congratulations!");
                    countdownText.setText("Session complete!");
                    speak("Session complete! Good job!");
                    actionImage.setImageResource(R.drawable.completed);
                }
            }
        }.start();

    }

    public  void wrongPose() {
        instructionText.setText("Return to " + instructionsDisplay[currentPose]);
        active = false;
        starting = false;
        countDownTimer.cancel();
        actionImage.setImageResource(badImages[currentPose]);
    }

    public void resumePose() {
        instructionText.setText("Hold " + instructionsDisplay[currentPose]);
        active = true;
        starting = true;
        actionImage.setImageResource(goodImages[currentPose]);
        countDownTimer =  new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long l) {
                countdownText.setText(l/1000 + " second(s) remaining");
                timeRemaining = l;
                myoGlobal.vibrate(Myo.VibrationType.SHORT);
            }

            @Override
            public void onFinish() {
                monitoring = false;
                currentPose += 1;
                if (currentPose < instructionsDisplay.length) {
                    countdownText.setText("Done!");
                    restTimer();
                } else {
                    //Show completion image
                    instructionText.setText( "Congratulations!");
                    countdownText.setText("Session complete!");
                    speak("Session complete! Good job!");
                    actionImage.setImageResource(R.drawable.completed);
                }
            }
        }.start();
    }
    public void handlePose(Pose pose) {
        if (monitoring) {
            if (pose.name().toString() == instructions[currentPose]) {
                if (!active) {
                    if (starting) {
                        startPose();
                    } else {
                        resumePose();
                    }
                }
            } else {
                if (active) {
                    wrongPose();
                }
            }
        }
    }

    public void restTimer() {
        instructionText.setText("Wait for " + instructionsDisplay[currentPose]);
        actionImage.setImageResource(neutralImages[currentPose]);
        countDownTimer =  new CountDownTimer(restDelay, 1000) {
            @Override
            public void onTick(long l) {
                countdownText.setText("In " + l/1000 + " second(s)");
            }

            @Override
            public void onFinish() {
                waitForPose();
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void speak(String message) {
        tts.speak(message, 0, null, null);
    }
}
