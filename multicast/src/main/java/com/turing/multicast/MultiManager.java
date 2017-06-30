package com.turing.multicast;

import java.net.MulticastSocket;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MultiManager {
    //错误码
    public static final int JOIN_FAILED = 1;
    public static final int LEAVE_FAILED = 2;
    public static final int SEND_FAILED = 3;
    //状态码
    public static final int CLOSED = 1;
    public static final int OPENED = 2;

    private long sleep_time = 100;

    private static final int RECEIVER_MSGID = 1;

    private static MultiManager multiClient = null;
    private String ip = "224.0.0.1";
    private int port = 9898;
    private static final String TAG = "MultiManager";
    private MulticastSocket multicastSocket;
    private InetAddress inetAddress;
    private MultiCallback multiCallback;
    private boolean isClose = false;

    public MultiCallback getMultiCallback() {
        return multiCallback;
    }

    public void setMultiCallback(MultiCallback multiCallback) {
        this.multiCallback = multiCallback;
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECEIVER_MSGID:
                    if (multiCallback != null) {
                        multiCallback.onReceiver((byte[]) msg.obj);
                    }
                    break;
            }
        }
    };

    private MultiManager() {
    }

    public void joinGroup() {
        try {
            //初始化组播
            isClose = false;
            inetAddress = InetAddress.getByName(ip);
            multicastSocket = new MulticastSocket(port);
            //设置多播数据的默认存活时间
            multicastSocket.setTimeToLive(1);
            multicastSocket.joinGroup(inetAddress);
            if (multiCallback != null) {
                multiCallback.onStatus(OPENED);
            }
        } catch (Exception e) {
            if (multiCallback != null) {
                multiCallback.onFailed(JOIN_FAILED, "Join group failed. exception msg:" + e.getMessage());
            }
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private Runnable receiveRunnable = new Runnable() {
        @Override
        public void run() {
            String host_ip = NetUtil.getLocalIPAddress();
            while (!isClose) {
                Log.d(TAG, "run");
                byte buf[] = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length,
                        inetAddress, port);
                try {
                    multicastSocket.receive(dp);
                    String quest_ip = dp.getAddress().toString();
                    Log.e(TAG, "收到来自: \n" + quest_ip.substring(1) + "\n" + "的udp请求\n");
                    if ((!"".equals(host_ip)) && host_ip.equals(quest_ip.substring(1))) {
                        continue;
                    }

                    Message msg = new Message();
                    msg.what = RECEIVER_MSGID;
                    msg.obj = dp.getData();
                    handler.sendMessage(msg);
                    try {
                        Thread.sleep(sleep_time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };

    //开始接收
    public void beginReceiver() {
        new Thread(receiveRunnable).start();
    }


    //离开组
    public void leaveGroup() {
        try {
            multicastSocket.leaveGroup(inetAddress);
        } catch (IOException e) {
            if (multiCallback != null) {
                multiCallback.onFailed(LEAVE_FAILED, "Leave group failed. exception msg:" + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    //关闭socket
    public void close() {
        isClose = true;
        if (multicastSocket == null && multiCallback != null) {
            multiCallback.onFailed(LEAVE_FAILED, "Close  failed. exception msg:socket is null");
            return;
        }
        if (multicastSocket.isConnected() && !multicastSocket.isClosed()) {
            multicastSocket.close();
        }
        if (multiCallback != null) {
            multiCallback.onStatus(CLOSED);
        }

    }


    public static MultiManager getInstance() {
        if (multiClient == null) {
            multiClient = new MultiManager();
        }
        return multiClient;
    }

    //发送数据
    public void send(final byte[] data) {
        new AsyncTask<String, Integer, String>() {

            @Override
            protected String doInBackground(String... paramVarArgs) {
                //构造要发送的数据
                DatagramPacket dataPacket = new DatagramPacket(data,
                        data.length, inetAddress, port);
                try {
                    multicastSocket.send(dataPacket);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    if (multiCallback != null) {
                        multiCallback.onFailed(SEND_FAILED, "Send failed.exception msg:" + e.getMessage());
                    }
                    return "send failed";
                }
                return "send success";
            }

            @Override
            protected void onPostExecute(String result) {
                Log.e(TAG, "onPostExecute: success" + result);
            }
        }.execute();
    }

    //开启组播.
    public void beginDefault() {
        joinGroup();
    }

}