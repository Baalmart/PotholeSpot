package dev.potholespot.android.viewer.map.overlay;

import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.Overlay;

import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.Overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.view.MotionEvent;
import dev.potholespot.android.viewer.map.LoggerMap;

public abstract class AsyncOverlay extends Overlay implements OverlayProvider
{
   private static final int OFFSET = 20;

   private static final String TAG = "PS.AsyncOverlay";

   /**
    * Handler provided by the MapActivity to recalculate graphics
    */

   private final Handler mHandler;

   private GeoPoint mGeoTopLeft;

   private GeoPoint mGeoBottumRight;

   private int mWidth;

   private int mHeight;

   private Bitmap mActiveBitmap;

   private GeoPoint mActiveTopLeft;

   private final Point mActivePointTopLeft;

   private Bitmap mCalculationBitmap;

   private final Paint mPaint;

   private final LoggerMap mLoggerMap;

   SegmentOsmOverlay mOsmOverlay;

   private final SegmentMapQuestOverlay mMapQuestOverlay;

   private int mActiveZoomLevel;

   private final Runnable mBitmapUpdater = new Runnable()
      {
         @Override
         public void run()
         {
            AsyncOverlay.this.postedBitmapUpdater = false;
            AsyncOverlay.this.mCalculationBitmap.eraseColor(Color.TRANSPARENT);
            AsyncOverlay.this.mGeoTopLeft = AsyncOverlay.this.mLoggerMap.fromPixels(0, 0);
            AsyncOverlay.this.mGeoBottumRight = AsyncOverlay.this.mLoggerMap.fromPixels(AsyncOverlay.this.mWidth, AsyncOverlay.this.mHeight);
            final Canvas calculationCanvas = new Canvas(AsyncOverlay.this.mCalculationBitmap);
            redrawOffscreen(calculationCanvas, AsyncOverlay.this.mLoggerMap);
            synchronized (AsyncOverlay.this.mActiveBitmap)
            {
               final Bitmap oldActiveBitmap = AsyncOverlay.this.mActiveBitmap;
               AsyncOverlay.this.mActiveBitmap = AsyncOverlay.this.mCalculationBitmap;
               AsyncOverlay.this.mActiveTopLeft = AsyncOverlay.this.mGeoTopLeft;
               AsyncOverlay.this.mCalculationBitmap = oldActiveBitmap;
            }
            AsyncOverlay.this.mLoggerMap.postInvalidate();
         }
      };

   private boolean postedBitmapUpdater;

   AsyncOverlay(final LoggerMap loggermap, final Handler handler)
   {
      this.mLoggerMap = loggermap;
      this.mHandler = handler;
      this.mWidth = 1;
      this.mHeight = 1;
      this.mPaint = new Paint();
      this.mActiveZoomLevel = -1;
      this.mActiveBitmap = Bitmap.createBitmap(this.mWidth, this.mHeight, Bitmap.Config.ARGB_8888);
      this.mActiveTopLeft = new GeoPoint(0, 0);
      this.mActivePointTopLeft = new Point();
      this.mCalculationBitmap = Bitmap.createBitmap(this.mWidth, this.mHeight, Bitmap.Config.ARGB_8888);

      this.mOsmOverlay = new SegmentOsmOverlay(this.mLoggerMap.getActivity(), this.mLoggerMap, this);
      this.mMapQuestOverlay = new SegmentMapQuestOverlay(this);
   }

   protected void reset()
   {
      synchronized (this.mActiveBitmap)
      {
         this.mCalculationBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
         this.mActiveBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      }
   }

   protected void considerRedrawOffscreen()
   {
      final int oldZoomLevel = this.mActiveZoomLevel;
      this.mActiveZoomLevel = this.mLoggerMap.getZoomLevel();

      boolean needNewCalculation = false;

      if (this.mCalculationBitmap.getWidth() != this.mWidth || this.mCalculationBitmap.getHeight() != this.mHeight)
      {
         this.mCalculationBitmap = Bitmap.createBitmap(this.mWidth, this.mHeight, Bitmap.Config.ARGB_8888);
         needNewCalculation = true;
      }

      final boolean unaligned = isOutAlignment();
      if (needNewCalculation || this.mActiveZoomLevel != oldZoomLevel || unaligned)
      {
         scheduleRecalculation();
      }
   }

   private boolean isOutAlignment()
   {
      final Point screenPoint = new Point(0, 0);
      if (this.mGeoTopLeft != null)
      {
         this.mLoggerMap.toPixels(this.mGeoTopLeft, screenPoint);
      }
      return this.mGeoTopLeft == null || this.mGeoBottumRight == null || screenPoint.x > OFFSET || screenPoint.y > OFFSET || screenPoint.x < -OFFSET || screenPoint.y < -OFFSET;
   }

   public void onDateOverlayChanged()
   {
      if (!this.postedBitmapUpdater)
      {
         this.postedBitmapUpdater = true;
         this.mHandler.post(this.mBitmapUpdater);
      }
   }

   protected abstract void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap);

   protected abstract void scheduleRecalculation();

   /**
    * {@inheritDoc}
    */
   public void draw(final Canvas canvas, final MapView mapView, final boolean shadow)
   {
      if (!shadow)
      {
         draw(canvas);
      }
   }

   private void draw(final Canvas canvas)
   {
      this.mWidth = canvas.getWidth();
      this.mHeight = canvas.getHeight();
      considerRedrawOffscreen();

      if (this.mActiveBitmap.getWidth() > 1)
      {
         synchronized (this.mActiveBitmap)
         {
            this.mLoggerMap.toPixels(this.mActiveTopLeft, this.mActivePointTopLeft);
            canvas.drawBitmap(this.mActiveBitmap, this.mActivePointTopLeft.x, this.mActivePointTopLeft.y, this.mPaint);
         }
      }
   }

   protected boolean isPointOnScreen(final Point point)
   {
      return point.x < 0 || point.y < 0 || point.x > this.mWidth || point.y > this.mHeight;
   }

   public boolean onTap(final GeoPoint tappedGeoPoint, final MapView mapview)
   {
      return commonOnTap(tappedGeoPoint);
   }

   /**************************************/
   /** Multi map support **/
   /**************************************/

   /*
   @Override
   public Overlay getGoogleOverlay()
   {
      return this;
   }
   */

   @Override
   public org.osmdroid.views.overlay.Overlay getOSMOverlay()
   {
      return this.mOsmOverlay;
   }

   @Override
   public com.mapquest.android.maps.Overlay getMapQuestOverlay()
   {
      return this.mMapQuestOverlay;
   }

   protected abstract boolean commonOnTap(GeoPoint tappedGeoPoint);

   static class SegmentOsmOverlay extends org.osmdroid.views.overlay.Overlay
   {
      AsyncOverlay mSegmentOverlay;
      LoggerMap mLoggerMap;

      public SegmentOsmOverlay(final Context ctx, final LoggerMap map, final AsyncOverlay segmentOverlay)
      {
         super(ctx);
         this.mLoggerMap = map;
         this.mSegmentOverlay = segmentOverlay;
      }

      public AsyncOverlay getSegmentOverlay()
      {
         return this.mSegmentOverlay;
      }

      @Override
      public boolean onSingleTapUp(final MotionEvent e, final org.osmdroid.views.MapView openStreetMapView)
      {
         final int x = (int) e.getX();
         final int y = (int) e.getY();
         final GeoPoint tappedGeoPoint = this.mLoggerMap.fromPixels(x, y);
         return this.mSegmentOverlay.commonOnTap(tappedGeoPoint);
      }

      @Override
      protected void draw(final Canvas canvas, final org.osmdroid.views.MapView view, final boolean shadow)
      {
         if (!shadow)
         {
            this.mSegmentOverlay.draw(canvas);
         }
      }
   }

   static class SegmentMapQuestOverlay extends com.mapquest.android.maps.Overlay
   {
      AsyncOverlay mSegmentOverlay;

      public SegmentMapQuestOverlay(final AsyncOverlay segmentOverlay)
      {
         super();
         this.mSegmentOverlay = segmentOverlay;
      }

      public AsyncOverlay getSegmentOverlay()
      {
         return this.mSegmentOverlay;
      }

      @Override
      public boolean onTap(final com.mapquest.android.maps.GeoPoint p, final com.mapquest.android.maps.MapView mapView)
      {
         final GeoPoint tappedGeoPoint = new GeoPoint(p.getLatitudeE6(), p.getLongitudeE6());
         return this.mSegmentOverlay.commonOnTap(tappedGeoPoint);
      }

      @Override
      public void draw(final Canvas canvas, final com.mapquest.android.maps.MapView mapView, final boolean shadow)
      {
         if (!shadow)
         {
            this.mSegmentOverlay.draw(canvas);
         }
      }

   }
}
