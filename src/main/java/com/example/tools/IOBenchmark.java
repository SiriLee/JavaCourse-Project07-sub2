package com.example.tools;

import java.io.*;

public class IOBenchmark {
    private static final int FILE_SIZE_MB = 10;
    private static final int FILE_SIZE_BYTES = FILE_SIZE_MB * 1024 * 1024;

    /**
     * Generate a test file filled with sequential byte values for read benchmark.
     */
    public static File generateTestFile(String path) throws IOException {
        File file = new File(path);
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] buffer = new byte[65536];
            int remaining = FILE_SIZE_BYTES;
            byte val = 0;
            while (remaining > 0) {
                int chunk = Math.min(remaining, buffer.length);
                for (int i = 0; i < chunk; i++) {
                    buffer[i] = val++;
                }
                bos.write(buffer, 0, chunk);
                remaining -= chunk;
            }
        }
        return file;
    }

    /**
     * Benchmark read performance: FileInputStream vs BufferedInputStream.
     * Both read byte-by-byte to highlight the system-call overhead difference.
     */
    public static void benchmarkRead(File file) throws IOException {
        // Unbuffered read
        long start = System.currentTimeMillis();
        try (FileInputStream fis = new FileInputStream(file)) {
            while (fis.read() != -1) { }
        }
        long unbufferedMs = System.currentTimeMillis() - start;

        // Buffered read
        start = System.currentTimeMillis();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            while (bis.read() != -1) { }
        }
        long bufferedMs = System.currentTimeMillis() - start;

        printResult("READ", unbufferedMs, bufferedMs);
    }

    /**
     * Benchmark write performance: FileOutputStream vs BufferedOutputStream.
     * Both write byte-by-byte to highlight the system-call overhead difference.
     */
    public static void benchmarkWrite(File srcFile, String dstUnbuffered, String dstBuffered) throws IOException {
        // Read source data into memory once so I/O of the source file doesn't affect timing
        byte[] data = new byte[(int) srcFile.length()];
        try (FileInputStream fis = new FileInputStream(srcFile)) {
            int bytesRead = 0;
            while (bytesRead < data.length) {
                int n = fis.read(data, bytesRead, data.length - bytesRead);
                if (n == -1) break;
                bytesRead += n;
            }
        }

        // Unbuffered write
        long start = System.currentTimeMillis();
        try (FileOutputStream fos = new FileOutputStream(dstUnbuffered)) {
            for (byte b : data) {
                fos.write(b);
            }
        }
        long unbufferedMs = System.currentTimeMillis() - start;

        // Buffered write
        start = System.currentTimeMillis();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dstBuffered))) {
            for (byte b : data) {
                bos.write(b);
            }
        }
        long bufferedMs = System.currentTimeMillis() - start;

        printResult("WRITE", unbufferedMs, bufferedMs);
    }

    private static void printResult(String mode, long unbufferedMs, long bufferedMs) {
        System.out.println("[" + mode + "] Unbuffered (byte-by-byte):   " + unbufferedMs + " ms");
        System.out.println("[" + mode + "] Buffered   (byte-by-byte):   " + bufferedMs + " ms");
        if (bufferedMs > 0) {
            double ratio = (double) unbufferedMs / bufferedMs;
            System.out.printf("[%s] Buffered is ~%.0fx faster%n", mode, ratio);
        } else {
            System.out.println("[" + mode + "] Buffered too fast to measure ratio");
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
        String testFilePath = "benchmark_test_file.tmp";
        String writeUnbufferedPath = "benchmark_write_unbuffered.tmp";
        String writeBufferedPath = "benchmark_write_buffered.tmp";

        System.out.println("=== I/O Benchmark: Buffered vs Unbuffered (" + FILE_SIZE_MB + " MB, byte-by-byte) ===\n");

        // Phase 1: Generate test file
        System.out.println("Generating " + FILE_SIZE_MB + " MB test file...");
        File testFile = generateTestFile(testFilePath);
        System.out.println("Test file created: " + testFile.getAbsolutePath() + "\n");

        // Phase 2: Read benchmark
        System.out.println("--- Read Benchmark: FileInputStream vs BufferedInputStream ---");
        benchmarkRead(testFile);

        // Phase 3: Write benchmark
        System.out.println("--- Write Benchmark: FileOutputStream vs BufferedOutputStream ---");
        benchmarkWrite(testFile, writeUnbufferedPath, writeBufferedPath);

        // Phase 4: Cleanup
        new File(testFilePath).delete();
        new File(writeUnbufferedPath).delete();
        new File(writeBufferedPath).delete();
        System.out.println("=== Done (temp files cleaned) ===");
    }
}
