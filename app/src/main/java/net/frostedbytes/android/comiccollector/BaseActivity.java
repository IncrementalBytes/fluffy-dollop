package net.frostedbytes.android.comiccollector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesListFragment;
import net.frostedbytes.android.comiccollector.fragments.ManualSearchFragment;
import net.frostedbytes.android.comiccollector.fragments.SyncFragment;
import net.frostedbytes.android.comiccollector.fragments.SystemMessageFragment;
import net.frostedbytes.android.comiccollector.fragments.TutorialFragment;
import net.frostedbytes.android.comiccollector.fragments.UserPreferenceFragment;

public class BaseActivity  extends AppCompatActivity {

    public static final String ARG_COMIC_BOOK = "comic_book";
    public static final String ARG_COMIC_PUBLISHER = "publisher";
    public static final String ARG_COMIC_PUBLISHERS = "publishers";
    public static final String ARG_COMIC_SERIES = "comic_series";
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

    public static final int REQUEST_IMAGE_CAPTURE = 4201;
    public static final int REQUEST_COMIC_ADD = 4202;
    public static final int REQUEST_SYNC = 4203;

    public static final int REQUEST_CAMERA_PERMISSIONS = 4701;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS = 4702;

    public static final int RESULT_EXPORT_SUCCESS = 4800;
    public static final int RESULT_IMPORT_SUCCESS = 4801;
    public static final int RESULT_SYNC_FAILED = 4802;

    public static final int RESULT_ADD_SUCCESS = 4900;
    public static final int RESULT_ADD_FAILED = 4901;

    public static final String BASE_TAG = "ComicCollector::";
    public static final String TAG = BASE_TAG + "BaseActivity";

    /**
     * Provides a varying title for the main window based on which fragment is currently being used.
     * @param fragment Fragment currently being displayed.
     */
    protected void updateTitle(Fragment fragment) {

        LogUtils.debug(TAG, "++updateTitle(%s)", fragment.getClass().getSimpleName());
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(ComicBookListFragment.class.getName())) {
            setTitle(getString(R.string.title_comic_library));
        } else if (fragmentClassName.equals(ComicSeriesListFragment.class.getName())) {
            setTitle(getString(R.string.title_series_library));
        } else if (fragmentClassName.equals(ComicBookFragment.class.getName())) {
            setTitle(getString(R.string.title_comic_book));
        } else if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
            setTitle(getString(R.string.title_preferences));
        } else if (fragmentClassName.equals(TutorialFragment.class.getName())) {
            setTitle(getString(R.string.title_tutorial));
        } else if (fragmentClassName.equals(ComicSeriesFragment.class.getName())) {
            setTitle(getString(R.string.title_comic_series));
        } else if (fragmentClassName.equals(SystemMessageFragment.class.getName())) {
            setTitle(getString(R.string.app_name));
        } else if (fragmentClassName.equals(ManualSearchFragment.class.getName())) {
            setTitle(getString(R.string.title_gathering_data));
        } else if (fragmentClassName.equals(SyncFragment.class.getName())) {
            setTitle(getString(R.string.title_sync));
        }
    }
}
