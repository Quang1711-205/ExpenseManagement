// 🎯 AddGoalActivity.java - Fixed for Android API 24

package com.example.expensemanagement;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import database.DatabaseHelper;

public class AddGoalActivity extends AppCompatActivity {
    private static final String TAG = "AddGoalActivity";

    private DatabaseHelper dbHelper;
    private int currentUserId;

    // Views
    private EditText etGoalName, etTargetAmount, etCurrentAmount;
    private TextView tvSelectedIcon, tvDeadline, tvCalculation;
    private GridLayout gridIcons;
    private Button btnSave, btnSelectDate;

    // Data
    private String selectedIcon = "🎯";
    private Calendar selectedDeadline = Calendar.getInstance();

    // Date formatter
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Icon options
    private final String[] icons = {
            "🎯", "🏠", "🚗", "🛵", "✈️", "💍", "📱", "💻",
            "🎓", "⚽", "🎸", "📷", "👗", "👟", "💄", "🍔",
            "🏖️", "🎪", "🎭", "🎨", "📚", "💎", "🏆", "💰"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        // Get userId from intent
        currentUserId = getIntent().getIntExtra("user_id", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "❌ Lỗi: Không xác định được user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set default deadline to 12 months from now
        selectedDeadline.add(Calendar.MONTH, 12);

        initViews();
        initDatabase();
        setupIconGrid();
        setupListeners();
        updateCalculation();
    }

    private void initViews() {
        etGoalName = findViewById(R.id.etGoalName);
        etTargetAmount = findViewById(R.id.etTargetAmount);
        etCurrentAmount = findViewById(R.id.etCurrentAmount);
        tvSelectedIcon = findViewById(R.id.tvSelectedIcon);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvCalculation = findViewById(R.id.tvCalculation);
        gridIcons = findViewById(R.id.gridIcons);
        btnSave = findViewById(R.id.btnSave);
        btnSelectDate = findViewById(R.id.btnSelectDate);

        // Set initial values
        tvSelectedIcon.setText(selectedIcon);
        updateDeadlineText();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initDatabase() {
        dbHelper = new DatabaseHelper(this);
    }

    private void setupIconGrid() {
        gridIcons.removeAllViews();
        gridIcons.setColumnCount(6);

        for (String icon : icons) {
            TextView tvIcon = new TextView(this);
            tvIcon.setText(icon);
            tvIcon.setTextSize(24);
            tvIcon.setPadding(16, 16, 16, 16);
            tvIcon.setBackground(getDrawable(R.drawable.circle_background));
            tvIcon.setBackgroundTintList(getResources().getColorStateList(android.R.color.white));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            tvIcon.setLayoutParams(params);

            tvIcon.setOnClickListener(v -> {
                selectedIcon = icon;
                tvSelectedIcon.setText(selectedIcon);
                updateIconSelection();
            });

            gridIcons.addView(tvIcon);
        }

        updateIconSelection();
    }

    private void updateIconSelection() {
        for (int i = 0; i < gridIcons.getChildCount(); i++) {
            TextView tvIcon = (TextView) gridIcons.getChildAt(i);
            if (tvIcon.getText().toString().equals(selectedIcon)) {
                tvIcon.setBackgroundTintList(getResources().getColorStateList(R.color.primary_light));
            } else {
                tvIcon.setBackgroundTintList(getResources().getColorStateList(android.R.color.white));
            }
        }
    }

    private void setupListeners() {
        // Date picker
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Text change listeners for calculation
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCalculation();
            }
        };

        etTargetAmount.addTextChangedListener(calculationWatcher);
        etCurrentAmount.addTextChangedListener(calculationWatcher);

        // Save button
        btnSave.setOnClickListener(v -> saveGoal());

        // Quick amount buttons
        findViewById(R.id.btn1M).setOnClickListener(v -> etTargetAmount.setText("1000000"));
        findViewById(R.id.btn5M).setOnClickListener(v -> etTargetAmount.setText("5000000"));
        findViewById(R.id.btn10M).setOnClickListener(v -> etTargetAmount.setText("10000000"));
        findViewById(R.id.btn50M).setOnClickListener(v -> etTargetAmount.setText("50000000"));
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDeadline.set(year, month, dayOfMonth);
                    updateDeadlineText();
                    updateCalculation();
                },
                selectedDeadline.get(Calendar.YEAR),
                selectedDeadline.get(Calendar.MONTH),
                selectedDeadline.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to tomorrow
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        datePickerDialog.getDatePicker().setMinDate(tomorrow.getTimeInMillis());

        datePickerDialog.show();
    }

    private void updateDeadlineText() {
        String formattedDate = dateFormatter.format(selectedDeadline.getTime());
        tvDeadline.setText(formattedDate);
        btnSelectDate.setText("📅 " + formattedDate);
    }

    private void updateCalculation() {
        try {
            String targetAmountStr = etTargetAmount.getText().toString().trim();
            String currentAmountStr = etCurrentAmount.getText().toString().trim();

            if (targetAmountStr.isEmpty()) {
                tvCalculation.setText("💡 Nhập số tiền mục tiêu để xem tính toán");
                tvCalculation.setTextColor(getResources().getColor(android.R.color.darker_gray));
                return;
            }

            double targetAmount = Double.parseDouble(targetAmountStr);
            double currentAmount = currentAmountStr.isEmpty() ? 0 : Double.parseDouble(currentAmountStr);

            if (targetAmount <= currentAmount) {
                tvCalculation.setText("🎉 Bạn đã đạt được mục tiêu!");
                tvCalculation.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                return;
            }

            // Calculate days left using Calendar
            Calendar today = Calendar.getInstance();
            long diffInMillis = selectedDeadline.getTimeInMillis() - today.getTimeInMillis();
            long daysLeft = diffInMillis / (24 * 60 * 60 * 1000);

            if (daysLeft <= 0) {
                tvCalculation.setText("⚠️ Hạn đã qua, vui lòng chọn ngày khác");
                tvCalculation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                return;
            }

            double remainingAmount = targetAmount - currentAmount;
            double dailyAmount = remainingAmount / daysLeft;
            double monthlyAmount = dailyAmount * 30;

            DecimalFormat formatter = new DecimalFormat("#,###");

            String calculation = String.format(
                    "📊 Để đạt mục tiêu trong %d ngày:\n" +
                            "💰 Còn thiếu: %s đ\n" +
                            "📅 Cần tiết kiệm: %s đ/ngày\n" +
                            "📆 Hoặc: %s đ/tháng",
                    daysLeft,
                    formatter.format(remainingAmount),
                    formatter.format(dailyAmount),
                    formatter.format(monthlyAmount)
            );

            tvCalculation.setText(calculation);
            tvCalculation.setTextColor(getResources().getColor(R.color.primary));

        } catch (NumberFormatException e) {
            tvCalculation.setText("💡 Nhập số hợp lệ để xem tính toán");
            tvCalculation.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void saveGoal() {
        // Validate input
        String goalName = etGoalName.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        String currentAmountStr = etCurrentAmount.getText().toString().trim();

        if (goalName.isEmpty()) {
            etGoalName.setError("Vui lòng nhập tên mục tiêu");
            etGoalName.requestFocus();
            return;
        }

        if (targetAmountStr.isEmpty()) {
            etTargetAmount.setError("Vui lòng nhập số tiền mục tiêu");
            etTargetAmount.requestFocus();
            return;
        }

        try {
            double targetAmount = Double.parseDouble(targetAmountStr);
            double currentAmount = currentAmountStr.isEmpty() ? 0 : Double.parseDouble(currentAmountStr);

            if (targetAmount <= 0) {
                etTargetAmount.setError("Số tiền phải lớn hơn 0");
                etTargetAmount.requestFocus();
                return;
            }

            if (currentAmount < 0) {
                etCurrentAmount.setError("Số tiền hiện tại không thể âm");
                etCurrentAmount.requestFocus();
                return;
            }

            if (currentAmount > targetAmount) {
                etCurrentAmount.setError("Số tiền hiện tại không thể lớn hơn mục tiêu");
                etCurrentAmount.requestFocus();
                return;
            }

            // Check if deadline is in the future
            Calendar today = Calendar.getInstance();
            if (!selectedDeadline.after(today)) {
                Toast.makeText(this, "❌ Hạn hoàn thành phải là ngày trong tương lai", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to database
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_GOAL_NAME, goalName);
            values.put(DatabaseHelper.COLUMN_GOAL_TARGET_AMOUNT, targetAmount);
            values.put(DatabaseHelper.COLUMN_GOAL_CURRENT_AMOUNT, currentAmount);
            values.put(DatabaseHelper.COLUMN_GOAL_DEADLINE, dateFormatter.format(selectedDeadline.getTime()));
            values.put(DatabaseHelper.COLUMN_GOAL_ICON, selectedIcon);
            values.put(DatabaseHelper.COLUMN_GOAL_STATUS, currentAmount >= targetAmount ? "completed" : "active");
            values.put(DatabaseHelper.COLUMN_USER_ID, currentUserId);

            long result = db.insert(DatabaseHelper.TABLE_GOALS, null, values);

            if (result != -1) {
                Log.d(TAG, "✅ Goal saved successfully with ID: " + result);
                Toast.makeText(this, "🎉 Đã tạo mục tiêu thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Log.e(TAG, "❌ Error saving goal");
                Toast.makeText(this, "❌ Lỗi khi lưu mục tiêu", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error saving goal: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi không xác định", Toast.LENGTH_SHORT).show();
        }
    }
}