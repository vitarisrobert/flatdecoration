package flatdecoration.shop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;

public class ShopActivity extends AppCompatActivity implements SensorEventListener {

    private static final String LOG_TAG = RegistrationActivity.class.getName();
    private static final int SECRET_KEY = 97;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;

    private ActivityResultLauncher<Intent> cartActivityLauncher;

    private FirebaseUser user;

    private RecyclerView mRecyclerView;
    private ArrayList<Item> mItemList;
    private ShoppingItemAdapter mAdapter;

    private Menu mMenu;

    private FirebaseFirestore mFirestrore;
    private CollectionReference mItems;

    private NotificationHandler mNotificationHandler;
    private JobScheduler mJobScheduler;

    private FrameLayout redCircle;
    private TextView contentTextView;
    private final int gridNumber = 1;
    private  int cartItems = 0;
    private int queryLimit = 10;

    private boolean viewRow = true;


    Sensor sensor;
    SensorManager   sensorManager;
    boolean isRunning = false;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if(secret_key != 97){
            finish();
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            Log.d(LOG_TAG, "Autentikált felhasználó!");
        }else{
            Log.d(LOG_TAG, "Nem autentikált felhasználó!");
            finish();
        }

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemList = new ArrayList<>();

        mAdapter = new ShoppingItemAdapter(this, mItemList);
        mRecyclerView.setAdapter(mAdapter);

        mFirestrore = FirebaseFirestore.getInstance();
        mItems = mFirestrore.collection("Items");

        clearUserCart();

        cartActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        refreshCartMenu();
                    }
                }
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(powerReceiver, filter);

        mNotificationHandler = new NotificationHandler(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        } else {
            mNotificationHandler.send("Üdvözlünk az alkalmazásban!");
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        setJobScheduler();
    }



    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action == null)
                return;

            switch(action){
                case Intent.ACTION_POWER_CONNECTED:
                    queryLimit = 6;
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    queryLimit = 4;
                    break;
            }
            queryData();
        }
    };

    private void queryData() {
        mItemList.clear();

        mItems.orderBy("productCounter", Query.Direction.DESCENDING)
                .limit(queryLimit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> addedTitles = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Item item = document.toObject(Item.class);
                        item.setId(document.getId());

                        if (!addedTitles.contains(item.getName())) {
                            mItemList.add(item);
                            addedTitles.add(item.getName());
                        }
                    }

                    if (mItemList.isEmpty()) {
                        initializeData();
                        return;
                    }

                    mAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Hiba történt a lekérdezés során: ", e);
                });
    }



    private void initializeData() {
        String[] itemList = getResources().getStringArray(R.array.shopping_item_names);
        String[] itemInformation = getResources().getStringArray(R.array.shopping_item_description);
        String[] itemPrice = getResources().getStringArray(R.array.shopping_item_prices);
        TypedArray itemImageResource = getResources().obtainTypedArray(R.array.shopping_item_images);
        TypedArray itemRate = getResources().obtainTypedArray(R.array.shopping_item_rates);


        for(int i = 0; i < itemList.length; i++){
            mItems.add(new Item(
                    itemList[i],
                    itemInformation[i],
                    itemPrice[i],
                    itemRate.getFloat(i,0),
                    itemImageResource.getResourceId(i,0),
                    0));
        }

        itemImageResource.recycle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shop_list_menu, menu);
        mMenu = menu;

        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) { return false; }

            @Override
            public boolean onQueryTextChange(String s) {
                mAdapter.getFilter().filter(s);
                return false;
            }
        });

        refreshCartMenu();

        return true;
    }


    private void refreshCartMenu() {
        if (mMenu == null) return;

        MenuItem alertMenuItem = mMenu.findItem(R.id.cart);
        if (alertMenuItem == null) return;

        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();
        if (rootView == null) return;

        redCircle = rootView.findViewById(R.id.view_alert_red_circle);
        contentTextView = rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(v -> onOptionsItemSelected(alertMenuItem));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        CollectionReference cartRef = mFirestrore.collection("Selected")
                .document(currentUser.getUid())
                .collection("Items");

        cartRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int totalCartItems = 0;
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Item item = document.toObject(Item.class);
                totalCartItems += item.getProductCounter();
            }

            cartItems = totalCartItems;
            if (cartItems > 0) {
                contentTextView.setText(String.valueOf(cartItems));
                redCircle.setVisibility(View.VISIBLE);
            } else {
                contentTextView.setText("");
                redCircle.setVisibility(View.GONE);
            }
        }).addOnFailureListener(e -> {
            Log.e(LOG_TAG, "Kosár adatainak lekérdezése nem sikerült.", e);
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int itemId = item.getItemId();

        if(itemId == R.id.log_out_button){
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }else if(itemId == R.id.camera) {
            Intent intentCamera = new Intent(this, CameraActivity.class);
            startActivity(intentCamera);
            return true;
        }else if(itemId == R.id.cart){
            startCart();
            return true;
        }else if(itemId == R.id.view_selector) {
            if(viewRow){
                changeSpanCount(item, R.drawable.ic_view_row, 2);
            }else{
                changeSpanCount(item, R.drawable.ic_view_grid, 1);
            }
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }

    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        redCircle = rootView.findViewById(R.id.view_alert_red_circle);
        contentTextView = rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(v -> onOptionsItemSelected(alertMenuItem));

        refreshCartMenu();

        return super.onPrepareOptionsMenu(menu);
    }


    public void increaseAlertIcon(Item item){
        cartItems = item.getProductCounter();

        mItems.document(item._getId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                long currentCounter = documentSnapshot.getLong("productCounter");
                int newCounter = (int) currentCounter + 1;

                mItems.document(item._getId()).update("productCounter", newCounter)
                        .addOnFailureListener(failure -> {
                            Toast.makeText(this, "Nem lehet megváltoztatni: " + item._getId(), Toast.LENGTH_LONG).show();
                        });

                mItems.orderBy("productCounter").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalCartItems = 0;
                    for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                        totalCartItems += document.getLong("productCounter").intValue();
                    }

                    cartItems = totalCartItems;
                    if(cartItems > 0) {
                        contentTextView.setText(String.valueOf(cartItems));
                        redCircle.setVisibility(View.VISIBLE);
                    } else {
                        contentTextView.setText("");
                        redCircle.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(failure -> {
                    Toast.makeText(this, "Error getting cart data: " + failure.getMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(failure -> {
            Toast.makeText(this, "Error getting item: " + failure.getMessage(), Toast.LENGTH_LONG).show();
        });

        mNotificationHandler.send(item.getName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(powerReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setJobScheduler(){
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        int hardDeadLine = 15000;

        ComponentName name = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, name)
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(true)
                .setOverrideDeadline(hardDeadLine);

        mJobScheduler.schedule(builder.build());
    }

    private void startCart() {
        Intent intent = new Intent(this, CartActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        cartActivityLauncher.launch(intent);
    }

    public void addCart(Item item) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        CollectionReference cartRef = mFirestrore.collection("Selected")
                .document(currentUser.getUid())
                .collection("Items");

        cartRef.document(item._getId()).set(item)
                .addOnSuccessListener(aVoid -> {
                    increaseAlertIcon(item);
                    updateCartIcon();
                    Toast.makeText(this, "Termék hozzáadva a kosárhoz", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hiba történt: nem sikerült hozzáadni a kosárhoz", Toast.LENGTH_SHORT).show();
                });
    }


    private void clearUserCart() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CollectionReference userCart = FirebaseFirestore.getInstance()
                .collection("Selected")
                .document(userId)
                .collection("Items");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference itemsCollection = db.collection("Items");


        userCart.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                String itemId = document.getId();

                userCart.document(itemId).delete();
                mItems.document(itemId).update("productCounter", 0);
            }

            Log.d(LOG_TAG, "Kosár ürítve bejelentkezéskor.");
        }).addOnFailureListener(e -> {
            Log.e(LOG_TAG, "Nem sikerült a kosár törlése: ", e);
        });

        queryData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartMenu();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void updateCartIcon() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        CollectionReference cartRef = mFirestrore.collection("Selected")
                .document(currentUser.getUid())
                .collection("Items");

        cartRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int totalCartItems = 0;
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Item item = document.toObject(Item.class);
                totalCartItems += item.getProductCounter();
            }

            cartItems = totalCartItems;
            if (cartItems > 0) {
                contentTextView.setText(String.valueOf(cartItems));
                redCircle.setVisibility(View.VISIBLE);
            } else {
                contentTextView.setText("");
                redCircle.setVisibility(View.GONE);
            }
        }).addOnFailureListener(e -> {
            Log.e(LOG_TAG, "Kosár adatainak lekérdezése nem sikerült.", e);
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.values[0] > 40 && isRunning == false){
            isRunning = true;
            mediaPlayer = new MediaPlayer();
            try{
                mediaPlayer.setDataSource("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3");
                mediaPlayer.prepare();
                mediaPlayer.start();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        if (event.values[0] < 40 && isRunning == true) {
            isRunning = false;
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}













