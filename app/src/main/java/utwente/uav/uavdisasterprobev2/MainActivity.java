package utwente.uav.uavdisasterprobev2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import dji.sdk.sdkmanager.DJISDKManager;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

public class MainActivity extends Activity implements OnMapReadyCallback {

    FlightPlan flightPlan;

    MapFragment mapFragment;

    Button createButton;
    Button loadButton;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(UAVDisasterProbeApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(receiver, filter);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        createButton = (Button) findViewById(R.id.button2);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*FlightPlanBuilder flightPlanBuilder = new FlightPlanBuilder();
                flightPlanBuilder.addWaypointElement(52.242482, 6.694040, 30, -90, 30);
                flightPlanBuilder.addHotpointElement(52.241855, 6.694812, 5, -30, 5, 120, true, 3, FlightPlanProtos.FlightPlan.FlightElement.HotpointElement.StartPoint.NEAREST);
                flightPlanBuilder.writeToFile();*/

                connectDialog();

            }
        });

        loadButton = (Button) findViewById(R.id.button3);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*File folder = new File(Environment.getExternalStorageDirectory(), "FlightPlans");

                if(folder.exists()) {
                    File file = new File(folder, "flight.fp");
                    if(file.exists()) {
                        try {
                            FileInputStream inputStream = new FileInputStream(file);
                            FlightPlanProtos.FlightPlan flightPlan = FlightPlanProtos.FlightPlan.parseDelimitedFrom(inputStream);
                            FlightPlan testFlight = new FlightPlan(flightPlan);
                            testFlight.verify();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }*/
            }
        });



    }

    private void connectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("BRIDGE APP IP");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String ip = input.getText().toString();
                try {
                    DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(ip);
                } finally {

                }
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        LatLng sydney = new LatLng(-33.852, 151.211);
        googleMap.addMarker(new MarkerOptions().position(sydney)
                .title("Marker in Sydney"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
