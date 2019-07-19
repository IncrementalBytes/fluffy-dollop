package net.frostedbytes.android.comiccollector.common;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.os.AsyncTask;
import com.crashlytics.android.Crashlytics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.AddActivity;
import net.frostedbytes.android.comiccollector.models.ComicSeries;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RetrieveComicSeriesDataTask extends AsyncTask<Void, Void, ComicSeries> {

  private static final String TAG = BASE_TAG + "RetrieveComicSeriesDataTask";

  private WeakReference<AddActivity> mActivityWeakReference;
  private ComicSeries mQueryForSeries;

  public RetrieveComicSeriesDataTask(AddActivity context, ComicSeries queryForSeries) {

    mActivityWeakReference = new WeakReference<>(context);
    mQueryForSeries = queryForSeries;
  }

  protected ComicSeries doInBackground(Void... params) {

    ComicSeries comicSeries = new ComicSeries();
    comicSeries.PublisherId = mQueryForSeries.PublisherId;
    comicSeries.Id = mQueryForSeries.Id;
    String urlString = String.format(
      Locale.US,
      "https://api.upcitemdb.com/prod/trial/lookup?upc=%s",
      mQueryForSeries.getProductId());

    LogUtils.debug(TAG, "Query: %s", urlString);
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
        LogUtils.error(TAG, "upcitemdb request failed. Response Code: " + responseCode);
        connection.disconnect();
        return comicSeries;
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

      return comicSeries;
    }

    JSONArray items;
    try {
      JSONObject responseJson = new JSONObject(builder.toString());
      items = (JSONArray) responseJson.get("items");
    } catch (JSONException e) {
      connection.disconnect();
      return comicSeries;
    }

    if (items != null) {
      for (int index = 0; index < items.length(); index++) {
        try { // errors parsing items should not prevent further parsing
          JSONObject item = (JSONObject) items.get(index);
          if (item.has("title")) {
            comicSeries.SeriesName = item.getString("title");
          }
        } catch (JSONException e) {
          LogUtils.debug(TAG, "Failed to parse JSON object.");
          Crashlytics.logException(e);
        }
      }
    } else {
      LogUtils.warn(TAG, "No expected items where found in response.");
    }

    connection.disconnect();
    return comicSeries;
  }

  protected void onPostExecute(ComicSeries comicSeries) {

    LogUtils.debug(TAG, "++onPostExecute(%s)", comicSeries.toString());
    AddActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      LogUtils.error(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.retrieveComicSeriesComplete(comicSeries);
  }
}
