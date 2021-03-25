package com.cookpad.puree.outputs;

import com.cookpad.puree.PureeLogger;
import com.cookpad.puree.async.AsyncResult;
import com.cookpad.puree.internal.PureeVerboseRunnable;
import com.cookpad.puree.internal.RetryableTaskRunner;
import com.cookpad.puree.storage.EnhancedPureeStorage;
import com.cookpad.puree.storage.Records;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.cookpad.puree.storage.EnhancedPureeStorage.ofType;

@ParametersAreNonnullByDefault
public abstract class PureeBufferedOutput extends PureeOutput {

    RetryableTaskRunner flushTask;

    ScheduledExecutorService executor;

    public PureeBufferedOutput() {
    }

    @Override
    public void initialize(PureeLogger logger) {
        super.initialize(logger);
        executor = logger.getExecutor();
        flushTask = new RetryableTaskRunner(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, conf.getFlushIntervalMillis(), conf.getMaxRetryCount(), executor);
    }

    @Override
    public void receive(final String jsonLog) {
        executor.execute(new PureeVerboseRunnable(new Runnable() {
            @Override
            public void run() {
                String filteredLog = applyFilters(jsonLog);
                if (filteredLog != null) {
                    storage.insert(type(), filteredLog);
                }
            }
        }));

        flushTask.tryToStart();
    }

    @Override
    public void flush() {
        executor.execute(new PureeVerboseRunnable(new Runnable() {
            @Override
            public void run() {
                flushSync();
            }
        }));
    }

    public void flushSync() {
        if (!storage.lock()) {
            flushTask.retryLater();
            return;
        }
        final Records records = getRecordsFromStorage();

        if (records.isEmpty()) {
            storage.unlock();
            flushTask.reset();
            return;
        }

        final List<String> jsonLogs = records.getJsonLogs();

        emit(jsonLogs, new AsyncResult() {
            @Override
            public void success() {
                flushTask.reset();
                storage.delete(records);
                storage.unlock();
            }

            @Override
            public void fail() {
                flushTask.retryLater();
                storage.unlock();
            }
        });
    }

    private Records getRecordsFromStorage() {
        if (storage instanceof EnhancedPureeStorage) {
            return ((EnhancedPureeStorage) storage).select(new EnhancedPureeStorage.QueryBuilder() {
                @Override
                public EnhancedPureeStorage.Query build(EnhancedPureeStorage.Query query) {
                    query.setPredicates(ofType(type()));
                    query.setSorting(conf.getSorting());
                    query.setCount(conf.getLogsPerRequest());
                    return query;
                }
            });
        } else {
            return storage.select(type(), conf.getLogsPerRequest());
        }
    }

    public abstract void emit(List<String> jsonLogs, final AsyncResult result);

    public void emit(String jsonLog) {
        // do nothing
    }
}

