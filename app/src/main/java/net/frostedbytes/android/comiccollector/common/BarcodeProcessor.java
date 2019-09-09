package net.frostedbytes.android.comiccollector.common;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;

public class BarcodeProcessor {

  private static final String TAG = BaseActivity.BASE_TAG + "BarcodeProcessor";

  public interface OnBarcodeProcessorListener {

    void onBarcodeProcessed(String barcode);
  }

  // To keep the latest images and its metadata.
  @GuardedBy("this")
  private ByteBuffer mLatestImage;

  @GuardedBy("this")
  private FrameMetadata mLatestImageMetaData;

  // To keep the images and metadata in process.
  @GuardedBy("this")
  private ByteBuffer mProcessingImage;

  @GuardedBy("this")
  private FrameMetadata mProcessingMetaData;

  private OnBarcodeProcessorListener mCallback;
  private final FirebaseVisionBarcodeDetector detector;

  public BarcodeProcessor(Context context) {

    try {
      mCallback = (OnBarcodeProcessorListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    FirebaseVisionBarcodeDetectorOptions options =
      new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_UPC_A, FirebaseVisionBarcode.FORMAT_UPC_E)
        .build();
    detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
  }

  public synchronized void process(ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {

    LogUtils.debug(TAG, "++process(ByteBuffer, FrameMetadata, GraphicOverlay)");
    mLatestImage = data;
    mLatestImageMetaData = frameMetadata;
    if (mProcessingImage == null && mProcessingMetaData == null) {
      processLatestImage(graphicOverlay);
    }
  }

  /*
    Private Method(s)
   */
  private void detectInVisionImage(
    final Bitmap originalCameraImage,
    FirebaseVisionImage image,
    final FrameMetadata metadata,
    final GraphicOverlay graphicOverlay) {

    LogUtils.debug(TAG, "++detectInVisionImage(Bitmap, FirebaseVisionImage, FrameMetadata, GraphicOverlay)");
    detectInImage(image)
      .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
        @Override
        public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
          BarcodeProcessor.this.onSuccess(originalCameraImage, firebaseVisionBarcodes, metadata, graphicOverlay);
            processLatestImage(graphicOverlay);
          }
        })
      .addOnFailureListener(
        new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            BarcodeProcessor.this.onFailure(e);
          }
        });
  }

  private void processImage(ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {

    LogUtils.debug(TAG, "++processImage(ByteBuffer, FrameMetadata, GraphicOverlay)");
    FirebaseVisionImageMetadata metadata =
      new FirebaseVisionImageMetadata.Builder()
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        .setWidth(frameMetadata.getWidth())
        .setHeight(frameMetadata.getHeight())
        .setRotation(frameMetadata.getRotation())
        .build();

    Bitmap bitmap = BitmapUtils.getBitmap(data, frameMetadata);
    detectInVisionImage(bitmap, FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicOverlay);
  }

  private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {

    LogUtils.debug(TAG, "++processLatestImage(GraphicOverlay)");
    mProcessingImage = mLatestImage;
    mProcessingMetaData = mLatestImageMetaData;
    mLatestImage = null;
    mLatestImageMetaData = null;
    if (mProcessingImage != null && mProcessingMetaData != null) {
      processImage(mProcessingImage, mProcessingMetaData, graphicOverlay);
    }
  }

  public void stop() {

    LogUtils.debug(TAG, "++stop()");
    try {
      detector.close();
    } catch (IOException e) {
      LogUtils.error(TAG, "Exception thrown while trying to close Barcode Detector: %s", e.getLocalizedMessage());
    }
  }

  protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {

    LogUtils.debug(TAG, "++detectInImage(FirebaseVisionImage)");
    return detector.detectInImage(image);
  }

  protected void onSuccess(
    @Nullable Bitmap originalCameraImage,
    @NonNull List<FirebaseVisionBarcode> barcodes,
    @NonNull FrameMetadata frameMetadata,
    @NonNull GraphicOverlay graphicOverlay) {

    LogUtils.debug(TAG, "++onSuccess(Bitmap, List<FirebaseVisionBarcode>, FrameMetadata, GraphicOverlay)");
    graphicOverlay.clear();
    if (originalCameraImage != null) {
      CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
      graphicOverlay.add(imageGraphic);
    }

    for (int i = 0; i < barcodes.size(); ++i) {
      FirebaseVisionBarcode barcode = barcodes.get(i);
      mCallback.onBarcodeProcessed(barcode.getDisplayValue());
    }

    graphicOverlay.postInvalidate();
  }

  protected void onFailure(@NonNull Exception e) {

    LogUtils.debug(TAG, "++onFailure(Exception)");
    LogUtils.error(TAG, "Barcode detection failed: %s", e.getMessage());
  }
}
