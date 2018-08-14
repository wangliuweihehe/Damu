package com.wlw.admin.danmu;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.wlw.admin.danmu.danmu.utils.DanmuProcess;

import master.flame.danmaku.ui.widget.DanmakuView;

public class MainActivity extends AppCompatActivity {
    private DanmakuView danmakuView;
    private DanmuProcess mDanmuProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        String roomid = "288016";
        danmakuView = findViewById(R.id.danMaKuView);
        mDanmuProcess = new DanmuProcess(this, danmakuView, Integer.valueOf(roomid));
        mDanmuProcess.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDanmuProcess.onResume();
        mDanmuProcess.setmDanmakuView(danmakuView);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (danmakuView != null && mDanmuProcess != null) {
            danmakuView.restart();
            mDanmuProcess.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDanmuProcess.onPause();
    }

    @Override
    protected void onDestroy() {
        mDanmuProcess.finish();
        mDanmuProcess.close();
        super.onDestroy();
    }

}
