package com.krpc;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.KRPC;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Vessel;
import org.javatuples.Triplet;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, RPCException, StreamException {
        Connection connection = Connection.newInstance("Server", "127.0.0.1", 50000,50001);
        try (connection) {
            System.out.println(KRPC.newInstance(connection).getStatus().getVersion());
            SpaceCenter spaceCenter = SpaceCenter.newInstance(connection);
            Vessel vessel = spaceCenter.getActiveVessel();

            Rocket rocket = new Rocket();
            rocket.setupRocket(connection, vessel);

            vessel.getControl().setSAS(false);
            vessel.getControl().setRCS(false);
            vessel.getControl().setThrottle(1);
            vessel.getControl().activateNextStage();

            while (true) {
                Triplet<Double,Double,Double> attitude = rocket.getAttitude(spaceCenter, vessel);
                double altitude = rocket.getAltitude();
                System.out.printf("altitude = %.1f, pitch = %.1f, yaw = %.1f, roll = %.1f\n", altitude, attitude.getValue0(), attitude.getValue1(), attitude.getValue2());
            }
        }
    }
}