package com.example.sensortask;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity{

    private AutoUpdateService.GetDataBinder getDataBinder ;
    private EditText editText;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            getDataBinder = (AutoUpdateService.GetDataBinder) iBinder;
            getDataBinder.getData(MainActivity.this);
            Intent intent = new Intent(MainActivity.this,AutoUpdateService.class);
            startService(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private static String str = "{\"method\":\"thing.service.property.set\",\"id\":\"674238177\",\"params\":{\"PowerSwitch\":2},\"version\":\"1.0.0\"}\n";
    private static String str1 = "{\"method\":\"thing.service.property.set\",\"id\":\"674238177\",\"params\":{\"PowerSwitch\":1},\"version\":\"1.0.0\"}\n";

    /* 设备三元组信息 */
     final public String PRODUCTKEY = "gy028w6Lu1k";
     final public String DEVICENAME = "7xlzThABT3AGwTvPFIHq";
     final public String DEVICESECRET = "0144574a60bccc6c28e126747c5c9dca";
     final public String REGIONID = "cn-shanghai";//服务器所在地区

    /* 自动Topic, 用于上报消息 */
     final public String PUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/update";
    /* 自动Topic, 用于接受消息 */
     final public String SUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/get";
    /* 阿里云Mqtt服务器域名 */
     final public String host = "tcp://" + PRODUCTKEY + ".iot-as-mqtt."+ REGIONID + ".aliyuncs.com:443";

    private final String TAG = "AiotMqtt";

    private String clientId = "AndroidTest";	//客户端ID
    private String userName = "xxxxxx";	//用户名
    private String passWord = "xxxxx";	//密码

    MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 获取Mqtt建连信息clientId, username, password */
        AiotMqttOption aiotMqttOption = new AiotMqttOption().getMqttOption(PRODUCTKEY, DEVICENAME, DEVICESECRET);
        if (aiotMqttOption == null) {
            Log.e(TAG, "device info error");
        } else {
            clientId = aiotMqttOption.getClientId();
            userName = aiotMqttOption.getUsername();
            passWord = aiotMqttOption.getPassword();
        }

        /* 创建MqttConnectOptions对象并配置username和password */
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());

        /* 创建MqttAndroidClient对象, 并设置回调接口 */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);
        TextView textView = findViewById(R.id.data);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));
                //转化并展示数据
                JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                float res = Float.parseFloat(jsonObject.getString("data"));
                int data = (int) Math.floor(res);
                int temp = 8-data;
                setView(data);
                textView.setText(String.valueOf(temp));
//                textView.setText(jsonObject.getString("data"));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });

        /* Mqtt建连 */
        try {
            mqttAndroidClient.connect(mqttConnectOptions,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");

                    subscribeTopic(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

        /* 通过按键发布消息 */
        Button pubButton = findViewById(R.id.publish);
        pubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage(str);
            }
        });
        Button openButton = findViewById(R.id.open);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage(str1);
            }
        });
        Button setThreshold = findViewById(R.id.set_threshold);
        editText = findViewById(R.id.et_threshold);
        setThreshold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int a = Integer.parseInt(editText.getText().toString());
                String str2 = "{\"method\":\"thing.service.property.set\",\"id\":\"674238177\",\"params\":{\"PowerSwitch\":3,\"Threshold\":"+a+"},\"version\":\"1.0.0\"}\n";
                publishMessage(str2);
            }
        });
        /**
         * 绑定服务
         */
        Intent intent = new Intent(this,AutoUpdateService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);
    }

    /**
     * 订阅特定的主题
     * @param topic mqtt主题
     */
    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向默认的主题/user/update发布消息
     * @param payload 消息载荷
     */
    public void publishMessage(String payload) {
        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 设置view的大小
     */
    private void setViewFullScreen(View view,int data) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        int temp = 8 - data;
        layoutParams.height = 300 + temp * 200;
        view.setLayoutParams(layoutParams);
    }
    private void setView(int data){
        View view = findViewById(R.id.view_bottle);
        setViewFullScreen(view,data);
    }

}
