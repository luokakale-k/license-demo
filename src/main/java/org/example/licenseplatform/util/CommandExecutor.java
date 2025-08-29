package org.example.licenseplatform.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandExecutor {

    public static String exec(String command) {
        StringBuilder result = new StringBuilder();
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line.trim());
            }
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
