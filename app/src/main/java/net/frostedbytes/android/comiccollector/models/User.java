package net.frostedbytes.android.comiccollector.models;

import java.io.Serializable;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class User implements Serializable {

    private static final String TAG = BaseActivity.BASE_TAG + "User";

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

    public static boolean isValid(User user) {

        LogUtils.debug(TAG, "++isValid(User)");
        return user != null &&
          !user.Id.isEmpty() &&
          !user.Id.equals(BaseActivity.DEFAULT_USER_ID) &&
          user.Id.length() == BaseActivity.DEFAULT_USER_ID.length();
    }
}
