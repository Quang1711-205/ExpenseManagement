package adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

import models.CategoryAnalytics;
import com.example.expensemanagement.R;

public class TopCategoryAdapter extends RecyclerView.Adapter<TopCategoryAdapter.CategoryViewHolder> {

    private List<CategoryAnalytics> categories;
    private DecimalFormat currencyFormatter;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryAnalytics category);
    }

    public TopCategoryAdapter(List<CategoryAnalytics> categories) {
        this.categories = categories;
        this.currencyFormatter = new DecimalFormat("#,###");
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryAnalytics category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<CategoryAnalytics> newCategories) {
        this.categories.clear();
        this.categories.addAll(newCategories);
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryIcon;
        private TextView tvCategoryName;
        private TextView tvCategoryAmount;
        private TextView tvCategoryPercentage;
        private ProgressBar progressCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvCategoryPercentage = itemView.findViewById(R.id.tvCategoryPercentage);
            progressCategory = itemView.findViewById(R.id.progressCategory);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCategoryClick(categories.get(getAdapterPosition()));
                }
            });
        }

        public void bind(CategoryAnalytics category) {
            // Set icon
            tvCategoryIcon.setText(category.getIcon());

            // Create circular background for icon
            GradientDrawable iconBackground = new GradientDrawable();
            iconBackground.setShape(GradientDrawable.OVAL);
            try {
                iconBackground.setColor(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                iconBackground.setColor(Color.parseColor("#2196F3"));
            }
            tvCategoryIcon.setBackground(iconBackground);

            // Set category name
            tvCategoryName.setText(category.getName());

            // Set amount with formatted currency
            String formattedAmount = currencyFormatter.format(category.getAmount()) + " đ";
            tvCategoryAmount.setText(formattedAmount);

            // Set percentage
            String formattedPercentage = String.format("%.1f%%", category.getPercentage());
            tvCategoryPercentage.setText(formattedPercentage);

            // Set progress bar
            int progress = (int) Math.min(category.getPercentage(), 100);
            progressCategory.setProgress(progress);

            // Set progress bar color based on category color
            try {
                int color = Color.parseColor(category.getColor());
                progressCategory.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            } catch (Exception e) {
                // Use default color if parsing fails
                int defaultColor = Color.parseColor("#2196F3");
                progressCategory.getProgressDrawable().setColorFilter(defaultColor, android.graphics.PorterDuff.Mode.SRC_IN);
            }

            // Set amount text color based on category color
            try {
                tvCategoryAmount.setTextColor(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                tvCategoryAmount.setTextColor(Color.parseColor("#F44336"));
            }

            // Add ranking indicator
            int position = getAdapterPosition() + 1;
            String rankingIcon = getRankingIcon(position);
            tvCategoryName.setText(rankingIcon + " " + category.getName());
        }

        private String getRankingIcon(int position) {
            switch (position) {
                case 1: return "🥇";
                case 2: return "🥈";
                case 3: return "🥉";
                case 4: return "4️⃣";
                case 5: return "5️⃣";
                default: return String.valueOf(position);
            }
        }
    }
}