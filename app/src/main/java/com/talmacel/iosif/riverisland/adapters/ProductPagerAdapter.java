package com.talmacel.iosif.riverisland.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.talmacel.iosif.riverisland.utils.ImageLoader;

/**
 * Created by Iosif on 17/03/2018.
 */

public class ProductPagerAdapter extends PagerAdapter {

    private Context mContext;
    private String[] mImages;
    private ImageLoader.OnLoadListener[] mListeners;
    private ColorDrawable colorDrawable;
    public ProductPagerAdapter(Context context, String[] images) {
        mContext = context;
        mImages = images;
        colorDrawable = new ColorDrawable(Color.rgb(232,232,232));

        if(images != null)
            mListeners = new ImageLoader.OnLoadListener[images.length];
    }

    public void setItems(String[] images){
        mImages = images;

        if(images != null)
            mListeners = new ImageLoader.OnLoadListener[images.length];
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        final ImageView imageView = new ImageView(mContext);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        mListeners[position] = new ImageLoader.OnLoadListener(){
            @Override
            public void OnLoad(Bitmap photo) {
                BitmapDrawable bitmapDrawable = new BitmapDrawable(photo);
                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{colorDrawable, bitmapDrawable});
                imageView.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(300);
            }
            @Override
            public void OnFailed() { }
        };
        ImageLoader.instance.load(mContext, mImages[position], true, mListeners[position]);
        collection.addView(imageView);
        return imageView;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
        ImageLoader.instance.removeListener(mListeners[position]);
    }

    @Override
    public int getCount() {
        return mImages == null ? 0 : mImages.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

}