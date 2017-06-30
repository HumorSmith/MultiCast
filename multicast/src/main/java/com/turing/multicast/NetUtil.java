package com.turing.multicast;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
/**
 * Created by lixiaotong on 17-3-9.
 */
public class NetUtil {
    private static final String TAG = "NetUtil";
    public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
        WifiManager wifi = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if(dhcp==null) {
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
    protected static Boolean isWifiApEnabled(Context context) {
        try {
            WifiManager manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            Method method = manager.getClass().getMethod("isWifiApEnabled");
            return (Boolean)method.invoke(manager);
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)  {
            e.printStackTrace();
        }
        return false;
    }
    public static String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("ROBOT_NET:NetUtil", ex.toString());
        }
        return null;
    }
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    public static boolean enableWifi(Context context){
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()){
            return wifiManager.setWifiEnabled(true);
        } else {
            return true;
        }
    }
    public static int connectWifi(Context context, String ssid, String password) {
        int type = -1;
        int prevNetworkId = -1;
        if(!TextUtils.isEmpty(ssid) && !TextUtils.isEmpty(password)){
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null) {
                prevNetworkId = networkInfo.isConnected() ? wifiManager.getConnectionInfo().getNetworkId() : -1;
            }
            List<ScanResult> wifiList;
            do {
                try {
                    wifiList = wifiManager.getScanResults();
                    if(!wifiManager.isWifiEnabled()){
                        wifiManager.setWifiEnabled(true);
                        wifiManager.startScan();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    wifiList = null;
                }
            } while (wifiList != null && wifiList.size() == 0);
            if(wifiList == null){
                return -1;
            }
            for (ScanResult result : wifiList) {
                Log.d(TAG, " Setting [ addWifiAp ] Scan list is " + result.SSID);
                if (ssid.equals(result.SSID)) {
                    String encryptType = result.capabilities;
                    if (encryptType.contains("WPA2")) {
                        type = 4;
                    } else if (encryptType.contains("WPA")) {
                        type = 3;
                    } else if (encryptType.contains("WEP")) {
                        type = 2;
                    } else {
                        type = 1;
                    }
                    break;
                }
            }
            if(type == -1){
                return type;
            }
            Log.d(TAG, " Setting [ addWifiAp ] Match object ssid : " + ssid + " | " + type);
            int wifiID = wifiManager.addNetwork(createWifiInfo(ssid, password, type));
            if(wifiID == -1){
                return wifiID;
            }
            boolean isSuccess = wifiManager.enableNetwork(wifiID, true);
            if(!isSuccess){
                Log.d(TAG, " Setting [ addWifiAp ] config network fail ");
                return -1;
            } else {
                if(isSuccess && prevNetworkId != -1 && wifiID != prevNetworkId){
                    wifiManager.removeNetwork(prevNetworkId);
                }
                wifiManager.saveConfiguration();
                Log.d(TAG, " Setting [ addWifiAp ] Config finish ");
                return 0;
            }
        } else {
            return -1;
        }
    }
    /**
     * 根据传入的信息，产生WifiConfiguration
     * @param SSID WIFI名称
     * @param password WIFI密码
     * @param Type WIFI类型 1WIFICIPHER_NOPASS 2WIFICIPHER_WEP 3WIFICIPHER_WPA 4RSN
     * @return
     */
    public static WifiConfiguration createWifiInfo(String SSID, String password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (Type == 1){  // WIFICIPHER_NOPASS
            // config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            // config.wepTxKeyIndex = 0;
        } else if (Type == 2) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            Log.d(TAG, " Setting [ createWifiInfo ] isHex:" + isHexWepKey(password));
            if (isHexWepKey(password)) {
                config.wepKeys[0] = password;
            } else
                config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (Type == 3) // WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else if (Type == 4) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }
    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();
        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }
        return isHex(wepKey);
    }
    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f')) {
                return false;
            }
        }
        return true;
    }
}