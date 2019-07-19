package net.frostedbytes.android.comiccollector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.fragments.ComicBookFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicBookListFragment;
import net.frostedbytes.android.comiccollector.fragments.ComicSeriesFragment;
import net.frostedbytes.android.comiccollector.fragments.ManualSearchFragment;
import net.frostedbytes.android.comiccollector.fragments.SystemMessageFragment;
import net.frostedbytes.android.comiccollector.fragments.TutorialFragment;
import net.frostedbytes.android.comiccollector.fragments.UserPreferenceFragment;

public class BaseActivity  extends AppCompatActivity {

    public static final String ARG_COMIC_BOOK = "comic_book";
    public static final String ARG_COMIC_BOOK_LIST = "comic_book_list";
    public static final String ARG_COMIC_PUBLISHER = "publisher";
    public static final String ARG_COMIC_PUBLISHERS = "publishers";
    public static final String ARG_COMIC_SERIES = "comic_series";
    public static final String ARG_EMAIL = "email";
    public static final String ARG_FIREBASE_USER_ID = "firebase_user_id";
    public static final String ARG_MESSAGE = "message";
    public static final String ARG_USER = "user";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_USER_NAME = "user_name";

    public static final String DEFAULT_COMIC_PUBLISHER_ID = "000000";
    public static final String DEFAULT_COMIC_SERIES_FILE = "localComicSeries.v02.txt";
    public static final String DEFAULT_COMIC_SERIES_ID = "000000";
    public static final String DEFAULT_ISSUE_CODE = "00000";
    public static final String DEFAULT_LIBRARY_FILE = "localLibrary.v02.txt";
    public static final String DEFAULT_PRODUCT_CODE = "000000000000";
    public static final String DEFAULT_USER_ID = "0000000000000000000000000000";

    public static final int REQUEST_IMAGE_CAPTURE = 4701;
    public static final int REQUEST_COMIC_ADD = 4702;
    public static final int REQUEST_CAMERA_PERMISSIONS = 4703;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS = 4704;

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
        }
    }
}
