package utwente.uav.uavdisasterprobev2;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

/**
 * Created by Mathijs on 4/11/2017.
 */

public class FlightPlan /*implements WaypointMissionOperatorListener*/ {

    private WaypointMissionOperator operator;
    private WaypointMission mission;
    private Status status = null;

    public FlightPlan(FlightPlanProtos.FlightPlan flightPlanProtos) {
        operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission = createFromProtosFile(flightPlanProtos);
        operator.loadMission(mission);
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
                if(gimbalPitch != -90) {
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

    /*
    @Override
    public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent event) {
        if (event.getProgress() != null
                && event.getProgress().isSummaryDownloaded
                && event.getProgress().downloadedWaypointIndex == (event.getProgress().totalWaypointCount - 1)) {
            //Toast.makeText(UAVDisasterProbeApplication.getContext(), "Download successful!", Toast.LENGTH_SHORT).show();
            Log.d("FlightPlan", "Download successful!");
        }
    }

    @Override
    public void onUploadUpdate(@NonNull WaypointMissionUploadEvent event) {
        if (event.getProgress() != null
                && event.getProgress().isSummaryUploaded
                && event.getProgress().uploadedWaypointIndex == (event.getProgress().totalWaypointCount - 1)) {
            //Toast.makeText(UAVDisasterProbeApplication.getContext(), "Upload successful!", Toast.LENGTH_SHORT).show();
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
        //Toast.makeText(UAVDisasterProbeApplication.getContext(), "Execution started!", Toast.LENGTH_SHORT).show();
        Log.d("FlightPlan", "Execution started!");
    }

    @Override
    public void onExecutionFinish(@Nullable DJIError djiError) {
        //Toast.makeText(UAVDisasterProbeApplication.getContext(), "Execution finished!", Toast.LENGTH_SHORT).show();
        Log.d("FlightPlan", "Execution finished!");
    }
    */

    public WaypointMissionOperator getOperator() {
        return operator;
    }

    public WaypointMission getMission() {
        return mission;
    }

    public enum Status {
        PREPARED,
        STARTED,
        PAUSED,
        RESUMED,
        STOPPED
    }
}