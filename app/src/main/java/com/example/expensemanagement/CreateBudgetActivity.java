package com.example.expensemanagement;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import database.DatabaseHelper;
import models.BudgetPlan;
import adapters.CreateBudgetAdapter;
import android.content.SharedPreferences;


import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateBudgetActivity extends AppCompatActivity {

    private static final String TAG = "CreateBudgetActivity";
    private static final String PREFS_NAME = "MoneyMasterPrefs";
    private static final String KEY_USER_ID = "userId";
    private String currentUserId;
    private EditText etBudgetName;
    private EditText etTotalBudgetPlan;
    private Spinner spinnerBudgetPeriod;
    private TextView tvStartDate, tvEndDate, tvTotalBudget, tvRemainingBudget;
    private Button btnSelectStartDate, btnSelectEndDate, btnSaveBudget, btnAddCategory;
    private ImageButton btnBack;
    private RecyclerView rvCategoryBudgets;
    private LinearLayout llEmptyCategories;

    private DatabaseHelper dbHelper;
    private CreateBudgetAdapter adapter;
    private List<BudgetPlan.CategoryBudget> categoryBudgets;
    private List<BudgetPlan.CategoryBudget> availableCategories;
    private final SimpleDateFormat dateFormatter;
    private final NumberFormat currencyFormatter;

    // ✅ THÊM MỚI: Biến để theo dõi số dư hiện tại
    private double currentBalance = 0;
    private ExecutorService executor;

    private Date startDate, endDate;

    public CreateBudgetActivity() {
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        this.executor = Executors.newSingleThreadExecutor(); // ✅ THÊM MỚI
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_budget);

        getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            return; // Activity sẽ bị đóng bởi showErrorAndFinish()
        }

        initializeComponents();
        loadAvailableCategories();
        setupEventListeners();
        setupBackPressHandler();
        updateUI();

        // ✅ THÊM MỚI: Tính số dư hiện tại
        calculateCurrentBalance();
    }

    private void getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userIdFromIntent = getIntent().getStringExtra("user_id");

        if (userIdFromIntent != null && !userIdFromIntent.isEmpty()) {
            Log.d(TAG, "✅ Got user ID from intent: " + userIdFromIntent);
            prefs.edit().putString(KEY_USER_ID, userIdFromIntent).apply();
            currentUserId = userIdFromIntent;
        } else {
            // ✅ FIX: Đọc userId dưới dạng int, sau đó convert sang String
            int userIdInt = prefs.getInt(KEY_USER_ID, -1);
            if (userIdInt != -1) {
                currentUserId = String.valueOf(userIdInt);
                Log.d(TAG, "🔍 Got user ID from prefs (int->string): " + currentUserId);
            } else {
                currentUserId = null;
                Log.d(TAG, "❌ No user ID found in prefs");
            }
        }

        // ✅ STRICT VALIDATION: Báo lỗi nếu không có user ID
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ CRITICAL: No user ID found!");
            showErrorAndFinish("Lỗi hệ thống: Không xác định được người dùng. Vui lòng đăng nhập lại.");
            return;
        }

        Log.d(TAG, "🎯 Using user ID: " + currentUserId);
    }

    private void showErrorAndFinish(String message) {
        new AlertDialog.Builder(this)
                .setTitle("❌ Lỗi")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void initializeComponents() {
        etBudgetName = findViewById(R.id.et_budget_name);
        etTotalBudgetPlan = findViewById(R.id.et_total_budget_plan);
        spinnerBudgetPeriod = findViewById(R.id.spinner_budget_period);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        tvTotalBudget = findViewById(R.id.tv_total_budget);
        tvRemainingBudget = findViewById(R.id.tv_remaining_budget);
        btnSelectStartDate = findViewById(R.id.btn_select_start_date);
        btnSelectEndDate = findViewById(R.id.btn_select_end_date);
        btnSaveBudget = findViewById(R.id.btn_save_budget);
        btnAddCategory = findViewById(R.id.btn_add_category);
        btnBack = findViewById(R.id.btn_back);
        rvCategoryBudgets = findViewById(R.id.rv_category_budgets);
        llEmptyCategories = findViewById(R.id.ll_empty_categories);

        dbHelper = new DatabaseHelper(this);
        categoryBudgets = new ArrayList<>();
        availableCategories = new ArrayList<>();

        // Setup RecyclerView
        adapter = new CreateBudgetAdapter(categoryBudgets, this::updateTotalBudget);
        rvCategoryBudgets.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryBudgets.setAdapter(adapter);

        // Setup Spinner with hardcoded array
        setupBudgetPeriodSpinner();

        // Set default dates based on monthly period
        autoSetDatesByPeriod();
    }

    // ✅ THÊM MỚI: Method tính số dư hiện tại
    private void calculateCurrentBalance() {
        executor.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                // ✅ SỬA: Sử dụng currentUserId thay vì hardcode "1"
                String incomeQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                        "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'income' " +
                        "AND " + DatabaseHelper.COLUMN_USER_ID + " = ?";

                Cursor incomeCursor = db.rawQuery(incomeQuery, new String[]{currentUserId});
                double totalIncome = 0;
                if (incomeCursor.moveToFirst()) {
                    totalIncome = incomeCursor.getDouble(0);
                }
                incomeCursor.close();

                // ✅ SỬA: Sử dụng currentUserId thay vì hardcode "1"
                String expenseQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                        "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                        "AND " + DatabaseHelper.COLUMN_USER_ID + " = ?";

                Cursor expenseCursor = db.rawQuery(expenseQuery, new String[]{currentUserId});
                double totalExpense = 0;
                if (expenseCursor.moveToFirst()) {
                    totalExpense = expenseCursor.getDouble(0);
                }
                expenseCursor.close();

                currentBalance = totalIncome - totalExpense;

                runOnUiThread(() -> {
                    Log.d(TAG, "💰 Current balance for user " + currentUserId + ": " +
                            String.format("%.0f VND", currentBalance));
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error calculating balance: " + e.getMessage());
                currentBalance = 0;
            }
        });
    }


    private void setupBudgetPeriodSpinner() {
        String[] budgetPeriods = {"Hàng tuần", "Hàng tháng", "Hàng năm"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, budgetPeriods);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudgetPeriod.setAdapter(spinnerAdapter);

        // Set default to "Hàng tháng"
        spinnerBudgetPeriod.setSelection(1);
    }

    private void loadAvailableCategories() {
        availableCategories.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_CATEGORIES +
                " WHERE " + DatabaseHelper.COLUMN_CATEGORY_TYPE + " = 'expense'" +
                " ORDER BY " + DatabaseHelper.COLUMN_NAME;

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                int nameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME);
                int iconIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY_ICON);
                int colorIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY_COLOR);

                if (idIndex >= 0 && nameIndex >= 0) {
                    BudgetPlan.CategoryBudget categoryBudget = new BudgetPlan.CategoryBudget();
                    categoryBudget.setCategoryId(cursor.getInt(idIndex));
                    categoryBudget.setCategoryName(cursor.getString(nameIndex));

                    if (iconIndex >= 0) {
                        categoryBudget.setCategoryIcon(cursor.getString(iconIndex));
                    } else {
                        categoryBudget.setCategoryIcon("📂");
                    }

                    if (colorIndex >= 0) {
                        categoryBudget.setCategoryColor(cursor.getString(colorIndex));
                    } else {
                        categoryBudget.setCategoryColor("#2196F3");
                    }

                    categoryBudget.setAllocatedAmount(0);
                    availableCategories.add(categoryBudget);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    private void setupEventListeners() {
        btnBack.setOnClickListener(v -> handleBackPress());

        btnSelectStartDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(false));

        btnSaveBudget.setOnClickListener(v -> saveBudget());

        btnAddCategory.setOnClickListener(v -> showCategorySelectionDialog());

        spinnerBudgetPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                autoSetDatesByPeriod();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etTotalBudgetPlan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateRemainingBudget();
            }
        });
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void handleBackPress() {
        boolean hasData = false;
        String budgetName = etBudgetName.getText().toString().trim();
        String budgetPlan = etTotalBudgetPlan.getText().toString().trim();

        if (!budgetName.isEmpty() || !budgetPlan.isEmpty()) {
            hasData = true;
        }

        for (BudgetPlan.CategoryBudget category : categoryBudgets) {
            if (category.getAllocatedAmount() > 0) {
                hasData = true;
                break;
            }
        }

        if (hasData) {
            new AlertDialog.Builder(this)
                    .setTitle("Thoát tạo ngân sách?")
                    .setMessage("Bạn có chắc chắn muốn thoát? Dữ liệu đã nhập sẽ bị mất.")
                    .setPositiveButton("Thoát", (dialog, which) -> finish())
                    .setNegativeButton("Ở lại", null)
                    .show();
        } else {
            finish();
        }
    }

    private void showCategorySelectionDialog() {
        List<BudgetPlan.CategoryBudget> notAddedCategories = new ArrayList<>();
        for (BudgetPlan.CategoryBudget available : availableCategories) {
            boolean isAlreadyAdded = false;
            for (BudgetPlan.CategoryBudget added : categoryBudgets) {
                if (added.getCategoryId() == available.getCategoryId()) {
                    isAlreadyAdded = true;
                    break;
                }
            }
            if (!isAlreadyAdded) {
                notAddedCategories.add(available);
            }
        }

        if (notAddedCategories.isEmpty()) {
            Toast.makeText(this, "Tất cả danh mục đã được thêm", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] categoryNames = new String[notAddedCategories.size()];
        for (int i = 0; i < notAddedCategories.size(); i++) {
            categoryNames[i] = notAddedCategories.get(i).getCategoryIcon() + " " +
                    notAddedCategories.get(i).getCategoryName();
        }

        boolean[] selectedCategories = new boolean[notAddedCategories.size()];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn danh mục")
                .setMultiChoiceItems(categoryNames, selectedCategories,
                        (dialog, which, isChecked) -> selectedCategories[which] = isChecked)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    for (int i = 0; i < selectedCategories.length; i++) {
                        if (selectedCategories[i]) {
                            BudgetPlan.CategoryBudget category = new BudgetPlan.CategoryBudget();
                            BudgetPlan.CategoryBudget source = notAddedCategories.get(i);

                            category.setCategoryId(source.getCategoryId());
                            category.setCategoryName(source.getCategoryName());
                            category.setCategoryIcon(source.getCategoryIcon());
                            category.setCategoryColor(source.getCategoryColor());
                            category.setAllocatedAmount(0);

                            categoryBudgets.add(category);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateUI();
                    updateRemainingBudget();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    if (isStartDate) {
                        startDate = selectedDate.getTime();
                        tvStartDate.setText(dateFormatter.format(startDate));
                    } else {
                        endDate = selectedDate.getTime();
                        tvEndDate.setText(dateFormatter.format(endDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void autoSetDatesByPeriod() {
        String selectedPeriod = spinnerBudgetPeriod.getSelectedItem().toString();
        Calendar calendar = Calendar.getInstance();

        String period = mapDisplayNameToPeriod(selectedPeriod);

        switch (period) {
            case "monthly":
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                startDate = calendar.getTime();
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                endDate = calendar.getTime();
                break;

            case "weekly":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                startDate = calendar.getTime();
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                endDate = calendar.getTime();
                break;

            case "yearly":
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                startDate = calendar.getTime();
                calendar.add(Calendar.YEAR, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                endDate = calendar.getTime();
                break;
        }

        if (startDate != null) tvStartDate.setText(dateFormatter.format(startDate));
        if (endDate != null) tvEndDate.setText(dateFormatter.format(endDate));
    }

    private String mapDisplayNameToPeriod(String displayName) {
        switch (displayName) {
            case "Hàng tuần": return "weekly";
            case "Hàng tháng": return "monthly";
            case "Hàng năm": return "yearly";
            default: return "monthly";
        }
    }

    private void updateTotalBudget() {
        double total = 0;
        for (BudgetPlan.CategoryBudget category : categoryBudgets) {
            total += category.getAllocatedAmount();
        }
        tvTotalBudget.setText(String.format("%.0f VND", total));

        updateUI();
        updateRemainingBudget();
    }

    private void updateRemainingBudget() {
        try {
            String plannedBudgetStr = etTotalBudgetPlan.getText().toString().trim();
            double plannedBudget = plannedBudgetStr.isEmpty() ? 0 : Double.parseDouble(plannedBudgetStr);

            double allocatedBudget = 0;
            for (BudgetPlan.CategoryBudget category : categoryBudgets) {
                allocatedBudget += category.getAllocatedAmount();
            }

            double remaining = plannedBudget - allocatedBudget;

            String remainingText;
            if (remaining > 0) {
                remainingText = "Còn lại: " + String.format("%.0f VND", remaining);
                tvRemainingBudget.setTextColor(getResources().getColor(android.R.color.black));
            } else if (remaining == 0) {
                remainingText = "Đã phân bổ đủ: " + String.format("%.0f VND", Math.abs(remaining));
                tvRemainingBudget.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                remainingText = "Vượt quá: " + String.format("%.0f VND", Math.abs(remaining));
                tvRemainingBudget.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            tvRemainingBudget.setText(remainingText);

        } catch (NumberFormatException e) {
            tvRemainingBudget.setText("Còn lại: 0 VND");
            tvRemainingBudget.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    private void updateUI() {
        if (categoryBudgets.isEmpty()) {
            llEmptyCategories.setVisibility(View.VISIBLE);
            rvCategoryBudgets.setVisibility(View.GONE);
        } else {
            llEmptyCategories.setVisibility(View.GONE);
            rvCategoryBudgets.setVisibility(View.VISIBLE);
        }
    }

    // ✅ SỬA LẠI: Method saveBudget với kiểm tra số dư
    private void saveBudget() {
        String budgetName = etBudgetName.getText().toString().trim();
        String plannedBudgetStr = etTotalBudgetPlan.getText().toString().trim();
        String selectedPeriod = spinnerBudgetPeriod.getSelectedItem().toString();

        // Tạo biến final để sử dụng trong lambda
        final String finalBudgetName = budgetName;
        final String finalPeriod = mapDisplayNameToPeriod(selectedPeriod);

        // Validation cơ bản
        if (budgetName.isEmpty()) {
            etBudgetName.setError("Vui lòng nhập tên ngân sách");
            etBudgetName.requestFocus();
            return;
        }

        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu và kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ KIỂM TRA NGÂN SÁCH DỰ KIẾN VỚI SỐ DƯ
        double plannedBudget = 0;
        if (!plannedBudgetStr.isEmpty()) {
            try {
                plannedBudget = Double.parseDouble(plannedBudgetStr);

                // Tạo biến final cho lambda
                final double finalPlannedBudget = plannedBudget;

                // Kiểm tra với số dư hiện tại
                if (plannedBudget > currentBalance) {
                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ Vượt quá số dư")
                            .setMessage("Ngân sách dự kiến (" + String.format("%.0f VND", finalPlannedBudget) +
                                    ") vượt quá số dư hiện tại của bạn (" + String.format("%.0f VND", currentBalance) +
                                    ").\n\nBạn có muốn tiếp tục tạo ngân sách không?")
                            .setPositiveButton("Tiếp tục", (dialog, which) -> validateAndSave(finalBudgetName, finalPeriod, finalPlannedBudget))
                            .setNegativeButton("Hủy", null)
                            .show();
                    return;
                }
            } catch (NumberFormatException e) {
                etTotalBudgetPlan.setError("Vui lòng nhập số tiền hợp lệ");
                etTotalBudgetPlan.requestFocus();
                return;
            }
        }

        // Tạo biến final cho lambda (trường hợp plannedBudget = 0)
        final double finalPlannedBudget = plannedBudget;

        // Tiếp tục validate và save
        validateAndSave(finalBudgetName, finalPeriod, finalPlannedBudget);
    }

    // ✅ THÊM MỚI: Method validate và save (tách riêng để tái sử dụng)
    private void validateAndSave(String budgetName, String period, double plannedBudget) {
        double totalBudget = 0;
        for (BudgetPlan.CategoryBudget category : categoryBudgets) {
            totalBudget += category.getAllocatedAmount();
        }

        if (totalBudget <= 0) {
            Toast.makeText(this, "Vui lòng phân bổ ngân sách cho ít nhất một danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo biến final để sử dụng trong lambda
        final String finalBudgetName = budgetName;
        final String finalPeriod = period;
        final double finalTotalBudget = totalBudget;

        // ✅ KIỂM TRA TỔNG PHÂN BỔ VỚI SỐ DƯ
        if (totalBudget > currentBalance) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Tổng phân bổ vượt số dư")
                    .setMessage("Tổng ngân sách đã phân bổ (" + String.format("%.0f VND", finalTotalBudget) +
                            ") vượt quá số dư hiện tại (" + String.format("%.0f VND", currentBalance) +
                            ").\n\nBạn có chắc chắn muốn tạo?")
                    .setPositiveButton("Tạo", (dialog, which) -> showConfirmationDialog(finalBudgetName, finalPeriod, finalTotalBudget))
                    .setNegativeButton("Hủy", null)
                    .show();
            return;
        }

        // Show confirmation dialog
        showConfirmationDialog(finalBudgetName, finalPeriod, finalTotalBudget);
    }

    // ✅ THÊM MỚI: Tách confirmation dialog thành method riêng
    private void showConfirmationDialog(String budgetName, String period, double totalBudget) {
        // Tạo biến final để sử dụng trong lambda
        final String finalBudgetName = budgetName;
        final String finalPeriod = period;
        final double finalTotalBudget = totalBudget;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận tạo ngân sách")
                .setMessage("Bạn có chắc chắn muốn tạo ngân sách \"" + finalBudgetName +
                        "\" với tổng số tiền " + String.format("%.0f VND", finalTotalBudget) + "?")
                .setPositiveButton("Tạo", (dialog, which) -> saveBudgetToDatabase(finalBudgetName, finalPeriod, finalTotalBudget))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveBudgetToDatabase(String budgetName, String period, double totalBudget) {
        // ✅ DOUBLE CHECK: Kiểm tra user ID trước khi save
        if (currentUserId == null || currentUserId.isEmpty()) {
            showErrorAndFinish("Lỗi: Không thể lưu ngân sách do thiếu thông tin người dùng.");
            return;
        }

        Toast.makeText(this, "Đang tạo ngân sách...", Toast.LENGTH_SHORT).show();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            for (BudgetPlan.CategoryBudget category : categoryBudgets) {
                if (category.getAllocatedAmount() > 0) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_CATEGORY_ID, category.getCategoryId());
                    values.put(DatabaseHelper.COLUMN_BUDGET_AMOUNT, category.getAllocatedAmount());
                    values.put(DatabaseHelper.COLUMN_BUDGET_PERIOD, period);
                    values.put(DatabaseHelper.COLUMN_BUDGET_START_DATE, dateFormatter.format(startDate));
                    values.put(DatabaseHelper.COLUMN_BUDGET_END_DATE, dateFormatter.format(endDate));
                    values.put(DatabaseHelper.COLUMN_BUDGET_NAME, budgetName);

                    // ✅ STRICT: Sử dụng currentUserId (đã validate)
                    values.put(DatabaseHelper.COLUMN_USER_ID, currentUserId);

                    long result = db.insert(DatabaseHelper.TABLE_BUDGETS, null, values);

                    if (result == -1) {
                        throw new Exception("Lỗi khi lưu danh mục: " + category.getCategoryName());
                    }

                    Log.d(TAG, "✅ Saved budget category for user " + currentUserId +
                            ": " + category.getCategoryName() + " - " + category.getAllocatedAmount());
                }
            }

            db.setTransactionSuccessful();

            Log.d(TAG, "🎉 Budget saved successfully for user: " + currentUserId);

            new AlertDialog.Builder(this)
                    .setTitle("Thành công!")
                    .setMessage("Ngân sách \"" + budgetName + "\" đã được tạo thành công với tổng số tiền " +
                            String.format("%.0f VND", totalBudget))
                    .setPositiveButton("OK", (dialog, which) -> {
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving budget: " + e.getMessage());
            new AlertDialog.Builder(this)
                    .setTitle("Lỗi")
                    .setMessage("Có lỗi khi tạo ngân sách: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        } finally {
            db.endTransaction();
        }
    }

    private void debugUserInfo() {
        Log.d(TAG, "=== DEBUG USER INFO ===");
        Log.d(TAG, "Current User ID: " + currentUserId);
        Log.d(TAG, "Intent User ID: " + getIntent().getStringExtra("user_id"));

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG, "Prefs User ID: " + prefs.getString(KEY_USER_ID, "none"));
        Log.d(TAG, "======================");

        // Show in Toast for immediate feedback
        Toast.makeText(this, "Current User ID: " + currentUserId, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}