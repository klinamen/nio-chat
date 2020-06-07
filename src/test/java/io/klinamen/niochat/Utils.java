package io.klinamen.niochat;

import java.util.Arrays;

public class Utils {
    public static byte[] buildFilledArray(byte value, int length) {
        byte[] arr = new byte[length];
        Arrays.fill(arr, value);
        return arr;
    }
}
