package flatdecoration.shop;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;

public class Item implements Parcelable {
    private String id;
    private String name;
    private String information;
    private String price;
    private float ratedInformation;
    private int imageResource;
    private int productCounter;

    private FirebaseFirestore fireStore;


    public Item() {
        fireStore = FirebaseFirestore.getInstance();
    }

    public Item(String name, String price, int productCounter) {
        this.name = name;
        this.price = price;
        this.productCounter = productCounter;
        fireStore = FirebaseFirestore.getInstance();
    }

    public Item(String name, String information, String price, float ratedInformation, int imageResource, int productCounter) {
        this.name = name;
        this.information = information;
        this.price = price;
        this.ratedInformation = ratedInformation;
        this.imageResource = imageResource;
        this.productCounter = productCounter;
        fireStore = FirebaseFirestore.getInstance();

    }


    public String getInformation() {return information;}
    public String getName() {return name;}
    public String getPrice() {return price;}
    public float getRatedInformation() {return ratedInformation;}
    public int getImageResource(){return imageResource;}
    public int getProductCounter(){return productCounter;}

    public void setProductCounter(int productCounter) {
        this.productCounter = productCounter;
    }

    public String _getId(){return id;}
    public void setId(String id){this.id = id;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {

    }
}
