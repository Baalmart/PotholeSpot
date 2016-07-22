package dev.potholespot.android.viewer.map.overlay;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import dev.potholespot.android.viewer.map.LoggerMap;

public class BitmapSegmentsOverlay extends AsyncOverlay
{
   private static final String TAG = "PS.BitmapSegmentsOverlay";

   List<SegmentRendering> mOverlays;
   Handler mOverlayHandler;

   public BitmapSegmentsOverlay(LoggerMap loggermap, Handler handler)
   {
      super(loggermap, handler);
      mOverlays = new LinkedList<SegmentRendering>();
      mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap)
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }

   @Override
   public synchronized void scheduleRecalculation()
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack();
      }
   }

   @Override
   synchronized protected boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      boolean handled = false;
      for (SegmentRendering segment : mOverlays)
      {
         if (!handled)
         {
            handled = segment.commonOnTap(tappedGeoPoint);
         }
      }
      return handled;
   }
//used to add segments on the map
   synchronized public void addSegment(SegmentRendering segment)
   {
      segment.setBitmapHolder(this);
      mOverlays.add(segment);
   }

   synchronized public void clearSegments()
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.closeResources();
      }
      mOverlays.clear();
      reset();
   }

   //set the color of using the setTrackColorMethod
   synchronized public void setTrackColoringMethod(int color, double speed, double height)
   {
      for (SegmentRendering segment : mOverlays)
      {
         segment.setTrackColoringMethod(color, speed, height);
      }
      scheduleRecalculation();
   }
//size of the arrayList
   public int size()
   {
      return mOverlays.size();
   }
}