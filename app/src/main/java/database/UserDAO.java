package database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import models.User;

public class UserDAO {
    private DatabaseHelper dbHelper;

    public UserDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Validate user credentials
     */
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT " + DatabaseHelper.COLUMN_ID + " FROM " + DatabaseHelper.TABLE_USERS
                    + " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ? AND " + DatabaseHelper.COLUMN_PASSWORD + " = ?";

            cursor = db.rawQuery(query, new String[]{username, password});
            boolean isValid = cursor.getCount() > 0;

            Log.d("UserDAO", "✅ validateUser - Username: " + username + ", Valid: " + isValid);
            return isValid;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error validating user: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) cursor.close();
            // ❌ KHÔNG đóng database ở đây - để DashboardActivity quản lý
            // if (db != null) db.close();
        }
    }

    /**
     * Get user ID by username - FIX: Không đóng database
     */
    public int getUserId(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT " + DatabaseHelper.COLUMN_ID + " FROM " + DatabaseHelper.TABLE_USERS
                    + " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?";

            cursor = db.rawQuery(query, new String[]{username});
            int userId = -1;

            if (cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            }

            Log.d("UserDAO", "✅ getUserId - Username: " + username + ", UserID: " + userId);
            return userId;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error getting user ID: " + e.getMessage());
            return -1;
        } finally {
            if (cursor != null) cursor.close();
            // ❌ KHÔNG đóng database
            // if (db != null) db.close();
        }
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS
                    + " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?";

            cursor = db.rawQuery(query, new String[]{username});
            User user = null;

            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)));
                user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FULL_NAME)));
                user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
            }

            Log.d("UserDAO", "✅ getUserByUsername - Found user: " + (user != null ? user.getUsername() : "null"));
            return user;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error getting user by username: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) cursor.close();
            // ❌ KHÔNG đóng database
            // if (db != null) db.close();
        }
    }

    /**
     * Insert new user - Chỉ đóng database khi ghi dữ liệu
     */
    public long insertUser(User user) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(DatabaseHelper.COLUMN_USERNAME, user.getUsername());
            values.put(DatabaseHelper.COLUMN_PASSWORD, user.getPassword());
            values.put(DatabaseHelper.COLUMN_EMAIL, user.getEmail());
            values.put(DatabaseHelper.COLUMN_FULL_NAME, user.getFullName());

            long result = db.insert(DatabaseHelper.TABLE_USERS, null, values);

            Log.d("UserDAO", "✅ insertUser - Result: " + result + " for username: " + user.getUsername());
            return result;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error inserting user: " + e.getMessage());
            return -1;
        } finally {
            // ✅ Chỉ đóng database khi INSERT/UPDATE/DELETE
            if (db != null) db.close();
        }
    }

    /**
     * Update user profile
     */
    public boolean updateUser(User user) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(DatabaseHelper.COLUMN_EMAIL, user.getEmail());
            values.put(DatabaseHelper.COLUMN_FULL_NAME, user.getFullName());
            values.put(DatabaseHelper.COLUMN_UPDATED_AT, "datetime('now')");

            int rowsAffected = db.update(DatabaseHelper.TABLE_USERS, values,
                    DatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(user.getId())});

            Log.d("UserDAO", "✅ updateUser - Rows affected: " + rowsAffected);
            return rowsAffected > 0;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error updating user: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Update user password
     */
    public boolean updatePassword(int userId, String newPassword) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);
            values.put(DatabaseHelper.COLUMN_UPDATED_AT, "datetime('now')");

            int rowsAffected = db.update(DatabaseHelper.TABLE_USERS, values,
                    DatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(userId)});

            Log.d("UserDAO", "✅ updatePassword - Rows affected: " + rowsAffected);
            return rowsAffected > 0;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error updating password: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    /**
     * Check if username exists
     */
    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT " + DatabaseHelper.COLUMN_ID + " FROM " + DatabaseHelper.TABLE_USERS
                    + " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?";

            cursor = db.rawQuery(query, new String[]{username});
            boolean exists = cursor.getCount() > 0;

            Log.d("UserDAO", "✅ isUsernameExists - Username: " + username + ", Exists: " + exists);
            return exists;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error checking username exists: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) cursor.close();
            // ❌ KHÔNG đóng database cho READ operations
            // if (db != null) db.close();
        }
    }

    /**
     * ✅ THÊM method để verify user tồn tại với database được truyền vào
     */
    public boolean verifyUserExists(int userId, SQLiteDatabase database) {
        Cursor cursor = null;

        try {
            String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
            cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            boolean exists = false;
            if (cursor != null && cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }

            Log.d("UserDAO", "✅ verifyUserExists - UserID: " + userId + ", Exists: " + exists);
            return exists;

        } catch (Exception e) {
            Log.e("UserDAO", "❌ Error verifying user exists: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) cursor.close();
            // ❌ KHÔNG đóng database - được quản lý bởi caller
        }
    }
}