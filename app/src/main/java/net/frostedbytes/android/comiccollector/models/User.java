package net.frostedbytes.android.comiccollector.models;

import java.io.Serializable;
import net.frostedbytes.android.comiccollector.BaseActivity;

public class User implements Serializable {

    public static final String ROOT = "Users";

    public String Email;

    public String FullName;

    public String Id;

    public boolean IsGeek;

    public boolean ShowBarcodeHint;

    public User() {

        this.Email = "";
        this.FullName = "";
        this.Id = BaseActivity.DEFAULT_USER_ID;
        this.IsGeek = false;
        this.ShowBarcodeHint = true;
    }
}
