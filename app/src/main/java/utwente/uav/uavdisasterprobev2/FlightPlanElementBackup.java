package utwente.uav.uavdisasterprobev2;

import android.support.annotation.Nullable;

import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.waypoint.WaypointMission;
import dji.sdk.mission.MissionControl;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos;

/**
 * Created by Mathijs on 4/27/2017.
 */

public class FlightPlanElementBackup {
    private enum Type {
        WAYPOINT_ELEMENT,
        HOTPOINT_ELEMENT
    }

    private Type type;
    private WaypointMission waypointMission;
    private HotpointMission hotpointMission;

    public FlightPlanElementBackup(WaypointMission waypointMission) {
        type = Type.WAYPOINT_ELEMENT;
        hotpointMission = null;
        this.waypointMission = waypointMission;
    }

    public FlightPlanElementBackup(HotpointMission hotpointMission) {
        type = Type.HOTPOINT_ELEMENT;
        waypointMission = null;
        this.hotpointMission = hotpointMission;

    }

    public void setMission(WaypointMission waypointMission) {
        type = Type.WAYPOINT_ELEMENT;
        hotpointMission = null;
        this.waypointMission = waypointMission;
    }

    public void setMission(HotpointMission hotpointMission) {
        type = Type.HOTPOINT_ELEMENT;
        waypointMission = null;
        this.hotpointMission = hotpointMission;
    }

    @Nullable
    public WaypointMission getWaypointMission() {
        return waypointMission;
    }

    @Nullable
    public HotpointMission getHotpointMission() {
        return hotpointMission;
    }

    public void startElement() {
        MissionControl missionControl = MissionControl.getInstance();
        if(missionControl != null) {
            switch (type) {
                case HOTPOINT_ELEMENT:
                    break;
                case WAYPOINT_ELEMENT:
                    break;
            }
        }
    }

}
