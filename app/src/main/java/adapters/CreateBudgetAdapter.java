package adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import models.BudgetPlan;
import com.example.expensemanagement.R;

import java.util.List;

public class CreateBudgetAdapter extends RecyclerView.Adapter<CreateBudgetAdapter.CategoryViewHolder> {

    private List<BudgetPlan.CategoryBudget> categoryBudgets;
    private OnBudgetChangeListener budgetChangeListener;
    private Context context;

    public interface OnBudgetChangeListener {
        void onBudgetChanged();
    }

    public CreateBudgetAdapter(List<BudgetPlan.CategoryBudget> categoryBudgets, OnBudgetChangeListener listener) {
        this.categoryBudgets = categoryBudgets;
        this.budgetChangeListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = createCategoryBudgetItemView(context);
        return new CategoryViewHolder(view);
    }

    // Create layout programmatically với thiết kế giống ảnh
    private View createCategoryBudgetItemView(Context context) {
        // Main container
        android.widget.LinearLayout container = new android.widget.LinearLayout(context);
        container.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT));
        container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        container.setPadding(48, 32, 48, 32);
        container.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Color indicator
        View colorIndicator = new View(context);
        colorIndicator.setTag("category_color_indicator");
        android.widget.LinearLayout.LayoutParams colorParams =
                new android.widget.LinearLayout.LayoutParams(12, 120); // 4dp x 40dp
        colorParams.setMarginEnd(36);
        colorIndicator.setLayoutParams(colorParams);
        colorIndicator.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
        container.addView(colorIndicator);

        // Icon với background tròn
        android.widget.FrameLayout iconFrame = new android.widget.FrameLayout(context);
        android.widget.LinearLayout.LayoutParams iconFrameParams =
                new android.widget.LinearLayout.LayoutParams(96, 96); // 32dp
        iconFrameParams.setMarginEnd(36);
        iconFrame.setLayoutParams(iconFrameParams);

        // Background tròn cho icon
        android.graphics.drawable.GradientDrawable iconBackground = new android.graphics.drawable.GradientDrawable();
        iconBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        iconBackground.setColor(android.graphics.Color.parseColor("#F5F5F5"));
        iconFrame.setBackground(iconBackground);

        TextView icon = new TextView(context);
        icon.setTag("tv_category_icon");
        android.widget.FrameLayout.LayoutParams iconParams =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        icon.setLayoutParams(iconParams);
        icon.setText("🍔");
        icon.setTextSize(18);
        icon.setGravity(android.view.Gravity.CENTER);
        iconFrame.addView(icon);
        container.addView(iconFrame);

        // Category name
        TextView name = new TextView(context);
        name.setTag("tv_category_name");
        android.widget.LinearLayout.LayoutParams nameParams =
                new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.weight = 1;
        nameParams.setMarginEnd(36);
        name.setLayoutParams(nameParams);
        name.setText("Category");
        name.setTextSize(16);
        name.setTextColor(android.graphics.Color.parseColor("#333333"));
        name.setMaxLines(2);
        container.addView(name);

        // Budget amount input với styling
        android.widget.LinearLayout inputContainer = new android.widget.LinearLayout(context);
        android.widget.LinearLayout.LayoutParams inputContainerParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        inputContainer.setLayoutParams(inputContainerParams);
        inputContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        inputContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);

        EditText budgetAmount = new EditText(context);
        budgetAmount.setTag("et_budget_amount");
        android.widget.LinearLayout.LayoutParams amountParams =
                new android.widget.LinearLayout.LayoutParams(300, 120); // 100dp x 40dp
        budgetAmount.setLayoutParams(amountParams);
        budgetAmount.setHint("0");
        budgetAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        budgetAmount.setTextSize(14);
        budgetAmount.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        budgetAmount.setPadding(24, 0, 24, 0);
        budgetAmount.setTextColor(android.graphics.Color.parseColor("#333333"));

        // Background cho EditText
        android.graphics.drawable.GradientDrawable editTextBackground = new android.graphics.drawable.GradientDrawable();
        editTextBackground.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        editTextBackground.setColor(android.graphics.Color.parseColor("#F8F9FA"));
        editTextBackground.setStroke(2, android.graphics.Color.parseColor("#E9ECEF"));
        editTextBackground.setCornerRadius(24);
        budgetAmount.setBackground(editTextBackground);

        inputContainer.addView(budgetAmount);

        // VND label
        TextView vndLabel = new TextView(context);
        android.widget.LinearLayout.LayoutParams vndParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        vndParams.setMarginStart(24);
        vndLabel.setLayoutParams(vndParams);
        vndLabel.setText("VND");
        vndLabel.setTextSize(12);
        vndLabel.setTextColor(android.graphics.Color.parseColor("#666666"));
        inputContainer.addView(vndLabel);

        // Remove button
        ImageButton removeButton = new ImageButton(context);
        removeButton.setTag("btn_remove");
        android.widget.LinearLayout.LayoutParams removeParams =
                new android.widget.LinearLayout.LayoutParams(72, 72); // 24dp
        removeParams.setMarginStart(24);
        removeButton.setLayoutParams(removeParams);
        removeButton.setImageResource(android.R.drawable.ic_delete); // Use system delete icon
        removeButton.setBackground(null);
        removeButton.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        removeButton.setColorFilter(android.graphics.Color.parseColor("#F44336"));
        inputContainer.addView(removeButton);

        container.addView(inputContainer);

        return container;
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        BudgetPlan.CategoryBudget category = categoryBudgets.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categoryBudgets.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName, tvCategoryIcon;
        private EditText etBudgetAmount;
        private View categoryColorIndicator;
        private ImageButton btnRemove;
        private TextWatcher textWatcher;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = (TextView) itemView.findViewWithTag("tv_category_name");
            tvCategoryIcon = (TextView) itemView.findViewWithTag("tv_category_icon");
            etBudgetAmount = (EditText) itemView.findViewWithTag("et_budget_amount");
            categoryColorIndicator = itemView.findViewWithTag("category_color_indicator");
            btnRemove = (ImageButton) itemView.findViewWithTag("btn_remove");

            // Setup remove button click listener
            btnRemove.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    categoryBudgets.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, categoryBudgets.size());

                    if (budgetChangeListener != null) {
                        budgetChangeListener.onBudgetChanged();
                    }
                }
            });

            // Create TextWatcher
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        BudgetPlan.CategoryBudget category = categoryBudgets.get(position);

                        try {
                            String amountText = s.toString().trim();
                            if (!amountText.isEmpty()) {
                                double amount = Double.parseDouble(amountText);
                                category.setAllocatedAmount(amount);
                            } else {
                                category.setAllocatedAmount(0);
                            }

                            if (budgetChangeListener != null) {
                                budgetChangeListener.onBudgetChanged();
                            }
                        } catch (NumberFormatException e) {
                            category.setAllocatedAmount(0);
                        }
                    }
                }
            };
        }

        public void bind(BudgetPlan.CategoryBudget category) {
            // Set category name
            tvCategoryName.setText(category.getCategoryName());

            // Set category icon
            String icon = category.getCategoryIcon();
            if (icon != null && !icon.isEmpty()) {
                tvCategoryIcon.setText(icon);
            } else {
                tvCategoryIcon.setText("📂");
            }

            // Set color indicator
            if (category.getCategoryColor() != null && !category.getCategoryColor().isEmpty()) {
                try {
                    int color = android.graphics.Color.parseColor(category.getCategoryColor());
                    categoryColorIndicator.setBackgroundColor(color);
                } catch (Exception e) {
                    categoryColorIndicator.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
                }
            } else {
                categoryColorIndicator.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
            }

            // Set budget amount without triggering listener
            etBudgetAmount.removeTextChangedListener(textWatcher);
            if (category.getAllocatedAmount() > 0) {
                etBudgetAmount.setText(String.valueOf((int) category.getAllocatedAmount()));
            } else {
                etBudgetAmount.setText("");
            }
            etBudgetAmount.addTextChangedListener(textWatcher);
        }
    }
}