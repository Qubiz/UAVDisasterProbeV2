package utwente.uav.uavdisasterprobev2;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos.FlightPlan;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos.FlightPlan.FlightElement;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos.FlightPlan.FlightElement.HotpointElement;
import utwente.uav.uavdisasterprobev2.protos.FlightPlanProtos.FlightPlan.FlightElement.WaypointElement;


/**
 * Created by Mathijs on 4/12/2017.
 */

public class FlightPlanBuilder {

    FlightPlan.Builder flightPlan;
    FlightPlan.FlightElement.Builder flightElement;

    public FlightPlanBuilder() {
        flightPlan = FlightPlan.newBuilder();
    }

    public void addWaypointElement(double latitude,
                                    double longitude,
                                    double altitude,
                                    int gimbalPitch,
                                    double yaw) {

        FlightElement.Builder flightElement = FlightElement.newBuilder();
        WaypointElement.Builder waypointElement = WaypointElement.newBuilder();

        flightElement.setFlightType(FlightElement.FlightType.WAYPOINT)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setAltitude(altitude)
                .setGimbalPitch(gimbalPitch);

        waypointElement.setYaw(yaw);

        flightElement.setWaypointElement(waypointElement);

        flightPlan.addFlightElement(flightElement);
    }

    public void addHotpointElement(double latitude,
                                    double longitude,
                                    double altitude,
                                    int gimbalPitch,
                                    double radius,
                                    double surroundingAngle,
                                    boolean clockwise,
                                    int photos,
                                    HotpointElement.StartPoint startPoint) {

        FlightElement.Builder flightElement = FlightElement.newBuilder();
        HotpointElement.Builder hotpointElement = HotpointElement.newBuilder();

        flightElement.setFlightType(FlightElement.FlightType.WAYPOINT)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setAltitude(altitude)
                .setGimbalPitch(gimbalPitch);

        hotpointElement.setRadius(radius)
                .setSurroundingAngle(surroundingAngle)
                .setClockwise(clockwise)
                .setPhotos(photos)
                .setStartPoint(startPoint);

        flightElement.setHotpointElement(hotpointElement);

        flightPlan.addFlightElement(flightElement);
    }

    public void writeToFile() {
        if(flightPlan.getFlightElementList().size() > 0) {
            try {
                File folder = new File(Environment.getExternalStorageDirectory(), "FlightPlans");

                if(!folder.exists()) {
                    if(folder.mkdir()) {
                        //TODO: LOG FOLDER CREATED
                    } else {
                        //TODO: LOG ERROR WHEN CREATING FOLDER
                    }
                }

                File file = new File(folder, "flight" + ".fp");

                if(!file.exists()) {
                    if(file.createNewFile()) {
                        //TODO: LOG FILE CREATED
                    } else {
                        //TODO: LOG ERROR WHEN CREATING FILE
                    }
                }

                FileOutputStream outputStream = new FileOutputStream(file);
                flightPlan.build().writeDelimitedTo(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
