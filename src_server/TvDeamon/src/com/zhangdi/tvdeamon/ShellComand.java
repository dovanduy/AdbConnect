package com.zhangdi.tvdeamon;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellComand {

	public static String exec(String cmd) {
        String[] cmdStrings = new String[]{"sh", "-c", cmd};
        StringBuffer retString = new StringBuffer();

        BufferedReader outBr = null;
        BufferedReader errBr = null;
        try {
            Process process = Runtime.getRuntime().exec(cmdStrings);
            outBr = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            errBr = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));

            String line;
            while (!TextUtils.isEmpty(line = outBr.readLine())) {
                retString.append(line + "\n");
            }
            retString.append("\n");
            while (!TextUtils.isEmpty(line = errBr.readLine())) {
                retString.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            retString.append(e.getMessage());
        } finally {
            try {
                if (outBr != null)
                    outBr.close();
                if (errBr != null)
                    errBr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return retString.toString();
	}

}
