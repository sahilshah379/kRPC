package com.krpc.rocket.threads;

import com.krpc.rocket.Rocket;

public class VelocityThread extends Thread {
    final long update = 100;

    private Rocket rocket;
    private double targetVelocity;

    public VelocityThread(Rocket rocket, double targetVelocity)  {
        this.targetVelocity = targetVelocity;
        this.rocket = rocket;
    }
    public void setTargetVelocity(double targetVelocity) {
        this.targetVelocity = targetVelocity;
    }
    public void run() {
        while (!Thread.interrupted()) {
            try {
                double currentVelocity = rocket.getVelocity();
                double thrust = 0;
                if (currentVelocity/targetVelocity > 1) {
                    thrust = 0.5 - (currentVelocity/targetVelocity)/3;
                } else if (currentVelocity/targetVelocity < 0.5) {
                    thrust = 1;
                } else {
                    thrust = (1-currentVelocity/targetVelocity)+0.5;
                }

                rocket.getVessel().getControl().setThrottle((float)thrust);

                Thread.sleep(update);
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
