package com.jeet.utils;

public class Timer {
    long t;

    // constructor
    public Timer() {
            reset();
    }

    // reset timer
    public void reset() {
            t = System.nanoTime();
    }

    // return elapsed time

    private long elapsed() {
            return System.nanoTime() - t;
    }

    // print explanatory string and elapsed time

    public void print(String s) {
            System.out.println(s + ": " + elapsed());
    }
}