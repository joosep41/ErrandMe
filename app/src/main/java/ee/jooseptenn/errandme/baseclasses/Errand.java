package ee.jooseptenn.errandme.baseclasses;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * A base class for errands.
 */

@IgnoreExtraProperties
public class Errand implements Parcelable {
    private String id;
    private String assignerId;
    private String accepterId;
    private String title;
    private String description;
    private String location;
    private String address;
    private String pay;
    private String currency;
    private String estimatedTime;
    private double distanceFromUser;

    /**
     * A constructor for the Errand class.
     */
    public Errand() {
    }

    /**
     * A constructor for the Errand class.
     *
     * @param id            identificator of the errand
     * @param assignerId    the assigner of the errand
     * @param title         the title of the errand
     * @param description   the description of the errand
     * @param location      comma separated latitude and longitude of the location where the errand has to be done
     * @param address       the address where the errand has to be done
     * @param pay           the pay that is given when the errand has been completed
     * @param currency      the currency in which the errand is payed for when the errand has been completed
     * @param estimatedTime the estimated completion time of the errand
     */
    public Errand(String id, String assignerId, String title, String description, String location, String address, String pay, String currency, String estimatedTime) {
        this.id = id;
        this.assignerId = assignerId;
        this.accepterId = "";
        this.title = title;
        this.description = description;
        this.location = location;
        this.address = address;
        this.pay = pay;
        this.currency = currency;
        this.estimatedTime = estimatedTime;
    }


    /**
     * A constructor for the Errand class.
     *
     * @param assignerId    the assigner of the errand
     * @param title         the title of the errand
     * @param description   the description of the errand
     * @param location      comma separated latitude and longitude of the location where the errand has to be done
     * @param address       the address where the errand has to be done
     * @param pay           the pay that is given when the errand has been completed
     * @param currency      the currency in which the errand is payed for when the errand has been completed
     * @param estimatedTime the estimated completion time of the errand
     */
    public Errand(String assignerId, String title, String description, String location, String address, String pay, String currency, String estimatedTime) {
        this.assignerId = assignerId;
        this.accepterId = "";
        this.title = title;
        this.description = description;
        this.location = location;
        this.address = address;
        this.pay = pay;
        this.currency = currency;
        this.estimatedTime = estimatedTime;
    }

    /**
     * A method that gets the id of the errand.
     *
     * @return the id of the errand
     */
    public String getId() {
        return id;
    }

    /**
     * A method that sets the id to the errand.
     *
     * @param id an id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * A method that gets the assignerId of the errand.
     *
     * @return the assignerId of the errand
     */
    public String getAssignerId() {
        return assignerId;
    }

    /**
     * A method that sets the assignerId to the errand.
     *
     * @param assignerId an assignerId to set
     */
    public void setAssignerId(String assignerId) {
        this.assignerId = assignerId;
    }

    /**
     * A method that gets the errand's distance from the user.
     *
     * @return distance from user in kilometres
     */
    public double getDistanceFromUser() {
        return distanceFromUser;
    }

    /**
     * A method that sets the errand's distance from the user.
     *
     * @param distanceFromUser distance from user in kilometres
     */
    public void setDistanceFromUser(double distanceFromUser) {
        this.distanceFromUser = distanceFromUser;
    }

    /**
     * A method that gets the currency in which the errand is payed for when the errand has been completed.
     *
     * @return the errand's pay currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * A method that sets the currency in which the errand is payed for when the errand has been completed.
     *
     * @param currency a currency to set to the errand
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * A method that gets the pay that is given when the errand has been completed.
     *
     * @return a pay for the errand
     */
    public String getPay() {
        return pay;
    }

    /**
     * A method that sets the pay that is given when the errand has been completed.
     *
     * @param pay amount of money given upon errand completion
     */
    public void setPay(String pay) {
        this.pay = pay;
    }

    /**
     * A method that gets the address where the errand has to be done.
     *
     * @return the errand's address
     */
    public String getAddress() {
        return address;
    }

    /**
     * A method that sets the address where the errand has to be done.
     *
     * @param address an address where the errand has to be done
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * A method that gets comma separated latitude and longitude of the location where the errand has to be done.
     *
     * @return location where the errand has to be done
     */
    public String getLocation() {
        return location;
    }

    /**
     * A method that sets a location where the errand has to be done as a comma separated string of latitude and longitude.
     *
     * @param location a location where the errand has to be done
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * A method that gets the errand's description.
     *
     * @return the errand's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * A method that sets the errand's description.
     *
     * @param description a description for the errand
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * A method that gets the errand's title.
     *
     * @return the errand's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * A method that sets the errand's title.
     *
     * @param title a title for the errand
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * A method that gets the errand accepter's id.
     *
     * @return the id of the errand accepter
     */
    public String getAccepterId() {
        return accepterId;
    }

    /**
     * A method that sets the errand accepter's id.
     *
     * @param accepterId id of the accepter
     */
    public void setAccepterId(String accepterId) {
        this.accepterId = accepterId;
    }

    /**
     * A method that gets the estimated completion time of the errand.
     *
     * @return the estimated errand completion time
     */
    public String getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * A method that sets the completion time of the errand.
     *
     * @param estimatedTime the estimated errand completion time
     */
    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.assignerId);
        dest.writeString(this.accepterId);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.location);
        dest.writeString(this.address);
        dest.writeString(this.pay);
        dest.writeString(this.currency);
        dest.writeDouble(this.distanceFromUser);
        dest.writeString(this.estimatedTime);
    }

    protected Errand(Parcel in) {
        this.id = in.readString();
        this.assignerId = in.readString();
        this.accepterId = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.location = in.readString();
        this.address = in.readString();
        this.pay = in.readString();
        this.currency = in.readString();
        this.distanceFromUser = in.readDouble();
        this.estimatedTime = in.readString();
    }

    public static final Parcelable.Creator<Errand> CREATOR = new Parcelable.Creator<Errand>() {
        @Override
        public Errand createFromParcel(Parcel source) {
            return new Errand(source);
        }

        @Override
        public Errand[] newArray(int size) {
            return new Errand[size];
        }
    };
}
