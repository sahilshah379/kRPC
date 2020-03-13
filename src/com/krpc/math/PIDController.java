package com.krpc.math;

public class PIDController {
    private double kP;
    private double kI;
    private double kD;

    private double target;
    private double maxI = Double.MAX_VALUE;
    private double lastTime;
    private double threshold;

    private double P = 0;
    private double I = 0;
    private double D = 0;

    public PIDController(double kP, double kI, double kD, double target, double threshold) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.target = target;
        this.threshold = threshold;

        lastTime = System.currentTimeMillis();
    }
    public double getPIDOutput(double currentValue) {
        double deltaTime = (System.currentTimeMillis()-lastTime)/1000.0;
        D = ((target - currentValue) - P)/deltaTime;
        P = target - currentValue;

        if (Math.abs(currentValue - target) > threshold) {
            I = deltaTime * P + I;
        } else {
            I = 0;
        }

        I = Math.min(I,maxI);
        lastTime = System.currentTimeMillis();
        return kP * P + kI * I + kD * D;
    }

    public void setKP(double kP) {
        this.kP = kP;
    }

    public void setKI(double kI) {
        this.kI = kI;
    }

    public void setKD(double kD) {
        this.kD = kD;
    }
    public void setTarget(double target) {
        this.target = target;
    }
}