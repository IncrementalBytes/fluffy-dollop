package net.frostedbytes.android.comiccollector.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import java.io.IOException;
import net.frostedbytes.android.comiccollector.BaseActivity;
import net.frostedbytes.android.comiccollector.R;
import net.frostedbytes.android.comiccollector.common.BarcodeScanningProcessor;
import net.frostedbytes.android.comiccollector.common.CameraSource;
import net.frostedbytes.android.comiccollector.common.CameraSourcePreview;
import net.frostedbytes.android.comiccollector.common.GraphicOverlay;
import net.frostedbytes.android.comiccollector.common.LogUtils;

public class CameraSourceFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "CameraSourceFragment";

  private CameraSource mCameraSource;
  private CameraSourcePreview mPreview;
  private GraphicOverlay mGraphicOverlay;

  public static CameraSourceFragment newInstance() {

    LogUtils.debug(TAG, "++newInstance()");
    return new CameraSourceFragment();
  }

  /*
  Fragment Override(s)
  */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    return inflater.inflate(R.layout.fragment_camera_source, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    LogUtils.debug(TAG, "++onDetach()");

    if (mCameraSource != null) {
      mCameraSource.release();
    }

    if (mGraphicOverlay != null) {
      mGraphicOverlay.clear();
    }

    if (mPreview != null) {
      mPreview.release();
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mPreview = view.findViewById(R.id.camera_preview);
    if (mPreview == null) {
      LogUtils.debug(TAG, "preview is null");
    }

    mGraphicOverlay = view.findViewById(R.id.camera_graphic_overlay);
    if (mGraphicOverlay == null) {
      LogUtils.debug(TAG, "graphicOverlay is null");
    } else {
      mGraphicOverlay.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {

        }
      });
    }

    if (mCameraSource == null) {
      mCameraSource = new CameraSource(getActivity(), mGraphicOverlay);
    }

    mCameraSource.setMachineLearningFrameProcessor(new BarcodeScanningProcessor(getContext()));
    if (mCameraSource != null) {
      try {
        if (mPreview == null) {
          LogUtils.debug(TAG, "resume: Preview is null");
        }
        if (mGraphicOverlay == null) {
          LogUtils.debug(TAG, "resume: graphOverlay is null");
        }

        mPreview.start(mCameraSource, mGraphicOverlay);
      } catch (IOException e) {
        LogUtils.error(TAG, "Unable to start camera source.", e);
        mCameraSource.release();
        mCameraSource = null;
      }
    }
  }
}
