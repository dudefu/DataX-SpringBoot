package com.wugui;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class Test {
    public static void main(String[] args) {
        final String tmpFilePath = "/xcloud/gpDataTest/temp_json/jobTmp-" + System.currentTimeMillis() + ".json";
        // 根据json写入到临时本地文件
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(tmpFilePath, "UTF-8");
            writer.println("jobJson");

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
