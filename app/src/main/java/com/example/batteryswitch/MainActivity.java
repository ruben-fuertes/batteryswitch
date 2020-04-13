package com.example.batteryswitch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnItemClickListener {
    private static final String TAG = "MainActivity";
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private int inputInt = 85;
    private String Open = "A00100A1";
    private String Close = "A00101A2";
    private boolean autoFinish = false;
    public String deviceName;
    public String deviceAddress;
    public BluetoothAdapter myBluetooth = null;
    private TextView bat_lvl;
    private TextView max_bat_lvl;
    private EditText max_bat_input;
    private Switch autoFinishSwitch;
    public Button btn1;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    public ListView lvNewDevices;
    public BluetoothDevice myBluetoothDevice;
    public BluetoothGattCharacteristic writableChar;
    public BluetoothGatt bluetoothGatt;
    public BluetoothGattCallback bluetoothGattCallbackConnect =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to GATT server.");
                        gatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Log.d(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                BluetoothGattService mBS = bluetoothGatt.getService(service.getUuid());
                                BluetoothGattCharacteristic mBc = mBS.getCharacteristic(characteristic.getUuid());

                                if (mBc.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                                    writableChar = mBc;
                                }
                            }
                        }
                        byte[] send = new byte[0];
                        try {
                            send = Hex.decodeHex(Close.toCharArray());
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }
                        writableChar.setValue(send);
                        boolean x = bluetoothGatt.writeCharacteristic(writableChar);

                        Log.d(TAG, "closeInput: " + bytesToHex(send));
                        Log.d(TAG, "closeInput: " + x);
                    }
                }
            };

    private BroadcastReceiver BatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            float level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

                /*
                int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                boolean present = intent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
                String technology = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
                int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                */

            bat_lvl.setText("Current  battery level: " + level + "%");

            double inputInt = Double.parseDouble(max_bat_input.getText().toString());
            if (level >= inputInt) {
                Toast.makeText(MainActivity.this, "CHARGED",
                        Toast.LENGTH_LONG).show();
                byte[] send = new byte[0];
                try {
                    send = Hex.decodeHex(Open.toCharArray());
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
                writableChar.setValue(send);
                boolean x = bluetoothGatt.writeCharacteristic(writableChar);

                Log.d(TAG, "closeInput: " + bytesToHex(send));
                Log.d(TAG, "closeInput: " + x);

                if (autoFinish) {
                    myBluetooth.disable();
                    finish();
                    System.exit(0);
                }
            }

        }
    };

    private final BroadcastReceiver mbroadcastrecieverBT = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(myBluetooth.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mbroadcastrecieverBL: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mbroadcastrecieverBL: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mbroadcastrecieverBL: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mbroadcastrecieverBTDiscover = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ":" + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);

                lvNewDevices.setAdapter(mDeviceListAdapter);

            }
        }
    };

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(BatteryReceiver, filter);

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        bat_lvl = (TextView) findViewById(R.id.batt_level);
        max_bat_lvl = (TextView) findViewById(R.id.max_batt_TextView);
        max_bat_input = (EditText) findViewById(R.id.max_bat_TextEdit);
        EditText openInput = (EditText) findViewById(R.id.editTextOpen);
        EditText closeInput = (EditText) findViewById(R.id.editTextClose);
        btn1 = (Button) findViewById(R.id.exitButton);
        autoFinishSwitch = (Switch) findViewById(R.id.autoFinishSwitch);
        mBTDevices = new ArrayList<>();

        max_bat_lvl.setText("Max battery level: " + inputInt + "%");
        lvNewDevices.setOnItemClickListener(MainActivity.this);

        autoFinishSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    autoFinish = true;
                } else {
                    autoFinish = false;
                }
            }
        });

        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Not BT capabilities");

            //finish apk
            finish();
        } else {
            if (myBluetooth.isEnabled()) {
                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(mbroadcastrecieverBT, BTIntent);
            } else {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(mbroadcastrecieverBT, BTIntent);
            }
        }

        btn1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetooth.disable();
                finish();
                System.exit(0);
            }
        });



        openInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Open = v.getText().toString();
                    handled = true;
                }
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                return handled;
            }
        });

        closeInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Close = v.getText().toString();

                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);

                    handled = true;
                }
                return handled;
            }
        });

        max_bat_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    inputInt = (int) Float.parseFloat(v.getText().toString());

                    if (inputInt > 95 || inputInt < 50) {
                        Toast.makeText(MainActivity.this, "Max battery must be between 50 and 95 \nSet default 85",
                                Toast.LENGTH_LONG).show();
                        inputInt = 85;

                    } else {
                        Toast.makeText(MainActivity.this, "The new maximum battery level is: " + inputInt,
                                Toast.LENGTH_LONG).show();

                    }
                    max_bat_lvl.setText("Max battery level: " + inputInt + "%");

                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);

                    handled = true;

                }
                return handled;
            }
        });

    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");


        if (myBluetooth.isDiscovering()) {
            myBluetooth.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            checkBTPermissions();

            myBluetooth.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mbroadcastrecieverBTDiscover, discoverDevicesIntent);
        }

        if (!myBluetooth.isDiscovering()) {
            checkBTPermissions();

            myBluetooth.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mbroadcastrecieverBTDiscover, discoverDevicesIntent);
        }
    }

    private void checkBTPermissions() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission(("Manifest.permission.ACCESS_COARSE_LOCATION"));
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        unregisterReceiver(BatteryReceiver);
        unregisterReceiver(mbroadcastrecieverBT);
        unregisterReceiver(mbroadcastrecieverBTDiscover);
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Intent intent = new Intent(this, myService.class);
        //startService(intent);

        myBluetooth.cancelDiscovery();

        Log.d(TAG, "onItemClick: You clicked on a device.");
        deviceName = mBTDevices.get(position).getName();
        deviceAddress = mBTDevices.get(position).getAddress();

        myBluetoothDevice = mBTDevices.get(position);
        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        Log.d(TAG, "Trying to pair with " + deviceName);

        bluetoothGatt = myBluetoothDevice.connectGatt(getApplicationContext(), true, bluetoothGattCallbackConnect);


    }
}
