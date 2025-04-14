package flatdecoration.shop;

public class ShoppingItem {
    private String name;
    private String information;
    private String price;
    private float ratedInformation;
    private final int imageResource;

    public ShoppingItem(String name, String information, String price, float ratedInformation,int imageResource) {
        this.name = name;
        this.information = information;
        this.price = price;
        this.ratedInformation = ratedInformation;
        this.imageResource = imageResource;
    }

    public String getInformation() {return information;}
    public String getName() {return name;}
    public String getPrice() {return price;}
    public float getRatedInformation() {return ratedInformation;}
    public int getImageResource(){return imageResource;}
}
