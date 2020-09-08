package com.libs.vlcLibWrapper;

import java.util.ArrayList;

public class VlcOptions {
    public ArrayList<String> getDefaultOptions() {
        ArrayList<String> options = new ArrayList<>();
        options.add("-vvv");
        return options;
    }
}
