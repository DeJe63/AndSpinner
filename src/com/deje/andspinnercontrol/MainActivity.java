package com.deje.andspinnercontrol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends Activity {
	
    private byte cmdByte[] = new byte[1];
//    private byte cmdString[] = new byte[16];
    /**
     * The device currently in use, or {@code null}.
     */
    private UsbSerialDriver mSerialDevice;

    /**
     * The system's USB service.
     */
    private UsbManager mUsbManager;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private TextView mText;
    private ScrollView mScroll;
    private View mStart;
    private View mStop;
    private View mBrake;
    private View mLeft;
    private View mRight;
    
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            ;
        }

        @Override
        public void onNewData(final byte[] data) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String str = new String(data, "UTF-8");
                        updateReceivedData(str);
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mText = ((TextView) findViewById(R.id.tv_status));
        mScroll = (ScrollView) findViewById(R.id.tv_scroll);
        mText.setMovementMethod(new ScrollingMovementMethod());
        
        // setup buttons color
        mStart = ((View) findViewById(R.id.sf_start));
        mStop = ((View) findViewById(R.id.sf_stop));
        mBrake = ((View) findViewById(R.id.sf_brake));
        mLeft = ((View) findViewById(R.id.sf_links));
        mRight = ((View) findViewById(R.id.sf_rechts));
        mStart.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
        mStop.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
        mLeft.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
        mRight.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }
    }

   @Override
    protected void onResume() {
        super.onResume();
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        if (mSerialDevice != null) {
            try {
                mSerialDevice.open();
            } catch (IOException e) {
                try {
                    mSerialDevice.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mSerialDevice = null;
                return;
            }
        }
        onDeviceStateChange();
    }

    /**
      * @autor Dana Jenett
      * @since 17.12.2012
      * Beschreibung: Wird aufgerufen, wenn ein Button gedrück wird.
      * @param view
      */
    public void onButtonPressed(final View view) throws IOException{
        switch (view.getId()) {
        case R.id.sf_plus:{
                cmdByte[0] = '+';
                break;			
        }case R.id.sf_minus:{
                cmdByte[0] = '-';
                break;			
        }case R.id.sf_start:{
                cmdByte[0] = 'e';
                mStart.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
                mStop.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
                break;			
        }case R.id.sf_stop:{
                cmdByte[0] = 'd';
                mStart.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
                mStop.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
                break;			
        }case R.id.sf_links:{
                cmdByte[0] = 'l';
                mLeft.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
                mRight.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
                break;			
        }case R.id.sf_rechts:{
                cmdByte[0] = 'r';
                mLeft.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
                mRight.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
                break;			
        }case R.id.sf_status:{
                cmdByte[0] = 's';
                break;			
        }case R.id.sf_brake:{
                cmdByte[0] = 'b';
                mStart.getBackground().setColorFilter(Color.LTGRAY,PorterDuff.Mode.MULTIPLY);
                mStop.getBackground().setColorFilter(Color.GREEN,PorterDuff.Mode.MULTIPLY);
                break;			
        }default:
                cmdByte[0] = '0';
                break;
        }

        if (mSerialDevice != null) {
            try {
                mSerialDevice.write(cmdByte, 500);
            } catch (IOException e) {
                // Deal with error.
            }
        }
    }	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mSerialDevice != null) {
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(String data) {
        //----------------------------------------------------------------------------
        mText.setText(mText.getText() + data);

        mScroll.post(new Runnable()
        {
            public void run()
            {
                mScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
