package flatdecoration.shop;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    private static final String LOG_TAG = CartActivity.class.getName();
    private static final int SECRET_KEY = 97;
    private RecyclerView mRecyclerView;
    private CollectionReference mItems;
    private ArrayList<Item> mItemList;

    private AlarmManager mAlarmManager;

    private ShoppingCartAdapter mAdapter;

    private  int cartItems = 0;
    private final int gridNumber = 1;

    private NotificationHandler mNotificationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemList = new ArrayList<>();
        mAdapter = new ShoppingCartAdapter(this, mItemList);
        mRecyclerView.setAdapter(mAdapter);

        mItems = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("Selected")
                .document(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Items");

        mNotificationHandler = new NotificationHandler(this);

        queryData();
        setAlarmManager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cart_menu, menu);
        return true;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            queryData();
        }
    }
    private void setAlarmManager() {
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        long repeatInterval = 2 * 1000;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        mAlarmManager.cancel(pendingIntent);

        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                repeatInterval,
                pendingIntent
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int itemId = item.getItemId();

        if(itemId == R.id.log_out_button){
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }else if(itemId == R.id.back) {
            finish();
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }
    }

    private void queryData(){
        mItemList.clear();

        mItems.orderBy("productCounter", Query.Direction.ASCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Item item = document.toObject(Item.class);
                item.setId(document.getId());
                mItemList.add(item);
            }

            mAdapter.notifyDataSetChanged();
        });
    }



    public void deleteItem(Item item) {
        FirebaseFirestore.getInstance()
                .collection("Items")
                .document(item._getId())
                .update("productCounter", 0)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ShoppingCartAdapter", "productCounter nullázva: 0");

                    mItems.document(item._getId()).delete()
                            .addOnSuccessListener(success -> {
                                Log.d(LOG_TAG, "Sikeresen törölve: " + item._getId());
                                Toast.makeText(this, "Elem törölve a kosárból", Toast.LENGTH_SHORT).show();
                                queryData();
                            })
                            .addOnFailureListener(failure -> {
                                Toast.makeText(this, "Nem lehet törölni: " + item._getId(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(failure -> {
                    Toast.makeText(this, "Nem lehet frissíteni: " + item._getId(), Toast.LENGTH_LONG).show();
                });

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        mNotificationHandler.cancel();
    }
}