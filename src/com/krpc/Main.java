package com.krpc;

import com.krpc.rocket.Rocket;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import org.javatuples.Triplet;

import java.io.IOException;

public class Main {
    public static Connection connection;
    public static SpaceCenter spaceCenter;
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        connection = Connection.newInstance("Server", "127.0.0.1", 50000,50001);
        spaceCenter = SpaceCenter.newInstance(connection);

        Rocket rocket = new Rocket();
        rocket.setupRocket();
        rocket.launchRocket(100000,25000, 1000, 5000);

        boolean thrust = true;

        while (true) {
            if (thrust && rocket.getAltitude() < rocket.getBurnoutAltitude()) {
                if (rocket.getAltitude() > rocket.getLowerGravityTurnAltitude() && rocket.getAltitude() < rocket.getUpperGravityTurnAltitude()) {
                    rocket.gravityTurn();
                }
            } else {
                if (thrust) {
                    rocket.stopRocket();
                    thrust = false;
                }
                rocket.getVessel().getAutoPilot().setTargetDirection(new Triplet<>(0.0, 1.0, 0.0));
            }
            Thread.sleep(10);
        }
    }
}