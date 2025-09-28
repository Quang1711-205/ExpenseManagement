package com.example.expensemanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class NotificationsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvNotifications;
    private View layoutEmptyNotifications;
    private TextView tvEmptyMessage;
    private MaterialButton btnMarkAllRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        initializeComponents();
        setupToolbar();
        setupRecyclerView();
        setupEventListeners();
        loadNotifications();
    }

    private void initializeComponents() {
        toolbar = findViewById(R.id.toolbar);
        rvNotifications = findViewById(R.id.rv_notifications);
        layoutEmptyNotifications = findViewById(R.id.layout_empty_notifications);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        btnMarkAllRead = findViewById(R.id.btn_mark_all_read);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông báo");
        }
    }

    private void setupRecyclerView() {
        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupEventListeners() {
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        }
    }

    private void loadNotifications() {
        // For now, show empty state
        showEmptyState();
    }

    private void showEmptyState() {
        if (layoutEmptyNotifications != null) {
            layoutEmptyNotifications.setVisibility(View.VISIBLE);
        }
        if (rvNotifications != null) {
            rvNotifications.setVisibility(View.GONE);
        }
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText("Không có thông báo nào");
        }
    }

    private void markAllAsRead() {
        // Implementation for marking all notifications as read
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}