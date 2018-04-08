package com.talmacel.iosif.riverisland.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.talmacel.iosif.riverisland.MyApplication;
import com.talmacel.iosif.riverisland.R;
import com.talmacel.iosif.riverisland.adapters.ProductsRecyclerAdapter;
import com.talmacel.iosif.riverisland.models.ProductsContainerData;
import com.talmacel.iosif.riverisland.utils.AutofitRecyclerView;
import com.talmacel.iosif.riverisland.utils.JsonLoader;

public class ProductsListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private ProductsRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setTitleTextColor(Color.BLACK);

        MyApplication app = (MyApplication)getApplicationContext();
        if(app.productsDataContainer != null){
            adapter = new ProductsRecyclerAdapter(this, app.productsDataContainer.Products);
        }else{
            adapter = new ProductsRecyclerAdapter(this, null);
            app.addApplicationCallback(new MyApplication.ApplicationCallBack(){
                @Override
                public void OnProductsDataReceived(ProductsContainerData parsedJson) {
                    adapter.setItems(parsedJson.Products);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        AutofitRecyclerView recyclerView = findViewById(R.id.products_recycler);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);


        DrawerLayout drawer =  findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView =  findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_items_list, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
