/*
 * Copyright 2019 Ryan Ward
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
import net.whollynugatory.android.comiccollector.AddActivity;
import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RetrieveComicSeriesDataTask extends AsyncTask<Void, Void, SeriesEntity> {

  private static final String TAG = BaseActivity.BASE_TAG + "RetrieveComicSeriesDataTask";

  private WeakReference<AddActivity> mActivityWeakReference;
  private String mQueryForSeries;

  public RetrieveComicSeriesDataTask(AddActivity context, String productCode) {

    mActivityWeakReference = new WeakReference<>(context);
    mQueryForSeries = productCode;
  }

  protected SeriesEntity doInBackground(Void... params) {

    SeriesEntity seriesEntity = new SeriesEntity();
    seriesEntity.parseProductCode(mQueryForSeries);
    String urlString = String.format(Locale.US, "https://api.upcitemdb.com/prod/trial/lookup?upc=%s", mQueryForSeries);


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
        return seriesEntity;
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

      return seriesEntity;
    }

    JSONArray items;
    try {
      JSONObject responseJson = new JSONObject(builder.toString());
      items = (JSONArray) responseJson.get("items");
    } catch (JSONException e) {
      connection.disconnect();
      return seriesEntity;
    }

    if (items != null) {
      for (int index = 0; index < items.length(); index++) {
        try { // errors parsing items should not prevent further parsing
          JSONObject item = (JSONObject) items.get(index);
          if (item.has("title")) {
            seriesEntity.Title = item.getString("title");
          }
        } catch (JSONException e) {
          Log.d(TAG, "Failed to parse JSON object.");
          Crashlytics.logException(e);
        }
      }
    } else {
      Log.w(TAG, "No expected items where found in response.");
    }

    connection.disconnect();
    return seriesEntity;
  }

  protected void onPostExecute(SeriesEntity seriesEntity) {

    Log.d(TAG, "++onPostExecute(SeriesEntity)");
    AddActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "AddActivity is null or detached.");
      return;
    }

    activity.retrieveComicSeriesComplete(seriesEntity);
  }
}
