package com.krpc;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import org.javatuples.Triplet;

import static com.krpc.math.MathUtils.angleBetweenVectors;
import static com.krpc.math.MathUtils.crossProduct;

public class Rocket {
    private Stream<Double> altitudeStream;

    public Rocket() {}

    public void setupRocket(Connection connection, Vessel vessel) throws RPCException, StreamException {
        ReferenceFrame refFrame = vessel.getSurfaceReferenceFrame();
        Flight flight = vessel.flight(refFrame);
        altitudeStream = connection.addStream(flight, "getMeanAltitude");
    }
    public double  getAltitude() throws RPCException, StreamException {
        return altitudeStream.get();
    }
    public Triplet<Double,Double,Double> getAttitude(SpaceCenter spaceCenter, Vessel vessel) throws RPCException {
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
}
