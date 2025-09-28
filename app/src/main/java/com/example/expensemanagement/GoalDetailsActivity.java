// 🔍 GoalDetailsActivity.java - IMPROVED VERSION
// Improved: Smart transaction filtering - only transactions with "tiết kiệm" AND goal name

package com.example.expensemanagement;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import database.DatabaseHelper;
import models.Goal;
import models.Transaction;

public class GoalDetailsActivity extends AppCompatActivity {
    private static final String TAG = "GoalDetailsActivity";

    private DatabaseHelper dbHelper;
    private int goalId, currentUserId;
    private Goal currentGoal;

    // ✅ FIXED: Multiple date formatters to handle different date formats
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // For database dates
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // For display
    private SimpleDateFormat deadlineFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // For goal deadlines

    // Views
    private TextView tvGoalName, tvGoalIcon, tvTargetAmount, tvCurrentAmount, tvProgress;
    private TextView tvDeadline, tvDaysLeft, tvDailyAmount, tvStatus;
    private ProgressBar progressBarGoal;
    private Button btnEditGoal, btnDeleteGoal, btnTogglePause;
    private FloatingActionButton fabAddMoney;
    private RecyclerView recyclerViewHistory;
    private LinearLayout layoutEmptyHistory;

    // History adapter
    private TransactionHistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_details);

        // Get data from intent
        goalId = getIntent().getIntExtra("goal_id", -1);
        currentUserId = getIntent().getIntExtra("user_id", -1);

        if (goalId == -1 || currentUserId == -1) {
            Toast.makeText(this, "❌ Lỗi: Không xác định được mục tiêu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupRecyclerView();
        loadGoalDetails();
        loadTransactionHistory();
        setupListeners();
    }

    private void initViews() {
        tvGoalName = findViewById(R.id.tvGoalName);
        tvGoalIcon = findViewById(R.id.tvGoalIcon);
        tvTargetAmount = findViewById(R.id.tvTargetAmount);
        tvCurrentAmount = findViewById(R.id.tvCurrentAmount);
        tvProgress = findViewById(R.id.tvProgress);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvDaysLeft = findViewById(R.id.tvDaysLeft);
        tvDailyAmount = findViewById(R.id.tvDailyAmount);
        tvStatus = findViewById(R.id.tvStatus);
        progressBarGoal = findViewById(R.id.progressBarGoal);
        btnEditGoal = findViewById(R.id.btnEditGoal);
        btnDeleteGoal = findViewById(R.id.btnDeleteGoal);
        btnTogglePause = findViewById(R.id.btnTogglePause);
        fabAddMoney = findViewById(R.id.fabAddMoney);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initDatabase() {
        dbHelper = new DatabaseHelper(this);
    }

    private void setupRecyclerView() {
        historyAdapter = new TransactionHistoryAdapter();
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(historyAdapter);
    }

    private void loadGoalDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_GOALS,
                    null,
                    DatabaseHelper.COLUMN_ID + " = ? AND " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(goalId), String.valueOf(currentUserId)},
                    null, null, null
            );

            if (cursor.moveToFirst()) {
                currentGoal = new Goal();
                currentGoal.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                // ✅ FIXED: Use COLUMN_NAME instead of COLUMN_GOAL_NAME
                currentGoal.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
                currentGoal.setTargetAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_TARGET_AMOUNT)));
                currentGoal.setCurrentAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_CURRENT_AMOUNT)));
                currentGoal.setDeadline(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_DEADLINE)));
                currentGoal.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_ICON)));
                currentGoal.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_STATUS)));
                currentGoal.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));

                updateUI();
            } else {
                Toast.makeText(this, "❌ Không tìm thấy mục tiêu", Toast.LENGTH_SHORT).show();
                finish();
            }
            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error loading goal details: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi tải thông tin mục tiêu", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ FIXED: Smart date parsing to handle different date formats
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Try different date formats
        SimpleDateFormat[] formatters = {
                deadlineFormat,     // dd/MM/yyyy (for goal deadlines)
                dbDateFormat,       // yyyy-MM-dd (for database dates)
                displayDateFormat   // dd/MM/yyyy (for display dates)
        };

        for (SimpleDateFormat formatter : formatters) {
            try {
                return formatter.parse(dateStr.trim());
            } catch (ParseException e) {
                // Continue to next format
            }
        }

        Log.e(TAG, "Could not parse date: " + dateStr);
        return null;
    }

    // ✅ FIXED: Improved date calculation with proper error handling
    private long calculateDaysLeft(String deadlineStr) {
        Date deadline = parseDate(deadlineStr);
        if (deadline == null) {
            return -999; // Parsing error
        }

        Date today = new Date();
        long diffInMillis = deadline.getTime() - today.getTime();
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    private String formatDeadline(String deadlineStr) {
        Date deadline = parseDate(deadlineStr);
        if (deadline == null) {
            return deadlineStr; // Return original if parsing fails
        }
        return displayDateFormat.format(deadline);
    }

    private int getProgressPercentage() {
        if (currentGoal == null || currentGoal.getTargetAmount() <= 0) return 0;
        return (int) ((currentGoal.getCurrentAmount() * 100) / currentGoal.getTargetAmount());
    }

    private boolean isGoalCompleted() {
        return currentGoal != null && currentGoal.getCurrentAmount() >= currentGoal.getTargetAmount();
    }

    private double getDailyAmountNeeded() {
        if (currentGoal == null) return 0;

        long daysLeft = calculateDaysLeft(currentGoal.getDeadline());
        if (daysLeft <= 0) return 0;

        double remaining = currentGoal.getTargetAmount() - currentGoal.getCurrentAmount();
        return Math.max(0, remaining / daysLeft);
    }

    private String getDaysLeftText() {
        if (currentGoal == null) return "--";

        long daysLeft = calculateDaysLeft(currentGoal.getDeadline());

        if (daysLeft == -999) {
            return "Lỗi ngày";
        } else if (daysLeft > 0) {
            return "Còn " + daysLeft + " ngày";
        } else if (daysLeft == 0) {
            return "Hết hạn hôm nay!";
        } else {
            return "Đã quá hạn " + Math.abs(daysLeft) + " ngày";
        }
    }

    private String getStatusDisplayText() {
        if (currentGoal == null) return "Không xác định";

        switch (currentGoal.getStatus()) {
            case "active":
                return "🟢 Đang thực hiện";
            case "paused":
                return "🟡 Tạm dừng";
            case "completed":
                return "✅ Hoàn thành";
            default:
                return "❓ Không xác định";
        }
    }

    private int getStatusColor() {
        if (currentGoal == null) return android.R.color.black;

        switch (currentGoal.getStatus()) {
            case "active":
                return android.R.color.holo_blue_bright;
            case "paused":
                return android.R.color.holo_orange_dark;
            case "completed":
                return android.R.color.holo_green_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    private void updateUI() {
        if (currentGoal == null) return;

        DecimalFormat formatter = new DecimalFormat("#,###");

        // Basic info
        tvGoalName.setText(currentGoal.getName());
        tvGoalIcon.setText(currentGoal.getIcon() != null ? currentGoal.getIcon() : "🎯");

        // Amounts
        tvTargetAmount.setText(formatter.format(currentGoal.getTargetAmount()) + " đ");
        tvCurrentAmount.setText(formatter.format(currentGoal.getCurrentAmount()) + " đ");

        // Progress
        int progress = getProgressPercentage();
        progressBarGoal.setProgress(progress);
        tvProgress.setText(progress + "%");

        // Deadline and calculation
        tvDeadline.setText("Hạn: " + formatDeadline(currentGoal.getDeadline()));
        tvDaysLeft.setText(getDaysLeftText());

        // ✅ FIXED: Set proper color for days left text
        long daysLeft = calculateDaysLeft(currentGoal.getDeadline());
        if (daysLeft == -999) {
            tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (daysLeft <= 0) {
            tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (daysLeft <= 7) {
            tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvDaysLeft.setTextColor(getResources().getColor(android.R.color.black));
        }

        // Daily amount needed
        if (isGoalCompleted()) {
            tvDailyAmount.setText("🎉 Đã hoàn thành!");
            tvDailyAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (daysLeft <= 0 && daysLeft != -999) {
            tvDailyAmount.setText("⚠️ Đã quá hạn");
            tvDailyAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (daysLeft > 0) {
            double dailyAmount = getDailyAmountNeeded();
            tvDailyAmount.setText("Cần: " + formatter.format(dailyAmount) + " đ/ngày");
            tvDailyAmount.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            tvDailyAmount.setText("--");
            tvDailyAmount.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Status
        tvStatus.setText(getStatusDisplayText());
        tvStatus.setTextColor(getResources().getColor(getStatusColor()));

        // Button states
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (currentGoal == null) return;

        // Edit button - always enabled
        btnEditGoal.setEnabled(true);

        // Delete button - always enabled
        btnDeleteGoal.setEnabled(true);

        // Pause/Resume button
        if ("completed".equals(currentGoal.getStatus())) {
            btnTogglePause.setVisibility(View.GONE);
            fabAddMoney.setVisibility(View.GONE);
        } else {
            btnTogglePause.setVisibility(View.VISIBLE);
            fabAddMoney.setVisibility(View.VISIBLE);

            if ("paused".equals(currentGoal.getStatus())) {
                btnTogglePause.setText("▶️ Tiếp tục");
                btnTogglePause.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                btnTogglePause.setText("⏸️ Tạm dừng");
                btnTogglePause.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
        }
    }

    // ✅ IMPROVED: Smart validation - only transactions with "tiết kiệm" AND goal name
    private boolean isTransactionRelatedToGoal(Transaction transaction, String goalName) {
        if (transaction == null || goalName == null) return false;

        String note = transaction.getNote();
        String paymentMethod = transaction.getPaymentMethod();

        if (note == null) note = "";
        if (paymentMethod == null) paymentMethod = "";

        String noteLower = note.toLowerCase().trim();
        String goalLower = goalName.toLowerCase().trim();
        String paymentLower = paymentMethod.toLowerCase().trim();

        // Check if contains "tiết kiệm" (in note or payment method)
        boolean hasSavings = noteLower.contains("tiết kiệm") ||
                noteLower.contains("tiet kiem") ||
                paymentLower.contains("tiết kiệm") ||
                paymentLower.contains("tiet kiem");

        // Check if contains goal name
        boolean hasGoalName = noteLower.contains(goalLower);

        // Also check for common savings phrases with goal name
        boolean hasSavingsPhrase = noteLower.contains("tiết kiệm cho " + goalLower) ||
                noteLower.contains("tiết kiệm để " + goalLower) ||
                noteLower.contains("cho mục tiêu " + goalLower) ||
                noteLower.contains("để mua " + goalLower) ||
                noteLower.contains("dành cho " + goalLower);

        // ✅ MAIN LOGIC: Must have BOTH "tiết kiệm" AND goal name, OR specific saving phrases
        boolean isRelated = (hasSavings && hasGoalName) || hasSavingsPhrase;

        if (isRelated) {
            Log.d(TAG, "✅ Transaction matched for goal '" + goalName + "': " + note);
        } else {
            Log.d(TAG, "❌ Transaction NOT matched for goal '" + goalName + "': " + note +
                    " (hasSavings: " + hasSavings + ", hasGoalName: " + hasGoalName + ")");
        }

        return isRelated;
    }

    // ✅ IMPROVED: Enhanced main transaction loading with strict filtering
    private void loadTransactionHistory() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // First try: Use goal_transactions table (preferred method)
            String query = "SELECT t.*, c." + DatabaseHelper.COLUMN_NAME + " as category_name, " +
                    "gt." + DatabaseHelper.COLUMN_GOAL_TRANSACTION_AMOUNT + " as goal_amount, " +
                    "gt." + DatabaseHelper.COLUMN_GOAL_TRANSACTION_NOTE + " as goal_note " +
                    "FROM " + DatabaseHelper.TABLE_GOAL_TRANSACTIONS + " gt " +
                    "JOIN " + DatabaseHelper.TABLE_TRANSACTIONS + " t ON gt." + DatabaseHelper.COLUMN_TRANSACTION_ID + " = t." + DatabaseHelper.COLUMN_ID + " " +
                    "LEFT JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                    "WHERE gt." + DatabaseHelper.COLUMN_GOAL_ID + " = ? " +
                    "AND t." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                    "ORDER BY t." + DatabaseHelper.COLUMN_DATE + " DESC, t." + DatabaseHelper.COLUMN_CREATED_AT + " DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(goalId), String.valueOf(currentUserId)});

            while (cursor.moveToNext()) {
                Transaction transaction = new Transaction();
                transaction.setId((long) cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));

                // Use the goal_amount if available, otherwise use transaction amount
                int goalAmountIndex = cursor.getColumnIndex("goal_amount");
                if (goalAmountIndex != -1 && !cursor.isNull(goalAmountIndex)) {
                    transaction.setAmount(cursor.getDouble(goalAmountIndex));
                } else {
                    transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
                }

                // Use goal_note if available, otherwise use transaction note
                String note = cursor.getString(cursor.getColumnIndex("goal_note"));
                if (note == null || note.trim().isEmpty()) {
                    note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE));
                }
                transaction.setNote(note);

                transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
                transaction.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD)));

                String categoryName = cursor.getString(cursor.getColumnIndex("category_name"));
                transaction.setCategoryName(categoryName != null ? categoryName : "Tiết kiệm");

                transactions.add(transaction);
            }
            cursor.close();

            // ✅ IMPROVED: Always use fallback if goal_transactions is empty or has few results
            if (transactions.size() < 3) {
                Log.d(TAG, "Using fallback method for transaction filtering (found " + transactions.size() + " in goal_transactions)");
                loadTransactionHistoryFallback(transactions, db);
            }

            // ✅ IMPROVED: Final strict filtering to ensure only relevant transactions
            List<Transaction> filteredTransactions = new ArrayList<>();
            for (Transaction t : transactions) {
                if (isTransactionRelatedToGoal(t, currentGoal.getName())) {
                    filteredTransactions.add(t);
                }
            }

            Log.d(TAG, "Final filtered transactions: " + filteredTransactions.size() + " out of " + transactions.size());

            // Update adapter with strictly filtered transactions
            historyAdapter.updateTransactions(filteredTransactions);

            // Show/hide empty state
            if (filteredTransactions.isEmpty()) {
                layoutEmptyHistory.setVisibility(View.VISIBLE);
                recyclerViewHistory.setVisibility(View.GONE);
            } else {
                layoutEmptyHistory.setVisibility(View.GONE);
                recyclerViewHistory.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading transaction history: " + e.getMessage());
            // Try fallback method
            try {
                loadTransactionHistoryFallback(transactions, db);

                // Apply strict filtering even on fallback results
                List<Transaction> filteredTransactions = new ArrayList<>();
                for (Transaction t : transactions) {
                    if (isTransactionRelatedToGoal(t, currentGoal.getName())) {
                        filteredTransactions.add(t);
                    }
                }

                historyAdapter.updateTransactions(filteredTransactions);

                if (filteredTransactions.isEmpty()) {
                    layoutEmptyHistory.setVisibility(View.VISIBLE);
                    recyclerViewHistory.setVisibility(View.GONE);
                } else {
                    layoutEmptyHistory.setVisibility(View.GONE);
                    recyclerViewHistory.setVisibility(View.VISIBLE);
                }
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback method also failed: " + fallbackError.getMessage());
            }
        }
    }

    // ✅ IMPROVED: Strict fallback method - only "tiết kiệm" AND goal name
    private void loadTransactionHistoryFallback(List<Transaction> transactions, SQLiteDatabase db) {
        if (currentGoal == null) return;

        Log.d(TAG, "Using strict fallback method for goal: " + currentGoal.getName());

        // Avoid duplicates by tracking existing IDs
        List<Long> existingIds = new ArrayList<>();
        for (Transaction t : transactions) {
            existingIds.add(t.getId());
        }

        // ✅ STRATEGY 1: Exact match - note contains both "tiết kiệm" AND goal name
        String goalNamePattern = "%" + currentGoal.getName().toLowerCase() + "%";
        String savingsPattern = "%tiết kiệm%";

        String query1 = "SELECT t.*, c." + DatabaseHelper.COLUMN_NAME + " as category_name " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t " +
                "LEFT JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                "WHERE t." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "AND LOWER(t." + DatabaseHelper.COLUMN_NOTE + ") LIKE ? " +
                "AND LOWER(t." + DatabaseHelper.COLUMN_NOTE + ") LIKE ? " +
                "ORDER BY t." + DatabaseHelper.COLUMN_DATE + " DESC";

        Cursor cursor1 = db.rawQuery(query1, new String[]{
                String.valueOf(currentUserId),
                savingsPattern,
                goalNamePattern
        });

        int strategy1Count = 0;
        while (cursor1.moveToNext()) {
            Transaction transaction = createTransactionFromCursor(cursor1);
            if (!existingIds.contains(transaction.getId())) {
                transactions.add(transaction);
                existingIds.add(transaction.getId());
                strategy1Count++;
                Log.d(TAG, "Strategy 1 match: " + transaction.getNote());
            }
        }
        cursor1.close();
        Log.d(TAG, "Strategy 1 found " + strategy1Count + " transactions");

        // ✅ STRATEGY 2: Payment method "Tiết kiệm" AND note contains goal name
        String query2 = "SELECT t.*, c." + DatabaseHelper.COLUMN_NAME + " as category_name " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t " +
                "LEFT JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                "WHERE t." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "AND LOWER(t." + DatabaseHelper.COLUMN_PAYMENT_METHOD + ") LIKE ? " +
                "AND LOWER(t." + DatabaseHelper.COLUMN_NOTE + ") LIKE ? " +
                "ORDER BY t." + DatabaseHelper.COLUMN_DATE + " DESC";

        Cursor cursor2 = db.rawQuery(query2, new String[]{
                String.valueOf(currentUserId),
                savingsPattern,
                goalNamePattern
        });

        int strategy2Count = 0;
        while (cursor2.moveToNext()) {
            Transaction transaction = createTransactionFromCursor(cursor2);
            if (!existingIds.contains(transaction.getId())) {
                transactions.add(transaction);
                existingIds.add(transaction.getId());
                strategy2Count++;
                Log.d(TAG, "Strategy 2 match: " + transaction.getNote());
            }
        }
        cursor2.close();
        Log.d(TAG, "Strategy 2 found " + strategy2Count + " transactions");

        // ✅ STRATEGY 3: Alternative "tiết kiệm" spellings with goal name
        String[] altSavingsPatterns = {"%tiet kiem%", "%tiết%kiệm%", "%tiet%kiem%"};

        for (String altPattern : altSavingsPatterns) {
            String query3 = "SELECT t.*, c." + DatabaseHelper.COLUMN_NAME + " as category_name " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t " +
                    "LEFT JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                    "WHERE t." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                    "AND (LOWER(t." + DatabaseHelper.COLUMN_NOTE + ") LIKE ? " +
                    "OR LOWER(t." + DatabaseHelper.COLUMN_PAYMENT_METHOD + ") LIKE ?) " +
                    "AND LOWER(t." + DatabaseHelper.COLUMN_NOTE + ") LIKE ? " +
                    "ORDER BY t." + DatabaseHelper.COLUMN_DATE + " DESC " +
                    "LIMIT 10";

            Cursor cursor3 = db.rawQuery(query3, new String[]{
                    String.valueOf(currentUserId),
                    altPattern,
                    altPattern,
                    goalNamePattern
            });

            while (cursor3.moveToNext()) {
                Transaction transaction = createTransactionFromCursor(cursor3);
                if (!existingIds.contains(transaction.getId())) {
                    transactions.add(transaction);
                    existingIds.add(transaction.getId());
                    Log.d(TAG, "Strategy 3 match (" + altPattern + "): " + transaction.getNote());
                }
            }
            cursor3.close();
        }

        Log.d(TAG, "Total fallback transactions found: " + (transactions.size() - existingIds.size() + transactions.size()) + " for goal: " + currentGoal.getName());
    }

    private Transaction createTransactionFromCursor(Cursor cursor) {
        Transaction transaction = new Transaction();
        transaction.setId((long) cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
        transaction.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE)));
        transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
        transaction.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD)));

        String categoryName = cursor.getString(cursor.getColumnIndex("category_name"));
        transaction.setCategoryName(categoryName != null ? categoryName : "");

        return transaction;
    }

    private void setupListeners() {
        // Add money button
        fabAddMoney.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMoneyToGoalActivity.class);
            intent.putExtra("user_id", currentUserId);
            intent.putExtra("goal_id", goalId); // Pass goal_id to AddMoneyToGoalActivity
            startActivityForResult(intent, 100);
        });

        // Edit goal button
        btnEditGoal.setOnClickListener(v -> editGoal());

        // Delete goal button
        btnDeleteGoal.setOnClickListener(v -> confirmDeleteGoal());

        // Toggle pause button
        btnTogglePause.setOnClickListener(v -> togglePauseGoal());
    }

    private void editGoal() {
        // For now, just show a simple dialog to edit the goal name
        // In a full implementation, you might want a separate EditGoalActivity

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔧 Chức năng đang phát triển");
        builder.setMessage("Chức năng chỉnh sửa mục tiêu sẽ được cập nhật trong phiên bản tiếp theo.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void confirmDeleteGoal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Xác nhận xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa mục tiêu \"" + currentGoal.getName() + "\"?\n\nHành động này không thể hoàn tác.");
        builder.setPositiveButton("Xóa", (dialog, which) -> deleteGoal());
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void deleteGoal() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Delete related goal_transactions first
            db.delete(
                    DatabaseHelper.TABLE_GOAL_TRANSACTIONS,
                    DatabaseHelper.COLUMN_GOAL_ID + " = ?",
                    new String[]{String.valueOf(goalId)}
            );

            // Then delete the goal
            int deletedRows = db.delete(
                    DatabaseHelper.TABLE_GOALS,
                    DatabaseHelper.COLUMN_ID + " = ? AND " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(goalId), String.valueOf(currentUserId)}
            );

            if (deletedRows > 0) {
                Toast.makeText(this, "✅ Đã xóa mục tiêu thành công", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "❌ Không thể xóa mục tiêu", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting goal: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi khi xóa mục tiêu", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePauseGoal() {
        if (currentGoal == null) return;

        String newStatus = "paused".equals(currentGoal.getStatus()) ? "active" : "paused";

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_GOAL_STATUS, newStatus);

            int updatedRows = db.update(
                    DatabaseHelper.TABLE_GOALS,
                    values,
                    DatabaseHelper.COLUMN_ID + " = ? AND " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(goalId), String.valueOf(currentUserId)}
            );

            if (updatedRows > 0) {
                currentGoal.setStatus(newStatus);
                String message = "active".equals(newStatus) ?
                        "▶️ Đã tiếp tục mục tiêu" : "⏸️ Đã tạm dừng mục tiêu";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                updateUI();
            } else {
                Toast.makeText(this, "❌ Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error toggling goal status: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi khi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // Refresh data after adding money
            loadGoalDetails();
            loadTransactionHistory();
            setResult(RESULT_OK); // Pass result back to GoalsActivity
        }
    }

    // Inner class for transaction history adapter
    private class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.HistoryViewHolder> {
        private List<Transaction> transactions = new ArrayList<>();

        public void updateTransactions(List<Transaction> newTransactions) {
            this.transactions.clear();
            this.transactions.addAll(newTransactions);
            notifyDataSetChanged();
        }

        @Override
        public HistoryViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_transaction_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            Transaction transaction = transactions.get(position);
            holder.bind(transaction);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            private TextView tvAmount, tvNote, tvDate, tvPaymentMethod;
            private View transactionIndicator;

            public HistoryViewHolder(View itemView) {
                super(itemView);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvNote = itemView.findViewById(R.id.tvNote);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
                transactionIndicator = itemView.findViewById(R.id.transactionIndicator);
            }

            public void bind(Transaction transaction) {
                DecimalFormat formatter = new DecimalFormat("#,###");

                // Amount
                tvAmount.setText("+" + formatter.format(transaction.getAmount()) + " đ");
                tvAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                // Note
                String note = transaction.getNote();
                if (note == null || note.trim().isEmpty()) {
                    note = "Tiết kiệm cho: " + currentGoal.getName();
                }
                tvNote.setText(note);

                // ✅ IMPROVED: Smart date formatting using improved date parsing
                Date date = parseDate(transaction.getDate());
                if (date != null) {
                    tvDate.setText(displayDateFormat.format(date));
                } else {
                    tvDate.setText(transaction.getDate());
                }

                // Payment method
                String paymentMethod = transaction.getPaymentMethod();
                if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                    paymentMethod = "Tiết kiệm";
                }
                tvPaymentMethod.setText(paymentMethod);

                // Indicator color
                transactionIndicator.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));

                // ✅ IMPROVED: Add debug info for matched transactions
                Log.d(TAG, "Displaying transaction: " + note + " | Amount: " + transaction.getAmount() + " | Date: " + transaction.getDate());
            }
        }
    }
}