package ee.jooseptenn.errandme.baseclasses;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;

/**
 * A base class for users.
 */

@IgnoreExtraProperties
public class User implements Parcelable {
    private String name;
    private String phoneNumber;
    private String email;
    private String sharedInformation;
    private ArrayList<String> activeAddedErrands;
    private ArrayList<String> activeAcceptedErrands;

    /**
     * A constructor for the User class.
     */
    public User() {
        this.activeAddedErrands = new ArrayList<String>();
        this.activeAcceptedErrands = new ArrayList<String>();
    }

    /**
     * A constructor for the User class.
     *
     * @param name              name of the user
     * @param phoneNumber       user's phone number
     * @param email             user's e-mail address
     * @param sharedInformation information that the user wants to share with other users
     */
    public User(String name, String phoneNumber, String email, String sharedInformation) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.sharedInformation = sharedInformation;
        this.activeAddedErrands = new ArrayList<String>();
        this.activeAcceptedErrands = new ArrayList<String>();
    }

    /**
     * A method that gets the name of the user.
     *
     * @return the first name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * A method that gets the phone number of the user.
     *
     * @return the phone number of the user
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * A method that gets the contact (not necessarily the same as the one used for logging in) e-mail address of the user.
     *
     * @return the contact e-mail address of the user
     */
    public String getEmail() {
        return email;
    }

    /**
     * A method that gets the information the user wants to share with other application users (e-mail address and/on phone number).
     *
     * @return the information the user wants to share with other users
     */
    public String getSharedInformation() {
        return sharedInformation;
    }

    /**
     * A method that gets the active (have not been removed by the user) added errands of the user.
     *
     * @return an ArrayList of the errands the user has added
     */
    public ArrayList<String> getActiveAddedErrands() {
        return activeAddedErrands;
    }

    /**
     * A method that gets the active (have not been removed by the assigner or declined by the user) accepted errands of the user.
     *
     * @return an ArrayList of the errands the user has accepted
     */
    public ArrayList<String> getActiveAcceptedErrands() {
        return activeAcceptedErrands;
    }

    /**
     * A method that sets the contact (not necessarily the same as the one used for logging in) e-mail address of the user.
     *
     * @return a contact e-mail address to use
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * A method that sets the phone number of the user.
     *
     * @return a phone number to use
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * A method that sets the information that the user wishes to share with other application users (e-mail address and/or phone number).
     *
     * @param information the information the user wishes to share, "email" for e-mail address, "phone" for phone number and "both" for both the e-mail and phone number
     */
    public void setSharedInformation(String information) {
        sharedInformation = information;
    }

    /**
     * A method that adds an errand id to the ArrayList of the errand ids that the user has accepted and that are still active (not removed by the assigner or declined by the user).
     *
     * @param errand an errand to add to activeAcceptedErrands
     */
    public void addAcceptedErrands(String errand) {
        activeAcceptedErrands.add(errand);
    }

    /**
     * A method that adds an errand to the ArrayList of the errands that the user has added and that are still active (not removed by the user).
     *
     * @param errand an errand to add to activeAddedErrands
     */
    public void addAddedErrands(String errand) {
        activeAddedErrands.add(errand);
    }

    /**
     * A method that is used to set the active added (not removed by the user) errands of the user. The errands are set as an ArrayList of errand ids.
      * @param errands
     */
    public void setActiveAddedErrands(ArrayList<String> errands) {
        activeAddedErrands = errands;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.email);
        dest.writeString(this.sharedInformation);
        dest.writeStringList(this.activeAddedErrands);
        dest.writeStringList(this.activeAcceptedErrands);
    }

    protected User(Parcel in) {
        this.name = in.readString();
        this.phoneNumber = in.readString();
        this.email = in.readString();
        this.sharedInformation = in.readString();
        this.activeAddedErrands = in.createStringArrayList();
        this.activeAcceptedErrands = in.createStringArrayList();
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
