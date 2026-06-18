package com.example;

import com.example.tools.DirReader;
import com.example.tools.IOBenchmark;

public class Main {
    public static void main(String[] args) throws Exception {
        // Experiment 1: Directory reader
        DirReader dirReader = new DirReader();
        dirReader.readDir(".\\src");
        System.out.print(dirReader.getResult());

        System.out.println("---");

        // Experiment 2: Buffered vs unbuffered I/O benchmark
        IOBenchmark.main(args);
    }
}
