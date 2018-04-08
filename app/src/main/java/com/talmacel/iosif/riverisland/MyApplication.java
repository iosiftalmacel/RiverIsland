package com.talmacel.iosif.riverisland;

import android.app.Application;
import android.util.Log;

import com.talmacel.iosif.riverisland.models.ProductsContainerData;
import com.talmacel.iosif.riverisland.utils.ImageLoader;
import com.talmacel.iosif.riverisland.utils.JsonLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Iosif on 17/03/2018.
 */

public class MyApplication extends Application {
    public static String URL = "https://ri.nn4m.net/RI/sv5/api/public/index.php/category/2508/products.json";
    public ProductsContainerData productsDataContainer;

    List<ApplicationCallBack> callbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        JsonLoader.CreateStaticInstance(this);
        ImageLoader.CreateStaticInstance(this, 3, 5000);

        callbacks = new ArrayList<>();

        JsonLoader.instance.downloadParsedJson(URL, ProductsContainerData.class, new JsonLoader.OnLoadListener<ProductsContainerData>() {
            @Override
            public void OnLoad(ProductsContainerData parsedJson) {
                productsDataContainer = parsedJson;
                Log.e("arrrrrrrrived", "arrriddsadsa");
                for (ApplicationCallBack callback : callbacks) {
                    callback.OnProductsDataReceived(parsedJson);
                }
            }
        });
    }

    public void addApplicationCallback(ApplicationCallBack appCallback){
        callbacks.add(appCallback);
    }

    public static abstract class ApplicationCallBack{
        public abstract void OnProductsDataReceived(ProductsContainerData parsedJson);
    }
}
