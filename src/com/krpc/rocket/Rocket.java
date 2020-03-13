package com.krpc.rocket;

import com.krpc.Main;
import com.krpc.math.MathUtils;
import com.krpc.math.PIDController;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.Vessel;
import org.javatuples.Quartet;
import org.javatuples.Triplet;

import static com.krpc.math.MathUtils.angleBetweenVectors;
import static com.krpc.math.MathUtils.crossProduct;

public class Rocket {
    private Stream<Double> altitudeStream, velocityStream, latitudeStream, longitudeStream;

    public final double R = 6e5; // equatorial radius of kerbin
    public final double M = 5.2915158e22; // mass of kerbin
    public final double G = 6.67408e-11; // universal gravitational constant
    public final double g = 9.81; // surface gravity

    public double targetLat;
    public double targetLon;
    private float targetHeading = 90;
    private double apogee;
    private double distance;

    private Vessel vessel;
    private PIDController controller = new PIDController(2.5,1.2,0,90, 0.1);

    public Rocket() throws RPCException {
        this.vessel = Main.spaceCenter.getActiveVessel();
    }

    public void setupRocket() throws RPCException, StreamException {
        Flight flight = vessel.flight(vessel.getOrbit().getBody().getReferenceFrame());
        altitudeStream = Main.connection.addStream(flight, "getMeanAltitude");
        velocityStream = Main.connection.addStream(flight, "getSpeed");
        latitudeStream = Main.connection.addStream(flight, "getLatitude");
        longitudeStream = Main.connection.addStream(flight, "getLongitude");
    }
    public double distBetweenLocations(double lat2, double lon2) throws RPCException, StreamException {
        double lat1 = latitudeStream.get()*Math.PI/180.0;
        double lon1 = longitudeStream.get()*Math.PI/180.0;
        double dLat = lat2-lat1;
        double dLon = lon2-lon1;
        double a = Math.sin(dLat/2.0)*Math.sin(dLat/2.0)+Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2.0)*Math.sin(dLon/2.0);
        double c = 2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
        return R*c;
    }
    public double angleBetweenLocations(double lat2, double lon2) throws RPCException, StreamException {
        double lat1 = latitudeStream.get()*Math.PI/180.0;
        double lon1 = longitudeStream.get()*Math.PI/180.0;
        double dLon = lon2-lon1;

        double y = Math.sin(dLon)*Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        return Math.toDegrees(Math.atan2(y,x));
    }
    public void launchRocket(double lat, double lon, double a) throws RPCException, StreamException {
        this.distance = distBetweenLocations(lat,lon);
        this.targetHeading = (float)angleBetweenLocations(lat,lon);
        System.out.println(distance + "  " + targetHeading);
        this.apogee = a;
        targetLat = getLatitude();
        targetLon = getLongitude();

        vessel.getControl().setSAS(false);
        vessel.getControl().setRCS(true);
        vessel.getControl().setThrottle(1);
        vessel.getControl().activateNextStage();

        vessel.getAutoPilot().engage();
        vessel.getAutoPilot().targetPitchAndHeading(90,targetHeading);
    }
    public Vessel getVessel() {
        return vessel;
    }
    public double getAltitude() throws RPCException, StreamException {
        return altitudeStream.get();
    }
    public double getPitch() throws RPCException, StreamException {
        return getAttitude().getValue0();
    }
    public double getHeading() throws RPCException, StreamException {
        return getAttitude().getValue1();
    }
    public double getLatitude() throws RPCException, StreamException {
        return latitudeStream.get();
    }
    public double getLongitude() throws RPCException, StreamException {
        return longitudeStream.get();
    }
    public double getVelocity() throws RPCException, StreamException {
        return velocityStream.get();
    }
    public Triplet<Double,Double,Double> getAttitude() throws RPCException {
        Triplet<Double,Double,Double> vesselVector = vessel.direction(vessel.getSurfaceReferenceFrame());
        Triplet<Double,Double,Double> surfaceVector = new Triplet<>(0.0, vesselVector.getValue1(), vesselVector.getValue2());
        double pitch = angleBetweenVectors(vesselVector, surfaceVector);
        if (vesselVector.getValue0() < 0) {
            pitch *= -1;
        }

        Triplet<Double,Double,Double> yVector = new Triplet<>(0.0,1.0,0.0);
        double yaw = angleBetweenVectors(yVector, surfaceVector);
        if (surfaceVector.getValue2() < 0) {
            yaw = 360 - yaw;
        }

        Triplet<Double,Double,Double> xVector = new Triplet<>(1.0,0.0,0.0);
        Triplet<Double,Double,Double> planeNormal = crossProduct(vesselVector, xVector);
        Triplet<Double,Double,Double> vesselX = Main.spaceCenter.transformDirection(new Triplet<>(0.0,0.0,-1.0), vessel.getReferenceFrame(), vessel.getSurfaceReferenceFrame());
        double roll = angleBetweenVectors(vesselX, planeNormal);
        if (vesselX.getValue0() > 0) {
            roll *= -1;
        } else if (roll < 0) {
            roll += 180;
        } else {
            roll -= 180;
        }

        return new Triplet<>(pitch, yaw, roll);
    }
    public Triplet<Double,Double,Double> getAngularVelocity() throws RPCException {
        Triplet direction = vessel.angularVelocity(vessel.getSurfaceReferenceFrame());
        Quartet rotation = vessel.rotation(vessel.getSurfaceReferenceFrame());
        Triplet<Double,Double,Double> worldDirection = MathUtils.multiply(rotation,direction);
        Triplet<Double,Double,Double> angularVelocity = MathUtils.inverse(MathUtils.multiply(MathUtils.inverse(rotation),worldDirection));
        return angularVelocity;
    }
    public void gravityTurn(double h_g1, double h_g2) throws RPCException, StreamException {
        double h = getAltitude();

        float angle = (float)MathUtils.map(h,h_g1,h_g2,90,0);
        controller.setTarget(angle);

        double output = controller.getPIDOutput(getPitch());
//        System.out.println(getPitch() + " " + angle + " " + (getPitch()-angle));
        vessel.getAutoPilot().targetPitchAndHeading((float)(getPitch()+output),targetHeading);
    }
    public void stopRocket() throws RPCException {
        vessel.getControl().setThrottle(0);
        vessel.getAutoPilot().setReferenceFrame(vessel.getOrbitalReferenceFrame());
    }
}
