package net.frostedbytes.android.comiccollector.fragments;

import static net.frostedbytes.android.comiccollector.BaseActivity.BASE_TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.LogUtils;
import net.frostedbytes.android.comiccollector.models.ComicBook;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchActionComplete(ComicBook comicBook);

    void onManualSearchCancel();
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueCodeEdit;
  private EditText mProductCodeEdit;

  private ComicBook mComicBook;
  private Bitmap mImageBitmap;

  public static ManualSearchFragment newInstance(ComicBook comicBook, Bitmap barcodeImage) {

    LogUtils.debug(TAG, "++newInstance(%s)", comicBook.toString());
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle args = new Bundle();
    args.putParcelable(BaseActivity.ARG_COMIC_BOOK, comicBook);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    barcodeImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
    byte[] byteArray = stream.toByteArray();
    args.putByteArray(BaseActivity.ARG_SNAPSHOT, byteArray);
    fragment.setArguments(args);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnManualSearchListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mComicBook = arguments.getParcelable(BaseActivity.ARG_COMIC_BOOK);
      byte[] byteArray = arguments.getByteArray(BaseActivity.ARG_SNAPSHOT);
      if (byteArray != null) {
        mImageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
      }
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_manual_search, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    LogUtils.debug(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    LogUtils.debug(TAG, "++onViewCreated(View, Bundle)");

    // 2 Scenarios:
    //   1) ComicBook has an ID (Publisher & Series), we just need the issue
    //   2) ComicBook is empty/null, we need Publisher, Series, & Issue

    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v -> {

      mComicBook.parseProductCode(mProductCodeEdit.getText().toString().trim());
      mComicBook.parseIssueCode(mIssueCodeEdit.getText().toString().trim());
      if (mComicBook == null || !mComicBook.isValid()) {
        mCallback.onManualSearchCancel();
      } else {
        mCallback.onManualSearchActionComplete(mComicBook);
      }
    });

    if (mImageBitmap != null) {
      ImageView snapShot = view.findViewById(R.id.manual_search_image_snapshot);
      snapShot.setImageBitmap(mImageBitmap);
    }

    mProductCodeEdit = view.findViewById(R.id.manual_search_edit_product);
    mIssueCodeEdit = view.findViewById(R.id.manual_search_edit_issue);
    TextView messageText = view.findViewById(R.id.manual_search_text_no_barcode);
    if (mComicBook == null || !isValid()) {
      mProductCodeEdit.addTextChangedListener(new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
          validateAll();
        }
      });
    } else {
      messageText.setVisibility(View.INVISIBLE);
      mProductCodeEdit.setText(mComicBook.getProductId());
      mProductCodeEdit.setEnabled(false);
    }

    mIssueCodeEdit.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        validateAll();
      }
    });
  }

  /*
    Private Method(s)
  */
  private boolean isValid() {

    LogUtils.debug(TAG, "++isValid()");
    if (mComicBook.PublisherId == null ||
      mComicBook.PublisherId.equals(BaseActivity.DEFAULT_COMIC_PUBLISHER_ID) ||
      mComicBook.PublisherId.length() != BaseActivity.DEFAULT_COMIC_PUBLISHER_ID.length()) {
      LogUtils.debug(TAG, "Publisher data is unexpected: %s", mComicBook.PublisherId);
      return false;
    }

    if (mComicBook.SeriesId == null ||
      mComicBook.SeriesId.equals(BaseActivity.DEFAULT_COMIC_SERIES_ID) ||
      mComicBook.SeriesId.length() != BaseActivity.DEFAULT_COMIC_SERIES_ID.length()) {
      LogUtils.debug(TAG, "Series data is unexpected: %s", mComicBook.SeriesId);
      return false;
    }
    return true;
  }

  private void validateAll() {

    if (mProductCodeEdit.getText().toString().length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() &&
      !mProductCodeEdit.getText().toString().equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
      mIssueCodeEdit.getText().toString().length() == BaseActivity.DEFAULT_ISSUE_CODE.length() &&
      !mIssueCodeEdit.getText().toString().equals(BaseActivity.DEFAULT_ISSUE_CODE)) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}
