package com.suy.main;

import com.suy.utils.TextUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String src = null;
        try {
            src = FileUtils.readFileToString(new File("test.txt"), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (src == null) {
            System.exit(-1);
        }
        TextUtil textUtil = new TextUtil(src);
    }
}
