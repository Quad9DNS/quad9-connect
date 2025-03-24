package com.quad9.aegis.Model;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class RestartJobService extends JobService {
    private static final String TAG = "RestartJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent intent = new Intent("restartService");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Job onStop");
        return true;
    }
}
