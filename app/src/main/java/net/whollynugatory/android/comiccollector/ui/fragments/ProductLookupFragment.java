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

package net.whollynugatory.android.comiccollector.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.db.entity.PublisherEntity;
import net.whollynugatory.android.comiccollector.db.entity.SeriesEntity;
import net.whollynugatory.android.comiccollector.db.viewmodel.CollectorViewModel;
import net.whollynugatory.android.comiccollector.db.views.ComicDetails;
import net.whollynugatory.android.comiccollector.db.views.SeriesDetails;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class ProductLookupFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ProductLookupFragment";

  public interface OnProductLookupListener {

    void onProductLookupCancel();

    void onProductLookupFound(ComicDetails comicDetails);

    void onProductLookupUnknown(SeriesDetails seriesDetails);
  }

  private OnProductLookupListener mCallback;

  private CollectorViewModel mCollectorViewModel;

  private String mProductCode;

  public static ProductLookupFragment newInstance(String productCode) {

    Log.d(TAG, "++newInstance(String)");
    ProductLookupFragment fragment = new ProductLookupFragment();
    Bundle arguments = new Bundle();
    arguments.putString(BaseActivity.ARG_PRODUCT_CODE, productCode);
    fragment.setArguments(arguments);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnProductLookupListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mProductCode = arguments.getString(BaseActivity.ARG_PRODUCT_CODE);
    } else {
      Log.e(TAG, "Arguments were null.");
    }

    mCollectorViewModel = new ViewModelProvider(this).get(CollectorViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_lookup, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Log.d(TAG, "++onViewCreated(View, Bundle)");
    Button cancelButton = view.findViewById(R.id.lookup_button_cancel);
    ProgressBar progressBar = view.findViewById(R.id.lookup_progress_wait);

    progressBar.setIndeterminate(true);
    cancelButton.setOnClickListener(v -> mCallback.onProductLookupCancel());

    String publisherCode = PublisherEntity.getPublisherCode(mProductCode);
    String seriesCode = SeriesEntity.getSeriesCode(mProductCode);
    mCollectorViewModel.getSeriesByPublisher(publisherCode).observe(getViewLifecycleOwner(), seriesDetailsList -> {

      ComicDetails comicDetails = new ComicDetails();
      comicDetails.PublisherCode = publisherCode;
      comicDetails.SeriesCode = seriesCode;
      if (seriesDetailsList != null) {
        // TODO: might change if known publisher with no series returns null instead of list with zero items
        for (SeriesDetails seriesDetails : seriesDetailsList) {
          if (comicDetails.PublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) ||
            comicDetails.PublisherId.length() != BaseActivity.DEFAULT_PUBLISHER_ID.length()) {
            comicDetails.PublisherId = seriesDetails.PublisherId;
            comicDetails.Publisher = seriesDetails.Publisher;
          }

          if (seriesDetails.SeriesCode.equals(seriesCode)) {
            comicDetails.PublisherId = seriesDetails.PublisherId;
            comicDetails.Publisher = seriesDetails.Publisher;
            comicDetails.SeriesId = seriesDetails.SeriesId;
            comicDetails.SeriesTitle = seriesDetails.SeriesTitle;
            comicDetails.Volume = seriesDetails.Volume;
            break;
          }
        }

        if (!comicDetails.PublisherId.equals(BaseActivity.DEFAULT_PUBLISHER_ID) &&
          comicDetails.PublisherId.length() == BaseActivity.DEFAULT_PUBLISHER_ID.length() &&
          !comicDetails.SeriesId.equals(BaseActivity.DEFAULT_SERIES_ID) &&
          comicDetails.SeriesId.length() == BaseActivity.DEFAULT_SERIES_ID.length()) {
          mCallback.onProductLookupFound(comicDetails);
        } else {
          mCallback.onProductLookupUnknown(comicDetails.toSeriesDetails());
        }
      } else {
        mCallback.onProductLookupUnknown(comicDetails.toSeriesDetails());
      }
    });
  }
}
