// 📂 CategoryManagementActivity.java - Complete Category Management System
package com.example.expensemanagement;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 📂 CHUYÊN SÂU: Category Management System - Complete Version
 * Features:
 * - ✅ Add/Edit/Delete categories
 * - ✅ Income/Expense category tabs
 * - ✅ Color & Icon picker
 * - ✅ User-specific categories
 * - ✅ Usage statistics
 * - ✅ Default category protection
 * - ✅ Search functionality
 */
public class CategoryManagementActivity extends AppCompatActivity {

    private static final String TAG = "CategoryManagement";
    private static final String PREFS_NAME = "ExpenseManagementPrefs";
    private static final String KEY_USER_ID = "user_id";

    // UI Components
    private TabLayout tabLayout;
    private RecyclerView recyclerViewCategories;
    private FloatingActionButton fabAddCategory;
    private EditText editTextSearch;
    private TextView tvTotalCategories, tvActiveCategories;
    private LinearLayout layoutEmpty, layoutStats;

    // Data
    private DatabaseHelper dbHelper;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    private List<Category> filteredCategories;
    private String currentUserId;
    private String currentType = "expense"; // default to expense
    private SharedPreferences sharedPreferences;

    // Color palette for categories
    private final String[] colorPalette = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
            "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548", "#9E9E9E",
            "#607D8B", "#000000"
    };

    // Icon options for categories
    private final String[] iconOptions = {
            // Food & Dining
            "🍔", "🍕", "🍜", "☕", "🍺", "🥗", "🍰", "🍎",
            // Transportation
            "🚗", "🚌", "🚇", "✈️", "⛽", "🚖", "🚲", "🛵",
            // Shopping
            "🛍️", "👕", "👟", "💄", "📱", "💻", "🎮", "📚",
            // Entertainment
            "🎬", "🎵", "🎭", "🎪", "🎮", "🎲", "🎸", "📺",
            // Health & Fitness
            "🏥", "💊", "🏃", "⚽", "🏋️", "🧘", "🦷", "👓",
            // Bills & Utilities
            "📄", "💡", "💧", "📞", "📶", "🏠", "🔧", "🔌",
            // Income
            "💰", "💵", "💎", "📈", "💼", "🎁", "🏆", "⭐",
            // Other
            "📦", "❓", "🎯", "🔥", "💝", "🌟", "🚀", "🎊"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        initializeUserSession();
        initializeViews();
        setupData();
        loadCategories();
        setupEventListeners();
    }

    /**
     * Initialize user session
     */
    private void initializeUserSession() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get user ID from intent or SharedPreferences
        currentUserId = getIntent().getStringExtra("user_id");
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = sharedPreferences.getString(KEY_USER_ID, null);
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Current User ID: " + currentUserId);
    }

    /**
     * Initialize all views
     */
    private void initializeViews() {
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📂 Quản lý danh mục");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Main views
        tabLayout = findViewById(R.id.tab_layout_category_type);
        recyclerViewCategories = findViewById(R.id.recycler_view_categories);
        fabAddCategory = findViewById(R.id.fab_add_category);
        editTextSearch = findViewById(R.id.edit_text_search);
        tvTotalCategories = findViewById(R.id.tv_total_categories);
        tvActiveCategories = findViewById(R.id.tv_active_categories);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutStats = findViewById(R.id.layout_stats);

        // Setup tabs
        setupTabs();

        // Setup RecyclerView
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter();
        recyclerViewCategories.setAdapter(categoryAdapter);
    }

    /**
     * Setup category type tabs
     */
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Chi tiêu").setIcon(android.R.drawable.stat_sys_warning));
        tabLayout.addTab(tabLayout.newTab().setText("Thu nhập").setIcon(android.R.drawable.stat_sys_upload_done));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition() == 0 ? "expense" : "income";
                filterCategories();
                updateStats();
                Log.d(TAG, "Tab selected: " + currentType);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Setup data structures
     */
    private void setupData() {
        dbHelper = new DatabaseHelper(this);
        categories = new ArrayList<>();
        filteredCategories = new ArrayList<>();
    }

    /**
     * Load categories from database
     */
    private void loadCategories() {
        categories.clear();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Load both user-specific and default categories
            String query = "SELECT * FROM " + DatabaseHelper.TABLE_CATEGORIES +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? OR " + DatabaseHelper.COLUMN_USER_ID + " IS NULL" +
                    " ORDER BY " + DatabaseHelper.COLUMN_CATEGORY_TYPE + ", " + DatabaseHelper.COLUMN_NAME;

            Cursor cursor = db.rawQuery(query, new String[]{currentUserId});

            if (cursor.moveToFirst()) {
                do {
                    Category category = new Category();
                    category.setId(cursor.getInt(getColumnIndex(cursor, DatabaseHelper.COLUMN_ID)));
                    category.setName(cursor.getString(getColumnIndex(cursor, DatabaseHelper.COLUMN_NAME)));
                    category.setType(cursor.getString(getColumnIndex(cursor, DatabaseHelper.COLUMN_CATEGORY_TYPE)));
                    category.setIcon(cursor.getString(getColumnIndex(cursor, DatabaseHelper.COLUMN_CATEGORY_ICON)));
                    category.setColor(cursor.getString(getColumnIndex(cursor, DatabaseHelper.COLUMN_CATEGORY_COLOR)));

                    String userIdFromDb = cursor.getString(getColumnIndex(cursor, DatabaseHelper.COLUMN_USER_ID));
                    category.setUserId(userIdFromDb);
                    category.setIsDefault(userIdFromDb == null); // Default categories have null user_id

                    categories.add(category);
                } while (cursor.moveToNext());
            }
            cursor.close();

            Log.d(TAG, "Loaded " + categories.size() + " categories");

        } catch (Exception e) {
            Log.e(TAG, "Error loading categories: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        filterCategories();
        updateStats();
    }

    /**
     * Filter categories based on current type and search query
     */
    private void filterCategories() {
        filteredCategories.clear();

        String searchQuery = editTextSearch.getText().toString().toLowerCase().trim();

        for (Category category : categories) {
            if (category.getType().equals(currentType)) {
                if (searchQuery.isEmpty() ||
                        category.getName().toLowerCase().contains(searchQuery)) {
                    filteredCategories.add(category);
                }
            }
        }

        // Update UI
        categoryAdapter.notifyDataSetChanged();

        // Show/hide empty state
        if (filteredCategories.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewCategories.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewCategories.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Filtered to " + filteredCategories.size() + " categories for type: " + currentType);
    }

    /**
     * Update category statistics
     */
    private void updateStats() {
        int totalCount = 0;
        int activeCount = 0;

        for (Category category : categories) {
            if (category.getType().equals(currentType)) {
                totalCount++;
                if (!category.isDefault() || hasTransactions(category.getId())) {
                    activeCount++;
                }
            }
        }

        tvTotalCategories.setText("Tổng: " + totalCount);
        tvActiveCategories.setText("Đang dùng: " + activeCount);
    }

    /**
     * Check if category has transactions
     */
    private boolean hasTransactions(int categoryId) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_USER_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(categoryId), currentUserId});

            boolean hasTransactions = false;
            if (cursor.moveToFirst()) {
                hasTransactions = cursor.getInt(0) > 0;
            }
            cursor.close();

            return hasTransactions;
        } catch (Exception e) {
            Log.e(TAG, "Error checking transactions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Setup event listeners
     */
    private void setupEventListeners() {
        fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Show add category dialog
     */
    private void showAddCategoryDialog() {
        showCategoryDialog(null, false);
    }

    /**
     * Show category dialog for add/edit
     */
    private void showCategoryDialog(Category categoryToEdit, boolean isEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);

        EditText editName = dialogView.findViewById(R.id.edit_category_name);
        RadioGroup radioType = dialogView.findViewById(R.id.radio_group_type);
        RadioButton radioExpense = dialogView.findViewById(R.id.radio_expense);
        RadioButton radioIncome = dialogView.findViewById(R.id.radio_income);
        RecyclerView recyclerIcons = dialogView.findViewById(R.id.recycler_icons);
        RecyclerView recyclerColors = dialogView.findViewById(R.id.recycler_colors);

        // Icon adapter
        IconAdapter iconAdapter = new IconAdapter();
        recyclerIcons.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 8));
        recyclerIcons.setAdapter(iconAdapter);

        // Color adapter
        ColorAdapter colorAdapter = new ColorAdapter();
        recyclerColors.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 10));
        recyclerColors.setAdapter(colorAdapter);

        // Pre-fill data if editing
        if (isEdit && categoryToEdit != null) {
            editName.setText(categoryToEdit.getName());
            if ("income".equals(categoryToEdit.getType())) {
                radioIncome.setChecked(true);
            } else {
                radioExpense.setChecked(true);
            }
            iconAdapter.setSelectedIcon(categoryToEdit.getIcon());
            colorAdapter.setSelectedColor(categoryToEdit.getColor());
        } else {
            // Set default type based on current tab
            if ("income".equals(currentType)) {
                radioIncome.setChecked(true);
            } else {
                radioExpense.setChecked(true);
            }
            iconAdapter.setSelectedIcon("📦");
            colorAdapter.setSelectedColor("#2196F3");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "✏️ Sửa danh mục" : "➕ Thêm danh mục mới")
                .setView(dialogView)
                .setPositiveButton(isEdit ? "Cập nhật" : "Thêm", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String name = editName.getText().toString().trim();
                String type = radioIncome.isChecked() ? "income" : "expense";
                String icon = iconAdapter.getSelectedIcon();
                String color = colorAdapter.getSelectedColor();

                if (validateCategoryInput(name, icon, color)) {
                    if (isEdit) {
                        updateCategory(categoryToEdit.getId(), name, type, icon, color);
                    } else {
                        addNewCategory(name, type, icon, color);
                    }
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    /**
     * Validate category input
     */
    private boolean validateCategoryInput(String name, String icon, String color) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (name.length() > 50) {
            Toast.makeText(this, "Tên danh mục không được quá 50 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(icon)) {
            Toast.makeText(this, "Vui lòng chọn biểu tượng", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(color)) {
            Toast.makeText(this, "Vui lòng chọn màu sắc", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Add new category to database
     */
    private void addNewCategory(String name, String type, String icon, String color) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_NAME, name);
            values.put(DatabaseHelper.COLUMN_CATEGORY_TYPE, type);
            values.put(DatabaseHelper.COLUMN_CATEGORY_ICON, icon);
            values.put(DatabaseHelper.COLUMN_CATEGORY_COLOR, color);
            values.put(DatabaseHelper.COLUMN_USER_ID, currentUserId);

            long result = db.insert(DatabaseHelper.TABLE_CATEGORIES, null, values);

            if (result != -1) {
                Toast.makeText(this, "✅ Đã thêm danh mục: " + name, Toast.LENGTH_SHORT).show();
                loadCategories(); // Reload to show new category
                Log.d(TAG, "Category added successfully: " + name);
            } else {
                Toast.makeText(this, "❌ Lỗi thêm danh mục", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding category: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update existing category
     */
    private void updateCategory(int categoryId, String name, String type, String icon, String color) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_NAME, name);
            values.put(DatabaseHelper.COLUMN_CATEGORY_TYPE, type);
            values.put(DatabaseHelper.COLUMN_CATEGORY_ICON, icon);
            values.put(DatabaseHelper.COLUMN_CATEGORY_COLOR, color);

            String whereClause = DatabaseHelper.COLUMN_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] whereArgs = {String.valueOf(categoryId), currentUserId};

            int rowsUpdated = db.update(DatabaseHelper.TABLE_CATEGORIES, values, whereClause, whereArgs);

            if (rowsUpdated > 0) {
                Toast.makeText(this, "✅ Đã cập nhật danh mục: " + name, Toast.LENGTH_SHORT).show();
                loadCategories();
                Log.d(TAG, "Category updated successfully: " + name);
            } else {
                Toast.makeText(this, "❌ Không thể cập nhật danh mục", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating category: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Delete category with confirmation
     */
    private void deleteCategory(Category category) {
        if (category.isDefault()) {
            Toast.makeText(this, "❌ Không thể xóa danh mục mặc định", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if category has transactions
        if (hasTransactions(category.getId())) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Xác nhận xóa")
                    .setMessage("Danh mục này đã có giao dịch. Việc xóa sẽ ảnh hưởng đến báo cáo. Bạn có chắc chắn?")
                    .setPositiveButton("Xóa", (d, w) -> performDelete(category))
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("🗑️ Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa danh mục '" + category.getName() + "'?")
                    .setPositiveButton("Xóa", (d, w) -> performDelete(category))
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    /**
     * Perform actual category deletion
     */
    private void performDelete(Category category) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String whereClause = DatabaseHelper.COLUMN_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] whereArgs = {String.valueOf(category.getId()), currentUserId};

            int rowsDeleted = db.delete(DatabaseHelper.TABLE_CATEGORIES, whereClause, whereArgs);

            if (rowsDeleted > 0) {
                Toast.makeText(this, "🗑️ Đã xóa danh mục: " + category.getName(), Toast.LENGTH_SHORT).show();
                loadCategories();
                Log.d(TAG, "Category deleted successfully: " + category.getName());
            } else {
                Toast.makeText(this, "❌ Không thể xóa danh mục", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting category: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Utility method to get column index safely
     */
    private int getColumnIndex(Cursor cursor, String columnName) {
        try {
            return cursor.getColumnIndex(columnName);
        } catch (Exception e) {
            Log.w(TAG, "Column not found: " + columnName);
            return -1;
        }
    }

    // ===== ADAPTERS =====

    /**
     * Category RecyclerView Adapter
     */
    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

        @Override
        public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_management, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CategoryViewHolder holder, int position) {
            Category category = filteredCategories.get(position);
            holder.bind(category);
        }

        @Override
        public int getItemCount() {
            return filteredCategories.size();
        }

        class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvType, tvUsage;
            View colorIndicator;
            Button btnEdit, btnDelete;

            CategoryViewHolder(View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tv_category_icon);
                tvName = itemView.findViewById(R.id.tv_category_name);
                tvType = itemView.findViewById(R.id.tv_category_type);
                tvUsage = itemView.findViewById(R.id.tv_category_usage);
                colorIndicator = itemView.findViewById(R.id.view_color_indicator);
                btnEdit = itemView.findViewById(R.id.btn_edit_category);
                btnDelete = itemView.findViewById(R.id.btn_delete_category);
            }

            void bind(Category category) {
                tvIcon.setText(category.getIcon());
                tvName.setText(category.getName());
                tvType.setText(category.getType().equals("income") ? "Thu nhập" : "Chi tiêu");

                // Set color
                try {
                    colorIndicator.setBackgroundColor(Color.parseColor(category.getColor()));
                } catch (Exception e) {
                    colorIndicator.setBackgroundColor(Color.GRAY);
                }

                // Show usage information
                if (category.isDefault()) {
                    tvUsage.setText("Mặc định");
                    tvUsage.setTextColor(Color.GRAY);
                    btnDelete.setVisibility(View.GONE);
                } else {
                    boolean hasTransactions = hasTransactions(category.getId());
                    tvUsage.setText(hasTransactions ? "Đang sử dụng" : "Chưa sử dụng");
                    tvUsage.setTextColor(hasTransactions ? Color.GREEN : Color.GRAY);
                    btnDelete.setVisibility(View.VISIBLE);
                }

                // Event listeners
                btnEdit.setOnClickListener(v -> {
                    if (category.isDefault()) {
                        Toast.makeText(CategoryManagementActivity.this,
                                "❌ Không thể chỉnh sửa danh mục mặc định", Toast.LENGTH_SHORT).show();
                    } else {
                        showCategoryDialog(category, true);
                    }
                });

                btnDelete.setOnClickListener(v -> deleteCategory(category));

                itemView.setOnClickListener(v -> {
                    // Show category details
                    showCategoryDetails(category);
                });
            }
        }
    }

    /**
     * Icon picker adapter
     */
    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
        private String selectedIcon = "📦";

        public void setSelectedIcon(String icon) {
            this.selectedIcon = icon;
            notifyDataSetChanged();
        }

        public String getSelectedIcon() {
            return selectedIcon;
        }

        @Override
        public IconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setTextSize(24);
            textView.setPadding(16, 16, 16, 16);
            textView.setGravity(android.view.Gravity.CENTER);

            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(120, 120);
            textView.setLayoutParams(layoutParams);

            return new IconViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(IconViewHolder holder, int position) {
            String icon = iconOptions[position];
            holder.bind(icon, icon.equals(selectedIcon));
        }

        @Override
        public int getItemCount() {
            return iconOptions.length;
        }

        class IconViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            IconViewHolder(TextView textView) {
                super(textView);
                this.textView = textView;
            }

            void bind(String icon, boolean isSelected) {
                textView.setText(icon);
                textView.setBackgroundColor(isSelected ? Color.LTGRAY : Color.TRANSPARENT);

                textView.setOnClickListener(v -> {
                    selectedIcon = icon;
                    notifyDataSetChanged();
                });
            }
        }
    }

    /**
     * Color picker adapter
     */
    private class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {
        private String selectedColor = "#2196F3";

        public void setSelectedColor(String color) {
            this.selectedColor = color;
            notifyDataSetChanged();
        }

        public String getSelectedColor() {
            return selectedColor;
        }

        @Override
        public ColorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = new View(parent.getContext());
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(80, 80);
            layoutParams.setMargins(8, 8, 8, 8);
            view.setLayoutParams(layoutParams);

            return new ColorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ColorViewHolder holder, int position) {
            String color = colorPalette[position];
            holder.bind(color, color.equals(selectedColor));
        }

        @Override
        public int getItemCount() {
            return colorPalette.length;
        }

        class ColorViewHolder extends RecyclerView.ViewHolder {
            View colorView;

            ColorViewHolder(View view) {
                super(view);
                this.colorView = view;
            }

            void bind(String color, boolean isSelected) {
                try {
                    colorView.setBackgroundColor(Color.parseColor(color));
                    if (isSelected) {
                        colorView.setScaleX(1.2f);
                        colorView.setScaleY(1.2f);
                    } else {
                        colorView.setScaleX(1.0f);
                        colorView.setScaleY(1.0f);
                    }
                } catch (Exception e) {
                    colorView.setBackgroundColor(Color.GRAY);
                }

                colorView.setOnClickListener(v -> {
                    selectedColor = color;
                    notifyDataSetChanged();
                });
            }
        }
    }

    /**
     * Show category details dialog
     */
    private void showCategoryDetails(Category category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_details, null);

        TextView tvIcon = dialogView.findViewById(R.id.tv_detail_icon);
        TextView tvName = dialogView.findViewById(R.id.tv_detail_name);
        TextView tvType = dialogView.findViewById(R.id.tv_detail_type);
        TextView tvUsage = dialogView.findViewById(R.id.tv_detail_usage);
        TextView tvTransactionCount = dialogView.findViewById(R.id.tv_transaction_count);
        TextView tvTotalAmount = dialogView.findViewById(R.id.tv_total_amount);
        View colorIndicator = dialogView.findViewById(R.id.view_detail_color);

        // Fill data
        tvIcon.setText(category.getIcon());
        tvName.setText(category.getName());
        tvType.setText(category.getType().equals("income") ? "Thu nhập" : "Chi tiêu");

        try {
            colorIndicator.setBackgroundColor(Color.parseColor(category.getColor()));
        } catch (Exception e) {
            colorIndicator.setBackgroundColor(Color.GRAY);
        }

        // Get usage statistics
        CategoryStats stats = getCategoryStats(category.getId());

        if (category.isDefault()) {
            tvUsage.setText("Danh mục mặc định của hệ thống");
        } else {
            tvUsage.setText("Danh mục tự tạo");
        }

        tvTransactionCount.setText("Số giao dịch: " + stats.transactionCount);
        tvTotalAmount.setText("Tổng tiền: " + formatCurrency(stats.totalAmount));

        new AlertDialog.Builder(this)
                .setTitle("📊 Chi tiết danh mục")
                .setView(dialogView)
                .setPositiveButton("Đóng", null)
                .setNeutralButton(category.isDefault() ? null : "✏️ Chỉnh sửa", (d, w) -> {
                    if (!category.isDefault()) {
                        showCategoryDialog(category, true);
                    }
                })
                .show();
    }

    /**
     * Get category usage statistics
     */
    private CategoryStats getCategoryStats(int categoryId) {
        CategoryStats stats = new CategoryStats();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT COUNT(*) as count, COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) as total " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_USER_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(categoryId), currentUserId});

            if (cursor.moveToFirst()) {
                stats.transactionCount = cursor.getInt(0);
                stats.totalAmount = cursor.getDouble(1);
            }
            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error getting category stats: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Format currency for display
     */
    private String formatCurrency(double amount) {
        return String.format("%,.0f đ", amount);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories(); // Refresh data when returning to activity
    }

    // ===== DATA MODELS =====

    /**
     * Category data model
     */
    public static class Category {
        private int id;
        private String name;
        private String type;
        private String icon;
        private String color;
        private String userId;
        private boolean isDefault;

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public boolean isDefault() { return isDefault; }
        public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }

        @Override
        public String toString() {
            return "Category{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", icon='" + icon + '\'' +
                    ", color='" + color + '\'' +
                    ", isDefault=" + isDefault +
                    '}';
        }
    }

    /**
     * Category statistics data model
     */
    private static class CategoryStats {
        int transactionCount = 0;
        double totalAmount = 0.0;
    }
}