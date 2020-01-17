package net.whollynugatory.android.comiccollector.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.whollynugatory.android.comiccollector.BaseActivity;
import net.whollynugatory.android.comiccollector.R;

public class Camera2Fragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "Camera2Fragment";
  private static final int REQUEST_CAMERA_PERMISSION = 200;
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  private TextureView textureView;

  private Handler mBackgroundHandler;
  private HandlerThread mBackgroundThread;
  private CameraCaptureSession mCameraCaptureSessions;
  private CameraDevice mCameraDevice;
  private CaptureRequest.Builder mCaptureRequestBuilder;
  private File mFile;
  private Size mImageDimension;
  private ImageReader mImageReader;

  public static Camera2Fragment newInstance() {

    Log.d(TAG, "++newInstance()");
    Camera2Fragment fragment = new Camera2Fragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  /*
  Fragment Override(s)
  */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_camera2, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
    startBackgroundThread();
    if (textureView.isAvailable()) {
      openCamera();
    } else {
      textureView.setSurfaceTextureListener(textureListener);
    }
  }

  @Override
  public void onPause() {

    Log.d(TAG, "++onPause()");
//    closeCamera();
    stopBackgroundThread();
    super.onPause();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
        // TODO: Toast.makeText(getActivity(), "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
        // TODO:
      }
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Log.d(TAG, "++onViewCreated(View, Bundle)");
    textureView = view.findViewById(R.id.camera2_texture);
    assert textureView != null;
    textureView.setSurfaceTextureListener(new SurfaceTextureListener() {
      @Override
      public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

      }

      @Override
      public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

      }

      @Override
      public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
      }

      @Override
      public void onSurfaceTextureUpdated(SurfaceTexture surface) {

      }
    });
    Button takePictureButton = view.findViewById(R.id.camera2_button_confirm);
    assert takePictureButton != null;
    takePictureButton.setOnClickListener(v -> takePicture());
  }

  private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

      openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

      // TODO: transform image captured size according to the surface width and height
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

      return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
  };

  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

    @Override
    public void onOpened(@NonNull CameraDevice camera) {

      Log.d(TAG, "++onOpened()");
      mCameraDevice = camera;
      createCameraPreview();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {

      mCameraDevice.close();
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {

      mCameraDevice.close();
      mCameraDevice = null;
    }
  };

  final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {

    @Override
    public void onCaptureCompleted(
      @NonNull CameraCaptureSession session,
      @NonNull CaptureRequest request,
      @NonNull TotalCaptureResult result) {
      super.onCaptureCompleted(session, request, result);

      // TODO: Toast.makeText(getActivity(), "Saved:" + file, Toast.LENGTH_SHORT).show();
      createCameraPreview();
    }
  };

  private void startBackgroundThread() {

    mBackgroundThread = new HandlerThread("Camera Background");
    mBackgroundThread.start();
    mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
  }

  private void stopBackgroundThread() {

    mBackgroundThread.quitSafely();
    try {
      mBackgroundThread.join();
      mBackgroundThread = null;
      mBackgroundHandler = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void takePicture() {

    if (null == mCameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }

    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    try {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
      Size[] jpegSizes = null;
      jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
      int width = 640;
      int height = 480;
      if (jpegSizes != null && 0 < jpegSizes.length) {
        width = jpegSizes[0].getWidth();
        height = jpegSizes[0].getHeight();
      }
      ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
      List<Surface> outputSurfaces = new ArrayList<Surface>(2);
      outputSurfaces.add(reader.getSurface());
      outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
      final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureBuilder.addTarget(reader.getSurface());
      captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

      int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
      captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
      final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");

      ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

          try (Image image = reader.acquireLatestImage()) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            save(bytes);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }

        private void save(byte[] bytes) throws IOException {

          try (OutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
          }
        }
      };

      reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
      final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(
          @NonNull CameraCaptureSession session,
          @NonNull CaptureRequest request,
          @NonNull TotalCaptureResult result) {
          super.onCaptureCompleted(session, request, result);

          // TODO: Toast.makeText(getActivity(), "Saved:" + file, Toast.LENGTH_SHORT).show();
          createCameraPreview();
        }
      };

      mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {

          try {
            session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
      }, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void createCameraPreview() {

    try {
      SurfaceTexture texture = textureView.getSurfaceTexture();
      assert texture != null;
      texture.setDefaultBufferSize(mImageDimension.getWidth(), mImageDimension.getHeight());
      Surface surface = new Surface(texture);
      mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      mCaptureRequestBuilder.addTarget(surface);
      mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

          if (null == mCameraDevice) {
            return;
          }

          mCameraCaptureSessions = cameraCaptureSession;
          updatePreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

          // TODO: Toast.makeText(getActivity(), "Configuration change", Toast.LENGTH_SHORT).show();
        }
      }, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void openCamera() {

    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    Log.d(TAG, "is camera open");
    try {
      String mCameraId = manager.getCameraIdList()[0];
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      assert map != null;
      mImageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
      if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat
          .requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
            REQUEST_CAMERA_PERMISSION);
        return;
      }

      manager.openCamera(mCameraId, stateCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }

    Log.d(TAG, "openCamera X");
  }

  private void updatePreview() {

    if (null == mCameraDevice) {
      Log.e(TAG, "updatePreview error, camera device is null.");
    }

    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    try {
      mCameraCaptureSessions.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void closeCamera() {

    if (null != mCameraDevice) {
      mCameraDevice.close();
      mCameraDevice = null;
    }

    if (null != mImageReader) {
      mImageReader.close();
      mImageReader = null;
    }
  }
}
