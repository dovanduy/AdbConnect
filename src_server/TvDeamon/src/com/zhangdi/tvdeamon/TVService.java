package com.zhangdi.tvdeamon;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * client                                 server
 * install:[id]:[package]:[url]           install_over:[0/1]:[id]:[]
 */
public class TVService extends Service {

    public static final String ACTION_INSTALLED = "action_installed";

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private static final int SOCKET_PORT = 7658;
    private static final int BROADCAST_PORT = 7659;

    private ServerSocket mServerSocket;
    private DatagramSocket mDatagramSocket;

    private boolean mSocketStarted = false;
    private boolean mBroadcastStarted = false;

    private Thread mSocketThread;
    private Thread mBroadcastThread;
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(16);

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            LOGD("onStartCommand action=" + action);
            LOGD("mSocketStarted=" + mSocketStarted);
            LOGD("mBroadcastStarted=" + mBroadcastStarted);
            if (action.equals(ACTION_START)) {
                if (!mSocketStarted) {
                    mSocketStarted = true;
                    startSocket();
                }
                if (!mBroadcastStarted) {
                    mBroadcastStarted = true;
                    startBroadcast();
                }
            } else if (action.equals(ACTION_STOP)) {
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        try {
            if (mServerSocket != null && !mServerSocket.isClosed()) {
                mServerSocket.close();
                mServerSocket = null;
            }
            if (mDatagramSocket != null && !mDatagramSocket.isClosed()) {
                mDatagramSocket.close();
                mDatagramSocket = null;
            }
            if (mSocketThread != null) {
                mSocketThread.interrupt();
            }
            if (mBroadcastThread != null) {
                mBroadcastThread.interrupt();
            }
            if (mExecutorService != null) {
                mExecutorService.shutdownNow();
            }
            mBroadcastStarted = false;
            mSocketStarted = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, TVService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, TVService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    private void startBroadcast() {
        mBroadcastThread = new Thread() {
            @Override
            public void run() {
                LOGD("starting broadcast");
                try {
                    int port = BROADCAST_PORT;
                    mDatagramSocket = new DatagramSocket(port);

                    byte[] buf = new byte[1024];
                    DatagramPacket getPacket = new DatagramPacket(buf, buf.length);
                    while (true) {
                        mDatagramSocket.receive(getPacket);
                        mExecutorService.execute(new BroadcastRunnable(mDatagramSocket, getPacket));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mBroadcastThread.start();

    }

    private class BroadcastRunnable implements Runnable {

        private DatagramSocket mDatagramSocket;

        private DatagramPacket datagramPacket;

        public BroadcastRunnable(DatagramSocket socket, DatagramPacket packet) {
            this.mDatagramSocket = socket;
            this.datagramPacket = packet;
        }

        @Override
        public void run() {
            try {
                LOGD("broadcast received packet");
                LOGD("datagramPacket.length=" + datagramPacket.getLength());
                String getMes = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                LOGD("packet:" + getMes);
                InetAddress sendIP = datagramPacket.getAddress();
                int sendPort = datagramPacket.getPort();
                LOGD("send address " + sendIP.getHostAddress());
                LOGD("send port " + sendPort);

                if (getMes != null && getMes.startsWith("i am xunlei tvzhushou!")) {
                    SocketAddress sendAddress = datagramPacket.getSocketAddress();
                    String feedback = "ok!i am tv!";
                    byte[] backBuf = feedback.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(backBuf, backBuf.length, sendAddress);
                    mDatagramSocket.send(sendPacket);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startSocket() {
        mSocketThread = new Thread() {
            @Override
            public void run() {
                LOGD("starting socket");
                try {
                    mServerSocket = new ServerSocket(SOCKET_PORT);
                    while (true) {
                        Socket socket = mServerSocket.accept();
                        mExecutorService.execute(new SocketRunnable(socket));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mSocketThread.start();
    }

    private class SocketRunnable implements Runnable {

        private Socket socket;

        public SocketRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                LOGD("client connected");

                this.socket.setSoTimeout(8000);

                BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(this.socket.getOutputStream())), true);

                while (true) {
                    String cmd = in.readLine();
                    if (cmd != null) {
                        LOGD("TVService received:" + cmd);
                        if (cmd.startsWith("install:")) {
                            int start = "install:".length();
                            int second = cmd.indexOf(":", start);
                            int third = cmd.indexOf(":", second + 1);
                            String id = cmd.substring(start, second);
                            String packageName = cmd.substring(second + 1, third);
                            String url = cmd.substring(third + 1);

                            LOGD("install apk: id = " + id + ", package = " + packageName + ", url = " + url);

                            String path = downloadApk(url);
                            if (!TextUtils.isEmpty(path)) {
                                LOGD("download " + url + " success " + path);

                                if (installApk(path)) {
                                    LOGD("install " + path + " success, " + "id = " + id);
                                    out.println("install_over:0:" + id + ":success");

                                    saveInstalledApk(packageName);
                                } else {
                                    LOGD("install " + path + " fail, " + "id = " + id);
                                    out.println("install_over:1:" + id + ":install error");
                                }

                                // remove path
                                File file = new File(path);
                                file.delete();
                                LOGD("delete tmp apk file " + path);
                            } else {
                                LOGD("download " + url + " fail, " + "id = " + id);
                                out.println("install_over:1:" + id + ":download error");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (this.socket != null && !this.socket.isClosed()) {
                        this.socket.close();
                        LOGD("socket closed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String downloadApk(String url) {
        if (TextUtils.isEmpty(url))
            return null;

        String path = generateSavePath();
        if (TextUtils.isEmpty(path))
            return null;

        InputStream is = null;
        FileOutputStream fos = null;
        try {
            URL uri = new URL(url);
            URLConnection conn = uri.openConnection();
            conn.connect();

            is = conn.getInputStream();
            if (is == null) {
                throw new IOException("无法获取文件内容");
            }
            fos = new FileOutputStream(new File(path));

            byte[] buf = new byte[1024];
            int size;
            while ((size = is.read(buf)) != -1) {
                fos.write(buf, 0, size);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            path = null;
        } finally {
            try {
                if (is != null)
                    is.close();
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    private String generateSavePath() {
        String dir = getExternalCacheDir().getAbsolutePath() + File.separator + "apk" + File.separator;
        File dirFile = new File(dir);
        if (dirFile.exists() && !dirFile.isDirectory()) {
            dirFile.delete();
        }
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        return dirFile.getAbsolutePath() + File.separator + "xltv" + System.currentTimeMillis() + ".apk";
    }

    private boolean installApk(String path) {
        if (TextUtils.isEmpty(path))
            return false;
        InetAddress inetAddress = getInetAddress(this);
        if (inetAddress == null)
            return false;
        String address = inetAddress.getHostAddress();
        if (!connect(address)) {
            LOGD("connect to " + address + " failed");
            return false;
        }

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String cmd = String.format("adb -s %s:%s shell pm install -r %s", address, "5555", path);
        String ret = ShellComand.exec(cmd);

        LOGD(cmd);
        LOGD(ret);

        if (ret != null && ret.contains("Success")) {
            LOGD("install " + path + " success");
            return true;
        } else {
            LOGD("install " + path + " failed");
            return false;
        }

    }

    private boolean connect(String address) {
        disconnect();
        String ret = ShellComand.exec("adb connect " + address);
        LOGD("adb connect " + address);
        LOGD(ret);
        if (ret != null && ret.contains("connected")) {
            return true;
        }
        return false;
    }

    private void disconnect() {
        ShellComand.exec("adb disconnect");
    }

    public static int getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getIpAddress();
    }

    public static InetAddress getInetAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        if (ip == 0) {
            return null;
        }
        try {
            byte[] arrayOfByte = new byte[4];
            arrayOfByte[3] = ((byte) (0xFF & (ip >> 24)));
            arrayOfByte[2] = ((byte) (0xFF & (ip >> 16)));
            arrayOfByte[1] = ((byte) (0xFF & (ip >> 8)));
            arrayOfByte[0] = ((byte) (ip & 0xFF));
            return InetAddress.getByAddress(arrayOfByte);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveInstalledApk(String packageName) {
        InstallManager im = new InstallManager(this);
        im.installPackage(packageName);

        Intent intent = new Intent(ACTION_INSTALLED);
        intent.putExtra("package", packageName);
        sendBroadcast(intent);
    }

    private void LOGD(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            Log.d(TVService.class.getSimpleName(), msg);
        }
    }


}
