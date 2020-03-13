package com.krpc;

import com.krpc.rocket.Rocket;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.UI;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.IOException;

public class Main {
    public static Connection connection;
    public static SpaceCenter spaceCenter;
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        connection = Connection.newInstance("Server", "127.0.0.1", 50000,50001);
        spaceCenter = SpaceCenter.newInstance(connection);

        double apogee = 15000;
        double lat = -1.540833;
        double lon = -71.90972;

        Rocket rocket = new Rocket();
        rocket.setupRocket();
        rocket.launchRocket(Math.toRadians(lat),Math.toRadians(lon),apogee);

        boolean gravityTurn = true;

        while (true) {
            if (gravityTurn && rocket.getAltitude() > 1000 && rocket.getAltitude() < apogee) {
                rocket.gravityTurn(1000,apogee);
            } else if (rocket.getAltitude() > apogee) {
                gravityTurn = false;
            }

//            pitchText.setContent("Pitch: "+rocket.getPitch());
//            headingText.setContent("Heading: "+rocket.getPitch());
//            System.out.println(rocket.getAttitude().getValue0() + " " + rocket.getAttitude().getValue1());
//            double dist = rocket.distBetweenLocations(rocket.targetLat,rocket.targetLon);
//            System.out.println("("+dist+","+rocket.getAltitude()+")");
            System.out.println(rocket.getLatitude() + " " + rocket.getLongitude());


            Thread.sleep(50);
        }
    }
    public void setupGUI() throws RPCException {
        UI ui = UI.newInstance(connection);
        UI.Canvas canvas = ui.getStockCanvas();
        Pair<Double, Double> screenSize = canvas.getRectTransform().getSize();
        UI.Panel panel = canvas.addPanel(true);
        UI.RectTransform rect = panel.getRectTransform();
        rect.setSize(new Pair<>(200.0, 150.0));
        rect.setPosition(new Pair<>((110-(screenSize.getValue0())/2.0),0.0));
        UI.Text pitchText = panel.addText("Pitch: 0", true);
        pitchText.getRectTransform().setPosition(new Pair<>(0.0, 0.0));
        pitchText.setColor(new Triplet<>(1.0, 1.0, 1.0));
        pitchText.setSize(14);
        UI.Text headingText = panel.addText("Heading: 90", true);
        headingText.getRectTransform().setPosition(new Pair<>(0.0, -30.0));
        headingText.setColor(new Triplet<>(1.0, 1.0, 1.0));
        headingText.setSize(14);
    }
}