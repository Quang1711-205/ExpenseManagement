// 💰 AddMoneyToGoalActivity.java - Fixed Version

package com.example.expensemanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import database.DatabaseHelper;
import models.Goal;

public class AddMoneyToGoalActivity extends AppCompatActivity {
    private static final String TAG = "AddMoneyToGoalActivity";

    private DatabaseHelper dbHelper;
    private int currentUserId;
    private List<Goal> activeGoals = new ArrayList<>();
    private Goal selectedGoal = null;

    // Views
    private LinearLayout layoutGoalSelection;
    private TextView tvSelectedGoal, tvGoalProgress, tvCurrentProgress;
    private ProgressBar progressBarGoal;
    private EditText etAmount, etNote;
    private TextView tvNewProgress, tvRemainingAmount;
    private Button btnSave;
    private LinearLayout layoutQuickAmounts;
    private TextView tvCurrentBalance, tvBalanceAfterTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_money_to_goal);

        // Get userId from intent
        currentUserId = getIntent().getIntExtra("user_id", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "❌ Lỗi: Không xác định được user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        updateBalanceDisplay();
        loadActiveGoals();
        setupListeners();
    }

    private void initViews() {
        layoutGoalSelection = findViewById(R.id.layoutGoalSelection);
        tvSelectedGoal = findViewById(R.id.tvSelectedGoal);
        tvGoalProgress = findViewById(R.id.tvGoalProgress);
        tvCurrentProgress = findViewById(R.id.tvCurrentProgress);
        progressBarGoal = findViewById(R.id.progressBarGoal);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        tvNewProgress = findViewById(R.id.tvNewProgress);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);
        btnSave = findViewById(R.id.btnSave);
        layoutQuickAmounts = findViewById(R.id.layoutQuickAmounts);

        // Thêm các TextView mới cho số dư (nếu có trong layout)
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvBalanceAfterTransaction = findViewById(R.id.tvBalanceAfterTransaction);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

//        updateBalanceDisplay();
    }

    private void updateBalanceDisplay() {
        double currentBalance = getCurrentBalance();
        DecimalFormat formatter = new DecimalFormat("#,###");

        if (tvCurrentBalance != null) {
            tvCurrentBalance.setText("Số dư hiện tại: " + formatter.format(currentBalance) + " đ");

            // Đổi màu cảnh báo nếu số dư thấp
            if (currentBalance < 100000) { // Dưới 100K
                tvCurrentBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else if (currentBalance < 500000) { // Dưới 500K
                tvCurrentBalance.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvCurrentBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private void initDatabase() {
        dbHelper = new DatabaseHelper(this);
    }

    private void loadActiveGoals() {
        activeGoals.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_GOALS,
                    null,
                    DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_GOAL_STATUS + " = 'active'",
                    new String[]{String.valueOf(currentUserId)},
                    null,
                    null,
                    DatabaseHelper.COLUMN_CREATED_AT + " DESC"
            );

            while (cursor.moveToNext()) {
                Goal goal = new Goal();
                goal.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                goal.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_NAME)));
                goal.setTargetAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_TARGET_AMOUNT)));
                goal.setCurrentAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_CURRENT_AMOUNT)));
                goal.setDeadline(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_DEADLINE)));
                goal.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_ICON)));
                goal.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_STATUS)));
                goal.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));

                activeGoals.add(goal);
            }
            cursor.close();

            displayGoalSelection();

        } catch (Exception e) {
            Log.e(TAG, "Error loading active goals: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi tải danh sách mục tiêu", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayGoalSelection() {
        layoutGoalSelection.removeAllViews();

        if (activeGoals.isEmpty()) {
            // No active goals
            CardView emptyCard = new CardView(this);
            emptyCard.setRadius(24);
            emptyCard.setCardElevation(12);
            emptyCard.setUseCompatPadding(true);

            LinearLayout emptyLayout = new LinearLayout(this);
            emptyLayout.setOrientation(LinearLayout.VERTICAL);
            emptyLayout.setPadding(48, 48, 48, 48);
            emptyLayout.setGravity(android.view.Gravity.CENTER);

            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("🎯\n\nChưa có mục tiêu nào đang hoạt động\n\nHãy tạo mục tiêu mới trước!");
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setTextColor(getResources().getColor(android.R.color.darker_gray));

            emptyLayout.addView(tvEmpty);
            emptyCard.addView(emptyLayout);
            layoutGoalSelection.addView(emptyCard);

            // Disable save button
            btnSave.setEnabled(false);
            btnSave.setText("Không có mục tiêu");
            return;
        }

        // Show goal selection cards
        for (Goal goal : activeGoals) {
            CardView goalCard = createGoalSelectionCard(goal);
            layoutGoalSelection.addView(goalCard);
        }

        // Auto-select first goal if only one
        if (activeGoals.size() == 1) {
            selectGoal(activeGoals.get(0));
        }
    }

    private CardView createGoalSelectionCard(Goal goal) {
        CardView cardView = new CardView(this);
        cardView.setRadius(16);
        cardView.setCardElevation(4);
        cardView.setUseCompatPadding(true);

        // FIX 1: Use proper selectableItemBackground
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            cardView.setForeground(ContextCompat.getDrawable(this, outValue.resourceId));
        }

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(16, 16, 16, 16);
        cardLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Icon
        TextView tvIcon = new TextView(this);
        tvIcon.setText(goal.getIcon() != null ? goal.getIcon() : "🎯");
        tvIcon.setTextSize(24);
        tvIcon.setPadding(16, 16, 16, 16);

        // FIX 2: Use a simple colored background instead of drawable
        tvIcon.setBackgroundResource(R.color.primary_light);

        // Goal info
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoParams.weight = 1;
        infoParams.setMargins(16, 0, 0, 0);
        infoLayout.setLayoutParams(infoParams);

        TextView tvName = new TextView(this);
        tvName.setText(goal.getName());
        tvName.setTextSize(16);
        tvName.setTextColor(getResources().getColor(android.R.color.black));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        // Progress info
        DecimalFormat formatter = new DecimalFormat("#,###");
        int progress = (int) ((goal.getCurrentAmount() * 100) / goal.getTargetAmount());
        String progressText = String.format("%s / %s đ (%d%%)",
                formatter.format(goal.getCurrentAmount()),
                formatter.format(goal.getTargetAmount()),
                progress);

        TextView tvProgress = new TextView(this);
        tvProgress.setText(progressText);
        tvProgress.setTextSize(14);
        tvProgress.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Progress bar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setProgress(Math.min(progress, 100));
        progressBar.setMax(100);
        progressBar.setProgressTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (8 * getResources().getDisplayMetrics().density)
        );
        progressParams.setMargins(0, 8, 0, 0);
        progressBar.setLayoutParams(progressParams);

        infoLayout.addView(tvName);
        infoLayout.addView(tvProgress);
        infoLayout.addView(progressBar);

        cardLayout.addView(tvIcon);
        cardLayout.addView(infoLayout);
        cardView.addView(cardLayout);

        // Click listener
        cardView.setOnClickListener(v -> selectGoal(goal));

        return cardView;
    }

    private void selectGoal(Goal goal) {
        selectedGoal = goal;
        updateSelectedGoalInfo();
        updateQuickAmounts();
        calculateNewProgress();

        // Enable save button
        btnSave.setEnabled(true);
        btnSave.setText("💾 Thêm tiền vào mục tiêu");

        // Update card selection visual feedback
        for (int i = 0; i < layoutGoalSelection.getChildCount(); i++) {
            CardView card = (CardView) layoutGoalSelection.getChildAt(i);
            if (i == activeGoals.indexOf(goal)) {
                card.setCardBackgroundColor(getResources().getColor(R.color.primary_light));
            } else {
                card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
            }
        }
    }

    // Fixed updateSelectedGoalInfo() method for AddMoneyToGoalActivity.java

    private void updateSelectedGoalInfo() {
        if (selectedGoal == null) return;

        // Update goal name với icon
        String goalIcon = selectedGoal.getIcon() != null ? selectedGoal.getIcon() : "🎯";
        tvSelectedGoal.setText(goalIcon + " " + selectedGoal.getName());

        // Update icon riêng nếu có TextView riêng cho icon
        TextView tvSelectedGoalIcon = findViewById(R.id.tvSelectedGoalIcon);
        if (tvSelectedGoalIcon != null) {
            tvSelectedGoalIcon.setText(goalIcon);
        }

        // Update current amount
        TextView tvCurrentAmount = findViewById(R.id.tvCurrentAmount);
        if (tvCurrentAmount != null) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            tvCurrentAmount.setText(formatter.format(selectedGoal.getCurrentAmount()) + " đ");
        }

        // Update target amount
        TextView tvTargetAmount = findViewById(R.id.tvTargetAmount);
        if (tvTargetAmount != null) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            tvTargetAmount.setText(formatter.format(selectedGoal.getTargetAmount()) + " đ");
        }

        // Update progress text
        DecimalFormat formatter = new DecimalFormat("#,###");
        String progressText = String.format("%s / %s đ",
                formatter.format(selectedGoal.getCurrentAmount()),
                formatter.format(selectedGoal.getTargetAmount()));
        tvGoalProgress.setText(progressText);

        // Update progress percentage and bar
        int progress = (int) ((selectedGoal.getCurrentAmount() * 100) / selectedGoal.getTargetAmount());
        tvCurrentProgress.setText(progress + "%");
        progressBarGoal.setProgress(Math.min(progress, 100));

        // Update remaining days
        TextView tvRemainingDays = findViewById(R.id.tvRemainingDays);
        if (tvRemainingDays != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date deadlineDate = sdf.parse(selectedGoal.getDeadline());
                Date currentDate = new Date();

                if (deadlineDate != null) {
                    long diffInMillies = deadlineDate.getTime() - currentDate.getTime();
                    long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);

                    if (diffInDays > 0) {
                        tvRemainingDays.setText("⏰ Còn " + diffInDays + " ngày để hoàn thành");
                    } else if (diffInDays == 0) {
                        tvRemainingDays.setText("⏰ Hạn chót là hôm nay!");
                    } else {
                        tvRemainingDays.setText("⏰ Đã quá hạn " + Math.abs(diffInDays) + " ngày");
                        tvRemainingDays.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating remaining days: " + e.getMessage());
                tvRemainingDays.setText("⏰ Không thể tính ngày còn lại");
            }
        }
    }

    private void updateQuickAmounts() {
        layoutQuickAmounts.removeAllViews();

        if (selectedGoal == null) return;

        double currentBalance = getCurrentBalance();
        double remaining = selectedGoal.getTargetAmount() - selectedGoal.getCurrentAmount();

        // Quick amount options (chỉ hiển thị những gì có thể chi trả được)
        double[] amounts = {100000, 500000, 1000000, remaining};
        String[] labels = {"100K", "500K", "1M", "Hoàn thành"};

        for (int i = 0; i < amounts.length; i++) {
            // Bỏ qua nếu số tiền <= 0 hoặc vượt quá số dư
            if (amounts[i] <= 0 || amounts[i] > currentBalance) continue;

            Button btn = new Button(this);
            btn.setText(labels[i]);
            btn.setTextSize(12);

            btn.setBackgroundResource(android.R.drawable.btn_default);
            btn.setTextColor(getResources().getColor(R.color.primary));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    (int) (36 * getResources().getDisplayMetrics().density));
            params.weight = 1;
            params.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(params);

            final double amount = amounts[i];
            btn.setOnClickListener(v -> {
                etAmount.setText(String.valueOf((long) amount));
            });

            layoutQuickAmounts.addView(btn);
        }

        // Thêm thông báo nếu không có quick amount nào khả dụng
        if (layoutQuickAmounts.getChildCount() == 0) {
            TextView tvNoQuickAmount = new TextView(this);
            tvNoQuickAmount.setText("Số dư không đủ cho các lựa chọn nhanh");
            tvNoQuickAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvNoQuickAmount.setGravity(android.view.Gravity.CENTER);
            tvNoQuickAmount.setPadding(16, 8, 16, 8);
            layoutQuickAmounts.addView(tvNoQuickAmount);
        }
    }

    private void setupListeners() {
        // Amount change listener
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateNewProgress();
            }
        });

        // Save button
        btnSave.setOnClickListener(v -> saveMoneyToGoal());
    }

    private boolean validateAmountInput(double amount) {
        double currentBalance = getCurrentBalance();

        // Kiểm tra số tiền tối thiểu
        if (amount < 1000) {
            etAmount.setError("Số tiền phải từ 1,000 đ trở lên");
            Toast.makeText(this, "Số tiền phải từ 1,000 đ trở lên", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Kiểm tra số dư
        if (amount > currentBalance) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            etAmount.setError("Số dư không đủ! Hiện có: " + formatter.format(currentBalance) + " đ");
            Toast.makeText(this, "Số dư không đủ để thực hiện giao dịch này!\nSố dư hiện tại: " +
                    formatter.format(currentBalance) + " đ", Toast.LENGTH_LONG).show();
            return false;
        }

        // Clear error nếu hợp lệ
        etAmount.setError(null);
        return true;
    }

    private void calculateNewProgress() {
        if (selectedGoal == null) {
            tvNewProgress.setVisibility(View.GONE);
            tvRemainingAmount.setVisibility(View.GONE);
            if (tvBalanceAfterTransaction != null) {
                tvBalanceAfterTransaction.setVisibility(View.GONE);
            }
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tvNewProgress.setVisibility(View.GONE);
            tvRemainingAmount.setVisibility(View.GONE);
            if (tvBalanceAfterTransaction != null) {
                tvBalanceAfterTransaction.setVisibility(View.GONE);
            }
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                tvNewProgress.setVisibility(View.GONE);
                tvRemainingAmount.setVisibility(View.GONE);
                if (tvBalanceAfterTransaction != null) {
                    tvBalanceAfterTransaction.setVisibility(View.GONE);
                }
                return;
            }

            DecimalFormat formatter = new DecimalFormat("#,###");
            double currentBalance = getCurrentBalance();

            // Kiểm tra số dư
            if (tvBalanceAfterTransaction != null) {
                double balanceAfter = currentBalance - amount;
                String balanceText = "Số dư sau giao dịch: " + formatter.format(balanceAfter) + " đ";
                tvBalanceAfterTransaction.setText(balanceText);
                tvBalanceAfterTransaction.setVisibility(View.VISIBLE);

                // Đổi màu cảnh báo
                if (balanceAfter < 0) {
                    tvBalanceAfterTransaction.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (balanceAfter < 100000) {
                    tvBalanceAfterTransaction.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else {
                    tvBalanceAfterTransaction.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            }

            double newCurrentAmount = selectedGoal.getCurrentAmount() + amount;
            int newProgress = (int) ((newCurrentAmount * 100) / selectedGoal.getTargetAmount());

            // New progress
            String newProgressText = String.format("📈 Tiến độ mới: %d%% (%s đ)",
                    Math.min(newProgress, 100),
                    formatter.format(Math.min(newCurrentAmount, selectedGoal.getTargetAmount())));

            tvNewProgress.setText(newProgressText);
            tvNewProgress.setVisibility(View.VISIBLE);
            tvNewProgress.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

            // Remaining amount
            if (newCurrentAmount >= selectedGoal.getTargetAmount()) {
                tvRemainingAmount.setText("🎉 Chúc mừng! Bạn đã đạt được mục tiêu!");
                tvRemainingAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                double remaining = selectedGoal.getTargetAmount() - newCurrentAmount;
                String remainingText = String.format("💪 Còn thiếu: %s đ", formatter.format(remaining));
                tvRemainingAmount.setText(remainingText);
                tvRemainingAmount.setTextColor(getResources().getColor(R.color.primary));
            }
            tvRemainingAmount.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            tvNewProgress.setVisibility(View.GONE);
            tvRemainingAmount.setVisibility(View.GONE);
            if (tvBalanceAfterTransaction != null) {
                tvBalanceAfterTransaction.setVisibility(View.GONE);
            }
        }
    }

    private void saveMoneyToGoal() {
        if (selectedGoal == null) {
            Toast.makeText(this, "❌ Vui lòng chọn mục tiêu", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("Vui lòng nhập số tiền");
            etAmount.requestFocus();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
                etAmount.setError("Số tiền phải lớn hơn 0");
                etAmount.requestFocus();
                return;
            }

            // ✅ Kiểm tra validation mới (số tiền tối thiểu và số dư)
            if (!validateAmountInput(amount)) {
                etAmount.requestFocus();
                return;
            }

            // Kiểm tra thêm: không cho phép số tiền quá lớn
            if (amount > 999999999) {
                etAmount.setError("Số tiền quá lớn");
                Toast.makeText(this, "Số tiền vượt quá giới hạn cho phép", Toast.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            try {
                // 1. Create transaction record
                ContentValues transactionValues = new ContentValues();
                transactionValues.put(DatabaseHelper.COLUMN_TYPE, "expense");
                transactionValues.put(DatabaseHelper.COLUMN_AMOUNT, amount);
                transactionValues.put(DatabaseHelper.COLUMN_CATEGORY_ID, getCategoryIdForGoalSaving(db));
                transactionValues.put(DatabaseHelper.COLUMN_USER_ID, currentUserId);
                transactionValues.put(DatabaseHelper.COLUMN_NOTE,
                        note.isEmpty() ? "Tiết kiệm cho: " + selectedGoal.getName() : note);

                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = dateFormatter.format(new Date());
                transactionValues.put(DatabaseHelper.COLUMN_DATE, currentDate);
                transactionValues.put(DatabaseHelper.COLUMN_PAYMENT_METHOD, "Tiết kiệm");

                long transactionId = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, transactionValues);

                if (transactionId == -1) {
                    throw new Exception("Failed to create transaction");
                }

                // 2. Update goal current amount
                double newCurrentAmount = selectedGoal.getCurrentAmount() + amount;
                String newStatus = newCurrentAmount >= selectedGoal.getTargetAmount() ? "completed" : "active";

                ContentValues goalValues = new ContentValues();
                goalValues.put(DatabaseHelper.COLUMN_GOAL_CURRENT_AMOUNT,
                        Math.min(newCurrentAmount, selectedGoal.getTargetAmount()));
                goalValues.put(DatabaseHelper.COLUMN_GOAL_STATUS, newStatus);

                int updatedRows = db.update(
                        DatabaseHelper.TABLE_GOALS,
                        goalValues,
                        DatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(selectedGoal.getId())}
                );

                if (updatedRows == 0) {
                    throw new Exception("Failed to update goal");
                }

                db.setTransactionSuccessful();

                Log.d(TAG, "✅ Successfully added money to goal");

                // Hiển thị thông báo thành công với số dư mới
                double newBalance = getCurrentBalance() - amount;
                DecimalFormat formatter = new DecimalFormat("#,###");

                String message = newStatus.equals("completed") ?
                        "🎉 Chúc mừng! Bạn đã hoàn thành mục tiêu!\nSố dư còn lại: " + formatter.format(newBalance) + " đ" :
                        "✅ Đã thêm tiền vào mục tiêu thành công!\nSố dư còn lại: " + formatter.format(newBalance) + " đ";

                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();

            } catch (Exception e) {
                Log.e(TAG, "Error saving money to goal: " + e.getMessage());
                throw e;
            } finally {
                db.endTransaction();
            }

        } catch (NumberFormatException e) {
            etAmount.setError("Vui lòng nhập số hợp lệ");
            etAmount.requestFocus();
        } catch (Exception e) {
            Log.e(TAG, "Error saving money to goal: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi khi lưu dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }

    // Thêm method để tính số dư hiện tại
    private double getCurrentBalance() {
        double balance = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String query = "SELECT " + DatabaseHelper.COLUMN_TYPE + ", " + DatabaseHelper.COLUMN_AMOUNT +
                    " FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(currentUserId)});

            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                double amount = cursor.getDouble(1);

                if ("income".equals(type)) {
                    balance += amount;
                } else if ("expense".equals(type)) {
                    balance -= amount;
                }
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating balance: " + e.getMessage());
        }

        return balance;
    }

    private int getCategoryIdForGoalSaving(SQLiteDatabase db) {
        // Try to find "Tiết kiệm" category, create if not exists
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CATEGORIES,
                new String[]{DatabaseHelper.COLUMN_ID},
                DatabaseHelper.COLUMN_CATEGORY_NAME + " = ? AND " + DatabaseHelper.COLUMN_CATEGORY_TYPE + " = ?",
                new String[]{"Tiết kiệm", "expense"},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            int categoryId = cursor.getInt(0);
            cursor.close();
            return categoryId;
        }
        cursor.close();

        // Create "Tiết kiệm" category
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_CATEGORY_NAME, "Tiết kiệm");
        values.put(DatabaseHelper.COLUMN_CATEGORY_TYPE, "expense");
        values.put(DatabaseHelper.COLUMN_CATEGORY_ICON, "💰");
        values.put(DatabaseHelper.COLUMN_CATEGORY_COLOR, "#4CAF50");

        long categoryId = db.insert(DatabaseHelper.TABLE_CATEGORIES, null, values);
        return (int) categoryId;
    }
}