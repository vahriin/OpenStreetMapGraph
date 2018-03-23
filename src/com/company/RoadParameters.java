package com.company;

class RoadParameters {
    private String color; // цвет дороги для svg
    private double width; // ширина дороги для svg
    RoadParameters(String input_color, double input_width){
        color = input_color;
        width = input_width;
    }
    String getColor(){
        return color;
    }
    double getWidth(){
        return width;
    }
}
