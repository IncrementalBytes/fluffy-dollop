package net.frostedbytes.android.comiccollector.models;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.comiccollector.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class User {

    @Exclude
    public static final String ROOT = "Users";

    public List<ComicBook> Comics;

    @Exclude
    public String Email;

    @Exclude
    public String FullName;

    @Exclude
    public String Id;

    public boolean IsGeek;

    public User() {

        this.Comics = new ArrayList<>();
        this.Email = "";
        this.FullName = "";
        this.Id = BaseActivity.DEFAULT_USER_ID;
        this.IsGeek = false;
    }
}
