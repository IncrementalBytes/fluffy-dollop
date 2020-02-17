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

package net.whollynugatory.android.comiccollector.db.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.whollynugatory.android.comiccollector.db.dao.ComicDetailsDao;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ComicDetailsRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "ComicDetailsRepository";

  private static volatile ComicDetailsRepository INSTANCE;

  private ComicDetailsDao mComicDetailsDao;

  private ComicDetailsRepository(ComicDetailsDao comicDetailsDao) {

    mComicDetailsDao = comicDetailsDao;
  }

  public static ComicDetailsRepository getInstance(final ComicDetailsDao comicDetailsDao) {

    if (INSTANCE == null) {
      synchronized (ComicDetailsRepository.class) {
        if (INSTANCE == null) {
          Log.d(TAG, "++getInstance(Context)");
          INSTANCE = new ComicDetailsRepository(comicDetailsDao);
        }
      }
    }

    return INSTANCE;
  }

  public void deleteById(String fullId) {

    mComicDetailsDao.deleteById(fullId);
  }

  public LiveData<ComicDetails> find(String productCode, String issueCode) {

    return mComicDetailsDao.find(productCode, issueCode);
  }

  public LiveData<List<ComicDetails>> findByProductCode(String productCode) {

    return mComicDetailsDao.findByProductCode(productCode);
  }

  public LiveData<List<ComicDetails>> getRecent() {

    return mComicDetailsDao.getRecent();
  }
}
