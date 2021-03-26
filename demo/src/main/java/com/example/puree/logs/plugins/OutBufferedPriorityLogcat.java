package com.example.puree.logs.plugins;

import com.cookpad.puree.async.AsyncResult;
import com.cookpad.puree.outputs.OutputConfiguration;
import com.cookpad.puree.plugins.OutBufferedLogcat;
import com.cookpad.puree.storage.EnhancedPureeStorage.Sort;

import org.json.JSONArray;

import android.util.Log;

import java.util.List;

import javax.annotation.Nonnull;

public class OutBufferedPriorityLogcat extends OutBufferedLogcat {
    public static final String TYPE = "buffered_priority_logcat";

    private static final String TAG = OutBufferedPriorityLogcat.class.getSimpleName();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void emit(List<String> jsonLogs, AsyncResult asyncResult) {
        JSONArray jsonLogsArray = new JSONArray();

        for (String jsonLog: jsonLogs) {
            jsonLogsArray.put(jsonLog);
        }

        Log.d(TAG, jsonLogs.toString());

        asyncResult.success();
    }

    @Nonnull
    @Override
    public OutputConfiguration configure(OutputConfiguration conf) {
        OutputConfiguration configuration = super.configure(conf);
        conf.setLogsPerRequest(2);
        configuration.setFlushIntervalMillis(1000); // 1s
        configuration.setPurgeAgeMillis(10 * 1000); // 10s
        configuration.setSorting(new Sort(Sort.Field.PRIORITY, Sort.Order.DESCENDING));
        return configuration;
    }
}
