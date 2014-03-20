package com.example.AdbConnect;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.text.TextUtils;

public class ShellComand {

	public static String exec(String cmd) {
		String[] cmdStrings = new String[] { "sh", "-c", cmd };
		StringBuffer retString = new StringBuffer();

		try {
			Process process = Runtime.getRuntime().exec(cmdStrings);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));

			String line;
			while (!TextUtils.isEmpty(line = stdout.readLine())) {
				retString.append(line + "\n");
			}
			retString.append("\n");
			while (!TextUtils.isEmpty(line = stderr.readLine())) {
				retString.append(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
			retString.append(e.getMessage());
		}
		return retString.toString();
	}

}
