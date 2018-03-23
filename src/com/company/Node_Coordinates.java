package com.company;

public class Node_Coordinates {
    private double lat, lon, euclid_X, euclid_Y;
    final private static double R_MAJOR = 6378137.0;
    final private static double R_MINOR = 6356752.3142;
    final private static double Multiplier = 1E-2;
    static double X, Y;

    static double  mercX(double lon) { return R_MAJOR * Math.toRadians(lon); }

    static double mercY(double lat) {
        if (lat > 89.5) {
            lat = 89.5;
        }
        if (lat < -89.5) {
            lat = -89.5;
        }
        double temp = R_MINOR / R_MAJOR;
        double es = 1.0 - (temp * temp);
        double eccent = Math.sqrt(es);
        double phi = Math.toRadians(lat);
        double sinphi = Math.sin(phi);
        double con = eccent * sinphi;
        double com = 0.5 * eccent;
        con = Math.pow(((1.0-con)/(1.0+con)), com);
        double ts = Math.tan(0.5 * ((Math.PI*0.5) - phi))/con;
        double y = 0 - R_MAJOR * Math.log(ts);
        return y;
    }

    Node_Coordinates(double input_lat, double input_lon){
        lat = input_lat;
        lon = input_lon;
        euclid_X = mercX(input_lon);
        euclid_Y = mercY(input_lat);
    }

    void set_coordinates(){
        euclid_X = (euclid_X - X) * Multiplier;
        euclid_Y = (euclid_Y - Y) * Multiplier;
    }

    double get_variable(String variable_name){
        switch (variable_name){
            case "lat":
                return lat;
            case "lon":
                return lon;
            case "euclid_X":
                return euclid_X;
            case "euclid_Y":
                return euclid_Y;
        }
        return 0;
    }

    @Override
    public String toString() {
        return " lat = " + lat + " lon = " + lon;
    }
}
