package com.talmacel.iosif.riverisland.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.talmacel.iosif.riverisland.R;
import com.talmacel.iosif.riverisland.activities.ProductInfoActivity;
import com.talmacel.iosif.riverisland.models.ProductItemData;
import com.talmacel.iosif.riverisland.utils.ImageLoader;

/**
 * Created by Iosif on 17/03/2018.
 */

public class ProductsRecyclerAdapter extends RecyclerView.Adapter<ProductsRecyclerAdapter.ItemViewHolder> {
    private ProductItemData[] mItems;
    private Context mContext;

    public ProductsRecyclerAdapter(Context context, ProductItemData[] items) {
        mItems = items;
        mContext = context;
    }

    public void setItems(ProductItemData[] items) {
        mItems = items;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_list_item, parent, false);

        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        ProductItemData currentData = mItems[position];
        holder.mTitleView.setText(currentData.name);
        holder.mCostView.setText("€" + currentData.costEUR);

        if(currentData.category != null)
            holder.mDescView.setText(currentData.category.split(",")[0]);

        String imageUrl = currentData.altImage != null && !currentData.altImage.isEmpty() ? currentData.altImage : currentData.allImages[0];
        holder.imageRequestTime = System.currentTimeMillis();
        ImageLoader.instance.load(imageUrl, position < 20, holder.listener);
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
        super.onViewRecycled(holder);
        ImageLoader.instance.removeListener(holder.listener);
        holder.mThumbView.setBackgroundColor(Color.rgb(232,232,232));
        holder.mThumbView.setImageDrawable(null);
        holder.imageRequestTime = 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.length;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView mTitleView;
        TextView mDescView;
        TextView mCostView;
        ImageView mThumbView;
        ImageLoader.OnLoadListener listener;
        ColorDrawable colorDrawable;
        BitmapDrawable bitmapDrawable;
        long imageRequestTime;
        ItemViewHolder(View v) {
            super(v);
            mTitleView = v.findViewById(R.id.title);
            mDescView = v.findViewById(R.id.desc);
            mCostView = v.findViewById(R.id.cost);
            mThumbView = v.findViewById(R.id.image);
            mThumbView = v.findViewById(R.id.image);
            colorDrawable = new ColorDrawable(Color.rgb(232,232,232));
            listener = new ImageLoader.OnLoadListener() {
                @Override
                public void OnLoad(Bitmap photo) {
                    if(imageRequestTime == 0 || System.currentTimeMillis() - imageRequestTime > 80){
                        bitmapDrawable = new BitmapDrawable(photo);
                        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{colorDrawable, bitmapDrawable});
                        mThumbView.setImageDrawable(transitionDrawable);
                        transitionDrawable.startTransition(300);
                    }else{
                        bitmapDrawable = new BitmapDrawable(photo);
                        mThumbView.setImageDrawable(bitmapDrawable);
                    }
                    mThumbView.setBackgroundColor(0);
                }
                @Override
                public void OnFailed() {}
            };

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ProductInfoActivity.class);
                    intent.putExtra("itemData", mItems[getAdapterPosition()]);
                    mContext.startActivity(intent);
                }
            });

        }
    }
}