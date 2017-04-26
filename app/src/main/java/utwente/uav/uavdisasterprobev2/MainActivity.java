package utwente.uav.uavdisasterprobev2;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.timeline.Mission;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.AircraftYawAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FlightController flightController;

    private LocationCoordinate3D aircraftLocation;

    private Marker droneMarker = null;

    FlightPlan testFlight;

    MapFragment mapFragment;

    private TextView productConnectedTextView;

    private Button createFlightButton;
    private Button prepareFlightButton;
    private Button startFlightButton;
    private Button stopFlightButton;

    private GoogleMap googleMap;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

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

        productConnectedTextView = (TextView) findViewById(R.id.product_connected_textview);
        productConnectedTextView.setTextColor(Color.WHITE);

        createFlightButton = (Button) findViewById(R.id.create_flight_button);
        createFlightButton.setText("CREATE");
        createFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("scheduledCount","" + MissionControl.getInstance().scheduledCount());

                /*FlightPlanBuilder flightPlanBuilder = new FlightPlanBuilder();
                flightPlanBuilder.addWaypointElement(52.242482, 6.694040, 30, -90, 30);
                flightPlanBuilder.addHotpointElement(52.241855, 6.694812, 5, -30, 5, 120, true, 3, FlightPlanProtos.FlightPlan.FlightElement.HotpointElement.StartPoint.NEAREST);
                flightPlanBuilder.writeToFile();
                Log.d("HELLOO", "1!");*/
            }
        });

        prepareFlightButton = (Button) findViewById(R.id.prepare_flight_button);
        prepareFlightButton.setText("PREPARE");
        prepareFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                initTimeline();

                /*File folder = new File(Environment.getExternalStorageDirectory(), "FlightPlans");

                if(folder.exists()) {
                    File file = new File(folder, "flight.fp");
                    if(file.exists()) {
                        try {
                            FileInputStream inputStream = new FileInputStream(file);
                            FlightPlanProtos.FlightPlan flightPlan = FlightPlanProtos.FlightPlan.parseDelimitedFrom(inputStream);
                            testFlight = new FlightPlan(flightPlan);
                            Log.d("HELLOO", "2!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }*/
            }
        });

        startFlightButton = (Button) findViewById(R.id.start_flight_button);
        startFlightButton.setText("START");
        startFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MissionControl.getInstance().startTimeline();
            }
        });

        stopFlightButton = (Button) findViewById(R.id.stop_flight_button);
        stopFlightButton.setText("STOP");
        stopFlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MissionControl missionControl = MissionControl.getInstance();
                missionControl.unscheduleEverything();
                missionControl.startElement(new GoHomeAction());

            }
        });
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
                        if(IP != null) {
                            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(IP);
                        }
                    }
                });
                ipAddressInputDialog.show();
                break;
            case R.id.action_refresh:
                updateProductConnectedTextView();
                cameraUpdate();
                break;
            default:
                break;
        }
        return true;
    }

    private void initTimeline() {
        final MissionControl missionControl = MissionControl.getInstance();
        MissionControl.Listener listener = new MissionControl.Listener() {
            @Override
            public void onEvent(@Nullable TimelineElement timelineElement, TimelineEvent timelineEvent, @Nullable DJIError djiError) {
                updateTimelineStatus(timelineElement, timelineEvent, djiError);
                if(timelineEvent.toString() != null && timelineEvent.toString().equals("FINISHED")) {
                    missionControl.unscheduleEverything();
                    missionControl.removeAllListeners();
                }
            }
        };

        if(missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }

        HotpointMission hotpointMission = new HotpointMission();
        hotpointMission.setRadius(5);
        hotpointMission.setStartPoint(HotpointStartPoint.NEAREST);
        hotpointMission.setHeading(HotpointHeading.TOWARDS_HOT_POINT);
        hotpointMission.setClockwise(true);
        hotpointMission.setAltitude(5);
        hotpointMission.setHotpoint(new LocationCoordinate2D(52.242557, 6.695059));
        hotpointMission.setAngularVelocity((float) HotpointMissionOperator.maxAngularVelocityForRadius(5));
        googleMap.addMarker(new MarkerOptions().position(new LatLng(52.242557, 6.695059)));



        List<TimelineElement> timeline = new ArrayList<>();
        timeline.add(new TakeOffAction());
        //timeline.add(new GoToAction(new LocationCoordinate2D(52.242557, 6.695059), 5));
        timeline.add(new HotpointAction(hotpointMission, 180));
        //timeline.add(new GoToAction(new LocationCoordinate2D(52.242293,6.6950328), 5));
        //googleMap.addMarker(new MarkerOptions().position(new LatLng(52.242293,6.6950328)));

        WaypointMission.Builder waypointMissionBuilder = new WaypointMission.Builder();
        waypointMissionBuilder.autoFlightSpeed(10f);
        waypointMissionBuilder.maxFlightSpeed(10f);
        waypointMissionBuilder.setExitMissionOnRCSignalLostEnabled(false);
        waypointMissionBuilder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        waypointMissionBuilder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        waypointMissionBuilder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        waypointMissionBuilder.headingMode(WaypointMissionHeadingMode.AUTO);
        waypointMissionBuilder.repeatTimes(1);

        List<Waypoint> waypoints = new LinkedList<>();
        Waypoint waypoint = new Waypoint(52.242293, 6.6950328, 5);


        timeline.add(new GoHomeAction());

        missionControl.scheduleElements(timeline);
        missionControl.addListener(listener);
    }

    private void onProductConnectionChange() {
        updateProductConnectedTextView();
        initFlightController();
        cameraUpdate();
    }

    private void initFlightController() {
        BaseProduct product = UAVDisasterProbeApplication.getProductInstance();

        if(product != null && product.isConnected()) {
            if(product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
        }

        if(flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    aircraftLocation = flightControllerState.getAircraftLocation();
                    updateDroneMarkerLocation(aircraftLocation.getLatitude(), aircraftLocation.getLongitude());
                }
            });
        }
    }

    private void updateProductConnectedTextView() {
        if(productConnectedTextView == null) return;

        BaseProduct product = UAVDisasterProbeApplication.getProductInstance();

        String productConnectedString = "Disconnected...";

        if(product != null) {
            if(product.isConnected()) {
                productConnectedString = UAVDisasterProbeApplication.getProductInstance().getModel() + " connected...";
            } else {
                if(product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        productConnectedString = "RC connected...";
                    }
                }
            }
        }

        productConnectedTextView.setText(productConnectedString);
    }

    private void updateDroneMarkerLocation(final double latitude, final double longitude) {
        LatLng location = new LatLng(latitude, longitude);

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(droneMarker != null) {
                    droneMarker.remove();
                }

                if(checkGpsCoordination(latitude, longitude)) {
                    droneMarker = googleMap.addMarker(markerOptions);
                }
            }
        });

    }

    private void cameraUpdate() {
        if(droneMarker != null) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(droneMarker.getPosition(), 18.0f);
            googleMap.moveCamera(update);
        }
    }

    private static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }
}
