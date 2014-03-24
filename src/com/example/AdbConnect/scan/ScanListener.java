package com.example.AdbConnect.scan;

import java.net.InetAddress;

public interface ScanListener {

	public void onScanStart();
	
	public void onScan(String address);

	public void onScanCompleted();

	public void onScanCancel();

}
