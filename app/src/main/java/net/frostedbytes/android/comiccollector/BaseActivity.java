package net.frostedbytes.android.comiccollector;

import android.support.v7.app.AppCompatActivity;

public class BaseActivity  extends AppCompatActivity {

    public static final String ARG_COMIC_BOOK = "comic_book";
    public static final String ARG_COMIC_LIST = "comic_list";
    public static final String ARG_EMAIL = "email";
    public static final String ARG_FIREBASE_USER_ID = "firebase_user_id";
    public static final String ARG_SCAN_RESULTS = "scan_results";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_USER_NAME = "user_name";

    public static final String DEFAULT_LIBRARY_FILE = "localLibrary.txt";
    public static final String DEFAULT_PRODUCT_CODE = "0000000000";
    public static final String DEFAULT_USER_ID = "0000000000000000000000000000";

    public static final String BASE_TAG = "ComicCollector::";
}
