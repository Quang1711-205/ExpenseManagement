package com.example.expensemanagement;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;

import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import database.DatabaseHelper;

public class AddTransactionActivity extends AppCompatActivity {

    // Database
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    // Shared Preferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MoneyMasterPrefs";
    private static final String KEY_USER_ID = "userId";

    // UI Components
    private ImageView btnBack;
    private TextView tvTitle;
    private SwitchMaterial switchTransactionType;
    private TextView tvTransactionTypeLabel;
    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private TextView tvAmountFormatted;
    private MaterialCardView cardSelectedCategory;
    private TextView tvSelectedCategoryName;
    private TextView tvSelectedCategoryIcon;
    private RecyclerView rvCategories;
    private TextInputLayout tilNote;
    private TextInputEditText etNote;
    private TextView tvSelectedDate;
    private MaterialCardView cardDatePicker;
    private ChipGroup chipGroupPaymentMethods;
    private MaterialButton btnSaveTransaction;
    private View layoutCategoryError;

    // Data
    private String transactionType = "expense"; // Default to expense
    private long selectedDate;
    private int selectedCategoryId = -1;
    private String selectedPaymentMethod = "Tiền mặt";
    private List<Category> categories = new ArrayList<>();
    private CategoryAdapter categoryAdapter;

    // Formatters
    private DecimalFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    private String selectedCategoryName = "";
    private InAppNotificationManager inAppNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStatusBar();
        setContentView(R.layout.activity_add_transaction);

        initializeComponents();
        setupDatabase();
        setupFormatters();
        handleIntent();
        setupEventListeners();
        setupCategoriesRecyclerView();
        loadCategories();
        setupPaymentMethods();
        setSelectedDate(System.currentTimeMillis());
        updateUI();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null && database.isOpen()) {
            database.close();
        }

        // Ẩn tất cả in-app notifications
        if (inAppNotificationManager != null) {
            inAppNotificationManager.hideAllNotifications(); // ✅ SỬA
        }
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void initializeComponents() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        switchTransactionType = findViewById(R.id.switch_transaction_type);
        tvTransactionTypeLabel = findViewById(R.id.tv_transaction_type_label);
        tilAmount = findViewById(R.id.til_amount);
        etAmount = findViewById(R.id.et_amount);
        tvAmountFormatted = findViewById(R.id.tv_amount_formatted);
        cardSelectedCategory = findViewById(R.id.card_selected_category);
        tvSelectedCategoryName = findViewById(R.id.tv_selected_category_name);
        tvSelectedCategoryIcon = findViewById(R.id.tv_selected_category_icon);
        rvCategories = findViewById(R.id.rv_categories);
        tilNote = findViewById(R.id.til_note);
        etNote = findViewById(R.id.et_note);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        cardDatePicker = findViewById(R.id.card_date_picker);
        chipGroupPaymentMethods = findViewById(R.id.chip_group_payment_methods);
        btnSaveTransaction = findViewById(R.id.btn_save_transaction);
        layoutCategoryError = findViewById(R.id.layout_category_error);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // ✅ KHỞI TẠO IN-APP NOTIFICATION MANAGER
        inAppNotificationManager = new InAppNotificationManager(this);
    }

    private void setupDatabase() {
        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();
    }

    private void setupFormatters() {
        currencyFormat = new DecimalFormat("#,###,### đ");
        dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("transaction_type")) {
            String type = intent.getStringExtra("transaction_type");
            if ("income".equals(type)) {
                transactionType = "income";
                switchTransactionType.setChecked(true);
            } else {
                transactionType = "expense";
                switchTransactionType.setChecked(false);
            }
        }
    }

    private void setupEventListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
        });

        // Transaction type switch
        switchTransactionType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            transactionType = isChecked ? "income" : "expense";
            loadCategories();
            clearSelectedCategory();
            updateUI();
            animateTypeChange();
        });

        // Amount input
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateAmountFormatted();
                validateForm();
            }
        });

        // Date picker
        cardDatePicker.setOnClickListener(v -> showDatePicker());

        // Save button
        btnSaveTransaction.setOnClickListener(v -> saveTransaction());
    }

    private void setupCategoriesRecyclerView() {
        categoryAdapter = new CategoryAdapter(categories, this::onCategorySelected);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 4));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupPaymentMethods() {
        String[] paymentMethods = {
                "Tiền mặt", "Thẻ tín dụng", "Thẻ ghi nợ",
                "Chuyển khoản", "Ví điện tử", "Khác"
        };

        chipGroupPaymentMethods.removeAllViews();

        for (int i = 0; i < paymentMethods.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(paymentMethods[i]);
            chip.setCheckable(true);
            if (i == 0) { // Select first option by default
                chip.setChecked(true);
                selectedPaymentMethod = paymentMethods[i];
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPaymentMethod = chip.getText().toString();
                    // Uncheck other chips
                    for (int j = 0; j < chipGroupPaymentMethods.getChildCount(); j++) {
                        Chip otherChip = (Chip) chipGroupPaymentMethods.getChildAt(j);
                        if (otherChip != chip) {
                            otherChip.setChecked(false);
                        }
                    }
                }
            });

            chipGroupPaymentMethods.addView(chip);
        }
    }

    private void loadCategories() {
        categories.clear();

        try {
            String query = "SELECT * FROM " + DatabaseHelper.TABLE_CATEGORIES +
                    " WHERE " + DatabaseHelper.COLUMN_CATEGORY_TYPE + " = ? ORDER BY " +
                    DatabaseHelper.COLUMN_CATEGORY_NAME;

            Cursor cursor = database.rawQuery(query, new String[]{transactionType});

            while (cursor.moveToNext()) {
                Category category = new Category(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ICON)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_COLOR))
                );
                categories.add(category);
            }
            cursor.close();

            if (categoryAdapter != null) {
                categoryAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Lỗi khi tải danh mục");
        }
    }

    private void onCategorySelected(Category category) {
        selectedCategoryId = category.getId();
        selectedCategoryName = category.getName();

        tvSelectedCategoryName.setText(category.getName());
        tvSelectedCategoryIcon.setText(category.getIcon());

        cardSelectedCategory.setVisibility(View.VISIBLE);
        layoutCategoryError.setVisibility(View.GONE);

        // Update category adapter selection
        if (categoryAdapter != null) {
            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
            categoryAdapter.notifyDataSetChanged();
        }

        validateForm();
        animateCategorySelection();
    }

    private void clearSelectedCategory() {
        selectedCategoryId = -1;
        selectedCategoryName = "";
        cardSelectedCategory.setVisibility(View.GONE);
        if (categoryAdapter != null) {
            categoryAdapter.setSelectedCategoryId(-1);
            categoryAdapter.notifyDataSetChanged();
        }
    }

    private void updateAmountFormatted() {
        String amountStr = etAmount.getText().toString().trim();
        if (!amountStr.isEmpty()) {
            try {
                double amount = Double.parseDouble(amountStr);
                String formattedAmount = currencyFormat.format(amount);

                // ✅ Hiển thị số dư khi nhập chi tiêu
                if ("expense".equals(transactionType)) {
                    double currentBalance = getCurrentBalance();
                    double remainingBalance = currentBalance - amount;

                    String balanceInfo = formattedAmount + "\n" +
                            "Số dư sau giao dịch: " + currencyFormat.format(remainingBalance);

                    tvAmountFormatted.setText(balanceInfo);

                    // Đổi màu cảnh báo nếu số dư âm
                    if (remainingBalance < 0) {
                        tvAmountFormatted.setTextColor(getResources().getColor(R.color.expense_red));
                    } else {
                        tvAmountFormatted.setTextColor(getResources().getColor(R.color.text_primary));
                    }
                } else {
                    // Thu nhập chỉ hiển thị số tiền
                    tvAmountFormatted.setText(formattedAmount);
                    tvAmountFormatted.setTextColor(getResources().getColor(R.color.income_green));
                }

                tvAmountFormatted.setVisibility(View.VISIBLE);
            } catch (NumberFormatException e) {
                tvAmountFormatted.setVisibility(View.GONE);
            }
        } else {
            tvAmountFormatted.setVisibility(View.GONE);
        }
    }

    private void updateUI() {
        // Update title and labels
        if ("income".equals(transactionType)) {
            tvTitle.setText("Thêm Thu Nhập");
            tvTransactionTypeLabel.setText("Thu nhập");
            btnSaveTransaction.setText("Lưu Thu Nhập");
            btnSaveTransaction.setBackgroundColor(getResources().getColor(R.color.income_green));
        } else {
            tvTitle.setText("Thêm Chi Tiêu");
            tvTransactionTypeLabel.setText("Chi tiêu");
            btnSaveTransaction.setText("Lưu Chi Tiêu");
            btnSaveTransaction.setBackgroundColor(getResources().getColor(R.color.expense_red));
        }

        // Update amount input hint
        tilAmount.setHint("income".equals(transactionType) ? "Số tiền thu nhập" : "Số tiền chi tiêu");
    }

    private void animateTypeChange() {
        int currentColor = "income".equals(transactionType) ?
                getResources().getColor(R.color.expense_red) :
                getResources().getColor(R.color.income_green);

        ObjectAnimator colorAnimator = ObjectAnimator.ofArgb(
                btnSaveTransaction,
                "backgroundColor",
                currentColor,
                "income".equals(transactionType) ?
                        getResources().getColor(R.color.income_green) :
                        getResources().getColor(R.color.expense_red)
        );
        colorAnimator.setDuration(300);
        colorAnimator.start();
    }

    private void animateCategorySelection() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardSelectedCategory, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardSelectedCategory, "scaleY", 0.8f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(cardSelectedCategory, "alpha", 0f, 1f);

        scaleX.setDuration(200);
        scaleY.setDuration(200);
        alpha.setDuration(200);

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void setSelectedDate(long timestamp) {
        selectedDate = timestamp;
        tvSelectedDate.setText(dateFormat.format(timestamp));
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    setSelectedDate(selectedCalendar.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void validateForm() {
        boolean isValid = true;

        // Validate amount
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tilAmount.setError("Vui lòng nhập số tiền");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountStr);

                // ✅ Kiểm tra số tiền tối thiểu 1000
                if (amount < 1000) {
                    tilAmount.setError("Số tiền phải từ 1,000 đ trở lên");
                    isValid = false;
                }
                // ✅ Kiểm tra số dư khi chi tiêu
                else if ("expense".equals(transactionType)) {
                    double currentBalance = getCurrentBalance();
                    if (amount > currentBalance) {
                        tilAmount.setError("Số dư không đủ! Hiện có: " + currencyFormat.format(currentBalance));
                        isValid = false;
                    } else {
                        tilAmount.setError(null);
                    }
                }
                // Thu nhập chỉ cần kiểm tra >= 1000
                else {
                    tilAmount.setError(null);
                }
            } catch (NumberFormatException e) {
                tilAmount.setError("Số tiền không hợp lệ");
                isValid = false;
            }
        }

        // Validate category
        if (selectedCategoryId == -1) {
            layoutCategoryError.setVisibility(View.VISIBLE);
            isValid = false;
        } else {
            layoutCategoryError.setVisibility(View.GONE);
        }

        btnSaveTransaction.setEnabled(isValid);
        btnSaveTransaction.setAlpha(isValid ? 1.0f : 0.6f);
    }

    // Sửa đổi trong AddTransactionActivity.java

//    private void saveTransaction() {
//        if (!validateFormForSave()) {
//            return;
//        }
//
//        try {
//            int userId = sharedPreferences.getInt(KEY_USER_ID, 1);
//            double amount = Double.parseDouble(etAmount.getText().toString().trim());
//            String note = etNote.getText().toString().trim();
//
//            ContentValues values = new ContentValues();
//            values.put(DatabaseHelper.COLUMN_TYPE, transactionType);
//            values.put(DatabaseHelper.COLUMN_AMOUNT, amount);
//            values.put(DatabaseHelper.COLUMN_CATEGORY_ID, selectedCategoryId);
//            values.put(DatabaseHelper.COLUMN_USER_ID, userId);
//            values.put(DatabaseHelper.COLUMN_NOTE, note.isEmpty() ? null : note);
//
//            // ✅ FIX 1: Lưu ngày dưới dạng chuẩn yyyy-MM-dd cho database
//            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            values.put(DatabaseHelper.COLUMN_DATE, dbDateFormat.format(selectedDate));
//
//            values.put(DatabaseHelper.COLUMN_PAYMENT_METHOD, selectedPaymentMethod);
//
//            // ✅ FIX 2: Thêm timestamp cho việc sắp xếp
//            values.put(DatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis());
//
//            long result = database.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);
//
//            if (result != -1) {
//                String message = "income".equals(transactionType) ?
//                        "Đã thêm thu nhập thành công!" :
//                        "Đã thêm chi tiêu thành công!";
//                showToast(message);
//
//                // ✅ THÊM THÔNG BÁO
//                try {
//                    NotificationHelper notificationHelper = new NotificationHelper(this);
//                    notificationHelper.showTransactionSuccessNotification(transactionType, amount, selectedCategoryName);
//                } catch (Exception e) {
//                    android.util.Log.e("NOTIFICATION", "Error: " + e.getMessage());
//                }
//
//                // ✅ CẢNH BÁO SỐ DƯ THẤP
//                if ("expense".equals(transactionType)) {
//                    double newBalance = getCurrentBalance() - amount;
//                    if (newBalance < 100000) {
//                        try {
//                            NotificationHelper notificationHelper = new NotificationHelper(this);
//                            notificationHelper.showLowBalanceWarning(newBalance);
//                        } catch (Exception e) {
//                            android.util.Log.e("NOTIFICATION", "Error: " + e.getMessage());
//                        }
//                    }
//                }
//
//                // ✅ FIX 3: Return result để Dashboard refresh
//                Intent resultIntent = new Intent();
//                resultIntent.putExtra("transaction_added", true);
//                resultIntent.putExtra("transaction_type", transactionType);
//                resultIntent.putExtra("transaction_amount", amount);
//                resultIntent.putExtra("refresh_needed", true);
//                setResult(RESULT_OK, resultIntent);
//
//                finish();
//                overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
//            } else {
//                showToast("Lỗi khi lưu giao dịch");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            showToast("Đã có lỗi xảy ra");
//        }
//    }

    // ✅ THAY THẾ METHOD saveTransaction() TRONG AddTransactionActivity

    private void saveTransaction() {
        if (!validateFormForSave()) {
            return;
        }

        try {
            int userId = sharedPreferences.getInt(KEY_USER_ID, 1);
            double amount = Double.parseDouble(etAmount.getText().toString().trim());
            String note = etNote.getText().toString().trim();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_TYPE, transactionType);
            values.put(DatabaseHelper.COLUMN_AMOUNT, amount);
            values.put(DatabaseHelper.COLUMN_CATEGORY_ID, selectedCategoryId);
            values.put(DatabaseHelper.COLUMN_USER_ID, userId);
            values.put(DatabaseHelper.COLUMN_NOTE, note.isEmpty() ? null : note);

            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            values.put(DatabaseHelper.COLUMN_DATE, dbDateFormat.format(selectedDate));
            values.put(DatabaseHelper.COLUMN_PAYMENT_METHOD, selectedPaymentMethod);
            values.put(DatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis());

            long result = database.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);

            if (result != -1) {
                String message = "income".equals(transactionType) ?
                        "Đã thêm thu nhập thành công!" :
                        "Đã thêm chi tiêu thành công!";

                android.util.Log.d("AddTransaction", "Transaction saved successfully");
                android.util.Log.d("AddTransaction", "Type: " + transactionType + ", Amount: " + amount + ", Category: " + selectedCategoryName);

                // ✅ 1. HIỂN THỊ TOAST
                showToast(message);

                // ✅ 2. HIỂN THỊ IN-APP NOTIFICATION (LUÔN HIỂN THỊ)
                try {
                    inAppNotificationManager.showTransactionSuccess(transactionType, amount, selectedCategoryName);
                    android.util.Log.d("AddTransaction", "In-app notification shown");
                } catch (Exception e) {
                    android.util.Log.e("AddTransaction", "Error showing in-app notification: " + e.getMessage());
                    e.printStackTrace();
                }

                // ✅ 3. HIỂN THỊ SYSTEM NOTIFICATION (NẾU CÓ QUYỀN)
                try {
                    if (NotificationPermissionHelper.hasNotificationPermission(this)) {
                        android.util.Log.d("AddTransaction", "Has notification permission, showing system notification");
                        NotificationHelper notificationHelper = new NotificationHelper(this);
                        notificationHelper.showTransactionSuccessNotification(transactionType, amount, selectedCategoryName);
                        android.util.Log.d("AddTransaction", "System notification sent");
                    } else {
                        android.util.Log.w("AddTransaction", "No notification permission, skipping system notification");
                    }
                } catch (Exception e) {
                    android.util.Log.e("AddTransaction", "Error showing system notification: " + e.getMessage());
                    e.printStackTrace();
                }

                // ✅ 4. KIỂM TRA VÀ CẢNH BÁO SỐ DƯ THẤP
                if ("expense".equals(transactionType)) {
                    double newBalance = getCurrentBalance() - amount;
                    android.util.Log.d("AddTransaction", "New balance after expense: " + newBalance);

                    if (newBalance < 100000) { // Dưới 100,000đ
                        // In-app warning (luôn hiện)
                        try {
                            inAppNotificationManager.showLowBalanceWarning(newBalance);
                            android.util.Log.d("AddTransaction", "In-app low balance warning shown");
                        } catch (Exception e) {
                            android.util.Log.e("AddTransaction", "Error showing in-app low balance warning: " + e.getMessage());
                        }

                        // System notification warning (nếu có quyền)
                        try {
                            if (NotificationPermissionHelper.hasNotificationPermission(this)) {
                                android.util.Log.d("AddTransaction", "Showing system low balance warning");
                                NotificationHelper notificationHelper = new NotificationHelper(this);

                                // Delay để không bị conflict với notification giao dịch
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    notificationHelper.showLowBalanceWarning(newBalance);
                                }, 1500);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("AddTransaction", "Error showing system low balance warning: " + e.getMessage());
                        }
                    }
                }

                // ✅ 5. RETURN RESULT
                Intent resultIntent = new Intent();
                resultIntent.putExtra("transaction_added", true);
                resultIntent.putExtra("transaction_type", transactionType);
                resultIntent.putExtra("transaction_amount", amount);
                resultIntent.putExtra("refresh_needed", true);
                setResult(RESULT_OK, resultIntent);

                // ✅ 6. DELAY TRƯỚC KHI FINISH
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    finish();
                    overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
                }, 2000); // 2 seconds để user thấy notification

            } else {
                android.util.Log.e("AddTransaction", "Failed to insert transaction");
                showToast("Lỗi khi lưu giao dịch");

                // Hiển thị error notification
                try {
                    inAppNotificationManager.showCustomNotification(
                            "❌ Lỗi", "Không thể lưu giao dịch. Vui lòng thử lại.");
                } catch (Exception e) {
                    android.util.Log.e("AddTransaction", "Error showing error notification: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            android.util.Log.e("AddTransaction", "Exception in saveTransaction: " + e.getMessage());
            e.printStackTrace();
            showToast("Đã có lỗi xảy ra");

            // Hiển thị exception notification
            try {
                inAppNotificationManager.showCustomNotification(
                        "❌ Lỗi hệ thống", "Đã có lỗi xảy ra. Vui lòng thử lại.");
            } catch (Exception ex) {
                android.util.Log.e("AddTransaction", "Error showing exception notification: " + ex.getMessage());
            }
        }
    }


    // Thêm method mới để tính số dư hiện tại
    private double getCurrentBalance() {
        double balance = 0;
        int userId = sharedPreferences.getInt(KEY_USER_ID, 1);

        try {
            String query = "SELECT " + DatabaseHelper.COLUMN_TYPE + ", " + DatabaseHelper.COLUMN_AMOUNT +
                    " FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";

            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

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
            e.printStackTrace();
        }

        return balance;
    }

    private boolean validateFormForSave() {
        boolean isValid = true;

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tilAmount.setError("Vui lòng nhập số tiền");
            showToast("Vui lòng nhập số tiền");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountStr);

                // ✅ Kiểm tra số tiền tối thiểu
                if (amount < 1000) {
                    tilAmount.setError("Số tiền phải từ 1,000 đ trở lên");
                    showToast("Số tiền phải từ 1,000 đ trở lên");
                    isValid = false;
                }
                // ✅ Kiểm tra số tiền quá lớn
                else if (amount > 999999999) {
                    tilAmount.setError("Số tiền quá lớn");
                    showToast("Số tiền quá lớn");
                    isValid = false;
                }
                // ✅ Kiểm tra số dư khi chi tiêu
                else if ("expense".equals(transactionType)) {
                    double currentBalance = getCurrentBalance();
                    if (amount > currentBalance) {
                        tilAmount.setError("Số dư không đủ! Hiện có: " + currencyFormat.format(currentBalance));
                        showToast("Số dư không đủ để thực hiện giao dịch này!\nSố dư hiện tại: " + currencyFormat.format(currentBalance));
                        isValid = false;
                    } else {
                        tilAmount.setError(null);
                    }
                }
                // Thu nhập OK
                else {
                    tilAmount.setError(null);
                }
            } catch (NumberFormatException e) {
                tilAmount.setError("Số tiền không hợp lệ");
                showToast("Số tiền không hợp lệ");
                isValid = false;
            }
        }

        // Validate category
        if (selectedCategoryId == -1) {
            layoutCategoryError.setVisibility(View.VISIBLE);
            showToast("Vui lòng chọn danh mục");
            isValid = false;
        } else {
            layoutCategoryError.setVisibility(View.GONE);
        }

        // Validate date
        if (selectedDate > System.currentTimeMillis() + 24 * 60 * 60 * 1000) {
            showToast("Không thể chọn ngày trong tương lai");
            isValid = false;
        }

        return isValid;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Category model class
    private static class Category {
        private int id;
        private String name;
        private String type;
        private String icon;
        private String color;

        public Category(int id, String name, String type, String icon, String color) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.icon = icon;
            this.color = color;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
    }

    // Category adapter
    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<Category> categories;
        private OnCategorySelectedListener listener;
        private int selectedCategoryId = -1;

        public CategoryAdapter(List<Category> categories, OnCategorySelectedListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        public void setSelectedCategoryId(int selectedCategoryId) {
            this.selectedCategoryId = selectedCategoryId;
        }

        @Override
        public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_selection, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CategoryViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.bind(category, selectedCategoryId == category.getId(), listener);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class CategoryViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardCategory;
            private TextView tvIcon;
            private TextView tvName;

            public CategoryViewHolder(View itemView) {
                super(itemView);
                cardCategory = itemView.findViewById(R.id.card_category);
                tvIcon = itemView.findViewById(R.id.tv_category_icon);
                tvName = itemView.findViewById(R.id.tv_category_name);
            }

            public void bind(Category category, boolean isSelected, OnCategorySelectedListener listener) {
                tvIcon.setText(category.getIcon());
                tvName.setText(category.getName());

                // Update selection state
                cardCategory.setCardElevation(isSelected ? 8f : 2f);
                cardCategory.setStrokeWidth(isSelected ? 4 : 0);

                try {
                    int color = Color.parseColor(category.getColor());
                    if (isSelected) {
                        cardCategory.setStrokeColor(color);
                    }
                } catch (Exception e) {
                    // Use default color if parsing fails
                }

                cardCategory.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCategorySelected(category);
                    }
                });
            }
        }

        interface OnCategorySelectedListener {
            void onCategorySelected(Category category);
        }
    }
}