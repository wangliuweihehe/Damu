package com.wlw.admin.danmu.danmu.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextPaint;
import android.util.Log;

import com.wlw.admin.danmu.danmu.client.DyBulletScreenClient;
import com.wlw.admin.danmu.danmu.parser.BiliDanmukuParser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SimpleTextCacheStuffer;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.util.SystemClock;
import master.flame.danmaku.ui.widget.DanmakuView;

public class DanmuProcess {
    private Context mContext;
    private DanmakuView mDanmakuView;
    private DanmakuContext mDanmakuContext;
    private BaseDanmakuParser mParser;
    private int mRoomId;
    private DyBulletScreenClient mDanmuClient;
    /**
     * 弹幕  随机颜色
     */
    private Random random;
    private int[] ranColor = {
            0xe0ffffff,
            0xe0F0E68C,
            0xe0F08080,
            0xe0FFC0CB,
            0xe000FA9A,
            0xe000FF7F,
            0xe0FFD700,
            0xe07FFFD4,
            0xe0FF7F50,
            0xe0DC143C,
            0xe0FFC0CB,
            0xe0DB7093,
            0xe87CEEB};


    public DanmuProcess(Context context, DanmakuView danmakuView, int roomId) {
        this.mContext = context;
        this.mDanmakuView = danmakuView;
        this.mRoomId = roomId;
        random = new Random();
        mDanmakuView.start();
    }

    public void start() {
        initDanmaku();
        getAndAddDanmu();
        Timer timer = new Timer();
        timer.schedule(new AsyncAddTask(), 0, 1000);
    }

    private void initDanmaku() {
        mDanmakuContext = DanmakuContext.create();
        try {
            mParser = createParser(null);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_LR, 5);
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(1.2f)
                .setScaleTextSize(1.2f)
                .setCacheStuffer(new SimpleTextCacheStuffer(), mCacheStufferAdapter)
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);

        if (mDanmakuView != null) {
            mDanmakuView.setCallback(new DrawHandler.Callback() {
                @Override
                public void prepared() {
                    mDanmakuView.start();
                }

                @Override
                public void updateTimer(DanmakuTimer timer) {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {

                }

                @Override
                public void drawingFinished() {

                }
            });
            mDanmakuView.prepare(mParser, mDanmakuContext);
            mDanmakuView.enableDanmakuDrawingCache(true);
        }
    }

    private BaseDanmakuParser createParser(InputStream stream) throws IllegalDataException {
        if (stream == null) {
            return new BaseDanmakuParser() {
                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);
        loader.load(stream);
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;
    }

    private void getAndAddDanmu() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int groupId = -9999;
                mDanmuClient = DyBulletScreenClient.getInstance();
                //设置需要连接和访问的房间ID，以及弹幕池分组号
                mDanmuClient.start(mRoomId, groupId);

                mDanmuClient.setmHandleMsgListener(new DyBulletScreenClient.HandleMsgListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void handleMessage(String txt) {
//                        发送弹幕
                        addDanmaku(txt);
                    }
                });
            }
        });
        thread.start();
    }

    public void setmDanmakuView(DanmakuView mDanmakuView) {
        this.mDanmakuView = mDanmakuView;
    }

    private void addDanmaku(String txt) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null) {
            Log.e("danmaku====='", "111");
            return;
        }
        if (mDanmakuView == null) {
            Log.e("mDanmakuView====='", "111");
            return;
        }
        danmaku.text = txt;
        danmaku.padding = 5;
        danmaku.priority = 0;
        danmaku.isLive = true;
        danmaku.textColor = Color.RED;
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        Log.e("data====='", txt);
        mDanmakuView.resume();
        mDanmakuView.addDanmaku(danmaku);
        Log.e("data====='", "---------------");
    }

    public void finish() {
        //停止从服务器获取弹幕
        mDanmuClient.stop();

    }

    private BaseCacheStuffer.Proxy mCacheStufferAdapter = new BaseCacheStuffer.Proxy() {

        @Override
        public void prepareDrawing(final BaseDanmaku danmaku, boolean fromWorkerThread) {
        }

        @Override
        public void releaseResource(BaseDanmaku danmaku) {
            danmaku.text = null;
        }
    };


//    class BackgroundCacheStuffer extends SpannedCacheStuffer {
//        // 通过扩展SimpleTextCacheStuffer或SpannedCacheStuffer个性化你的弹幕样式
//        final Paint paint = new Paint();
//        final RectF rf = new RectF();
//
//        @Override
//        public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
//            danmaku.padding = 15;  // 在背景绘制模式下增加padding
//
//            super.measure(danmaku, paint, fromWorkerThread);
//        }
//
//        @Override
//        public void drawBackground(BaseDanmaku danmaku, Canvas canvas, float left, float top) {
//            int ranNumber = random.nextInt(ranColor.length);
//            int color = ranColor[ranNumber];
//            paint.setAntiAlias(true);
//            if (color != 0xe0ffffff && ranNumber % 2 == 0) {
//                paint.setColor(color);  //弹幕背景颜色
//                rf.left = left;
//                rf.right = left + danmaku.paintWidth;
//                rf.top = top;
//                rf.bottom = top + danmaku.paintHeight;
//                danmaku.textColor = 0xe0ffffff;
//                paint.setStyle(Paint.Style.FILL);
//                canvas.drawRoundRect(rf, 40, 40, paint);
//            } else {
//                danmaku.textColor = color;
//                paint.setColor(color);  //弹幕背景颜色
//                rf.left = left;
//                rf.right = left + danmaku.paintWidth;
//                rf.top = top;
//                rf.bottom = top + danmaku.paintHeight;
//                paint.setStyle(Paint.Style.STROKE);
//                canvas.drawRoundRect(rf, 40, 40, paint);
//            }
//        }
//
//        @Override
//        public void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint) {
//            // 禁用描边绘制
//        }
//    }

    public void onResume() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.resume();
        }
    }

    public void onPause() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    public void close() {
        if (mContext != null) {
            mContext = null;
        }
        if (mDanmakuView != null) {
            mDanmakuView.release();
            mDanmakuView.clear();
            mDanmakuView = null;
        }
        if (mDanmakuContext != null) {
            mDanmakuContext = null;
        }
        if (mDanmuClient != null) {
            mDanmuClient = null;
        }
        if (mParser != null) {
            mParser = null;
        }

    }

    class AsyncAddTask extends TimerTask {
        @Override
        public void run() {
            for (int i = 0; i < 20; i++) {
                addDanmakuT();
            }
        }
    }

    private void addDanmakuT() {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null) {
            Log.e("tag", "danmaku==null");
            return;
        }
        if (mDanmakuView == null) {
            Log.e("tag", "mDanmakuView==null");
            return;
        }
        danmaku.text = "这是一条弹幕" + System.nanoTime();
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = true;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200);
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = Color.WHITE;
        danmaku.borderColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);
    }
}
