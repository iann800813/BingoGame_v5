package com.example.bingo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bingo.MySQL.DBConnector;
import com.example.bingo.MySQL.MySQL_update;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;

import static com.example.bingo.R.id.IpaddrEdit;
import static com.example.bingo.R.id.UsernameEdit;
import static com.example.bingo.R.id.default_activity_button;

public class LoginActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    private EditText UsernameEdit;
    private Button joinBtn, openBtn, settingBtn;
    private TextView tv_comname;

    //登入資訊
    String input_ip = "";//測試用預設140.123.174.165
    String input_name = "player";//預設
    String input_channel = "1"; //頻道


    //資料庫
    String DatabaseName = "BingoGame";
    String[][] jsonDataList;//放公司相關資訊
    int[] jsonRoomList;//放遊戲房目前狀態


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);


        UsernameEdit = (EditText) findViewById(R.id.UsernameEdit);
        joinBtn = (Button) findViewById(R.id.joinBtn);
        openBtn = (Button) findViewById(R.id.openBtn);
        settingBtn = (Button) findViewById(R.id.btn_setting);
        tv_comname = (TextView) findViewById(R.id.tv_comname);


        //連結資料庫SQL 背景作業
        BackTask bt = new BackTask();
        bt.execute();

        //判斷是否已選擇serverip
        String log_comname = getConfig(LoginActivity.this,
                "Config", "db_comname", "");//取得記憶的ip

        if (log_comname.length() == 0) {
            //AlertDialog 跳出詢問公司視窗
            Handler myHandler = new Handler();
            myHandler.postDelayed(myListAlertDialog, 1 * 1000);//幾秒後(delaySec)呼叫runTimerStop這個Runnable，再由這個Runnable去呼叫你想要做的事情
        }
        tv_comname.setText(getConfig(LoginActivity.this,
                "Config", "db_comname", ""));


        joinBtn.setOnClickListener(new BtnListener());
        openBtn.setOnClickListener(new BtnListener());
        settingBtn.setOnClickListener(new BtnListener());

        //連結資料庫時以防除錯會一直跑Catch所以加入下面兩段，即可避免此情形
        StrictMode
                .setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()   // or .detectAll() for all detectable problems
                        .penaltyLog()
                        .build());
        StrictMode
                .setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());
    }//oncreate


    /**
     * Button 監聽器 開始
     **/
    public class BtnListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

            //取得記憶的ip
            input_ip = getConfig(LoginActivity.this,
                    "Config", "db_server", "");//最後一欄為預設
            input_name = UsernameEdit.getText().toString();


            switch (v.getId()) {
                //加入
                case R.id.joinBtn:
                    try {

                        if (input_ip.length() == 0) {
                            Toast.makeText(getApplicationContext(), "請點選設定，選擇您的伺服器位置", Toast.LENGTH_SHORT).show();
                        } else if (input_name.length() == 0) {
                            Toast.makeText(getApplicationContext(), "請取一個暱稱吧!", Toast.LENGTH_SHORT).show();
                        } else if (UsernameEdit.getText().toString().getBytes("UTF-8").length > 18) {
                            //server端不可接受超過18個字元的內容，超過Server端會出錯請注意!
                            Toast.makeText(getApplicationContext(), "名稱太長了!", Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            //選房間
                            sqlroom();

                            //AlertDialog
                            Handler myHandler2 = new Handler();
                            myHandler2.postDelayed(roomListAlertDialog, 1 * 1000); //跑出選單

                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;

                //建立
                case R.id.openBtn:
                    if (input_ip.length() == 0) {
                        Toast.makeText(getApplicationContext(), "請點選設定，選擇您的伺服器位置", Toast.LENGTH_SHORT).show();
                    } else {
                        //判斷是否已登入
                        checkhostlogin();
                    }
                    break;

                //關於
                case R.id.btn_setting:
//                    //改成關於我們
//                    new AlertDialog.Builder(LoginActivity.this)
//
//                            .setTitle("關於")
//                            .setMessage(R.string.about_content2)
//                            .setPositiveButton("確定", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                }
//                            }).show();
//                    intent_method(input_ip, input_name, 2);
                    //AlertDialog 跳出詢問公司視窗
                    Handler myHandler = new Handler();
                    myHandler.postDelayed(myListAlertDialog, 1 * 1000);//幾秒後(delaySec)呼叫runTimerStop這個Runnable，再由這個Runnable去呼叫你想要做的事情
                    break;


            }
        }
    }

    /***************
     * 建立按鈕 按下後的動作 開始
     *********************/
    //確認是否已登入
    public void checkhostlogin() {

        String loginstatus = getConfig(LoginActivity.this,
                "Config", "HostLogin", "false");//最後一欄為預設
        if (loginstatus.equals("true")) {
            //已登入
            openRoomStatus();
        } else {
            //尚未登入，出現登入畫面
            final View dialogview = LayoutInflater.from(LoginActivity.this).inflate(R.layout.adminlogin_fomat, null);
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("管理員登入")
                    .setView(dialogview)
                    .setPositiveButton("登入", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText edit_acc = (EditText) dialogview.findViewById(R.id.edit_acc);
                            EditText edit_pwd = (EditText) dialogview.findViewById(R.id.edit_pwd);
                            checkAdmin(edit_acc.getText().toString(), edit_pwd.getText().toString());
                        }
                    })
                    .show();
        }
    }

    //帳戶確認
    public void checkAdmin(String edit_acc, String edit_pwd) {
        //連線資料庫
        try {
            String result = DBConnector.executeQuery("SELECT * FROM " + DatabaseName + " WHERE account = '" + edit_acc + "' AND password = '" + edit_pwd + "' ");
            Log.e("log_tag checkAdmin", "result=" + result);
            if (result.equals("false")) {
                Toast.makeText(getApplicationContext(), "網路連線不穩，請檢查網路連線", Toast.LENGTH_SHORT).show();
                Log.e("log_tag checkAdmin", "result = network false");

            } else if (result.equals(null)) {
                Toast.makeText(getApplicationContext(), "登入失敗，請再確認一次", Toast.LENGTH_SHORT).show();
            } else {
                //記憶已登入
                setConfig(LoginActivity.this, "Config", "HostLogin", "true");
                Toast.makeText(getApplicationContext(), "登入成功", Toast.LENGTH_SHORT).show();
                openRoomStatus();//開遊戲房
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "網路連線失敗，請稍待再連線", Toast.LENGTH_SHORT).show();
            Log.e("log_tag checkAdmin", "解析JSON異常" + e);
        }


    }

    //開遊戲房
    public void openRoomStatus() {
        Log.e("log_tag openRoomStatus", "openRoomStatus IN");
        sqlroom();//更新目前遊戲房狀態

        //取得記憶的account
        String input_acc = getConfig(LoginActivity.this,
                "Config", "db_account", "");//最後一欄為預設
        String openRoom = "0";
        if (jsonRoomList[0] == -1) {
            openRoom = "1";
            new MySQL_update(input_acc, "room1", 0).execute();
        } else if (jsonRoomList[1] == -1) {
            openRoom = "2";
            new MySQL_update(input_acc, "room2", 0).execute();
        } else if (jsonRoomList[2] == -1) {
            openRoom = "3";
            new MySQL_update(input_acc, "room3", 0).execute();
        } else if (jsonRoomList[3] == -1) {
            openRoom = "4";
            new MySQL_update(input_acc, "room4", 0).execute();
        } else if (jsonRoomList[4] == -1) {
            openRoom = "5";
            new MySQL_update(input_acc, "room5", 0).execute();
        } else {
            openRoom = "0";
        }

        if (openRoom.equals("0")) {
            Toast.makeText(getApplicationContext(), "目前遊戲房全滿，請稍後再試！" + openRoom, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "建立" + openRoom + "號遊戲房！", Toast.LENGTH_SHORT).show();
            //換頁
            input_name = "host";
            intent_method(input_ip, input_name, 1);
        }

        Log.e("log_tag checkRoomStatus", "openRoom: " + openRoom);

    }
    /**************** 建立按鈕 按下後的動作 結束 *********************/

    /****************
     * 加入按鈕 按下後的動作 開始
     *********************/

    //呼叫資料庫 查詢遊戲房數
    public void sqlroom() {

        //取得記憶的account
        String input_acc = getConfig(LoginActivity.this,
                "Config", "db_account", "");//最後一欄為預設
        Log.e("log_tag sqlroom", "acc: " + input_acc);


        try {
            String result = DBConnector.executeQuery("SELECT * FROM " + DatabaseName + " where account = '" + input_acc + "' ");
            Log.e("log_tag sqlroom", "result: " + result);
            result = result.substring(result.indexOf("{"));//JSONObject 要加;JSONArray則不用 ,否則會錯
            JSONObject jsonData = new JSONObject(result);

            //存進陣列
            jsonRoomList = new int[5];
            jsonRoomList[0] = jsonData.getInt("room1");
            jsonRoomList[1] = jsonData.getInt("room2");
            jsonRoomList[2] = jsonData.getInt("room3");
            jsonRoomList[3] = jsonData.getInt("room4");
            jsonRoomList[4] = jsonData.getInt("room5");

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "伺服器連線失敗，請稍待再連線", Toast.LENGTH_SHORT).show();
            Log.e("log_tag sqlroom", "解析JSON異常" + e);
        }
    }

    private Runnable roomListAlertDialog = new Runnable() {
        @Override
        public void run() {
            Log.e("log_tag roomList", "roomListAlertDialog in");

            AlertDialog builder;
            LayoutInflater inflater = LayoutInflater.from(LoginActivity.this);
            final View FormatView = inflater
                    .inflate(R.layout.room_format, null); //自訂layout
            builder = new AlertDialog.Builder(LoginActivity.this).create();
            builder.setTitle("遊戲房列表");
            LinearLayout linear_room1 = (LinearLayout) FormatView.findViewById(R.id.linear_room1);
            LinearLayout linear_room2 = (LinearLayout) FormatView.findViewById(R.id.linear_room2);
            LinearLayout linear_room3 = (LinearLayout) FormatView.findViewById(R.id.linear_room3);
            LinearLayout linear_room4 = (LinearLayout) FormatView.findViewById(R.id.linear_room4);
            LinearLayout linear_room5 = (LinearLayout) FormatView.findViewById(R.id.linear_room5);

            final TextView tv_room1 = (TextView) FormatView.findViewById(R.id.tv_room1);
            final TextView tv_room2 = (TextView) FormatView.findViewById(R.id.tv_room2);
            final TextView tv_room3 = (TextView) FormatView.findViewById(R.id.tv_room3);
            final TextView tv_room4 = (TextView) FormatView.findViewById(R.id.tv_room4);
            final TextView tv_room5 = (TextView) FormatView.findViewById(R.id.tv_room5);

            for (int i = 0; i < 5; i++) {
                String status = "未開啟";
                int room = jsonRoomList[i];
                if (room == -1) {
                    status = "未開啟";
                } else if (room == 0) {
                    status = "等待中";
                } else if (room == 1) {
                    status = "已開始";
                }

                //房間狀態&設定文字顏色(red/green/gray)
                if (i == 0) {
                    tv_room1.setText(status);
                    tv_room1.setTextColor(changeColorRoom(status));
                } else if (i == 1) {
                    tv_room2.setText(status);
                    tv_room2.setTextColor(changeColorRoom(status));
                } else if (i == 2) {
                    tv_room3.setText(status);
                    tv_room3.setTextColor(changeColorRoom(status));
                } else if (i == 3) {
                    tv_room4.setText(status);
                    tv_room4.setTextColor(changeColorRoom(status));
                } else if (i == 4) {
                    tv_room5.setText(status);
                    tv_room5.setTextColor(changeColorRoom(status));
                }
            }

            linear_room1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Toast.makeText(getApplicationContext(), "room1", Toast.LENGTH_SHORT).show();
                    input_channel = "1";
                    checkRoomStatus(tv_room1.getText().toString());
                }
            });
            linear_room2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Toast.makeText(getApplicationContext(), "room2", Toast.LENGTH_SHORT).show();
                    input_channel = "2";
                    checkRoomStatus(tv_room2.getText().toString());
                }
            });
            linear_room3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Toast.makeText(getApplicationContext(), "room3", Toast.LENGTH_SHORT).show();
                    input_channel = "3";
                    checkRoomStatus(tv_room3.getText().toString());
                }
            });
            linear_room4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Toast.makeText(getApplicationContext(), "room4", Toast.LENGTH_SHORT).show();
                    input_channel = "4";
                    checkRoomStatus(tv_room4.getText().toString());
                }
            });
            linear_room5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Toast.makeText(getApplicationContext(), "room5", Toast.LENGTH_SHORT).show();
                    input_channel = "5";
                    checkRoomStatus(tv_room5.getText().toString());
                }
            });
            builder.setView(FormatView);
            builder.show();
        }
    };

    //更改房間狀態文字顏色:未開啟/等待中/已開始
    public int changeColorRoom(String status) {
        int color = R.color.gray;
        switch (status) {
            case "未開啟":
                color = R.color.gray;
                break;
            case "等待中":
                color = R.color.green;
                break;
            case "已開始":
                color = R.color.colorAccent;
                break;
            default:
                break;
        }
        return color;
    }

    //更改底色:未開啟/等待中/已開始
    public int changeBGRoom(String status) {
        int BG = R.drawable.button_cus;
        switch (status) {
            case "未開啟":
                BG = R.drawable.button_cus;
                break;
            case "等待中":
                BG = R.drawable.button_cus_blue;
                break;
            case "已開始":
                BG = R.drawable.button_cus_pink;
                break;
            default:
                break;
        }
        return BG;
    }

    //確認房間狀態並換頁
    public void checkRoomStatus(String status) {
        switch (status) {
            case "未開啟":
                Toast.makeText(getApplicationContext(), input_channel + "號尚未開啟，請選擇其他遊戲房!", Toast.LENGTH_SHORT).show();
                break;
            case "等待中":
                intent_method(input_ip, input_name, 0);
                break;
            case "已開始":
                Toast.makeText(getApplicationContext(), input_channel + "號已開始遊戲，請選擇其他遊戲房!", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
    /**************** 加入按鈕 按下後的動作 結束 *********************/


    /**
     * Intent換頁統一設定 開始
     **/
    public void intent_method(String input_ip, String input_name, int mode) {

        //記憶ip位置.暱稱
        setConfig(LoginActivity.this, "Config", "input_ip", input_ip);
        setConfig(LoginActivity.this, "Config", "input_name", input_name + "_" + input_channel);
        setConfig(LoginActivity.this, "Config", "input_channel", input_channel);

        Intent intent = new Intent();

        switch (mode) {
            //使用者
            case 0:
                intent.setClass(LoginActivity.this, UserRoomActivity.class);
                break;
            //管理者
            case 1:
                intent.setClass(LoginActivity.this, HostRoomActivity.class);
                break;
            //設定
            case 2:
                intent.setClass(LoginActivity.this, SettingActivity.class);
                break;


        }
        LoginActivity.this.startActivity(intent);

    }


    /**
     * AlertDialog 跳出詢問公司視窗
     */
    private Runnable myListAlertDialog = new Runnable() {
        @Override
        public void run() {
            Log.e("log_tag", "myListAlertDialog in");
            String[] listname = new String[jsonDataList.length];
            for (int i = 0; i < jsonDataList.length; i++) {
                listname[i] = jsonDataList[i][1];
            }

            AlertDialog.Builder MyListAlertDialog = new AlertDialog.Builder(LoginActivity.this);
            MyListAlertDialog.setTitle("請選擇您的伺服器位置");
            // 建立List的事件
            DialogInterface.OnClickListener ListClick = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(LoginActivity.this, jsonDataList[which][1],// 顯示所點選的選項
                            Toast.LENGTH_SHORT).show();

                    //記憶ip位置,公司名稱,介紹,信箱
                    setConfig(LoginActivity.this, "Config", "db_email", jsonDataList[which][0]);
                    setConfig(LoginActivity.this, "Config", "db_comname", jsonDataList[which][1]);
                    setConfig(LoginActivity.this, "Config", "db_comintro", jsonDataList[which][2]);
                    setConfig(LoginActivity.this, "Config", "db_server", jsonDataList[which][3]);
                    setConfig(LoginActivity.this, "Config", "db_account", jsonDataList[which][4]);
                    tv_comname.setText(getConfig(LoginActivity.this,
                            "Config", "db_comname", ""));
                }
            };
            // 建立按下取消什麼事情都不做的事件
            DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            };
            MyListAlertDialog.setItems(listname, ListClick);
            MyListAlertDialog.setNeutralButton("取消", OkClick);
            MyListAlertDialog.show();
        }
    };


    /**
     * SharedPreferences暫時記憶 開始
     **/
    //設定檔儲存
    public static void setConfig(Context context, String name, String key,
                                 String value) {
        SharedPreferences settings = context.getSharedPreferences(name, 0);
        SharedPreferences.Editor PE = settings.edit();
        PE.putString(key, value);
        PE.commit();
    }

    //設定檔讀取
    public static String getConfig(Context context, String name, String
            key, String def) {
        SharedPreferences settings = context.getSharedPreferences(name, 0);
        return settings.getString(key, def);
    }

    /**
     * 背景作業 連結呼叫資料庫
     **/
    private class BackTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            try {
                String result = DBConnector.executeQuery("SELECT * FROM BingoGameDatabace");
                JSONArray jsonArray = new JSONArray(result);

                jsonDataList = new String[jsonArray.length()][5];

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonData = jsonArray.getJSONObject(i);

                    //存進陣列
                    jsonDataList[i][0] = jsonData.getString("email");
                    jsonDataList[i][1] = jsonData.getString("com_name");
                    jsonDataList[i][2] = jsonData.getString("com_intro");
                    jsonDataList[i][3] = jsonData.getString("server");
                    jsonDataList[i][4] = jsonData.getString("account");
                }

            } catch (Exception e) {
                Log.e("log_tag", "解析JSON異常");
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            Log.e("log_tag", "onPostExecute result");
        }
    }

}