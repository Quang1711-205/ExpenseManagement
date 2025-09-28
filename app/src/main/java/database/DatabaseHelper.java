// 🛠️ DatabaseHelper.java - Enhanced Version for Goals (FIXED)

package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MoneyMasterPro.db";
    private static final int DATABASE_VERSION = 7; // ✅ Tăng version để add cột mới

    // Common columns
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";

    // ✅ FIX: Thêm hằng số COLUMN_NAME chung
    public static final String COLUMN_NAME = "name";

    // User table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_FULL_NAME = "full_name";

    // Transaction table
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_NOTE = "note";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_PAYMENT_METHOD = "payment_method";
    public static final String COLUMN_USER_ID = "user_id";

    // Category table
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COLUMN_CATEGORY_NAME = "name";
    public static final String COLUMN_CATEGORY_TYPE = "type";
    public static final String COLUMN_CATEGORY_ICON = "icon";
    public static final String COLUMN_CATEGORY_COLOR = "color";

    // Budget table
    public static final String TABLE_BUDGETS = "budgets";
    public static final String COLUMN_BUDGET_AMOUNT = "amount";
    public static final String COLUMN_BUDGET_PERIOD = "period";
    public static final String COLUMN_BUDGET_START_DATE = "start_date";
    public static final String COLUMN_BUDGET_END_DATE = "end_date";
    public static final String COLUMN_BUDGET_NAME = "budget_name"; // ✅ FIX: Thêm cột thiếu

    // Goal table
    public static final String TABLE_GOALS = "goals";
    public static final String COLUMN_GOAL_NAME = "name";
    public static final String COLUMN_GOAL_TARGET_AMOUNT = "target_amount";
    public static final String COLUMN_GOAL_CURRENT_AMOUNT = "current_amount";
    public static final String COLUMN_GOAL_DEADLINE = "deadline";
    public static final String COLUMN_GOAL_ICON = "icon";
    public static final String COLUMN_GOAL_STATUS = "status";

    // 🎯 NEW: Goal Transactions table (để track chính xác lịch sử tiết kiệm)
    public static final String TABLE_GOAL_TRANSACTIONS = "goal_transactions";
    public static final String COLUMN_GOAL_ID = "goal_id";
    public static final String COLUMN_TRANSACTION_ID = "transaction_id";
    public static final String COLUMN_GOAL_TRANSACTION_AMOUNT = "amount";
    public static final String COLUMN_GOAL_TRANSACTION_NOTE = "note";

    // ✅ Create table statements - Sử dụng COLUMN_NAME thống nhất
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_USERNAME + " TEXT NOT NULL UNIQUE, "
            + COLUMN_PASSWORD + " TEXT NOT NULL, "
            + COLUMN_EMAIL + " TEXT, "
            + COLUMN_FULL_NAME + " TEXT, "
            + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP);";

    private static final String CREATE_TABLE_CATEGORIES = "CREATE TABLE " + TABLE_CATEGORIES + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT NOT NULL, "  // ✅ FIX: Sử dụng COLUMN_NAME
            + COLUMN_CATEGORY_TYPE + " TEXT NOT NULL CHECK(" + COLUMN_CATEGORY_TYPE + " IN ('income', 'expense')), "
            + COLUMN_CATEGORY_ICON + " TEXT, "
            + COLUMN_CATEGORY_COLOR + " TEXT DEFAULT '#2196F3', "
            + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP);";

    private static final String CREATE_TABLE_TRANSACTIONS = "CREATE TABLE " + TABLE_TRANSACTIONS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_TYPE + " TEXT NOT NULL CHECK(" + COLUMN_TYPE + " IN ('income', 'expense')), "
            + COLUMN_AMOUNT + " REAL NOT NULL CHECK(" + COLUMN_AMOUNT + " > 0), "
            + COLUMN_CATEGORY_ID + " INTEGER NOT NULL, "
            + COLUMN_USER_ID + " INTEGER NOT NULL, "
            + COLUMN_NOTE + " TEXT, "
            + COLUMN_DATE + " DATE NOT NULL, "
            + COLUMN_PAYMENT_METHOD + " TEXT DEFAULT 'Tiền mặt', "
            + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY(" + COLUMN_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_ID + ") ON DELETE RESTRICT, "
            + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";

    // ✅ FIX: Thêm COLUMN_BUDGET_NAME vào bảng budgets
    private static final String CREATE_TABLE_BUDGETS = "CREATE TABLE " + TABLE_BUDGETS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_CATEGORY_ID + " INTEGER NOT NULL, "
            + COLUMN_USER_ID + " INTEGER NOT NULL, "
            + COLUMN_BUDGET_NAME + " TEXT NOT NULL, " // ✅ FIX: Thêm cột budget_name
            + COLUMN_BUDGET_AMOUNT + " REAL NOT NULL CHECK(" + COLUMN_BUDGET_AMOUNT + " > 0), "
            + COLUMN_BUDGET_PERIOD + " TEXT NOT NULL CHECK(" + COLUMN_BUDGET_PERIOD + " IN ('weekly', 'monthly', 'yearly')), "
            + COLUMN_BUDGET_START_DATE + " DATE, "
            + COLUMN_BUDGET_END_DATE + " DATE, "
            + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY(" + COLUMN_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_ID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";

    private static final String CREATE_TABLE_GOALS = "CREATE TABLE " + TABLE_GOALS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT NOT NULL, "  // ✅ FIX: Sử dụng COLUMN_NAME thay vì COLUMN_GOAL_NAME
            + COLUMN_GOAL_TARGET_AMOUNT + " REAL NOT NULL CHECK(" + COLUMN_GOAL_TARGET_AMOUNT + " > 0), "
            + COLUMN_GOAL_CURRENT_AMOUNT + " REAL NOT NULL DEFAULT 0 CHECK(" + COLUMN_GOAL_CURRENT_AMOUNT + " >= 0), "
            + COLUMN_GOAL_DEADLINE + " DATE NOT NULL, "
            + COLUMN_GOAL_ICON + " TEXT, "
            + COLUMN_GOAL_STATUS + " TEXT DEFAULT 'active' CHECK(" + COLUMN_GOAL_STATUS + " IN ('active', 'completed', 'paused')), "
            + COLUMN_USER_ID + " INTEGER NOT NULL, "
            + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";

    // 🎯 NEW: Goal Transactions table
    private static final String CREATE_TABLE_GOAL_TRANSACTIONS = "CREATE TABLE " + TABLE_GOAL_TRANSACTIONS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GOAL_ID + " INTEGER NOT NULL, "
            + COLUMN_TRANSACTION_ID + " INTEGER NOT NULL, "
            + COLUMN_GOAL_TRANSACTION_AMOUNT + " REAL NOT NULL CHECK(" + COLUMN_GOAL_TRANSACTION_AMOUNT + " > 0), "
            + COLUMN_GOAL_TRANSACTION_NOTE + " TEXT, "
            + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY(" + COLUMN_GOAL_ID + ") REFERENCES " + TABLE_GOALS + "(" + COLUMN_ID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY(" + COLUMN_TRANSACTION_ID + ") REFERENCES " + TABLE_TRANSACTIONS + "(" + COLUMN_ID + ") ON DELETE CASCADE, "
            + "UNIQUE(" + COLUMN_GOAL_ID + ", " + COLUMN_TRANSACTION_ID + "));";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DatabaseHelper", "🏗️ Creating database tables...");

        // Create tables in correct order (respecting foreign key dependencies)
        db.execSQL(CREATE_TABLE_USERS);
        Log.d("DatabaseHelper", "✅ Created users table");

        db.execSQL(CREATE_TABLE_CATEGORIES);
        Log.d("DatabaseHelper", "✅ Created categories table");

        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        Log.d("DatabaseHelper", "✅ Created transactions table");

        db.execSQL(CREATE_TABLE_BUDGETS);
        Log.d("DatabaseHelper", "✅ Created budgets table");

        db.execSQL(CREATE_TABLE_GOALS);
        Log.d("DatabaseHelper", "✅ Created goals table");

        // 🎯 NEW: Create goal transactions table
        db.execSQL(CREATE_TABLE_GOAL_TRANSACTIONS);
        Log.d("DatabaseHelper", "✅ Created goal_transactions table");

        // Insert default categories
        insertDefaultCategories(db);
        insertGoalCategories(db); // 🎯 Add goal-specific categories

        Log.d("DatabaseHelper", "✅ Database setup complete - Ready for user data");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "🔄 Upgrading database from v" + oldVersion + " to v" + newVersion);

        if (oldVersion < 7) {
            // ✅ FIX: Thêm cột COLUMN_BUDGET_NAME nếu chưa có
            try {
                db.execSQL("ALTER TABLE " + TABLE_BUDGETS + " ADD COLUMN " + COLUMN_BUDGET_NAME + " TEXT DEFAULT 'My Budget'");
                Log.d("DatabaseHelper", "✅ Added budget_name column to budgets table");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error adding budget_name column: " + e.getMessage());
            }
        }

        if (oldVersion < 5) {
            // Add goal_transactions table for existing users
            try {
                db.execSQL(CREATE_TABLE_GOAL_TRANSACTIONS);
                Log.d("DatabaseHelper", "✅ Added goal_transactions table");

                // Add updated_at column to goals table if not exists
                db.execSQL("ALTER TABLE " + TABLE_GOALS + " ADD COLUMN " + COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP");
                Log.d("DatabaseHelper", "✅ Added updated_at column to goals");

                insertGoalCategories(db);
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error upgrading to v5: " + e.getMessage());
            }
        }

        if (oldVersion < 4) {
            // Full recreate for major changes
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOAL_TRANSACTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

            onCreate(db);
        }
    }

    private void insertDefaultCategories(SQLiteDatabase db) {
        Log.d("DatabaseHelper", "📂 Inserting default categories...");

        try {
            // Income Categories - ✅ FIX: Sử dụng COLUMN_NAME
            String incomeCategories = "INSERT INTO " + TABLE_CATEGORIES + " ("
                    + COLUMN_NAME + ", " + COLUMN_CATEGORY_TYPE + ", "
                    + COLUMN_CATEGORY_ICON + ", " + COLUMN_CATEGORY_COLOR + ") VALUES "
                    + "('Lương chính', 'income', '💰', '#4CAF50'), "
                    + "('Lương thêm', 'income', '💵', '#66BB6A'), "
                    + "('Freelance', 'income', '💻', '#2196F3'), "
                    + "('Thưởng', 'income', '🎁', '#FF9800'), "
                    + "('Đầu tư', 'income', '📈', '#9C27B0'), "
                    + "('Bán hàng', 'income', '🏪', '#00BCD4'), "
                    + "('Thu nhập khác', 'income', '💎', '#607D8B');";

            db.execSQL(incomeCategories);

            // Expense Categories - ✅ FIX: Sử dụng COLUMN_NAME
            String expenseCategories = "INSERT INTO " + TABLE_CATEGORIES + " ("
                    + COLUMN_NAME + ", " + COLUMN_CATEGORY_TYPE + ", "
                    + COLUMN_CATEGORY_ICON + ", " + COLUMN_CATEGORY_COLOR + ") VALUES "
                    + "('Ăn uống', 'expense', '🍔', '#F44336'), "
                    + "('Đi lại', 'expense', '🚗', '#FF5722'), "
                    + "('Mua sắm', 'expense', '🛍️', '#E91E63'), "
                    + "('Giải trí', 'expense', '🎬', '#3F51B5'), "
                    + "('Y tế', 'expense', '🏥', '#009688'), "
                    + "('Giáo dục', 'expense', '📚', '#8BC34A'), "
                    + "('Hóa đơn', 'expense', '📄', '#795548'), "
                    + "('Nhà ở', 'expense', '🏠', '#FF9800'), "
                    + "('Thể thao', 'expense', '⚽', '#4CAF50'), "
                    + "('Du lịch', 'expense', '✈️', '#2196F3'), "
                    + "('Làm đẹp', 'expense', '💄', '#E91E63'), "
                    + "('Chi tiêu khác', 'expense', '📦', '#607D8B');";

            db.execSQL(expenseCategories);

            Log.d("DatabaseHelper", "✅ Default categories inserted successfully");

        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ Error inserting default categories: " + e.getMessage());
        }
    }

    // 🎯 NEW: Insert goal-specific categories
    private void insertGoalCategories(SQLiteDatabase db) {
        Log.d("DatabaseHelper", "🎯 Inserting goal-specific categories...");

        try {
            // Check if "Tiết kiệm" category already exists - ✅ FIX: Sử dụng COLUMN_NAME
            android.database.Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_CATEGORIES + " WHERE " + COLUMN_NAME + " = 'Tiết kiệm'",
                    null
            );

            boolean exists = false;
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            cursor.close();

            if (!exists) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME, "Tiết kiệm");  // ✅ FIX: Sử dụng COLUMN_NAME
                values.put(COLUMN_CATEGORY_TYPE, "expense");
                values.put(COLUMN_CATEGORY_ICON, "🎯");
                values.put(COLUMN_CATEGORY_COLOR, "#4CAF50");

                long result = db.insert(TABLE_CATEGORIES, null, values);
                if (result != -1) {
                    Log.d("DatabaseHelper", "✅ Added 'Tiết kiệm' category for goals");
                }
            } else {
                Log.d("DatabaseHelper", "ℹ️ 'Tiết kiệm' category already exists");
            }

        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ Error inserting goal categories: " + e.getMessage());
        }
    }

    // 🎯 NEW: Helper method to link goal with transaction
    public long insertGoalTransaction(SQLiteDatabase db, int goalId, long transactionId, double amount, String note) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GOAL_ID, goalId);
        values.put(COLUMN_TRANSACTION_ID, transactionId);
        values.put(COLUMN_GOAL_TRANSACTION_AMOUNT, amount);
        values.put(COLUMN_GOAL_TRANSACTION_NOTE, note);

        return db.insert(TABLE_GOAL_TRANSACTIONS, null, values);
    }

    // 🎯 NEW: Get goal transactions with details
    public android.database.Cursor getGoalTransactions(int goalId) {
        SQLiteDatabase db = getReadableDatabase();

        String query = "SELECT gt.*, t." + COLUMN_DATE + ", t." + COLUMN_PAYMENT_METHOD + ", t." + COLUMN_NOTE + " as transaction_note " +
                "FROM " + TABLE_GOAL_TRANSACTIONS + " gt " +
                "JOIN " + TABLE_TRANSACTIONS + " t ON gt." + COLUMN_TRANSACTION_ID + " = t." + COLUMN_ID + " " +
                "WHERE gt." + COLUMN_GOAL_ID + " = ? " +
                "ORDER BY gt." + COLUMN_CREATED_AT + " DESC";

        return db.rawQuery(query, new String[]{String.valueOf(goalId)});
    }

    // ✅ Existing utility methods...
    public void logDatabaseInfo(SQLiteDatabase db) {
        Log.d("DatabaseHelper", "=== DATABASE INFO ===");

        try {
            android.database.Cursor tablesCursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'", null);

            Log.d("DatabaseHelper", "📋 Tables in database:");
            while (tablesCursor.moveToNext()) {
                String tableName = tablesCursor.getString(0);

                android.database.Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
                int recordCount = 0;
                if (countCursor.moveToFirst()) {
                    recordCount = countCursor.getInt(0);
                }
                countCursor.close();

                Log.d("DatabaseHelper", "  📋 " + tableName + ": " + recordCount + " records");
            }
            tablesCursor.close();

        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ Error logging database info: " + e.getMessage());
        }
    }

    public boolean isDatabaseReady() {
        try {
            SQLiteDatabase db = getReadableDatabase();

            android.database.Cursor categoryCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORIES, null);
            boolean hasCategories = false;
            if (categoryCursor.moveToFirst()) {
                hasCategories = categoryCursor.getInt(0) > 0;
            }
            categoryCursor.close();

            return hasCategories;

        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ Database not ready: " + e.getMessage());
            return false;
        }
    }
}