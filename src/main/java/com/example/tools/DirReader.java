package com.example.tools;

import java.io.File;

public class DirReader {
    private StringBuilder resultBuilder;
    private int level;

    public DirReader() {
        this.resultBuilder = new StringBuilder();
        this.level = 1;
    }

    public void readDir(String path) {
        this.resultBuilder.setLength(0);
        this.level = 1;
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + path);
        }
        readDir(dir);
    }

    private void readDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            // 缩进与前缀
            for (int i = 0; i < level - 1; i++) {
                resultBuilder.append("\t");
            }
            if (level > 1) {
                resultBuilder.append("|- ");
            }
            // 文件添加与判断
            resultBuilder.append(file.getName());
            if (file.isDirectory()) {
                resultBuilder.append(":\n");
                ++level;
                readDir(file); // 递归读取子目录
                --level;
            } else {
                resultBuilder.append("\n");
            }
        }
    }

    public String getResult() {
        return resultBuilder.toString();
    }

}