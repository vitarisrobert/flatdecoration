package flatdecoration.shop;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCartAdapter extends RecyclerView.Adapter<ShoppingCartAdapter.ViewHolder> implements Filterable {
    private ArrayList<Item> mShoppingItemData;
    private ArrayList<Item> mShoppingItemDataAll;
    private  Context mContext;
    private int lastPosition = -1;
    ShoppingCartAdapter(Context context, ArrayList<Item> itemsData){
        this.mShoppingItemData = itemsData;
        this.mShoppingItemDataAll = new ArrayList<>(itemsData);
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_cart, parent, false));
    }

    @Override
    public void onBindViewHolder(ShoppingCartAdapter.ViewHolder holder, int position) {
        Item currentItem = mShoppingItemData.get(position);

        holder.bindTo(currentItem);

        if(holder.getAdapterPosition() > lastPosition){
            Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
            holder.itemView.startAnimation(fadeIn);
        }
    }

    @Override
    public int getItemCount() {return mShoppingItemData.size();}

    @Override
    public Filter getFilter() {return shoppingFilter;}
    private Filter shoppingFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<Item> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0 ){
                results.count = mShoppingItemDataAll.size();
                results.values = mShoppingItemDataAll;
            }else{
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for(Item item : mShoppingItemDataAll) {
                    if(item.getName().toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                }
                results.count = filteredList.size();
                results.values = filteredList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {
            mShoppingItemData = (ArrayList) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mTitleText;
        private TextView mPriceText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitleText = itemView.findViewById(R.id.itemTitle);
            mPriceText = itemView.findViewById(R.id.price);
        }

        private List<Integer> getSpinnerValues() {
            List<Integer> values = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                values.add(i);
            }
            return values;
        }

        public void bindTo(Item currentItem) {
            mTitleText.setText(currentItem.getName());

            Spinner productNumberSpinner = itemView.findViewById(R.id.productNumberSpinner);
            TextView numberText = itemView.findViewById(R.id.number);
            mPriceText = itemView.findViewById(R.id.price);

            ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    getSpinnerValues()
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            productNumberSpinner.setAdapter(spinnerAdapter);

            FirebaseFirestore.getInstance()
                    .collection("Items")
                    .document(currentItem._getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long counter = documentSnapshot.getLong("productCounter");
                            int productCount = counter != null ? counter.intValue() : 1;

                            int spinnerPosition = spinnerAdapter.getPosition(productCount);
                            numberText.setText(String.valueOf(productCount));

                            if (spinnerPosition >= 0) {
                                productNumberSpinner.setSelection(spinnerPosition);
                            } else {
                                productNumberSpinner.setSelection(0);
                            }

                            int unitPrice = parsePriceSafely(currentItem.getPrice());
                            int totalPrice = unitPrice * productCount;
                            mPriceText.setText(totalPrice + " Ft");
                        }
                    });

            itemView.findViewById(R.id.updateCart).setOnClickListener(view -> {
                int selectedValue = (int) productNumberSpinner.getSelectedItem();

                FirebaseFirestore.getInstance()
                        .collection("Items")
                        .document(currentItem._getId())
                        .update("productCounter", selectedValue)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "Kosár frissítve", Toast.LENGTH_SHORT).show();

                            numberText.setText(String.valueOf(selectedValue));

                            int unitPrice = parsePriceSafely(currentItem.getPrice());
                            int totalPrice = unitPrice * selectedValue;
                            mPriceText.setText(totalPrice + " Ft");
                        });
            });

            itemView.findViewById(R.id.deleteCart).setOnClickListener(view -> {
                Animation slideOut = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.slide_out_left);

                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        ((CartActivity) mContext).deleteItem(currentItem);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });

                itemView.startAnimation(slideOut);
            });
        }
    }

    private int parsePriceSafely(String priceString) {
        if (priceString == null) return 0;

        try {
            String cleaned = priceString.replaceAll("[^0-9]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            Log.e("ShoppingCartAdapter", "Nem sikerült a price konvertálása: " + priceString, e);
            return 0;
        }
    }
}

