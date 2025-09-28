package com.example.expensemanagement;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.content.SharedPreferences;



import androidx.activity.OnBackPressedCallback;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import database.DatabaseHelper;
import adapters.TransactionAdapter;
import models.Transaction;
import models.Category;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsActivity extends AppCompatActivity {

    // Database
    private DatabaseHelper dbHelper;

    // Views
    private MaterialToolbar toolbar;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance, tvPeriodTitle;
    private TabLayout tabLayoutPeriod;
    private MaterialCardView btnAddIncome, btnAddExpense, searchCard;
    private ExtendedFloatingActionButton fabAddTransaction;
    private FloatingActionButton fabFilter, fabSearch;
    private Chip chipDateFilter, chipCategoryFilter;
    private ChipGroup chipGroupActiveFilters;
    private TextInputEditText etSearch;

    // RecyclerView
    private RecyclerView recyclerViewTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private List<Transaction> filteredTransactionList;
    private LinearLayout layoutEmpty, layoutLoading;
    private ImageView ivEmptyIcon;
    private MaterialButton btnQuickAddIncome, btnQuickAddExpense;

    // State
    private String currentPeriod = "all";
    private String customStartDate = null;
    private String customEndDate = null;
    private boolean isCustomDateRange = false;
    private List<Long> selectedCategoryIds = new ArrayList<>();
    private String searchQuery = "";
    private boolean isSearchVisible = false;
    private boolean isFabMenuOpen = false;

    // Formatters
    private DecimalFormat currencyFormatter;
    private SimpleDateFormat dateFormatter;
    private SimpleDateFormat displayDateFormatter;

    // Animation Handler
    private Handler animationHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        initializeFormatters();
        initViews();
        setupDatabase();
        setupRecyclerView();
        setupClickListeners();
        setupTabs();
        setupSearch();
        setupOnBackPressed();
        debugUserId();

        // Load initial data with animation
        showLoading(true);
        loadTransactions();
        calculateTotals();
    }

    private void initializeFormatters() {
        currencyFormatter = new DecimalFormat("#,###,###");
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    private void initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Summary views
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvPeriodTitle = findViewById(R.id.tvPeriodTitle);

        // Tab and filter views
        tabLayoutPeriod = findViewById(R.id.tabLayoutPeriod);
        chipDateFilter = findViewById(R.id.chipDateFilter);
        chipCategoryFilter = findViewById(R.id.chipCategoryFilter);
        chipGroupActiveFilters = findViewById(R.id.chipGroupActiveFilters);

        // Action buttons
        btnAddIncome = findViewById(R.id.btnAddIncome);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);
        fabFilter = findViewById(R.id.fabFilter);
        fabSearch = findViewById(R.id.fabSearch);

        // Search
        searchCard = findViewById(R.id.searchCard);
        etSearch = findViewById(R.id.etSearch);

        // Content views
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutLoading = findViewById(R.id.layoutLoading);
        ivEmptyIcon = findViewById(R.id.ivEmptyIcon);
//        btnQuickAddIncome = findViewById(R.id.btnQuickAddIncome);
//        btnQuickAddExpense = findViewById(R.id.btnQuickAddExpense);
    }

    private void setupDatabase() {
        dbHelper = new DatabaseHelper(this);
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        filteredTransactionList = new ArrayList<>();
        adapter = new TransactionAdapter(filteredTransactionList);
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTransactions.setAdapter(adapter);

        // Set up click listeners
        adapter.setOnTransactionClickListener(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                openTransactionDetails(transaction);
            }

            @Override
            public void onTransactionLongClick(Transaction transaction) {
                showTransactionOptions(transaction);
            }
        });

        // Add item decoration for spacing
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        recyclerViewTransactions.addItemDecoration(new SpaceItemDecoration(spacingInPixels));
    }

    private void setupTabs() {
        tabLayoutPeriod.addTab(tabLayoutPeriod.newTab().setText("Hôm nay"));
        tabLayoutPeriod.addTab(tabLayoutPeriod.newTab().setText("Tuần này"));
        tabLayoutPeriod.addTab(tabLayoutPeriod.newTab().setText("Tháng này"));
        tabLayoutPeriod.addTab(tabLayoutPeriod.newTab().setText("Tất cả"));

        tabLayoutPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        selectPeriod("today");
                        break;
                    case 1:
                        selectPeriod("week");
                        break;
                    case 2:
                        selectPeriod("month");
                        break;
                    case 3:
                        selectPeriod("all");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        // Toolbar navigation
        toolbar.setNavigationOnClickListener(v -> finish());

        // Action buttons
        btnAddIncome.setOnClickListener(v -> openAddTransaction("income"));
        btnAddExpense.setOnClickListener(v -> openAddTransaction("expense"));
//        btnQuickAddIncome.setOnClickListener(v -> openAddTransaction("income"));
//        btnQuickAddExpense.setOnClickListener(v -> openAddTransaction("expense"));

        // FAB
        fabAddTransaction.setOnClickListener(v -> toggleFabMenu());
        fabFilter.setOnClickListener(v -> showCategoryFilterDialog());
        fabSearch.setOnClickListener(v -> toggleSearch());

        // Filter chips
        chipDateFilter.setOnClickListener(v -> showDateRangePickerDialog());
        chipCategoryFilter.setOnClickListener(v -> showCategoryFilterDialog());
    }

    private void selectPeriod(String period) {
        if (period.equals(currentPeriod) && !isCustomDateRange) return;

        currentPeriod = period;
        isCustomDateRange = false;
        customStartDate = null;
        customEndDate = null;

        updatePeriodTitle();
        loadTransactions();
    }

    private void updatePeriodTitle() {
        String title = "";
        switch (currentPeriod) {
            case "today":
                title = "Hôm nay";
                break;
            case "week":
                title = "Tuần này";
                break;
            case "month":
                title = "Tháng này";
                break;
            case "all":
                title = "Tất cả";
                break;
        }

        if (isCustomDateRange && customStartDate != null && customEndDate != null) {
            title = formatCustomDateRange();
        }

        tvPeriodTitle.setText(title);
    }

    private String formatCustomDateRange() {
        try {
            Date startDate = dateFormatter.parse(customStartDate);
            Date endDate = dateFormatter.parse(customEndDate);
            return displayDateFormatter.format(startDate) + " - " + displayDateFormatter.format(endDate);
        } catch (Exception e) {
            return "Khoảng thời gian tùy chọn";
        }
    }

    private void showDateRangePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        // Start date picker
        DatePickerDialog startDatePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(year, month, dayOfMonth);
                    customStartDate = dateFormatter.format(startCal.getTime());

                    // End date picker
                    DatePickerDialog endDatePicker = new DatePickerDialog(this,
                            (view2, year2, month2, dayOfMonth2) -> {
                                Calendar endCal = Calendar.getInstance();
                                endCal.set(year2, month2, dayOfMonth2);
                                customEndDate = dateFormatter.format(endCal.getTime());

                                isCustomDateRange = true;
                                updateDateFilterChip();
                                updatePeriodTitle();
                                loadTransactions();
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));

                    endDatePicker.getDatePicker().setMinDate(startCal.getTimeInMillis());
                    endDatePicker.setTitle("Chọn ngày kết thúc");
                    endDatePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        startDatePicker.setTitle("Chọn ngày bắt đầu");
        startDatePicker.show();
    }

    private void showCategoryFilterDialog() {
        List<Category> categories = loadCategories();
        String[] categoryNames = new String[categories.size()];
        boolean[] checkedItems = new boolean[categories.size()];

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            categoryNames[i] = category.getIcon() + " " + category.getName();
            checkedItems[i] = selectedCategoryIds.contains(category.getId());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Lọc theo danh mục")
                .setMultiChoiceItems(categoryNames, checkedItems,
                        (dialog, which, isChecked) -> {
                            Long categoryId = categories.get(which).getId();
                            if (isChecked) {
                                if (!selectedCategoryIds.contains(categoryId)) {
                                    selectedCategoryIds.add(categoryId);
                                }
                            } else {
                                selectedCategoryIds.remove(categoryId);
                            }
                        })
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    updateCategoryFilterChip();
                    applyFilters();
                })
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa bộ lọc", (dialog, which) -> {
                    selectedCategoryIds.clear();
                    updateCategoryFilterChip();
                    applyFilters();
                })
                .show();
    }

    private void toggleSearch() {
        isSearchVisible = !isSearchVisible;

        if (isSearchVisible) {
            searchCard.setVisibility(View.VISIBLE);
            searchCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down));
            etSearch.requestFocus();
            fabSearch.setImageResource(R.drawable.ic_close);
        } else {
            searchCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
            searchCard.setVisibility(View.GONE);
            etSearch.clearFocus();
            etSearch.setText("");
            fabSearch.setImageResource(R.drawable.ic_search);
        }

        closeFabMenu();
    }

    private void toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen;

        if (isFabMenuOpen) {
            fabFilter.setVisibility(View.VISIBLE);
            fabSearch.setVisibility(View.VISIBLE);
            fabFilter.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fab_open));
            fabSearch.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fab_open));
            fabAddTransaction.setIcon(getDrawable(R.drawable.ic_close));
        } else {
            closeFabMenu();
        }
    }
    private void closeFabMenu() {
        if (isFabMenuOpen) {
            fabFilter.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fab_close));
            fabSearch.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fab_close));
            animationHandler.postDelayed(() -> {
                fabFilter.setVisibility(View.GONE);
                fabSearch.setVisibility(View.GONE);
            }, 200);
            fabAddTransaction.setIcon(getDrawable(R.drawable.ic_add));
            isFabMenuOpen = false;
        }
    }

    private void updateDateFilterChip() {
        if (isCustomDateRange && customStartDate != null && customEndDate != null) {
            chipDateFilter.setText(formatCustomDateRange());
            chipDateFilter.setChecked(true);
            addActiveFilterChip("date", formatCustomDateRange());
        } else {
            chipDateFilter.setText("Chọn ngày");
            chipDateFilter.setChecked(false);
            removeActiveFilterChip("date");
        }
    }

    private void updateCategoryFilterChip() {
        if (!selectedCategoryIds.isEmpty()) {
            String text = selectedCategoryIds.size() == 1 ?
                    "1 danh mục" : selectedCategoryIds.size() + " danh mục";
            chipCategoryFilter.setText(text);
            chipCategoryFilter.setChecked(true);
            addActiveFilterChip("category", text);
        } else {
            chipCategoryFilter.setText("Danh mục");
            chipCategoryFilter.setChecked(false);
            removeActiveFilterChip("category");
        }
    }

    private void addActiveFilterChip(String type, String text) {
        // Remove existing chip of this type first
        removeActiveFilterChip(type);

        Chip filterChip = new Chip(this);
        filterChip.setText(text);
        filterChip.setTag(type);
        filterChip.setCloseIconVisible(true);
        filterChip.setCheckable(false);
        filterChip.setOnCloseIconClickListener(v -> {
            if ("date".equals(type)) {
                clearDateFilter();
            } else if ("category".equals(type)) {
                clearCategoryFilter();
            }
        });

        chipGroupActiveFilters.addView(filterChip);
        chipGroupActiveFilters.setVisibility(View.VISIBLE);
    }

    private void removeActiveFilterChip(String type) {
        for (int i = 0; i < chipGroupActiveFilters.getChildCount(); i++) {
            View child = chipGroupActiveFilters.getChildAt(i);
            if (child instanceof Chip && type.equals(child.getTag())) {
                chipGroupActiveFilters.removeView(child);
                break;
            }
        }

        if (chipGroupActiveFilters.getChildCount() == 0) {
            chipGroupActiveFilters.setVisibility(View.GONE);
        }
    }

    private void clearDateFilter() {
        isCustomDateRange = false;
        customStartDate = null;
        customEndDate = null;
        updateDateFilterChip();
        loadTransactions();
    }

    private void clearCategoryFilter() {
        selectedCategoryIds.clear();
        updateCategoryFilterChip();
        applyFilters();
    }

    private void openAddTransaction(String type) {
        try {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            if (type != null) {
                intent.putExtra("transaction_type", type);
            }
            startActivityForResult(intent, 100); // Sử dụng startActivityForResult
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở giao diện thêm giao dịch", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTransactions() {
        Log.d("TransactionsActivity", "=== Starting loadTransactions ===");
        showLoading(true);

        animationHandler.postDelayed(() -> {
            transactionList.clear();
            Log.d("TransactionsActivity", "Cleared transaction list");

            SQLiteDatabase db = null;
            Cursor cursor = null;

            try {
                db = dbHelper.getReadableDatabase();
                Log.d("TransactionsActivity", "Database opened successfully");

                // Lấy userId từ SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("MoneyMasterPrefs", MODE_PRIVATE);
                int userId = sharedPreferences.getInt("userId", -1);

                Log.d("TransactionsActivity", "Loading transactions for userId: " + userId);

                String dateCondition = getDateCondition();
                Log.d("TransactionsActivity", "Date condition: " + dateCondition);

                String query = "SELECT " +
                        "t." + DatabaseHelper.COLUMN_ID + ", " +
                        "t." + DatabaseHelper.COLUMN_TYPE + ", " +
                        "t." + DatabaseHelper.COLUMN_AMOUNT + ", " +
                        "t." + DatabaseHelper.COLUMN_CATEGORY_ID + ", " +
                        "t." + DatabaseHelper.COLUMN_NOTE + ", " +
                        "t." + DatabaseHelper.COLUMN_DATE + ", " +
                        "t." + DatabaseHelper.COLUMN_PAYMENT_METHOD + ", " +
                        "t." + DatabaseHelper.COLUMN_CREATED_AT + ", " +
                        "c." + DatabaseHelper.COLUMN_CATEGORY_NAME + ", " +
                        "c." + DatabaseHelper.COLUMN_CATEGORY_ICON + ", " +
                        "c." + DatabaseHelper.COLUMN_CATEGORY_COLOR +
                        " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t" +
                        " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c" +
                        " ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID +
                        " WHERE " + dateCondition + " AND t." + DatabaseHelper.COLUMN_USER_ID + " = ?" +
                        " ORDER BY t." + DatabaseHelper.COLUMN_DATE + " DESC, " +
                        "t." + DatabaseHelper.COLUMN_CREATED_AT + " DESC";

                Log.d("TransactionsActivity", "Executing query: " + query);
                Log.d("TransactionsActivity", "Query params: userId=" + userId);

                cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

                Log.d("TransactionsActivity", "Query executed, cursor count: " + cursor.getCount());

                if (cursor.moveToFirst()) {
                    Log.d("TransactionsActivity", "Processing cursor results...");
                    int count = 0;
                    do {
                        try {
                            Transaction transaction = createTransactionFromCursor(cursor);
                            transactionList.add(transaction);
                            count++;
                            Log.d("TransactionsActivity", "Added transaction #" + count +
                                    ": " + transaction.getCategoryName() +
                                    ", Amount: " + transaction.getAmount() +
                                    ", Date: " + transaction.getDate());
                        } catch (Exception e) {
                            Log.e("TransactionsActivity", "Error creating transaction from cursor at position " + cursor.getPosition(), e);
                        }
                    } while (cursor.moveToNext());
                    Log.d("TransactionsActivity", "Finished processing cursor, added " + count + " transactions");
                } else {
                    Log.w("TransactionsActivity", "Cursor is empty - no results from query");

                    // Debug: Kiểm tra lại với query đơn giản hơn
                    Cursor debugCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                            new String[]{String.valueOf(userId)});
                    if (debugCursor.moveToFirst()) {
                        int totalCount = debugCursor.getInt(0);
                        Log.d("TransactionsActivity", "Debug: Total transactions for userId " + userId + ": " + totalCount);
                    }
                    debugCursor.close();

                    // Debug: Kiểm tra ngày
                    Log.d("TransactionsActivity", "Current period: " + currentPeriod);
                    Log.d("TransactionsActivity", "Is custom date range: " + isCustomDateRange);
                    if (isCustomDateRange) {
                        Log.d("TransactionsActivity", "Custom date range: " + customStartDate + " to " + customEndDate);
                    }
                }

            } catch (Exception e) {
                Log.e("TransactionsActivity", "Exception in loadTransactions", e);
                Toast.makeText(this, "Lỗi khi tải dữ liệu giao dịch: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    Log.d("TransactionsActivity", "Cursor closed");
                }
                if (db != null) {
                    db.close();
                    Log.d("TransactionsActivity", "Database closed");
                }
            }

            Log.d("TransactionsActivity", "Final transaction list size: " + transactionList.size());

            applyFilters();
            Log.d("TransactionsActivity", "Applied filters");

            calculateTotals();
            Log.d("TransactionsActivity", "Calculated totals");

            showLoading(false);
            Log.d("TransactionsActivity", "=== Finished loadTransactions ===");
        }, 300);
    }

    private void applyFilters() {
        filteredTransactionList.clear();

        for (Transaction transaction : transactionList) {
            boolean matchesCategory = selectedCategoryIds.isEmpty() ||
                    selectedCategoryIds.contains(transaction.getCategoryId());

            boolean matchesSearch = searchQuery.isEmpty() ||
                    transaction.getCategoryName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    (transaction.getNote() != null &&
                            transaction.getNote().toLowerCase().contains(searchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredTransactionList.add(transaction);
            }
        }

        updateUI();
    }

    private List<Category> loadCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.TABLE_CATEGORIES,
                    null, null, null, null, null,
                    DatabaseHelper.COLUMN_CATEGORY_NAME + " ASC");

            if (cursor.moveToFirst()) {
                do {
                    Category category = new Category();
                    category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                    category.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_NAME)));
                    category.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_TYPE)));
                    category.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ICON)));
                    category.setColor(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_COLOR)));
                    categories.add(category);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return categories;
    }

    private Transaction createTransactionFromCursor(Cursor cursor) {
        Transaction transaction = new Transaction();

        transaction.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
        transaction.setCategoryId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ID)));
        transaction.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE)));
        transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
        transaction.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD)));
        transaction.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));

        // Category information
        transaction.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_NAME)));
        transaction.setCategoryIcon(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ICON)));
        transaction.setCategoryColor(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_COLOR)));

        return transaction;
    }

    private String getDateCondition() {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dbDateFormat.format(new Date());

        if (isCustomDateRange && customStartDate != null && customEndDate != null) {
            return "DATE(t." + DatabaseHelper.COLUMN_DATE + ") BETWEEN DATE('" +
                    customStartDate + "') AND DATE('" + customEndDate + "')";
        }

        switch (currentPeriod) {
            case "today":
                return "DATE(t." + DatabaseHelper.COLUMN_DATE + ") = DATE('" + today + "')";
            case "week":
                // Get date 7 days ago
                Calendar weekAgo = Calendar.getInstance();
                weekAgo.add(Calendar.DAY_OF_MONTH, -7);
                String weekAgoStr = dbDateFormat.format(weekAgo.getTime());
                return "DATE(t." + DatabaseHelper.COLUMN_DATE + ") >= DATE('" + weekAgoStr + "')";
            case "month":
                // Get current month start
                Calendar monthStart = Calendar.getInstance();
                monthStart.set(Calendar.DAY_OF_MONTH, 1);
                String monthStartStr = dbDateFormat.format(monthStart.getTime());
                return "DATE(t." + DatabaseHelper.COLUMN_DATE + ") >= DATE('" + monthStartStr + "')";
            case "all":
            default:
                return "1=1"; // All transactions
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            boolean transactionAdded = data.getBooleanExtra("transaction_added", false);
            if (transactionAdded) {
                Log.d("Transactions", "Transaction added, refreshing data...");
                loadTransactions();
            }
        }
    }


    private void showLoading(boolean show) {
        if (show) {
            layoutLoading.setVisibility(View.VISIBLE);
            recyclerViewTransactions.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        } else {
            layoutLoading.setVisibility(View.GONE);
            updateUI();
        }
    }

    private void updateUI() {
        if (filteredTransactionList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewTransactions.setVisibility(View.GONE);
            startEmptyStateAnimation();
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewTransactions.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    private void startEmptyStateAnimation() {
        ivEmptyIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void calculateTotals() {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction transaction : filteredTransactionList) {
            if ("income".equals(transaction.getType())) {
                totalIncome += transaction.getAmount();
            } else if ("expense".equals(transaction.getType())) {
                totalExpense += transaction.getAmount();
            }
        }

        // Update UI with formatted amounts
        updateTotalDisplay(tvTotalIncome, totalIncome, true);
        updateTotalDisplay(tvTotalExpense, totalExpense, false);

        // Calculate and display net balance
        double netBalance = totalIncome - totalExpense;
        updateNetBalanceDisplay(netBalance);
    }

    private void updateTotalDisplay(TextView textView, double amount, boolean isIncome) {
        String formattedAmount = currencyFormatter.format(amount) + "₫";
        textView.setText(formattedAmount);

        // Add animation for amount changes
        textView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.number_change));
    }

    private void updateNetBalanceDisplay(double netBalance) {
        String formattedBalance = currencyFormatter.format(Math.abs(netBalance)) + "₫";
        if (netBalance >= 0) {
            tvNetBalance.setText("+" + formattedBalance);
            tvNetBalance.setTextColor(getResources().getColor(R.color.success));
        } else {
            tvNetBalance.setText("-" + formattedBalance);
            tvNetBalance.setTextColor(getResources().getColor(R.color.error));
        }

        tvNetBalance.startAnimation(AnimationUtils.loadAnimation(this, R.anim.number_change));
    }

    private void openTransactionDetails(Transaction transaction) {
        try {
            Intent intent = new Intent(this, TransactionDetailsActivity.class);
            intent.putExtra("transaction_id", transaction.getId());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Chi tiết giao dịch: " + transaction.getCategoryName(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showTransactionOptions(Transaction transaction) {
        String[] options = {"Xem chi tiết", "Chỉnh sửa", "Xóa"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(transaction.getCategoryName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openTransactionDetails(transaction);
                            break;
                        case 1:
                            editTransaction(transaction);
                            break;
                        case 2:
                            deleteTransaction(transaction);
                            break;
                    }
                })
                .show();
    }

    private void editTransaction(Transaction transaction) {
        try {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("transaction_id", transaction.getId());
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Chức năng chỉnh sửa đang được phát triển",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTransaction(Transaction transaction) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    performDeleteTransaction(transaction);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performDeleteTransaction(Transaction transaction) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            int result = db.delete(DatabaseHelper.TABLE_TRANSACTIONS,
                    DatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(transaction.getId())});

            if (result > 0) {
                Toast.makeText(this, "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                loadTransactions(); // Reload data
            } else {
                Toast.makeText(this, "Không thể xóa giao dịch", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi xóa giao dịch", Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null) db.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadTransactions();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    private void setupOnBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFabMenuOpen) {
                    closeFabMenu();
                } else if (isSearchVisible) {
                    toggleSearch();
                } else {
                    // Gọi hành vi back mặc định
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    // ItemDecoration for spacing
    private static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int verticalSpaceHeight;

        public SpaceItemDecoration(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.bottom = verticalSpaceHeight;
        }
    }

    private void debugUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MoneyMasterPrefs", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);
        String username = sharedPreferences.getString("username", "unknown");

        Log.d("TransactionsActivity", "Current userId: " + userId + ", username: " + username);

        // Debug: Kiểm tra tất cả transactions trong database (không filter user)
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*), " + DatabaseHelper.COLUMN_USER_ID +
                " FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                " GROUP BY " + DatabaseHelper.COLUMN_USER_ID, null);

        Log.d("TransactionsActivity", "=== All transactions by user ===");
        if (cursor.moveToFirst()) {
            do {
                int count = cursor.getInt(0);
                int dbUserId = cursor.getInt(1);
                Log.d("TransactionsActivity", "UserId " + dbUserId + ": " + count + " transactions");
            } while (cursor.moveToNext());
        } else {
            Log.d("TransactionsActivity", "No transactions found in database");
        }
        cursor.close();

        // Debug: Kiểm tra transactions cho userId hiện tại
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            Log.d("TransactionsActivity", "Found " + count + " transactions for current userId " + userId);
        }
        cursor.close();
        db.close();
    }


}