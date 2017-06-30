package multicast.turing.com.multicasttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.turing.multicast.MultiCallback;
import com.turing.multicast.MultiManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView receiverTv;
    private TextView statusTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_join).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
        findViewById(R.id.btn_receive).setOnClickListener(this);
        receiverTv = (TextView) findViewById(R.id.tv_receive);
        statusTv = (TextView) findViewById(R.id.tv_status);
        MultiManager.getInstance().setMultiCallback(new MultiCallback() {
            @Override
            public void onReceiver(byte[] data) {
                receiverTv.setText(new String(data));
                Log.e(TAG, "onReceiver: " + new String(data).trim());
            }

            @Override
            public void onFailed(int code, String msg) {
                statusTv.setText("code=" + code + "==msg=" + msg);
                Log.e(TAG, "onFailed: code=" + code + "====msg" + msg);
            }

            @Override
            public void onStatus(int code) {
                if (code == MultiManager.CLOSED) {
                    statusTv.setText("onStatus:CLOSED");
                } else {
                    statusTv.setText("onStatus:OPENED");
                }

                Log.e(TAG, "onStatus: "+code);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                MultiManager.getInstance().send(("hello world" + System.currentTimeMillis()).getBytes());
                break;
            case R.id.btn_join:
                MultiManager.getInstance().beginDefault();
                break;
            case R.id.btn_receive:
                MultiManager.getInstance().beginReceiver();
                break;
            case R.id.btn_close:
                MultiManager.getInstance().leaveGroup();
                MultiManager.getInstance().close();
                break;
        }
    }
}
