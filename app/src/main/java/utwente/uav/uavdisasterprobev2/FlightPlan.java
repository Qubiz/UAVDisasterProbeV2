package utwente.uav.uavdisasterprobev2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

/**
 * Created by Mathijs on 4/11/2017.
 */

public class FlightPlan {

    private String name;
    private ArrayList<Marker> markers;
    private Polyline path;
    private WaypointMissionOperator operator;
    private WaypointMission mission;
    private Status status = null;
    private MainActivity mainActivity;
    public FlightPlan(FlightPlanProtos.FlightPlan flightPlanProtos, String name, MainActivity mainActivity) {
        this.name = name;
        operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission = createFromProtosFile(flightPlanProtos);
        operator.loadMission(mission);
        this.mainActivity = mainActivity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void update(FlightPlanProtos.FlightPlan flightPlanProtos) {
        status = null;
        operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission = createFromProtosFile(flightPlanProtos);
        operator.loadMission(mission);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(@Nullable Status status) {
        this.status = status;
    }

    @Nullable
    private WaypointMission createFromProtosFile(FlightPlanProtos.FlightPlan flightPlanProtos) {
        WaypointMission.Builder builder = new WaypointMission.Builder();
        List<Waypoint> waypoints = new ArrayList<>();

        // SET-UP BUILDER PARAMETERS
        builder.autoFlightSpeed(15f);
        builder.maxFlightSpeed(15f);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(1);

        // READ THE WAYPOINTS FROM THE PROTO FILE
        if (flightPlanProtos.getFlightElementList().size() > 1) {
            for (FlightPlanProtos.FlightPlan.FlightElement element : flightPlanProtos.getFlightElementList()) {
                double latitude = element.getLatitude();
                double longitude = element.getLongitude();
                double altitude = element.getAltitude();
                int gimbalPitch = element.getGimbalPitch();
                double yaw = element.getWaypointElement().getYaw();

                Waypoint waypoint = new Waypoint(latitude, longitude, (float) altitude);
                waypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, gimbalPitch));
                if (gimbalPitch != -90) {
                    waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int) yaw));
                }
                waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
                waypoints.add(waypoint);
            }
        } else {
            Log.d("createFromProtosFile", "Not enough flight elements, should be > 1! (getFlightElementList().size() = " + flightPlanProtos.getFlightElementList().size() + ")");
            return null;
        }

        builder.waypointList(waypoints).waypointCount(waypoints.size());
        return builder.build();
    }

    public WaypointMissionOperator getOperator() {
        return operator;
    }

    public WaypointMission getMission() {
        return mission;
    }

    public void showOnMap(GoogleMap googleMap) {
        createMarkers(mission.getWaypointList(), googleMap);
        createPolylinePath(mission.getWaypointList(), googleMap);
        zoomTo(googleMap);
    }

    /**
     * Creates markers to show the waypoints on the map. The markers are clickable and show the
     * details (lat, long, alt, pitch and yaw) of the waypoints.
     *
     * @param waypoints The waypoints list containing the locations for the markers.
     * @param googleMap The GoogleMap reference to show the markers on.
     */
    private void createMarkers(List<Waypoint> waypoints, GoogleMap googleMap) {
        if (markers == null) {
            markers = new ArrayList<>();
        } else {
            for (Marker marker : markers) {
                marker.remove();
            }
        }

        for (int i = 0; i < waypoints.size(); i++) {
            LatLng point = new LatLng(waypoints.get(i).coordinate.getLatitude(), waypoints.get(i).coordinate.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions().position(point);

            if (i == 0) { // START POINT OF THE FLIGHT PATH
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            } else if (i == waypoints.size() - 1) { // END POINT OF THE FLIGHT PATH
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }

            markerOptions.title("Waypoint " + (i + 1) + "/" + waypoints.size());
            markerOptions.snippet("Latitude: " + waypoints.get(i).coordinate.getLatitude()
                    + "\n" + "Longitude: " + waypoints.get(i).coordinate.getLongitude()
                    + "\n" + "Altitude: " + waypoints.get(i).altitude);

            Marker marker = googleMap.addMarker(markerOptions);
            marker.setTag(waypoints.get(i));
            markers.add(marker);
        }

        // Set a custom window adapter to show a multiline snippet to the user when they click on a marker.
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Context context = mainActivity;

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    /**
     * Creates a line on the map that represents the path the drone will fly.
     *
     * @param waypoints The list of waypoints.
     * @param googleMap The GoogleMap reference to show the path on.
     */
    private void createPolylinePath(List<Waypoint> waypoints, GoogleMap googleMap) {
        if (path != null) {
            path.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions();

        for (int i = 0; i < waypoints.size() - 1; i++) {
            LatLng latLngBegin = new LatLng(waypoints.get(i).coordinate.getLatitude(), waypoints.get(i).coordinate.getLongitude());
            LatLng latLngEnd = new LatLng(waypoints.get(i + 1).coordinate.getLatitude(), waypoints.get(i + 1).coordinate.getLongitude());

            polylineOptions.add(latLngBegin, latLngEnd);
            polylineOptions.width(5);
            polylineOptions.color(Color.RED);
        }

        path = googleMap.addPolyline(polylineOptions);
    }

    /**
     * Removes the flight path from the map.
     */
    public void removeFromMap() {
        if (markers == null) {
            markers = new ArrayList<>();
        } else {
            Log.d("removeFromMap()", "Removing markers...");
            for (Marker marker : markers) {
                marker.remove();
            }
        }

        if (path != null) {
            path.remove();
        }
    }

    /**
     * Zooms the camera the the flight path.
     *
     * @param googleMap The GoogleMap reference.
     */
    public void zoomTo(GoogleMap googleMap) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(path), 200));
    }

    /**
     * Determines a bounding box around the given path. Used to determine the size of the GoogleMap
     * view when zooming in.
     *
     * @param path The path to create a bounding box around.
     * @return
     */
    private LatLngBounds getBounds(Polyline path) {
        double minLatitude = path.getPoints().get(0).latitude;
        double maxLatitude = path.getPoints().get(0).latitude;
        double minLongitude = path.getPoints().get(0).longitude;
        double maxLongitude = path.getPoints().get(0).longitude;

        for (LatLng point : path.getPoints()) {
            if (point.latitude < minLatitude) minLatitude = point.latitude;
            if (point.latitude > maxLatitude) maxLatitude = point.latitude;
            if (point.longitude < minLongitude) minLongitude = point.longitude;
            if (point.longitude > maxLongitude) maxLongitude = point.longitude;
        }

        return new LatLngBounds(new LatLng(minLatitude, minLongitude), new LatLng(maxLatitude, maxLongitude));
    }

    public enum Status {
        PREPARED,
        STARTED,
        PAUSED,
        RESUMED,
        STOPPED
    }
}