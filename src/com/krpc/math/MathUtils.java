package com.krpc.math;

import org.javatuples.Quartet;
import org.javatuples.Triplet;


public class MathUtils {
    private static final double epsilon = 0.01;

    public static int sgn(double n) {
        if (MathUtils.equals(n, 0)) {
            return 0;
        } else if (n > 0) {
            return 1;
        } else {
            return -1;
        }
    }
    public static double sinDegrees(double  d) {
        return Math.sin(Math.toRadians(d));
    }
    public static double cosDegrees(double  d) {
        return Math.cos(Math.toRadians(d));
    }
    public static double tanDegrees(double d) {
        return Math.tan(Math.toRadians(d));
    }
    public static double clamp(double number, double lowerBound, double upperBound) {
        return Math.max(lowerBound, Math.min(upperBound, number));
    }
    public static double truncate(double d, int place) {
        return (double) ((int)(d * place)) / place;
    }
    public static boolean equals(double d, double e) {
        return equals(d,e,epsilon);
    }
    public static boolean equals(double d, double e, double tolerance) {
        if (Double.isNaN(d) || Double.isNaN(e))
            return false;
        return Math.abs(d-e) < tolerance;
    }

    public static double wrap(double d, double min, double max) {
        if (!equals(d, min) &&d < min) {
            d = max - (min - d);
        } else if (!equals(d, max) && d > max) {
            d = min + (d - max);
        }
        return d;
    }

    public static double standardDeviation(int[] values) {
        int len = values.length;
        double mean = 0;
        for (int i = 0; i < len; i++) {
            mean += values[i];
        }
        mean /= len;
        double stddev = 0;
        for (int i = 0; i < len; i++) {
            double diff = values[i] - mean;
            stddev += diff * diff;
        }
        stddev /= len;
        return Math.sqrt(stddev);
    }
    public static double map(double value, double lower1, double upper1, double lower2, double upper2) {
        return ((value-lower1)*(upper2-lower2)/(upper1-lower1))+lower2;
    }
    public static double angleBetweenVectors(Triplet<Double, Double, Double> u, Triplet<Double, Double, Double> v) {
        double dp = u.getValue0()*v.getValue0()+u.getValue1()*v.getValue1()+u.getValue2()*v.getValue2();
        double mu = Math.sqrt(Math.pow(u.getValue0(),2)+Math.pow(u.getValue1(),2)+Math.pow(u.getValue2(),2));
        double mv = Math.sqrt(Math.pow(v.getValue0(),2)+Math.pow(v.getValue1(),2)+Math.pow(v.getValue2(),2));
        return Math.toDegrees(Math.acos(dp/(mu*mv)));
    }
    public static Triplet<Double,Double,Double> crossProduct(Triplet<Double, Double, Double> u, Triplet<Double, Double, Double> v) {
        return new Triplet<>(
                u.getValue1()*v.getValue2()-u.getValue2()*v.getValue1(),
                u.getValue2()*v.getValue0()-u.getValue0()*v.getValue2(),
                u.getValue0()*v.getValue1()-u.getValue1()*v.getValue0()
        );
    }
    public static Triplet multiply(Quartet<Double,Double,Double,Double> quat, Triplet<Double,Double,Double> vec){
        double x = quat.getValue0();
        double y = quat.getValue1();
        double z = quat.getValue2();
        double w = quat.getValue3();
        double vx = vec.getValue0();
        double vy = vec.getValue1();
        double vz = vec.getValue2();

        double x1 = 1-2*(y*y+z*z);
        double y1 = 2*(x*y-z*w);
        double z1 = 2*(x*z+y*w);
        double x2 = 2*(x*y+z*w);
        double y2 = 1-2*(x*x+z*z);
        double z2 = 2*(y*z-x*w);
        double x3 = 2*(x*z-y*w);
        double y3 = 2*(y*z+x*w);
        double z3 = 1-2*(x*x+y*y);

        double rx = x1*vx+y1*vy+z1*vz;
        double ry = x2*vx+y2*vy+z2*vz;
        double rz = x3*vx+y3*vy+z3*vz;

        return new Triplet<>(rx, ry, rz);
    }
    public static Quartet inverse(Quartet<Double,Double,Double,Double> quat) {
        return new Quartet<>(-quat.getValue0(), -quat.getValue1(), -quat.getValue2(), quat.getValue3());
    }
    public static Triplet inverse(Triplet<Double,Double,Double> vec) {
        return new Triplet<>(-vec.getValue0(), -vec.getValue1(), -vec.getValue2());
    }
}