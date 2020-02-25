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

package net.whollynugatory.android.comiccollector.common;

import android.os.AsyncTask;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;
import net.whollynugatory.android.comiccollector.ui.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RetrieveSeriesDataTask extends AsyncTask<Void, Void, SeriesDetails> {

  private static final String TAG = BaseActivity.BASE_TAG + "RetrieveSeriesDataTask";

  private WeakReference<MainActivity> mActivityWeakReference;
  private SeriesDetails mSeriesDetails;

  public RetrieveSeriesDataTask(MainActivity context, SeriesDetails seriesDetails) {

    mActivityWeakReference = new WeakReference<>(context);
    mSeriesDetails = seriesDetails;
  }

  protected SeriesDetails doInBackground(Void... params) {

    SeriesDetails seriesDetails = new SeriesDetails();
    String urlString = String.format(
      Locale.US,
      "https://api.upcitemdb.com/prod/trial/lookup?upc=%s",
      mSeriesDetails.getProductCode());
    Log.d(TAG, "Query: " + urlString);
    HttpURLConnection connection = null;
    StringBuilder builder = new StringBuilder();
    try {
      URL url = new URL(urlString);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setReadTimeout(20000); // 5 seconds
      connection.setConnectTimeout(20000); // 5 seconds

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        Log.e(TAG, "upcitemdb request failed. Response Code: " + responseCode);
        connection.disconnect();
        return seriesDetails;
      }

      BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = responseReader.readLine();
      while (line != null) {
        builder.append(line);
        line = responseReader.readLine();
      }
    } catch (IOException e) {
      if (connection != null) {
        connection.disconnect();
      }

      return seriesDetails;
    }

    JSONArray items;
    try {
      JSONObject responseJson = new JSONObject(builder.toString());
      items = (JSONArray) responseJson.get("items");
    } catch (JSONException e) {
      connection.disconnect();
      return seriesDetails;
    }

    if (items != null) {
      for (int index = 0; index < items.length(); index++) {
        String upc = "";
        String title = "";
        String brand = "";
        try { // errors parsing items should not prevent further parsing
          JSONObject item = (JSONObject) items.get(index);
          if (item.has("upc")) {
            upc = item.getString("upc");
          }

          if (item.has("title")) {
            title = item.getString("title");
          }

          if (item.has("brand")) {
            brand = item.getString("brand");
          }
        } catch (JSONException e) {
          Log.d(TAG, "Failed to parse JSON object.");
          Crashlytics.logException(e);
        }

        if (!mSeriesDetails.getProductCode().equals(upc)) {
          Log.e(TAG, "UPC code returned not expected: " + upc);
          continue;
        }

        if (mSeriesDetails.PublisherCode.equals(BaseActivity.DEFAULT_PUBLISHER_CODE) ||
          mSeriesDetails.PublisherCode.length() != BaseActivity.DEFAULT_PUBLISHER_CODE.length()) {
          seriesDetails.PublisherChanged = true;
          seriesDetails.PublisherCode = PublisherEntity.getPublisherCode(upc);
        } else {
          seriesDetails.PublisherCode = mSeriesDetails.PublisherCode;
        }

        if (mSeriesDetails.PublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) ||
          mSeriesDetails.PublisherId.length() != BaseActivity.DEFAULT_PUBLISHER_ID.length()) {
          seriesDetails.PublisherChanged = true;
          seriesDetails.PublisherId = UUID.randomUUID().toString();
        } else {
          seriesDetails.PublisherId = mSeriesDetails.PublisherId;
        }

        if (mSeriesDetails.Publisher.isEmpty()) {
          seriesDetails.PublisherChanged = true;
          seriesDetails.Publisher = brand.toUpperCase();
        } else {
          seriesDetails.Publisher = mSeriesDetails.Publisher;
        }

        if (mSeriesDetails.SeriesCode.equals(BaseActivity.DEFAULT_SERIES_CODE) ||
          mSeriesDetails.SeriesCode.length() != BaseActivity.DEFAULT_SERIES_CODE.length()) {
          seriesDetails.SeriesChanged = true;
          seriesDetails.SeriesCode = SeriesEntity.getSeriesCode(upc);
        } else {
          seriesDetails.SeriesCode = mSeriesDetails.SeriesCode;
        }
        if (mSeriesDetails.SeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) ||
          mSeriesDetails.SeriesId.length() != BaseActivity.DEFAULT_SERIES_ID.length()) {
          seriesDetails.SeriesChanged = true;
          seriesDetails.SeriesId = UUID.randomUUID().toString();
        } else {
          seriesDetails.SeriesId = mSeriesDetails.SeriesId;
        }
        if (mSeriesDetails.SeriesTitle.isEmpty()) {
          seriesDetails.SeriesChanged = true;
          seriesDetails.SeriesTitle = title;
        } else {
          seriesDetails.SeriesTitle = mSeriesDetails.SeriesTitle;
        }
      }
    } else {
      Log.w(TAG, "No expected items where found in response.");
    }

    connection.disconnect();
    return seriesDetails;
  }

  protected void onPostExecute(SeriesDetails seriesDetails) {

    Log.d(TAG, "++onPostExecute(SeriesDetails)");
    MainActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.retrieveComicSeriesComplete(seriesDetails);
  }
}
