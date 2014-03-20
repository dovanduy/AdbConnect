package com.example.AdbConnect;

import java.net.InetAddress;

public interface ScanListener {

	public void onScanStart();
	
	public void onScan(InetAddress address);

	public void onScanCompleted();

	public void onScanCancel();

}
