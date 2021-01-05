package app.multicontactpicker;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import app.multicontactpicker.RxContacts.Contact;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ContactResult implements Parcelable {

    private String mContactID;
    private final String mDisplayName;
    private final boolean mStarred;
    private final Uri mPhoto;
    private final Uri mThumbnail;
    private List<String> mEmails = new ArrayList<>();
    private final String mPhoneNumber;

    public String getContactID() {
        return mContactID;
    }

    public void setContactID(String mContactID) {
        this.mContactID = mContactID;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean isStarred() {
        return mStarred;
    }

    public Uri getPhoto() {
        return mPhoto;
    }

    public Uri getThumbnail() {
        return mThumbnail;
    }

    public List<String> getEmails() {
        return mEmails;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    /*public PhoneNumber> getPhoneNumbers() {
        return mPhoneNumbers;
    }*/

    public ContactResult(Contact contact) {
        this.mContactID = String.valueOf(contact.getId());
        this.mDisplayName = contact.getDisplayName();
        this.mStarred = contact.isStarred();
        this.mPhoto = contact.getPhoto();
        this.mThumbnail = contact.getThumbnail();
        this.mEmails.clear();
        this.mEmails.addAll(contact.getEmails());
        this.mPhoneNumber = contact.getPhoneNumber();
    }

    protected ContactResult(Parcel in) {
        this.mContactID = in.readString();
        this.mDisplayName = in.readString();
        this.mStarred = in.readByte() != 0;
        this.mPhoto = in.readParcelable(Uri.class.getClassLoader());
        this.mThumbnail = in.readParcelable(Uri.class.getClassLoader());
        this.mEmails = in.createStringArrayList();
        this.mPhoneNumber = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mContactID);
        dest.writeString(this.mDisplayName);
        dest.writeByte(this.mStarred ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.mPhoto, flags);
        dest.writeParcelable(this.mThumbnail, flags);
        dest.writeStringList(this.mEmails);
        dest.writeString(this.mPhoneNumber);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ContactResult> CREATOR = new Parcelable.Creator<ContactResult>() {
        @Override
        public ContactResult createFromParcel(Parcel in) {
            return new ContactResult(in);
        }

        @Override
        public ContactResult[] newArray(int size) {
            return new ContactResult[size];
        }
    };
}
