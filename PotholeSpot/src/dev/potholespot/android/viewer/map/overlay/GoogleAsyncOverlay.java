/*package dev.potholespot.android.viewer.map.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import dev.potholespot.android.viewer.map.GoogleInterface;
import dev.potholespot.android.viewer.map.GoogleLoggerMap;
import dev.potholespot.android.viewer.map.LoggerMap;
import dev.potholespot.android.viewer.map.MapQuestLoggerMap;

public abstract class GoogleAsyncOverlay extends Overlay implements GoogleOverlayProvider
{
   private static final int OFFSET = 20;

   private static final String TAG = "PS.GoogleAsyncOverlay";

   *//**
    * Handler provided by the MapActivity to recalculate graphics
    *//*
   
   private Handler mHandler;

   private GeoPoint mGeoTopLeft;

   private GeoPoint
   mGeoBottumRight;

   private int mWidth;

   private int mHeight;
   
   private int mW;
   private int mH;

   private Bitmap mActiveBitmap;

   private GeoPoint mActiveTopLeft;

   private Point mActivePointTopLeft;

   private Bitmap mCalculationBitmap;

   private Paint mPaint;

   private MapQuestLoggerMap mMapQuestLoggerMap;

   SegmentOsmOverlay mOsmOverlay;

   private SegmentMapQuestOverlay mMapQuestOverlay;

   private int mActiveZoomLevel;

   private Runnable mBitmapUpdater = new Runnable()
   {
      @Override
      public void run()
      {
         postedBitmapUpdater = false;
         mCalculationBitmap.eraseColor(Color.TRANSPARENT);
         //mGeoTopLeft = mLoggerMap.fromPixels(mW, mH);
         mGeoBottumRight = mMapQuestLoggerMap.fromPixels(0, 0);
         Canvas calculationCanvas = new Canvas(mCalculationBitmap);
         redrawOffscreen(calculationCanvas, mMapQuestLoggerMap);
         synchronized (mActiveBitmap)
         {
            Bitmap oldActiveBitmap = mActiveBitmap;
            mActiveBitmap = mCalculationBitmap;
            mActiveTopLeft = mGeoTopLeft;
            mCalculationBitmap = oldActiveBitmap;
         }
         mMapQuestLoggerMap.postInvalidate();
      }
   };

   private boolean postedBitmapUpdater;

   GoogleAsyncOverlay(MapQuestLoggerMap mLoggerMap, Handler handler)
   {
      mMapQuestLoggerMap = mLoggerMap;
      mHandler = handler;
      mWidth = 1;
      mHeight = 1;
      mW = 0;
      mH = 0;
      mPaint = new Paint();
      mActiveZoomLevel = -1;
      mActiveBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
      mActiveTopLeft = new GeoPoint(0, 0);
      mActivePointTopLeft = new Point();
      mCalculationBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);

      mOsmOverlay = new SegmentOsmOverlay(mMapQuestLoggerMap.getActivity(), mMapQuestLoggerMap, this);
      mMapQuestOverlay = new SegmentMapQuestOverlay(this);
   }

   protected void reset()
   {
      synchronized (mActiveBitmap)
      {
         mCalculationBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
         mActiveBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      }
   }

   protected void considerRedrawOffscreen()
  {
      int oldZoomLevel = mActiveZoomLevel;
      mActiveZoomLevel = mMapQuestLoggerMap.getZoomLevel();

      boolean needNewCalculation = false;

      if (mCalculationBitmap.getWidth() != mWidth || mCalculationBitmap.getHeight() != mHeight)
      {
         mCalculationBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
         needNewCalculation = true;
      }

      boolean unaligned = isOutAlignment();
      if (needNewCalculation || mActiveZoomLevel != oldZoomLevel || unaligned)
      {
         scheduleRecalculation();
      }
   }

   private boolean isOutAlignment()
   {
      Point screenPoint = new Point(0, 0);
      if (mGeoTopLeft != null)
      {
         mMapQuestLoggerMap.toPixels(mGeoTopLeft, screenPoint);
      }
      return mGeoTopLeft == null || mGeoBottumRight == null || screenPoint.x > OFFSET || screenPoint.y > OFFSET || screenPoint.x < -OFFSET
            || screenPoint.y < -OFFSET;
   }

   public void onDateOverlayChanged()
   {
      if (!postedBitmapUpdater)
      {
         postedBitmapUpdater = true;
         mHandler.post(mBitmapUpdater);
      }
   }

   protected abstract void redrawOffscreen(Canvas asyncBuffer, GoogleInterface mLoggerMap2);

   protected abstract void scheduleRecalculation();

   *//**
    * {@inheritDoc}
    *//*
   @Override
   public void draw(Canvas canvas, MapView mapView, boolean shadow)
   {
      if (!shadow)
      {
         draw(canvas);
      }
   }

   private void draw(Canvas canvas)
   {
      mWidth = canvas.getWidth();
      mHeight = canvas.getHeight();
      considerRedrawOffscreen();

      if (mActiveBitmap.getWidth() > 1)
      {
         synchronized (mActiveBitmap)
         {
            mMapQuestLoggerMap.toPixels(mActiveTopLeft, mActivePointTopLeft);
            canvas.drawBitmap(mActiveBitmap, mActivePointTopLeft.x, mActivePointTopLeft.y, mPaint);
         }
      }
   }

   protected boolean isPointOnScreen(Point point)
   {
      return point.x < 0 || point.y < 0 || point.x > mWidth || point.y > mHeight;
   }

   @Override
   public boolean onTap(LatLng tappedGeoPoint, MapView mapview)
   {
      return commonOnTap(tappedGeoPoint);
   }
   
   *//**************************************//*
   *//** Multi map support **//*
   *//**************************************//*

   @Override
   public Overlay getGoogleOverlay()
   {
      return this;
   }

   @Override
   public org.osmdroid.views.overlay.Overlay getOSMOverlay()
   {
      return mOsmOverlay;
   }

   @Override
   public com.mapquest.android.maps.Overlay getMapQuestOverlay()
   {
      return mMapQuestOverlay;
   }

   protected abstract boolean commonOnTap(LatLng tappedGeoPoint);

   static class SegmentOsmOverlay extends org.osmdroid.views.overlay.Overlay
   {
      GoogleAsyncOverlay mSegmentOverlay;
      MapQuestLoggerMap mLoggerMap;
      GoogleLoggerMap mGoogleLoggerMap;

      public SegmentOsmOverlay(Context ctx, MapQuestLoggerMap mLoggerMap2, GoogleAsyncOverlay segmentOverlay)
      {
         super(ctx);
         mLoggerMap = mLoggerMap2;
         mSegmentOverlay = segmentOverlay;
      }

      public GoogleAsyncOverlay getSegmentOverlay()
      {
         return mSegmentOverlay;
      }

      @Override
      public boolean onSingleTapUp(MotionEvent e, org.osmdroid.views.MapView openStreetMapView)
      {
         int x = (int) e.getX();
         int y = (int) e.getY();
         GeoPoint tappedGeoPoint = mLoggerMap.fromPixels(x, y);
         LatLng tapppedLatLong = mGoogleLoggerMap.fromPixels(x, y);
         return mSegmentOverlay.commonOnTap(tapppedLatLong);
      }

      @Override
      protected void draw(Canvas canvas, org.osmdroid.views.MapView view, boolean shadow)
      {
         if (!shadow)
         {
            mSegmentOverlay.draw(canvas);
         }
      }
   }

   static class SegmentMapQuestOverlay extends com.mapquest.android.maps.Overlay
   {
      GoogleAsyncOverlay mSegmentOverlay;

      public SegmentMapQuestOverlay(GoogleAsyncOverlay segmentOverlay)
      {
         super();
         mSegmentOverlay = segmentOverlay;
      }

      public GoogleAsyncOverlay getSegmentOverlay()
      {
         return mSegmentOverlay;
      }

      @Override
      public boolean onTap(com.mapquest.android.maps.GeoPoint p, com.mapquest.android.maps.MapView mapView)
      {
         LatLng tappedGeoPoint = new LatLng(p.getLatitudeE6(), p.getLongitudeE6());
         return mSegmentOverlay.commonOnTap(tappedGeoPoint);
      }

      @Override
      public void draw(Canvas canvas, com.mapquest.android.maps.MapView mapView, boolean shadow)
      {
         if (!shadow)
         {
            mSegmentOverlay.draw(canvas);
         }
      }

   }

   protected void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap)
   {
      // TODO Auto-generated method stub
      
   }

   protected boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      // TODO Auto-generated method stub
      return false;
   }
}
*/