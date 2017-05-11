package utwente.uav.uavdisasterprobev2;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.KeyListener;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.Mission;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, WaypointMissionOperatorListener {

    private static final int FILE_REQUEST_CODE = 42;

    MapFragment mapFragment;
    private FlightController flightController;
    private LocationCoordinate3D aircraftLocation;
    private Marker droneMarker = null;
    private GoogleMap googleMap;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };
    private Button loadFileButton;
    private Button prepareFlightButton;
    private Button startPauseFlightButton;
    private Button stopFlightButton;

    private TextView currentFileTextView;
    private TextView statusPreparedTextView;

    private FlightPlanProtos.FlightPlan flightPlanProtos;
    private FlightPlan flightPlan;
    private FlightPlan loadedFlightPlan;

    CameraKey cameraShootPhotoKey = CameraKey.create(CameraKey.IS_SHOOTING_PHOTO);
    KeyListener cameraShootPhotoListener = new KeyListener() {
        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object o1) {
            Log.d("cameraListener", "Shoot photo:" + o1);

            Log.d("Yaw", "" + KeyManager.getInstance().getValue(yawKey));
            Log.d("Pitch", "" + KeyManager.getInstance().getValue(pitchKey));
            Log.d("Roll", "" + KeyManager.getInstance().getValue(rollKey));

        }
    };

    FlightControllerKey yawKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW);
    FlightControllerKey pitchKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH);
    FlightControllerKey rollKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL);

    private static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the SDK work well.
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

        currentFileTextView = (TextView) findViewById(R.id.current_file_text_view);
        statusPreparedTextView = (TextView) findViewById(R.id.status_prepared_text_view);

        initButtons();
    }

    private void updateTimelineStatus(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {
        if (element != null) {
            if (element instanceof Mission) {
                Log.d("timelineStatus", (((Mission) element).getMissionObject().getClass().getSimpleName()
                        + " event is "
                        + event.toString()
                        + " "
                        + (error == null ? "" : error.getDescription())));
            } else {
                Log.d("timelineStatus", (element.getClass().getSimpleName()
                        + " event is "
                        + event.toString()
                        + " "
                        + (error == null ? "" : error.getDescription())));
            }
        } else {
            Log.d("timelineStatus", ("Timeline Event is " + event.toString() + " " + (error == null
                    ? ""
                    : "Failed:"
                    + error.getDescription())));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        updateDroneMarkerLocation(52.242588, 6.694284);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect_bridge:
                IPAddressInputDialog ipAddressInputDialog = new IPAddressInputDialog(this, new IPAddressInputDialog.OnFinishedListener() {
                    @Override
                    public void onFinished(String IP) {
                        if (IP != null) {
                            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(IP);
                        }
                    }
                });
                ipAddressInputDialog.show();
                break;
            case R.id.action_locate:
                cameraUpdate();
                break;
            case R.id.create_fp_file:
                createFlightPlan();
                break;
            default:
                break;
        }
        return true;
    }

    private void onProductConnectionChange() {
        initFlightController();
        cameraUpdate();
    }

    private void initFlightController() {
        BaseProduct product = UAVDisasterProbeApplication.getProductInstance();

        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();

                MissionControl.getInstance().addListener(new MissionControl.Listener() {
                    @Override
                    public void onEvent(@Nullable TimelineElement timelineElement, TimelineEvent timelineEvent, @Nullable DJIError djiError) {
                        updateTimelineStatus(timelineElement, timelineEvent, djiError);
                    }
                });

            }
        }

        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    aircraftLocation = flightControllerState.getAircraftLocation();
                    updateDroneMarkerLocation(aircraftLocation.getLatitude(), aircraftLocation.getLongitude());
                }
            });
        }
    }

    private void updateDroneMarkerLocation(final double latitude, final double longitude) {
        LatLng location = new LatLng(latitude, longitude);

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(latitude, longitude)) {
                    droneMarker = googleMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void cameraUpdate() {
        if (droneMarker != null) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(droneMarker.getPosition(), 18.0f);
            googleMap.moveCamera(update);
        }
    }

    private void initButtons() {
        loadFileButton = (Button) findViewById(R.id.load_file_button);
        loadFileButton.setEnabled(true);
        loadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileLoader();
            }
        });

        prepareFlightButton = (Button) findViewById(R.id.prepare_flight_button);
        prepareFlightButton.setEnabled(false);
        prepareFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepare(flightPlan);
            }
        });


        startPauseFlightButton = (Button) findViewById(R.id.start_pause_flight_button);
        startPauseFlightButton.setEnabled(false);
        startPauseFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (flightPlan.getStatus()) {
                    case PREPARED:
                        start(flightPlan);
                        break;
                    case STARTED:
                        pause(flightPlan);
                        break;
                    case PAUSED:
                        resume(flightPlan);
                        break;
                    case RESUMED:
                        pause(flightPlan);
                        break;
                    default:
                        break;
                }
            }
        });

        stopFlightButton = (Button) findViewById(R.id.stop_flight_button);
        stopFlightButton.setEnabled(false);
        stopFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop(flightPlan);
            }
        });
    }

    private void openFileLoader() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        if(intent.resolveActivity(getPackageManager()) != null) {
            Log.d("Hello!", "HELLO!");
            startActivityForResult(intent, FILE_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            File file = new File(data.getData().getPath());
            if(file.getName().endsWith(".fp")) {
                try {
                    flightPlanProtos = FlightPlanProtos.FlightPlan.parseDelimitedFrom(new FileInputStream(file));

                    if(loadedFlightPlan != null) {
                        loadedFlightPlan.removeFromMap();
                    }
                    loadedFlightPlan = new FlightPlan(flightPlanProtos, this);
                    loadedFlightPlan.showOnMap(googleMap);

                    if(flightPlan == null) {
                        flightPlan = loadedFlightPlan;
                    }

                    currentFileTextView.setText(file.getName());
                    statusPreparedTextView.setText("not prepared");

                    if(flightPlan.getStatus() == null) {
                        prepareFlightButton.setEnabled(true);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "File " + file.getName() + " does not have the right extension.", Toast.LENGTH_SHORT).show();
            }
            // TODO: Check whether the file has the right extension.
            // TODO: Create a flight plan from the file.
        }
    }

    private void createFlightPlan() {
        FlightPlanBuilder builder = new FlightPlanBuilder();

        builder.addWaypointElement(52.242370, 6.694944,5,-90,0);
        builder.addWaypointElement(52.242314, 6.695175,6,-45,90);
        builder.addWaypointElement(52.242271, 6.695444,7,0,180);
        builder.addWaypointElement(52.242209, 6.695718,8,-15,-60);
        builder.writeToFile("flight1");

        builder = new FlightPlanBuilder();
        builder.addWaypointElement(52.242123, 6.696072,9,-30,60);
        builder.addWaypointElement(52.242034, 6.696470,10,-45,-90);
        builder.addWaypointElement(52.241932, 6.695637,5,-60,90);
        builder.addWaypointElement(52.242363, 6.694966,5,-75,0);
        builder.addWaypointElement(52.242383, 6.694284,5,-90,90);
        builder.writeToFile("flight2");

        builder = new FlightPlanBuilder();
        builder.addWaypointElement(52.250882, 6.853363, 15, -90, 0);
        builder.addWaypointElement(52.250810, 6.853647, 15, -90, 0);
        builder.addWaypointElement(52.250721, 6.853997, 15, -90, 0);
        builder.addWaypointElement(52.250629, 6.853931, 15, -90, 0);
        builder.addWaypointElement(52.250704, 6.853603, 15, -90, 0);
        builder.addWaypointElement(52.250770, 6.853336, 15, -90, 0);
        builder.addWaypointElement(52.250674, 6.853269, 15, -90, 0);
        builder.addWaypointElement(52.250609, 6.853515, 15, -90, 0);
        builder.addWaypointElement(52.250548, 6.853759, 15, -90, 0);
        builder.writeToFile("9wp_nadir_only_test");

        builder = new FlightPlanBuilder();
        builder.addWaypointElement(52.250880, 6.853384, 5, 0, 30);
        builder.addWaypointElement(52.250762, 6.853575, 10, -30, 30);
        builder.addWaypointElement(52.250600, 6.853616, 15, -60, 0);
        builder.addWaypointElement(52.250540, 6.853982, 15, -45, -90);
        builder.addWaypointElement(52.250584, 6.854316, 15, 0, 180);
        builder.writeToFile("5wp_multi_directions_test");

        builder = new FlightPlanBuilder();
        builder.addWaypointElement(52.216180, 7.027042, 80, -90, 0);
        builder.addWaypointElement(52.216491, 7.027503, 80, -90, 0);
        builder.addWaypointElement(52.216452, 7.027254, 80, -90, 0);
        builder.addWaypointElement(52.216501, 7.027004, 80, -90, 0);
        builder.addWaypointElement(52.216581, 7.026779, 80, -90, 0);
        builder.addWaypointElement(52.216124, 7.027301, 80, -90, 0);
        builder.addWaypointElement(52.216256, 7.027411, 80, -90, 0);
        builder.addWaypointElement(52.216308, 7.027144, 80, -90, 0);
        builder.addWaypointElement(52.216388, 7.026907, 80, -90, 0);
        builder.addWaypointElement(52.216463, 7.026666, 80, -90, 0);
        builder.addWaypointElement(52.216646, 7.026509, 80, -90, 0);
        builder.addWaypointElement(52.216532, 7.027623, 80, -90, 0);
        builder.addWaypointElement(52.216595, 7.027364, 80, -90, 0);
        builder.addWaypointElement(52.216718, 7.026892, 80, -90, 0);
        builder.addWaypointElement(52.216779, 7.026654, 80, -90, 0);
        builder.addWaypointElement(52.216542, 7.026372, 80, -90, 0);
        builder.addWaypointElement(52.216262, 7.026779, 80, -90, 0);
        builder.addWaypointElement(52.216346, 7.026526, 80, -90, 0);
        builder.addWaypointElement(52.216428, 7.026237, 80, -90, 0);
        builder.addWaypointElement(52.216057, 7.026941, 80, -90, 0);
        builder.addWaypointElement(52.216138, 7.026676, 80, -90, 0);
        builder.addWaypointElement(52.216204, 7.026406, 80, -90, 0);
        builder.addWaypointElement(52.216296, 7.026182, 80, -90, 0);
        builder.writeToFile("nadir_flight");

        builder = new FlightPlanBuilder();
        builder.addWaypointElement(52.215985, 7.026647, 80, -40, 25);
        builder.addWaypointElement(52.215985, 7.026647, 60, -40, 25);
        builder.addWaypointElement(52.216287, 7.027463, 80, -40, -65);
        builder.addWaypointElement(52.216287, 7.027463, 60, -40, -65);
        builder.addWaypointElement(52.216865, 7.027001, 80, -30, -155);
        builder.addWaypointElement(52.216865, 7.027001, 60, -30, -155);
        builder.addWaypointElement(52.216941, 7.026729, 80, -30, -155);
        builder.addWaypointElement(52.216941, 7.026729, 60, -30, -155);
        builder.addWaypointElement(52.216123, 7.026113, 80, -40, 25);
        builder.addWaypointElement(52.216123, 7.026113, 60, -40, 25);
        builder.addWaypointElement(52.216192, 7.027357, 80, -40, -65);
        builder.addWaypointElement(52.216192, 7.027357, 60, -40, -65);
        builder.addWaypointElement(52.216052, 7.026408, 80, -40, 25);
        builder.addWaypointElement(52.216052, 7.026408, 60, -40, 25);
        builder.addWaypointElement(52.216427, 7.027534, 80, -40, -65);
        builder.addWaypointElement(52.216427, 7.027534, 60, -40, -65);
        builder.addWaypointElement(52.216797, 7.027221, 80, -30, -155);
        builder.addWaypointElement(52.216797, 7.027221, 60, -30, -155);
        builder.writeToFile("oblique_flight");
    }

    public void prepare(final FlightPlan flightPlan) {
        KeyManager.getInstance().addListener(cameraShootPhotoKey, cameraShootPhotoListener);

        /*
        UAVDisasterProbeApplication.getCameraInstance().setSystemStateCallback(new SystemState.Callback() {
            @Override
            public void onUpdate(@NonNull SystemState systemState) {
                if(systemState.isShootingSinglePhoto()) {
                    Log.d("systemState", "Shooting photo.");
                    FlightControllerKey yawKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW);
                }
            }
        });*/

        if(flightPlan.getStatus() == null
                || flightPlan.getStatus() == FlightPlan.Status.STOPPED
                || flightPlan.getStatus() == FlightPlan.Status.PREPARED) {
            Log.d("Prepare", "Update");
            flightPlan.update(flightPlanProtos);
        }

        if(flightPlan.getStatus() == null) {
            Log.d("Prepare", "1");
            WaypointMissionOperator operator = flightPlan.getOperator();
            operator.addListener(this);
            Log.d("Prepare", "State: " + operator.getCurrentState().getName());
            if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(operator.getCurrentState())
                    || WaypointMissionState.READY_TO_UPLOAD.equals(operator.getCurrentState())
                    || WaypointMissionState.READY_TO_EXECUTE.equals(operator.getCurrentState())) {
                Log.d("Prepare", "2");
                operator.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        showErrorToToast(error);
                        if(error == null) {
                            flightPlan.setStatus(FlightPlan.Status.PREPARED);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("Prepare", "3");
                                    startPauseFlightButton.setEnabled(true);
                                    startPauseFlightButton.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getApplicationContext(), R.drawable.play_black_small), null, null);
                                    prepareFlightButton.setEnabled(false);
                                    statusPreparedTextView.setText("prepared");
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    public void start(final FlightPlan flightPlan) {
        if(flightPlan.getMission() != null) {
            flightPlan.getOperator().startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    showErrorToToast(error);
                    if(error == null) {
                        flightPlan.setStatus(FlightPlan.Status.STARTED);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startPauseFlightButton.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getApplicationContext(), R.drawable.pause_black_small), null, null);
                                startPauseFlightButton.setText("PAUSE");
                                stopFlightButton.setEnabled(true);
                            }
                        });
                    }
                }
            });
        }
    }

    public void stop(final FlightPlan flightPlan) {
        flightPlan.getOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showErrorToToast(error);
                if(error == null) {
                    flightPlan.setStatus(FlightPlan.Status.STOPPED);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startPauseFlightButton.setEnabled(false);
                            startPauseFlightButton.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getApplicationContext(), R.drawable.play_black_small), null, null);
                            startPauseFlightButton.setText("START");
                            stopFlightButton.setEnabled(false);
                            prepareFlightButton.setEnabled(true);
                            statusPreparedTextView.setText("not prepared");
                        }
                    });
                }
            }
        });
    }

    public void pause(final FlightPlan flightPlan) {
        flightPlan.getOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showErrorToToast(error);
                if(error == null) {
                    flightPlan.setStatus(FlightPlan.Status.PAUSED);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startPauseFlightButton.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getApplicationContext(), R.drawable.play_black_small), null, null);
                            startPauseFlightButton.setText("RESUME");
                        }
                    });
                }
            }
        });
    }

    public void resume(final FlightPlan flightPlan) {
        flightPlan.getOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showErrorToToast(error);
                if(error == null) {
                    flightPlan.setStatus(FlightPlan.Status.RESUMED);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startPauseFlightButton.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(getApplicationContext(), R.drawable.pause_black_small), null, null);
                            startPauseFlightButton.setText("PAUSE");
                        }
                    });
                }
            }
        });
    }

    public void showErrorToToast(final DJIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UAVDisasterProbeApplication.getContext(), error == null ? "Action started!" : error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent event) {
        if (event.getProgress() != null
                && event.getProgress().isSummaryDownloaded
                && event.getProgress().downloadedWaypointIndex == (event.getProgress().totalWaypointCount - 1)) {
            Toast.makeText(UAVDisasterProbeApplication.getContext(), "Download successful!", Toast.LENGTH_SHORT).show();
            Log.d("FlightPlan", "Download successful!");
        }
    }

    @Override
    public void onUploadUpdate(@NonNull WaypointMissionUploadEvent event) {
        if (event.getProgress() != null
                && event.getProgress().isSummaryUploaded
                && event.getProgress().uploadedWaypointIndex == (event.getProgress().totalWaypointCount - 1)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UAVDisasterProbeApplication.getContext(), "Upload successful!", Toast.LENGTH_SHORT).show();
                }
            });

            Log.d("FlightPlan", "Upload successful!");
        }
    }

    @Override
    public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent event) {
        Log.d("FlightPlan",
                (event.getPreviousState() == null
                        ? ""
                        : event.getPreviousState().getName())
                        + ", "
                        + event.getCurrentState().getName()
                        + (event.getProgress() == null
                        ? ""
                        : event.getProgress().targetWaypointIndex));
    }

    @Override
    public void onExecutionStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UAVDisasterProbeApplication.getContext(), "Execution started!", Toast.LENGTH_SHORT).show();
            }
        });
        Log.d("FlightPlan", "Execution started!");
    }

    @Override
    public void onExecutionFinish(@Nullable DJIError djiError) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UAVDisasterProbeApplication.getContext(), "Execution finished!", Toast.LENGTH_SHORT).show();
            }
        });

        stop(flightPlan);

        Log.d("FlightPlan", "Execution finished!");
    }
}
