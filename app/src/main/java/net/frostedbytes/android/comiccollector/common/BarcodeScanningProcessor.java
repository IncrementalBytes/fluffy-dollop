package net.frostedbytes.android.comiccollector.common;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.frostedbytes.android.comiccollector.BaseActivity;

public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {

  private static final String TAG = BaseActivity.BASE_TAG + "BarcodeScanningProcessor";

  public interface OnBarcodeScanningListener {

    void onBarcodeProcessed(String barcode);
  }

  private OnBarcodeScanningListener mCallback;
  private final FirebaseVisionBarcodeDetector detector;

  public BarcodeScanningProcessor(Context context) {

    try {
      mCallback = (OnBarcodeScanningListener) context;
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

  @Override
  public void stop() {

    try {
      detector.close();
    } catch (IOException e) {
      LogUtils.error(TAG, "Exception thrown while trying to close Barcode Detector: %s", e.getLocalizedMessage());
    }
  }

  @Override
  protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
    return detector.detectInImage(image);
  }

  @Override
  protected void onSuccess(
    @Nullable Bitmap originalCameraImage,
    @NonNull List<FirebaseVisionBarcode> barcodes,
    @NonNull FrameMetadata frameMetadata,
    @NonNull GraphicOverlay graphicOverlay) {

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

  @Override
  protected void onFailure(@NonNull Exception e) {
    LogUtils.error(TAG, "Barcode detection failed: %s", e.getMessage());
  }
}
