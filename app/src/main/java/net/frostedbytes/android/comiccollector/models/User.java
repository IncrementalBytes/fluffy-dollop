package net.frostedbytes.android.comiccollector.models;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.comiccollector.BaseActivity;

public class User {

    @Exclude
    public static final String ROOT = "Users";

    @Exclude
    public String Email;

    @Exclude
    public String FullName;

    @Exclude
    public String Id;

    public final boolean IsGeek;

    public User() {

        this.Email = "";
        this.FullName = "";
        this.Id = BaseActivity.DEFAULT_USER_ID;
        this.IsGeek = false;
    }
}
