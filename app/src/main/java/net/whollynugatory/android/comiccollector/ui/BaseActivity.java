/*
 * Copyright 2020 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.whollynugatory.android.comiccollector.ui;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity  extends AppCompatActivity {

    public static final String ARG_COMIC_BOOK = "comic_book";
    public static final String ARG_DEBUG_FILE_NAME = "debug_file_name";
    public static final String ARG_EMAIL = "email";
    public static final String ARG_FIREBASE_USER_ID = "firebase_user_id";
    public static final String ARG_LIST_BOOK = "list_book";
    public static final String ARG_PRODUCT_CODE = "product_code";
    public static final String ARG_SERIES = "series";
    public static final String ARG_USER = "user";
    public static final String ARG_USER_NAME = "user_name";

    public static final String DEFAULT_PUBLISHER_ID = "000000";
    public static final String DEFAULT_SERIES_ID = "000000";
    public static final String DEFAULT_EXPORT_FILE = "exportedLibrary.json";
    public static final String DEFAULT_ISSUE_CODE = "00000";
    public static final String DEFAULT_LIBRARY_FILE = "localLibrary.json";
    public static final String DEFAULT_PRODUCT_CODE = "000000000000";
    public static final String DEFAULT_PUBLISHED_DATE = "00/0000";
    public static final String DEFAULT_SERIES_FILE = "Series.json";
    public static final String DEFAULT_USER_ID = "0000000000000000000000000000";

    public static final String DATABASE_NAME = "collector-db.sqlite";
    public static final String REMOTE_PATH = "bin";

    public static final int REQUEST_IMAGE_CAPTURE = 4201;

    public static final int REQUEST_CAMERA_PERMISSIONS = 4701;
    public static final int REQUEST_STORAGE_PERMISSIONS = 4702;

    public static final String BASE_TAG = "ComicCollector::";
    public static final String TAG = BASE_TAG + "BaseActivity";
}
