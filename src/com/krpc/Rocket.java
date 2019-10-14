package com.krpc;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.KRPC;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import org.javatuples.Triplet;

import java.io.IOException;

public class Rocket {
    public static void main(String[] args) throws IOException, RPCException, StreamException {
        try (Connection connection = Connection.newInstance("Server", "127.0.0.1", 50000,50001)) {
            System.out.println(KRPC.newInstance(connection).getStatus().getVersion());
            SpaceCenter spaceCenter = SpaceCenter.newInstance(connection);
            Vessel vessel = spaceCenter.getActiveVessel();

            spaceCenter.getUT();
            Stream<Double> ut = connection.addStream(SpaceCenter.class, "getUT");
            ReferenceFrame refFrame = vessel.getSurfaceReferenceFrame();
            Flight flight = vessel.flight(refFrame);
            Stream<Double> altitude = connection.addStream(flight, "getMeanAltitude");

            vessel.getControl().setSAS(false);
            vessel.getControl().setRCS(false);
            vessel.getControl().setThrottle(1);
            vessel.getControl().activateNextStage();

            while (true) {
                Triplet<Double,Double,Double> attitude = getAttitude(spaceCenter, vessel);
                System.out.printf("pitch = %.1f, yaw = %.1f, roll = %.1f\n", attitude.getValue0(), attitude.getValue1(), attitude.getValue2());
            }
        }
    }

    private static Triplet<Double,Double,Double> getAttitude(SpaceCenter spaceCenter, Vessel vessel) throws RPCException {
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
        Triplet<Double,Double,Double> vesselX = spaceCenter.transformDirection(new Triplet<>(0.0,0.0,-1.0), vessel.getReferenceFrame(), vessel.getSurfaceReferenceFrame());
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
    private static double angleBetweenVectors(Triplet<Double,Double,Double> u, Triplet<Double,Double,Double> v) {
        double dp = u.getValue0()*v.getValue0()+u.getValue1()*v.getValue1()+u.getValue2()*v.getValue2();
        double mu = Math.sqrt(Math.pow(u.getValue0(),2)+Math.pow(u.getValue1(),2)+Math.pow(u.getValue2(),2));
        double mv = Math.sqrt(Math.pow(v.getValue0(),2)+Math.pow(v.getValue1(),2)+Math.pow(v.getValue2(),2));
        return Math.toDegrees(Math.acos(dp/(mu*mv)));
    }

    private static Triplet<Double,Double,Double> crossProduct(Triplet<Double,Double,Double> u, Triplet<Double,Double,Double> v) {
        return new Triplet<>(
                u.getValue1()*v.getValue2()-u.getValue2()*v.getValue1(),
                u.getValue2()*v.getValue0()-u.getValue0()*v.getValue2(),
                u.getValue0()*v.getValue1()-u.getValue1()*v.getValue0()
        );
    }
}