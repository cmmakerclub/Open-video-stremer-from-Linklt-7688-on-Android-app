package com.example.bunhan.rxvideofromlinkit7688;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.bunhan.rxvideofromlinkit7688.JoystickView.OnJoystickMoveListener;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final String TAG = "MjpegActivity";

    private MjpegView mv;
    private TextView testT,debug;
    static String hostip = "192.168.100.1";//Host of Linkit Smart
    static String portS = "12345";//port udp
    byte   packetDataControl[] = {(byte)0xfe,0,0,0,0,0};//startBit,ch1,ch2,ch3,ch4,sum(ch1...4)

    int ch1_roll ,ch2_ele,ch3_power,ch4_rudder;
    float y ,y2;
    float x ,x2;
    boolean sendDatas = true;
    boolean sendDataFished = true;
    boolean startJoyLeft = false;
    SendDataViaUDP mySendDataViaUDP;

    private JoystickView joystickR,joystickL;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ...


        // ...

        //sample public cam
        //String URL = "http://trackfield.webcam.oregonstate.edu/axis-cgi/mjpg/video.cgi?resolution=800x600&amp%3bdummy=1333689998337";
        String URL = "http://192.168.100.1:8080/?action=stream";
        /*
        set full screen
         */

        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //      WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//Alway on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //
        setContentView(R.layout.activity_main);
        testT = (TextView) findViewById(R.id.textViewT);
        testT.setTextColor(Color.CYAN);

        debug = (TextView) findViewById(R.id.debugT);
        debug.setTextColor(Color.CYAN);
        //

        mv = new MjpegView(this);


        //add mv
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.relativeView);
        relativeLayout.addView(mv, 0);


        new DoRead().execute(URL);
        //
        //Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
        joystickR = (JoystickView) findViewById(R.id.joystickViewRight);
        joystickR.setOnJoystickMoveListener(new OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub


                //angleTextView.setText(" " + String.valueOf(angle) + "'");
                //powerTextView.setText(" " + String.valueOf(power) + "%");

                y = (int) ((Math.cos(Math.toRadians(angle))) * power);
                x = (int) ((Math.sin(Math.toRadians(angle))) * power);

                //ch1_ele	 =
                sendUDPdata();
                //sendFinish = false;
                //end send data

                //directionTextView.setText("y = "+String.valueOf(y)+"   x = "+String.valueOf(x));
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
        //joy left trotor
        joystickL = (JoystickView) findViewById(R.id.joystickViewLeft);
        //set for ch3 low at start
        joystickL.tLMode = true;
        joystickL.setyPositionY((int)y2);
        joystickL.setOnJoystickMoveListener(new OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub
                startJoyLeft = true;
                joystickL.tMode = true;
                joystickL.tLMode = false;

                //angleTextView.setText(" " + String.valueOf(angle) + "'");
                //powerTextView.setText(" " + String.valueOf(power) + "%");

                y2 = (int) ((Math.cos(Math.toRadians(angle)))*power);
                x2 = (int) ((Math.sin(Math.toRadians(angle)))*power);

                //ch1_ele	 =
                sendUDPdata();
                //sendFinish = false;
                //end send data

                //directionTextView.setText("y = "+String.valueOf(y)+"   x = "+String.valueOf(x));
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);


        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // What you want to do goes here

                //if(myClientTask.getStatus() != AsyncTask.Status.RUNNING){
                // My AsyncTask is not currently doing work in doInBackground()
                sendUDPdata();
                //}


            }
        }, 0, 50);
        //
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }//end Oncretate

    public void onPause() {
        super.onPause();
        mv.stopPlayback();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.bunhan.rxvideofromlinkit7688/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.bunhan.rxvideofromlinkit7688/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            // Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                //Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                // Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                //Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
        }
    }//end Doread

    public class SendDataViaUDP extends AsyncTask<Void, Void, Void> {
        SendDataViaUDP() {

        }

        @Override
        protected Void doInBackground(Void... params) {

            DatagramSocket ds = null;
            InetAddress serverAddr;
            try {
                ds = new DatagramSocket();
                //if(SetPid.getHostIpToSend().equals("a")){
                serverAddr = InetAddress.getByName(hostip);

                //DatagramSocket ds = new DatagramSocket();
                // DatagramPacket dp;
                DatagramPacket dp = new DatagramPacket(packetDataControl, packetDataControl.length, serverAddr,Integer.parseInt(portS));
                ds.send(dp);
                //}else{
                // serverAddr = InetAddress.getByName("192.168.5.1");

                //DatagramSocket ds = new DatagramSocket();
                // DatagramPacket dp;
                //   DatagramPacket dp = new DatagramPacket(packetDataControl, packetDataControl.length, serverAddr,12345);
                //   ds.send(dp);
                //}
                //ds.send(dp);


            }catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //response = "IOException: " + e.toString();
            }finally{
                ds.close();
            }
        //}//end ceck task cancel

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            sendDataFished = true;
            debug.setText("ch1 roll:"+String.valueOf(ch1_roll)+ "ch2 ele :"+ String.valueOf(ch2_ele)+" ch3:"+String.valueOf(ch3_power)+" ch4:"+String.valueOf(ch4_rudder));
        }
    }

public void sendUDPdata(){
    ch1_roll = (int) ((int) x*1.6f);
    ch2_ele = (int) ((int) y*1.6f);
    ch3_power = (int) ((int) y2*1.6f);

    ch4_rudder = (int) ((int) x2*1.6f);
   /* if(startJoy2 == true){
        ch3_power =  (int) ((y2 + 60)*0.9*0.85470085470085470085470085470085);
        if(ch3_power<0){
            ch3_power = 0;
        }
    }else{
        ch3_power = 0;
    }
    ch4_yaw =  (int) ((x2-7)*1.4705882352941176470588235294118);
    //offset
    /*/
    //.... CH1 ....

    if(ch1_roll > 0){//offset up elev
        if(ch1_roll < 10){
            ch1_roll = 0;
        }else{
            ch1_roll -= 9;
        }

    }
    if(ch1_roll < 0){//offset down elev
        if(ch1_roll > -10){
            ch1_roll = 0;
        }else{
            ch1_roll += 9;
        }
    }

    //
    //------- Ch2 ------

    if(ch2_ele > 0){//offset right roll
        if(ch2_ele < 10){
            ch2_ele = 0;
        }else{
            ch2_ele -= 9;
        }

    }
    if(ch2_ele < 0){//offset left roll
        if(ch2_ele > -10){
            ch2_ele = 0;
        }
        else{
            ch2_ele += 9;
        }
    }

    //
    //++++++++  CH4  ++++++++
    if(ch4_rudder > 0){//offset right yaw
        if(ch4_rudder < 10){
            ch4_rudder = 0;
        }else{
            ch4_rudder -= 10;
        }

    }
    if(ch4_rudder < 0){//offset left yaw
        if(ch4_rudder > -10){
            ch4_rudder = 0;
        }else{
            ch4_rudder += 10;
        }
    }
   // ch4_yaw += yv;
    //ch3
    ch3_power = fMap(ch3_power,-108, 108, 0, 100);
    if(!startJoyLeft){
        ch3_power = 0;
    }
    //KI! off when control roll pitch

    //dataOut.setText("Ele = "+String.valueOf(ch1_ele)+"  Roll = "+String.valueOf(ch2_roll)+"  Power = "+String.valueOf(ch3_power)+"  Yaw = "+String.valueOf(ch4_yaw));
    //
    if(sendDatas){

        //String dataSends =  "a"+Integer.toString(ch1_ele)+"b"+Integer.toString(ch2_roll)+"c"+Integer.toString(ch3_power)+"d"+Integer.toString(ch4_yaw)+"p"+Integer.toString(kpSend)+"i"+Integer.toString(kiSend)+"k"+Integer.toString(kdSend)+"z"+Integer.toString(kiRoll)+"x"+Integer.toString(kiPitch)+"!";

        packetDataControl[1] = (byte) ((byte) ch1_roll);
        packetDataControl[2] = (byte) ((byte) ch2_ele);
        packetDataControl[3] = (byte) ((byte) ch3_power);
        packetDataControl[4] =  (byte) ((byte) ch4_rudder) ;
        packetDataControl[5] =  (byte) ((byte) (((byte) ch1_roll) + ((byte) ch2_ele) + ((byte)ch3_power) + ((byte) ch4_rudder ))); //sum
        //sendCount++;
        if(sendDataFished){
            //if(myClientTask.getStatus() == AsyncTask.Status.PENDING){
            // My AsyncTask is not currently doing work in doInBackground()
            //if(myClientTask.getStatus().equals(AsyncTask.Status.PENDING)){
            mySendDataViaUDP = new SendDataViaUDP();//send data to UDP
            mySendDataViaUDP.execute();//work UDP
            sendDataFished = false;
            //}
            //myClientTask.execute();//work UDP


            //sendCount++;
            //sendDataFished = false;
            //}
            // count++;
            // if(count > 10000){
            //	 count = 0;
            //}
        }

    }else{

        //
    }
    //}

    // }
}
    public  int fMap(int x, int in_min, int in_max, int out_min, int out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

}//end MainActivity
