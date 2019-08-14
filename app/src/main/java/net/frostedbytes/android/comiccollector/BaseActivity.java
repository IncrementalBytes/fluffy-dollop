package net.frostedbytes.android.comiccollector;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity  extends AppCompatActivity {

    public static final String ARG_COMIC_BOOK = "comic_book";
    public static final String ARG_COMIC_PUBLISHER = "publisher";
    public static final String ARG_COMIC_PUBLISHERS = "publishers";
    public static final String ARG_COMIC_SERIES = "comic_series";
    public static final String ARG_DEBUG_FILE_NAME = "debug_file_name";
    public static final String ARG_EMAIL = "email";
    public static final String ARG_FIREBASE_USER_ID = "firebase_user_id";
    public static final String ARG_MESSAGE = "message";
    public static final String ARG_NEW_COMIC_BOOK = "new_comic_book";
    public static final String ARG_USER = "user";
    public static final String ARG_USER_NAME = "user_name";

    public static final String DEFAULT_COMIC_PUBLISHER_ID = "000000";
    public static final String DEFAULT_COMIC_SERIES_ID = "000000";
    public static final String DEFAULT_ISSUE_CODE = "00000";
    public static final String DEFAULT_LIBRARY_FILE = "localLibrary.json";
    public static final String DEFAULT_PRODUCT_CODE = "000000000000";
    public static final String DEFAULT_PUBLISHED_DATE = "00/0000";
    public static final String DEFAULT_USER_ID = "0000000000000000000000000000";

    public static final int NUM_SUB_FRAGMENTS = 2;

    public static final int REQUEST_IMAGE_CAPTURE = 4201;
    public static final int REQUEST_COMIC_ADD = 4202;

    public static final int REQUEST_CAMERA_PERMISSIONS = 4701;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS = 4702;

    public static final int RESULT_ADD_SUCCESS = 4900;
    public static final int RESULT_ADD_FAILED = 4901;

    public static final String BASE_TAG = "ComicCollector::";
    public static final String TAG = BASE_TAG + "BaseActivity";
}
