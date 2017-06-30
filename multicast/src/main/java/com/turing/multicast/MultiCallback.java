package com.turing.multicast;

/**
 * @author wuyihua
 * @Date 2017/6/29
 * @todo 多播回调
 */

public interface MultiCallback {
    void onReceiver(byte[] data);

    void onFailed(int code, String msg);

    void onStatus(int code);
}
