package com.libs.disonoVLC;

import java.util.ArrayList;

public class VlcOptions {
    public ArrayList<String> getDefaultOptions() {
        ArrayList<String> options = new ArrayList<>();
        options.add("-vvv");
        return options;
    }
}
