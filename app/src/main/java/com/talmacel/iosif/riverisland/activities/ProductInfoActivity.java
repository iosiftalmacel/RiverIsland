package com.talmacel.iosif.riverisland.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.talmacel.iosif.riverisland.R;
import com.talmacel.iosif.riverisland.adapters.ProductPagerAdapter;
import com.talmacel.iosif.riverisland.models.ProductItemData;


public class ProductInfoActivity extends AppCompatActivity {
    ProductPagerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setElevation(0);

        ProductItemData itemData = (ProductItemData) getIntent().getSerializableExtra("itemData");
        if(itemData == null){
            finish();
            return;
        }

        ViewPager pager = findViewById(R.id.product_info_pager);
        TabLayout tab = findViewById(R.id.product_info_tabs);
        adapter = new ProductPagerAdapter(this, itemData.allImages);
        pager.setAdapter(adapter);
        tab.setupWithViewPager(pager, true);



        TextView mTitleView = findViewById(R.id.title);
        mTitleView.setText(itemData.name);
        TextView mDescView = findViewById(R.id.desc);
        if(itemData.category != null)
            mDescView.setText(itemData.category.split(",")[0]);
        TextView mCostView = findViewById(R.id.cost);
        mCostView.setText("€" + itemData.costEUR);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_item_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
