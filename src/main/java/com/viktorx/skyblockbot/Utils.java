package com.viktorx.skyblockbot;

import java.util.function.LongSupplier;

public class Utils {

    public static LongSupplier nanoTimeSupplier = System::nanoTime;

    public static long getNanoTime() {
        return nanoTimeSupplier.getAsLong();
    }
}
