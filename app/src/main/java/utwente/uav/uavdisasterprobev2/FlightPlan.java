package utwente.uav.uavdisasterprobev2;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.timeline.Mission;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.AircraftYawAction;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

/**
 * Created by Mathijs on 4/11/2017.
 */

public class FlightPlan {

    private FlightPlanProtos.FlightPlan flightPlan;
    List<TimelineElement> elements;

    public FlightPlan(FlightPlanProtos.FlightPlan flightPlan) {
        this.flightPlan = flightPlan;
        createTimeline();
    }

    private void createTimeline() {
        elements = new ArrayList<>();

        MissionControl missionControl = MissionControl.getInstance();

        MissionControl.Listener missionControlListener = new MissionControl.Listener() {
            @Override
            public void onEvent(@Nullable TimelineElement element, TimelineEvent event, @Nullable DJIError error) {

            }
        };

        // FIRST TIMELINE ELEMENT IS ALWAYS THE TAKEOFF ACTION
        elements.add(new TakeOffAction());

        // GET TIMELINE ELEMENTS FROM FLIGHT PLAN DATA
        for(FlightPlanProtos.FlightPlan.FlightElement flightElement : flightPlan.getFlightElementList()) {
            elements.add(new GimbalAttitudeAction(new Attitude(flightElement.getGimbalPitch(), 0, 0)));
            switch(flightElement.getFlightType()) {
                case WAYPOINT:
                    elements.add(new GoToAction(new LocationCoordinate2D(flightElement.getLatitude(), flightElement.getLongitude()), (float) flightElement.getAltitude()));
                    elements.add(new AircraftYawAction((float) flightElement.getWaypointElement().getYaw(), 100));
                    // elements.add(createWaypointElement(flightElement));
                    break;
                case HOTPOINT:
                    elements.add(createHotpointElement(flightElement));
                    break;
            }
            elements.add(new ShootPhotoAction());
        }

        // LAST TIMELINE ELEMENT IS ALWAYS THE GO HOME ACTION
        elements.add(new GoHomeAction());

        // EMPTY THE TIMELINE IF THERE ARE SCHEDULED TIMELINE ELEMENTS
        if(missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }

        // SCHEDULE NEW TIMELINE ELEMENTS AND ADD THE LISTENER
        missionControl.scheduleElements(elements);
        missionControl.addListener(missionControlListener);
    }

    private TimelineElement createWaypointElement(FlightPlanProtos.FlightPlan.FlightElement flightElement) {
        TimelineElement timelineElement;

        WaypointMission.Builder waypointMissionBuilder = new WaypointMission.Builder();
        waypointMissionBuilder.autoFlightSpeed(10f);
        waypointMissionBuilder.maxFlightSpeed(10f);
        waypointMissionBuilder.setExitMissionOnRCSignalLostEnabled(false);
        waypointMissionBuilder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        waypointMissionBuilder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        waypointMissionBuilder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        waypointMissionBuilder.headingMode(WaypointMissionHeadingMode.AUTO);
        waypointMissionBuilder.repeatTimes(1);

        Waypoint waypoint = new Waypoint(flightElement.getLatitude(), flightElement.getLongitude(), (float) flightElement.getAltitude());
        waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int) flightElement.getWaypointElement().getYaw()));

        waypointMissionBuilder.addWaypoint(waypoint);

        timelineElement = Mission.elementFromWaypointMission(waypointMissionBuilder.build());

        String error = (waypointMissionBuilder.checkParameters() == null) ? "No Error" : waypointMissionBuilder.checkParameters().getDescription();

        Log.d("createWaypointElement", error);

        return timelineElement;
    }

    private TimelineElement createHotpointElement(FlightPlanProtos.FlightPlan.FlightElement flightElement) {
        TimelineElement timelineElement;

        HotpointMission hotpointMission = new HotpointMission();
        hotpointMission.setHotpoint(new LocationCoordinate2D(flightElement.getLatitude(), flightElement.getLongitude()));
        hotpointMission.setAltitude(flightElement.getAltitude());
        hotpointMission.setRadius(flightElement.getHotpointElement().getRadius());
        hotpointMission.setClockwise(flightElement.getHotpointElement().getClockwise());
        hotpointMission.setAngularVelocity((float) HotpointMissionOperator.maxAngularVelocityForRadius(hotpointMission.getRadius()));
        hotpointMission.setHeading(HotpointHeading.TOWARDS_HOT_POINT);

        switch(flightElement.getHotpointElement().getStartPoint()) {
            case NORTH:
                hotpointMission.setStartPoint(HotpointStartPoint.NORTH);
                break;
            case EAST:
                hotpointMission.setStartPoint(HotpointStartPoint.EAST);
                break;
            case WEST:
                hotpointMission.setStartPoint(HotpointStartPoint.WEST);
                break;
            case SOUTH:
                hotpointMission.setStartPoint(HotpointStartPoint.SOUTH);
                break;
            case NEAREST:
                hotpointMission.setStartPoint(HotpointStartPoint.NEAREST);
                break;
        }

        timelineElement = new HotpointAction(hotpointMission, (float) flightElement.getHotpointElement().getSurroundingAngle());

        String error = (hotpointMission.checkParameters() == null) ? "No Error" : hotpointMission.checkParameters().getDescription();

        Log.d("createHotpointElement", error);

        return timelineElement;
    }

    public void start() {
        if(MissionControl.getInstance().scheduledCount() > 0) {
            MissionControl.getInstance().startTimeline();
            Log.d("START", "LETS GO!");
        }
    }

    public void pause() {
        MissionControl.getInstance().pauseTimeline();
    }

    public void resume() {
        MissionControl.getInstance().resumeTimeline();
    }

    public void stop() {
        MissionControl.getInstance().stopTimeline();
    }

    public void updateFlightPlan(FlightPlanProtos.FlightPlan flightPlan) {
        this.flightPlan = flightPlan;
        createTimeline();
    }

    public void verify() {

    }

}
