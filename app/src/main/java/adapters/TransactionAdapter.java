package adapters;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import com.google.android.material.card.MaterialCardView;

import models.Transaction;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private OnTransactionClickListener listener;
    private DecimalFormat currencyFormatter = new DecimalFormat("#,###,###");
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onTransactionLongClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);

        // Add entrance animation
        animateItemEntry(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private void animateItemEntry(View view, int position) {
        view.setAlpha(0f);
        view.setTranslationY(50f);

        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
                .setDuration(300)
                .start();

        ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                .setDuration(300)
                .start();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        // Chỉ khai báo những View thực sự tồn tại trong layout
        private LinearLayout layoutCategoryIcon;  // Sửa từ FrameLayout thành LinearLayout
        private TextView tvCategoryIcon;
        private TextView tvCategoryName;
        private TextView tvTransactionNote;
        private TextView tvTransactionDate;
        private TextView tvTransactionAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);

            // Chỉ findViewById những View thực sự tồn tại
            layoutCategoryIcon = itemView.findViewById(R.id.layoutCategoryIcon);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTransactionNote = itemView.findViewById(R.id.tvTransactionNote);
            tvTransactionDate = itemView.findViewById(R.id.tvTransactionDate);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);

            setupClickListeners();
            setupCardAnimation();
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    animateClick();
                    listener.onTransactionClick(transactions.get(pos));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    animateLongClick();
                    listener.onTransactionLongClick(transactions.get(pos));
                    return true;
                }
                return false;
            });
        }

        private void setupCardAnimation() {
            // Animation cơ bản cho itemView
            itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        animateCardPress(true);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        animateCardPress(false);
                        break;
                }
                return false;
            });
        }

        private void animateCardPress(boolean pressed) {
            float scale = pressed ? 0.98f : 1.0f;
            ObjectAnimator.ofFloat(itemView, "scaleX", scale).setDuration(100).start();
            ObjectAnimator.ofFloat(itemView, "scaleY", scale).setDuration(100).start();
        }

        private void animateClick() {
            ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0.7f, 1f)
                    .setDuration(150)
                    .start();
        }

        private void animateLongClick() {
            ObjectAnimator.ofFloat(itemView, "scaleX", 1f, 1.05f, 1f)
                    .setDuration(200)
                    .start();
            ObjectAnimator.ofFloat(itemView, "scaleY", 1f, 1.05f, 1f)
                    .setDuration(200)
                    .start();
        }

        public void bind(Transaction transaction) {
            // Set category icon and background color
            if (tvCategoryIcon != null) {
                tvCategoryIcon.setText(transaction.getCategoryIcon());
            }
            setupCategoryIconBackground(transaction);

            // Set category name
            if (tvCategoryName != null) {
                tvCategoryName.setText(transaction.getCategoryName());
            }

            // Set transaction note
            setupTransactionNote(transaction);

            // Set formatted date and time
            setupDateTime(transaction);

            // Set amount with proper formatting and color
            setupAmount(transaction);
        }

        private void setupCategoryIconBackground(Transaction transaction) {
            if (layoutCategoryIcon == null) return;

            try {
                String colorStr = transaction.getCategoryColor();
                if (colorStr != null && !colorStr.isEmpty()) {
                    int color = Color.parseColor(colorStr);
                    int lightColor = adjustColorOpacity(color, 0.15f);
                    layoutCategoryIcon.setBackgroundColor(lightColor);
                } else {
                    setDefaultIconBackground();
                }
            } catch (IllegalArgumentException e) {
                setDefaultIconBackground();
            }
        }

        private void setDefaultIconBackground() {
            if (layoutCategoryIcon != null) {
                // Sử dụng màu mặc định của system
                layoutCategoryIcon.setBackgroundColor(
                        itemView.getContext().getResources().getColor(android.R.color.holo_blue_light)
                );
            }
        }

        private void setupTransactionNote(Transaction transaction) {
            if (tvTransactionNote == null) return;

            String note = transaction.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvTransactionNote.setText(note);
                tvTransactionNote.setVisibility(View.VISIBLE);
            } else {
                tvTransactionNote.setVisibility(View.GONE);
            }
        }

        private void setupDateTime(Transaction transaction) {
            if (tvTransactionDate == null) return;

            String dateTimeString = formatDateTime(transaction.getDate(), transaction.getCreatedAt());
            tvTransactionDate.setText(dateTimeString);
        }

        private void setupAmount(Transaction transaction) {
            if (tvTransactionAmount == null) return;

            String formattedAmount = formatAmount(transaction.getAmount(), transaction.isIncome());
            tvTransactionAmount.setText(formattedAmount);

            // Sử dụng màu system để tránh lỗi resource not found
            int color = transaction.isIncome() ?
                    itemView.getContext().getResources().getColor(android.R.color.holo_green_dark) :
                    itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
            tvTransactionAmount.setTextColor(color);
        }

        private String formatAmount(double amount, boolean isIncome) {
            String prefix = isIncome ? "+" : "-";
            return prefix + currencyFormatter.format(amount) + "₫";
        }

        private String formatDateTime(String dateStr, String createdAtStr) {
            try {
                // Parse and format date
                Date date = inputDateFormat.parse(dateStr);
                String formattedDate = outputDateFormat.format(date);

                // Extract time from createdAt if available
                String timeStr = "";
                if (createdAtStr != null && !createdAtStr.isEmpty()) {
                    try {
                        Date fullDate = fullDateTimeFormat.parse(createdAtStr);
                        timeStr = " • " + timeFormat.format(fullDate);
                    } catch (ParseException e) {
                        // If time parsing fails, just show date
                        timeStr = "";
                    }
                }

                return formattedDate + timeStr;
            } catch (ParseException e) {
                // If date parsing fails, return original date string
                return dateStr;
            }
        }

        private int adjustColorOpacity(int color, float opacity) {
            int alpha = Math.round(255 * opacity);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            return Color.argb(alpha, red, green, blue);
        }
    }

    // Public methods for external updates
    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(0, transaction); // Add at beginning
        notifyItemInserted(0);
    }

    public void removeTransaction(int position) {
        if (position >= 0 && position < transactions.size()) {
            transactions.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, transactions.size());
        }
    }

    public void updateTransaction(int position, Transaction transaction) {
        if (position >= 0 && position < transactions.size()) {
            transactions.set(position, transaction);
            notifyItemChanged(position);
        }
    }

    public Transaction getTransaction(int position) {
        if (position >= 0 && position < transactions.size()) {
            return transactions.get(position);
        }
        return null;
    }

    public void animateItemRemoval(int position) {
        if (position >= 0 && position < transactions.size()) {
            View itemView = null; // Get the view if needed
            if (itemView != null) {
                ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0f)
                        .setDuration(250)
                        .start();
                ObjectAnimator.ofFloat(itemView, "translationX", 0f, itemView.getWidth())
                        .setDuration(250)
                        .start();
            }
        }
    }

    // Filter methods
    public void filter(String query) {
        // This method can be used for additional filtering if needed
        notifyDataSetChanged();
    }

    public void clearSelection() {
        notifyDataSetChanged();
    }
}