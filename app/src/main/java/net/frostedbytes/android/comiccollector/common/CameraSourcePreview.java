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
package net.frostedbytes.android.comiccollector.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;

import java.io.IOException;
import net.frostedbytes.android.comiccollector.BaseActivity;

public class CameraSourcePreview extends ViewGroup {

  private static final String TAG = BaseActivity.BASE_TAG + "CameraSourcePreview";

  private CameraSource mCameraSource;
  private Context mContext;
  private GraphicOverlay mOverlay;
  private boolean mStartRequested;
  private boolean mSurfaceAvailable;

  public CameraSourcePreview(Context context, AttributeSet attrs) {
    super(context, attrs);

    mContext = context;
    mStartRequested = false;
    mSurfaceAvailable = false;
    SurfaceView mSurfaceView = new SurfaceView(context);
    mSurfaceView.getHolder().addCallback(new SurfaceCallback());
    addView(mSurfaceView);
  }

  public void start(CameraSource cameraSource) throws IOException {

    if (cameraSource == null) {
      stop();
    }

    mCameraSource = cameraSource;

    if (mCameraSource != null) {
      mStartRequested = true;
      startIfReady();
    }
  }

  public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {

    mOverlay = overlay;
    start(cameraSource);
  }

  public void stop() {

    if (mCameraSource != null) {
      mCameraSource.stop();
    }
  }

  public void release() {

    if (mCameraSource != null) {
      mCameraSource.release();
      mCameraSource = null;
    }
  }

  @SuppressLint("MissingPermission")
  private void startIfReady() throws IOException {

    if (mStartRequested && mSurfaceAvailable) {
      mCameraSource.start();
      if (mOverlay != null) {
        Size size = mCameraSource.getPreviewSize();
        int min = Math.min(size.getWidth(), size.getHeight());
        int max = Math.max(size.getWidth(), size.getHeight());
        if (isPortraitMode()) { // Swap width and height sizes when in portrait, since it will be rotated by90 degrees
          mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
        } else {
          mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
        }

        mOverlay.clear();
      }

      mStartRequested = false;
    }
  }

  private class SurfaceCallback implements SurfaceHolder.Callback {

    @Override
    public void surfaceCreated(SurfaceHolder surface) {

      mSurfaceAvailable = true;
      try {
        startIfReady();
      } catch (IOException e) {
        LogUtils.error(TAG, "Could not start camera source: %s", e.getMessage());
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surface) {
      mSurfaceAvailable = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
  }

  @SuppressWarnings("SuspiciousNameCombination")
  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

    int width = 320;
    int height = 240;
    if (mCameraSource != null) {
      Size size = mCameraSource.getPreviewSize();
      if (size != null) {
        width = size.getWidth();
        height = size.getHeight();
      }
    }

    // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
    if (isPortraitMode()) {
      int tmp = width;
      width = height;
      height = tmp;
    }

    final int layoutWidth = right - left;
    final int layoutHeight = bottom - top;

    // Computes height and width for potentially doing fit width.
    int childWidth = layoutWidth;
    int childHeight = (int) (((float) layoutWidth / (float) width) * height);

    // If height is too tall using fit width, does fit height instead.
    if (childHeight > layoutHeight) {
      childHeight = layoutHeight;
      childWidth = (int) (((float) layoutHeight / (float) height) * width);
    }

    for (int i = 0; i < getChildCount(); ++i) {
      getChildAt(i).layout(0, 0, childWidth, childHeight);
      LogUtils.debug(TAG, "Assigned view: %d", i);
    }

    try {
      startIfReady();
    } catch (IOException e) {
      LogUtils.error(TAG, "Could not start camera source: %s", e.getMessage());
    }
  }

  private boolean isPortraitMode() {

    int orientation = mContext.getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      return false;
    }
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      return true;
    }

    LogUtils.debug(TAG, "isPortraitMode returning false by default");
    return false;
  }
}