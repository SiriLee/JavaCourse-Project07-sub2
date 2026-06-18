package com.example;

import com.example.tools.DirReader;

public class Main {
    public static void main(String[] args) {
        DirReader dirReader = new DirReader();
        dirReader.readDir(".\\src");
        System.out.print(dirReader.getResult());
    }
}
