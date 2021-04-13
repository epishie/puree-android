package com.cookpad.puree.storage;

import com.cookpad.puree.internal.ProcessName;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

@ParametersAreNonnullByDefault
public class PureeSQLiteStorage extends EnhancedPureeStorage {

    private static final String DATABASE_NAME = "puree.db";

    private static final String TABLE_NAME = "logs";

    private static final String COLUMN_NAME_ID = "id";

    private static final String COLUMN_NAME_TYPE = "type";

    private static final String COLUMN_NAME_LOG = "log";

    private static final String COLUMN_NAME_CREATED_AT = "created_at";

    private static final int DATABASE_VERSION = 2;

    private final SupportSQLiteOpenHelper openHelper;

    private final boolean isOrderByDesc;

    private final AtomicBoolean lock = new AtomicBoolean(false);

    static String databaseName(Context context) {
        // do not share the database file in multi processes
        String processName = ProcessName.getAndroidProcessName(context);
        if (TextUtils.isEmpty(processName)) {
            return DATABASE_NAME;
        } else {
            return processName + "." + DATABASE_NAME;
        }
    }

    public PureeSQLiteStorage(Context context) {
        this(context, false);
    }

    public PureeSQLiteStorage(Context context, boolean isOrderByDesc) {
        this.isOrderByDesc = isOrderByDesc;
        openHelper = new FrameworkSQLiteOpenHelperFactory()
                .create(
                        SupportSQLiteOpenHelper.Configuration
                                .builder(context)
                                .name(databaseName(context))
                                .callback(new SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {
                                    @Override
                                    public void onCreate(SupportSQLiteDatabase db) {
                                        PureeSQLiteStorage.this.onCreate(db);
                                    }

                                    @Override
                                    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                        PureeSQLiteStorage.this.onUpgrade(db, oldVersion, newVersion);
                                    }
                                })
                                .build()
                );
    }

    public void insert(String type, String jsonLog) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_TYPE, type);
        contentValues.put(COLUMN_NAME_LOG, jsonLog);
        contentValues.put(COLUMN_NAME_CREATED_AT, System.currentTimeMillis());
        openHelper.getWritableDatabase().insert(TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, contentValues);
    }

    private String getOrderType() {
        String orderType;
        if (this.isOrderByDesc) {
            orderType = "DESC";
        } else {
            orderType = "ASC";
        }
        return orderType;
    }

    public Records select(String type, int logsPerRequest) {
        String query = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + COLUMN_NAME_TYPE + " = ?" +
                " ORDER BY id " + getOrderType() +
                " LIMIT " + logsPerRequest;
        Cursor cursor = openHelper.getReadableDatabase().query(query, new String[]{type});

        try {
            return recordsFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    @Override
    public Records selectAll() {
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY id " + getOrderType();
        Cursor cursor = openHelper.getReadableDatabase().query(query);

        try {
            return recordsFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    private Records recordsFromCursor(Cursor cursor) {
        Records records = new Records();
        while (cursor.moveToNext()) {
            Record record = buildRecord(cursor);
            records.add(record);
        }
        return records;
    }

    private Record buildRecord(Cursor cursor) {
        return new Record(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2));

    }

    private int getRecordCount() {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Cursor cursor = openHelper.getReadableDatabase().query(query);
        int count = 0;
        if (cursor.moveToNext()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    @Override
    public void delete(Records records) {
        String where = COLUMN_NAME_ID + " IN (" + records.getIdsAsString() + ")";
        openHelper.getWritableDatabase().delete(TABLE_NAME, where, null);
    }

    @Override
    public void truncateBufferedLogs(int maxRecords) {
        int recordSize = getRecordCount();
        if (recordSize > maxRecords) {
            String where = COLUMN_NAME_ID + " IN ( SELECT " + COLUMN_NAME_ID +" FROM " + TABLE_NAME +
                    " ORDER BY " + COLUMN_NAME_ID + " ASC LIMIT " + (recordSize - maxRecords) + ")";
            openHelper.getWritableDatabase().delete(TABLE_NAME, where, null);
        }
    }

    @Override
    public void clear() {
        openHelper.getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    private void onCreate(SupportSQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME_TYPE + " TEXT," +
                COLUMN_NAME_LOG + " TEXT," +
                COLUMN_NAME_CREATED_AT + " TEXT" +
                ")";
        db.execSQL(query);
    }

    private void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_NAME_CREATED_AT + " INTEGER;");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        openHelper.close();
        super.finalize();
    }

    @Override
    public boolean lock() {
        return lock.compareAndSet(false, true);
    }

    @Override
    public void unlock() {
        lock.set(false);
    }

    @Override
    public Records select(QueryBuilder queryBuilder) {
        Query query = queryBuilder.build(new Query());

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ");
        sb.append(TABLE_NAME);

        List<Object> values = new ArrayList<>();

        List<String> wheres = new ArrayList<>();
        if (query.getPredicates() != null) {
            for (Predicate predicate : query.getPredicates()) {
                if (predicate instanceof OfType) {
                    wheres.add(COLUMN_NAME_TYPE + " = ?");
                    values.add(((OfType) predicate).getType());
                }
            }
        }
        if (!wheres.isEmpty()) {
            sb.append(" WHERE ");
            sb.append(TextUtils.join(" AND ", wheres));
        }

        sb.append(" ORDER BY ");
        //noinspection SwitchStatementWithTooFewBranches
        switch (query.getSort().getField()) {
            case ID:
                sb.append(COLUMN_NAME_ID);
                break;
        }
        switch (query.getSort().getOrder()) {
            case ASCENDING:
                sb.append(" ASC");
                break;
            case DESCENDING:
                sb.append(" DESC");
        }
        if (query.getCount() != null) {
            sb.append(" LIMIT ");
            sb.append(query.getCount());
        }

        Cursor cursor = openHelper.getReadableDatabase().query(sb.toString(), values.toArray());

        try {
            return recordsFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }
}
