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

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.Chip;
import com.google.common.base.Objects;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import java.io.IOException;
import java.util.Locale;
import net.whollynugatory.android.comiccollector.R;
import net.whollynugatory.android.comiccollector.barcodedetection.BarcodeProcessor;
import net.whollynugatory.android.comiccollector.camera.CameraSource;
import net.whollynugatory.android.comiccollector.camera.CameraSourcePreview;
import net.whollynugatory.android.comiccollector.camera.GraphicOverlay;
import net.whollynugatory.android.comiccollector.camera.WorkflowModel;
import net.whollynugatory.android.comiccollector.ui.BaseActivity;

public class BarcodeScanFragment extends Fragment implements View.OnClickListener {

  private static final String TAG = BaseActivity.BASE_TAG + "BarcodeScanFragment";

  public interface OnBarcodeScanListener {

    void onBarcodeManual();
    void onBarcodeScanClose();
    void onBarcodeScanned(String barcodeValue);
    void onBarcodeScanSettings();
  }

  private OnBarcodeScanListener mCallback;

  private CameraSource mCameraSource;
  private WorkflowModel.WorkflowState mCurrentWorkflowState;
  private View mFlashButton;
  private GraphicOverlay mGraphicOverlay;
  private CameraSourcePreview mPreview;
  private Chip mPromptChip;
  private AnimatorSet mPromptChipAnimator;
  private View mSettingsButton;
  private WorkflowModel mWorkflowModel;

  public static BarcodeScanFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new BarcodeScanFragment();
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnBarcodeScanListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_barcode_scan, container, false);
    View closeButton = view.findViewById(R.id.close_button);
    mFlashButton = view.findViewById(R.id.flash_button);
    mGraphicOverlay = view.findViewById(R.id.camera_preview_graphic_overlay);
    View manualButton = view.findViewById(R.id.manual_button);
    mPreview = view.findViewById(R.id.camera_preview);
    mPromptChip = view.findViewById(R.id.bottom_prompt_chip);
    mSettingsButton = view.findViewById(R.id.settings_button);

    mGraphicOverlay.setOnClickListener(this);
    mCameraSource = new CameraSource(mGraphicOverlay);

    mPromptChipAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.bottom_prompt_chip_enter);
    mPromptChipAnimator.setTarget(mPromptChip);

    closeButton.setOnClickListener(this);
    mFlashButton.setOnClickListener(this);
    manualButton.setOnClickListener(this);
    mSettingsButton.setOnClickListener(this);
    return view;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    if (mCameraSource != null) {
      mCameraSource.release();
      mCameraSource = null;
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "++onActivityCreated()");
    setUpWorkflowModel();
  }

  @Override
  public void onPause() {
    super.onPause();

    Log.d(TAG, "++onPause()");
    mCurrentWorkflowState = WorkflowModel.WorkflowState.NOT_STARTED;
    stopCameraPreview();
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
    mWorkflowModel.markCameraFrozen();
    mSettingsButton.setEnabled(true);
    mCurrentWorkflowState = WorkflowModel.WorkflowState.NOT_STARTED;
    mCameraSource.setFrameProcessor(new BarcodeProcessor(mGraphicOverlay, mWorkflowModel));
    mWorkflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTING);
  }

  @Override
  public void onClick(View view) {

    Log.d(TAG, "++onClick(View)");
    switch (view.getId()) {
      case R.id.close_button:
        mCallback.onBarcodeScanClose();
        break;
      case R.id.flash_button:
        if (mFlashButton.isSelected()) {
          mFlashButton.setSelected(false);
          mCameraSource.updateFlashMode(Parameters.FLASH_MODE_OFF);
        } else {
          mFlashButton.setSelected(true);
          mCameraSource.updateFlashMode(Parameters.FLASH_MODE_TORCH);
        }
        break;
      case R.id.manual_button:
        mCallback.onBarcodeManual();
        break;
      case R.id.settings_button:
        mSettingsButton.setEnabled(false);
        mCallback.onBarcodeScanSettings();
        break;
    }
  }

  private void startCameraPreview() {

    Log.d(TAG, "++startCameraPreview()");
    if (!mWorkflowModel.isCameraLive() && mCameraSource != null) {
      try {
        mWorkflowModel.markCameraLive();
        mPreview.start(mCameraSource);
      } catch (IOException e) {
        Log.e(TAG, "Failed to start camera preview!", e);
        mCameraSource.release();
        mCameraSource = null;
      }
    }
  }

  private void stopCameraPreview() {

    Log.d(TAG, "++stopCameraPreview()");
    if (mWorkflowModel.isCameraLive()) {
      mWorkflowModel.markCameraFrozen();
      mFlashButton.setSelected(false);
      mPreview.stop();
    }
  }

  private void setUpWorkflowModel() {

    Log.d(TAG, "++setUpWorkflowModel()");
    mWorkflowModel = new ViewModelProvider(this).get(WorkflowModel.class);

    // observes the workflow state changes, if happens, update the overlay view indicators and camera preview state.
    mWorkflowModel.workflowState.observe(getViewLifecycleOwner(), workflowState -> {

      if (workflowState == null || Objects.equal(mCurrentWorkflowState, workflowState)) {
        return;
      }

      mCurrentWorkflowState = workflowState;
      Log.d(TAG, "Current workflow state: " + mCurrentWorkflowState.name());

      boolean wasPromptChipGone = (mPromptChip.getVisibility() == View.GONE);
      switch (workflowState) {
        case DETECTING:
          mPromptChip.setVisibility(View.VISIBLE);
          mPromptChip.setText(R.string.prompt_point_at_a_barcode);
          startCameraPreview();
          break;
        case CONFIRMING:
          mPromptChip.setVisibility(View.VISIBLE);
          mPromptChip.setText(R.string.prompt_move_camera_closer);
          startCameraPreview();
          break;
        case SEARCHING:
          mPromptChip.setVisibility(View.VISIBLE);
          mPromptChip.setText(R.string.prompt_searching);
          stopCameraPreview();
          break;
        case DETECTED:
        case SEARCHED:
          mPromptChip.setVisibility(View.GONE);
          stopCameraPreview();
          break;
        default:
          mPromptChip.setVisibility(View.GONE);
          break;
      }

      boolean shouldPlayPromptChipEnteringAnimation =
        wasPromptChipGone && (mPromptChip.getVisibility() == View.VISIBLE);
      if (shouldPlayPromptChipEnteringAnimation && !mPromptChipAnimator.isRunning()) {
        mPromptChipAnimator.start();
      }
    });

    mWorkflowModel.detectedBarcode.observe(
      getViewLifecycleOwner(),
      barcode -> {
        if (barcode != null) {
          if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_PRODUCT) {
            mCallback.onBarcodeScanned(barcode.getDisplayValue());
          } else {
            Log.w(TAG, "Unexpected bar code: " + barcode.getDisplayValue());
          }
        }
      });
  }
}
