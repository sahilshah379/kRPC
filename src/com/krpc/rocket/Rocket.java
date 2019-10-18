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

    private final double INCREMENT = 10e-4;
    private final double R = 6378000;//600000; // equatorial radius of kerbin
    private final double g = 9.81; // surface gravity
    private double v_0; // initial velocity
    private double b; // launch angle
    private double t_0; // time of launch
    private double t_i; // time of impact
    private double H; // max height
    private double L; // range
    private double a;

    private Vessel vessel;

    public Rocket() throws RPCException {
        this.vessel = Main.spaceCenter.getActiveVessel();
    }

    public void setupRocket() throws RPCException, StreamException {
        Flight flight = vessel.flight(vessel.getOrbit().getBody().getReferenceFrame());
        altitudeStream = Main.connection.addStream(flight, "getMeanAltitude");
        velocityStream = Main.connection.addStream(flight, "getSpeed");
    }
    public void launchRocket(double v_0, double b) throws RPCException {
        this.v_0 = v_0;
        this.b = b;
        this.a = 2*g*R/(v_0*v_0);
        this.t_0 = System.currentTimeMillis();

        vessel.getControl().setSAS(false);
        vessel.getControl().setRCS(false);
        vessel.getControl().setThrottle(1);
        vessel.getControl().activateNextStage();

        vessel.getAutoPilot().engage();
        vessel.getAutoPilot().targetPitchAndHeading(45,0);

        setVelocity(100);

        t_i = calcImpactTime()*1000+t_0;
        H = calcMaxHeight();
        L = calcRange();
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
        if (targetVelocity == -1) {
            velocityThread.interrupt();
        } else {
            if (velocityThread == null) {
                velocityThread = new VelocityThread(this, targetVelocity);
                velocityThread.start();
            } else {
                velocityThread.setTargetVelocity(targetVelocity);
                if (!velocityThread.isAlive()) {
                    velocityThread.start();
                }
            }
        }
    }
    public double calcMaxHeight() {
        return R*(-1+((a+Math.sqrt(a*a-4*(a-1)*Math.pow(Math.cos(b),2)))/(2*(a-1))));
    }
    private double getRadiusRatio(double y) {
        return y/Math.sqrt(y*y*(1-a)+a*y-Math.pow(Math.cos(b),2));
    }
    public double calcImpactTime() {
        double area = 0;
        for(double i = 1 + INCREMENT; i < (1+H/R); i += INCREMENT) {
            double dFromA = i - 1;
            area += (INCREMENT / 2) * (getRadiusRatio(1 + dFromA) + getRadiusRatio(1 + dFromA - INCREMENT));
        }

        return (2*R/v_0) * area;
    }
    public double calcRange() {
        return ((t_i-t_0)/1000)*v_0*Math.cos(b);
    }
    public double getInitialVelocity() {
        return v_0;
    }
    public double getInitialAngle() {
        return b;
    }
    public double getLaunchTime() {
        return t_0;
    }
    public double getImpactTime() {
        return t_i;
    }
    public double getRange() {
        return L;
    }
    public double getMaxHeight() {
        return H;
    }
}
