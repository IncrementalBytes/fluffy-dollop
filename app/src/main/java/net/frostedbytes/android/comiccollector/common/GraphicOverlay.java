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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.ArrayList;
import java.util.List;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview). The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 *
 * <p>Supports scaling and mirroring of the graphics relative the camera's preview properties. The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 *
 * <p>Associated {@link Graphic} items should use the following methods to convert to view
 * coordinates for the graphics that are drawn:
 */
public class GraphicOverlay extends View {

  private final Object lock = new Object();
  private int previewWidth;
  private int previewHeight;
  private final List<Graphic> graphics = new ArrayList<>();

  /**
   * Base class for a custom graphics object to be rendered within the graphic overlay. Subclass
   * this and implement the {@link Graphic#draw(Canvas)} method to define the graphics element. Add
   * instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
   */
  public abstract static class Graphic {

    Graphic(GraphicOverlay overlay) {
    }

    /**
     * Draw the graphic on the supplied canvas. Drawing should use the following methods to convert
     * to view coordinates for the graphics that are drawn:
     * @param canvas drawing canvas
     */
    public abstract void draw(Canvas canvas);
}

  public GraphicOverlay(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   *  Removes all graphics from the overlay.
   */
  public void clear() {

    synchronized (lock) {
      graphics.clear();
    }

    postInvalidate();
  }

  /**
   * Adds a graphic to the overlay.
   */
  public void add(Graphic graphic) {

    synchronized (lock) {
      graphics.add(graphic);
    }
  }

  /**
   * Sets the camera attributes for size and facing direction, which informs how to transform image
   * coordinates later.
   */
  public void setCameraInfo(int previewWidth, int previewHeight, int facing) {

    synchronized (lock) {
      this.previewWidth = previewWidth;
      this.previewHeight = previewHeight;
    }

    postInvalidate();
  }

  /**
   *  Draws the overlay with its associated graphic objects.
   **/
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    synchronized (lock) {
      if ((previewWidth != 0) && (previewHeight != 0)) {
        float widthScaleFactor = (float) getWidth() / (float) previewWidth;
        float heightScaleFactor = (float) getHeight() / (float) previewHeight;
      }

      for (Graphic graphic : graphics) {
        graphic.draw(canvas);
      }
    }
  }
}