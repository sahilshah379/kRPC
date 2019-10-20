package com.krpc.rocket;

import com.krpc.Main;
import com.krpc.math.MathUtils;
import com.krpc.rocket.threads.VelocityThread;
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
    private Stream<Double> altitudeStream;
    private Stream<Double> velocityStream;

    private VelocityThread velocityThread;

    private final double R = 6e5; // equatorial radius of kerbin
    private final double M = 5.2915158e22; // mass of kerbin
    private final double G = 6.67408e-11; // universal gravitational constant
    private final double g = 9.81; // surface gravity

    private double h_g1; // gravitational turn lower altitude
    private double h_g2; // gravitational turn lower altitude
    private double v_0; // initial velocity
    private double h_b; // burnout altitude
    private double thetaMax; // ellipse angle

    private double gravityTurnAngle = 0;

    private Vessel vessel;

    public Rocket() throws RPCException {
        this.vessel = Main.spaceCenter.getActiveVessel();
    }

    public void setupRocket() throws RPCException, StreamException {
        Flight flight = vessel.flight(vessel.getOrbit().getBody().getReferenceFrame());
        altitudeStream = Main.connection.addStream(flight, "getMeanAltitude");
        velocityStream = Main.connection.addStream(flight, "getSpeed");
    }
    public void launchRocket() throws RPCException {
        vessel.getControl().setSAS(false);
        vessel.getControl().setRCS(true);
        vessel.getControl().setThrottle(1);
        vessel.getControl().activateNextStage();
    }
    public void launchRocket(double L, double h_b, double h_g1, double h_g2) throws RPCException, StreamException {
        this.h_b = h_b;
        this.h_g1 = h_g1;
        this.h_g2 = h_g2;

        this.thetaMax = L/(2*R);
        this.v_0 = Math.sqrt(2*G*M/R)*Math.sqrt(Math.sin(thetaMax)/(1+Math.sin(thetaMax)));

        vessel.getControl().setSAS(false);
        vessel.getControl().setRCS(true);
        vessel.getControl().setThrottle(1);
        vessel.getControl().activateNextStage();

        vessel.getAutoPilot().engage();
        vessel.getAutoPilot().targetPitchAndHeading(90,90);

        setVelocity(v_0);
    }
    public Vessel getVessel() {
        return vessel;
    }
    public double getAltitude() throws RPCException, StreamException {
        return altitudeStream.get();
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
    public void setVelocity(double targetVelocity) {
        if (velocityThread == null || !velocityThread.isAlive()) {
            velocityThread = new VelocityThread(this, targetVelocity);
            velocityThread.start();
        } else {
            velocityThread.setTargetVelocity(targetVelocity);
        }
    }
    public void gravityTurn() throws RPCException, StreamException {
        double h = getAltitude();

        double angle = ((h-h_g1)/(h_g2-h_g1))*getLaunchAngle();
        if (Math.abs(angle-gravityTurnAngle) > 0.5) {
            gravityTurnAngle = angle;
            vessel.getAutoPilot().targetPitchAndHeading((float)(90-gravityTurnAngle), 90);
        }
    }
    public void stopRocket() throws RPCException {
        velocityThread.interrupt();
        vessel.getControl().setThrottle(0);
        vessel.getAutoPilot().setReferenceFrame(vessel.getOrbitalReferenceFrame());
    }
    public double getMaxAltitude() {
        return (((-G*M)/((.5*v_0*v_0)-((G*M)/R)))-R);
    }
    public double getLaunchAngle() {
        return Math.toDegrees(Math.PI/4-thetaMax/2);
    }
    public double getRange() {
        return 2*R*thetaMax;
    }
    public double getInitialVelocity() {
        return v_0;
    }
    public double getBurnoutAltitude() {
        return h_b;
    }
    public double getLowerGravityTurnAltitude() {
        return h_g1;
    }
    public double getUpperGravityTurnAltitude() {
        return h_g2;
    }
}
