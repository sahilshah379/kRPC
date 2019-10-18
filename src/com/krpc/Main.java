package com.krpc;

import com.krpc.rocket.Rocket;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;

public class Main {
    public static Connection connection;
    public static SpaceCenter spaceCenter;
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        connection = Connection.newInstance("Server", "127.0.0.1", 50000,50001);
        spaceCenter = SpaceCenter.newInstance(connection);

        Rocket rocket = new Rocket();
        rocket.setupRocket();

        rocket.launchRocket(5593,Math.toRadians(30));
        System.out.println(rocket.getMaxHeight());
        System.out.println(rocket.getImpactTime());
        System.out.println(rocket.getRange());

        while (true) {

        }
    }
}