package com.gianlu.aria2android;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.gianlu.commonutils.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PerformanceMonitor implements Runnable {
    private static final Pattern pattern = Pattern.compile("(\\d*?)\\s+(\\d*?)\\s+(\\d*?)%\\s(.)\\s+(\\d*?)\\s+(\\d*?)K\\s+(\\d*?)K\\s+(..)\\s(.*?)\\s+(.*)$");
    private final NotificationManager manager;
    private final Context context;
    private final int delay;
    private final int NOTIFICATION_ID;
    private final NotificationCompat.Builder builder;
    private final long startTime;
    private boolean _stop = false;

    PerformanceMonitor(Context context, int delay, int NOTIFICATION_ID, NotificationCompat.Builder builder) {
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
        this.delay = delay;
        this.NOTIFICATION_ID = NOTIFICATION_ID;
        this.builder = builder;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            Process process = Runtime.getRuntime().exec("top -d " + String.valueOf(delay));

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null && !_stop) {
                if (line.contains("aria2c")) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        sendNotification(matcher.group(1), matcher.group(3), matcher.group(7));
                    }
                }
            }
        } catch (IOException ex) {
            stop();
            CommonUtils.logMe(context, ex);
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private void sendNotification(String pid, String cpuUsage, String rss) {
        if (_stop)
            return;

        RemoteViews layout = new RemoteViews(context.getPackageName(), R.layout.custom_notification);
        layout.setTextViewText(R.id.customNotification_runningTime, "Running time: " + CommonUtils.timeFormatter((System.currentTimeMillis() - startTime) / 1000));
        layout.setTextViewText(R.id.customNotification_pid, "PID: " + pid);
        layout.setTextViewText(R.id.customNotification_cpu, "CPU: " + cpuUsage + "%");
        layout.setTextViewText(R.id.customNotification_memory, "Memory: " + CommonUtils.dimensionFormatter(Integer.parseInt(rss) * 1024));
        builder.setCustomContentView(layout);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    void stop() {
        _stop = true;
    }
}
