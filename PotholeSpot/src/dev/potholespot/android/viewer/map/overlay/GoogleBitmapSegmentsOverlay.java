/*package dev.potholespot.android.viewer.map.overlay;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.maps.GeoPoint;

import dev.potholespot.android.viewer.map.GoogleInterface;
import dev.potholespot.android.viewer.map.LoggerMap;
import dev.potholespot.android.viewer.map.MapQuestLoggerMap;

public class GoogleBitmapSegmentsOverlay extends GoogleAsyncOverlay
{
   private static final String TAG = "PS.BitmapSegmentsOverlay";

   List<GoogleSegmentRendering> mOverlays;
   Handler mOverlayHandler;

   public GoogleBitmapSegmentsOverlay(MapQuestLoggerMap mLoggerMap, Handler handler)
   {
      super(mLoggerMap, handler);
      mOverlays = new LinkedList<GoogleSegmentRendering>();
      mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap)
   {
      for (GoogleSegmentRendering segment : mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }

   @Override
   public synchronized void scheduleRecalculation()
   {
      for (GoogleSegmentRendering segment : mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack();
      }
   }

   @Override
   synchronized protected boolean commonOnTap(LatLng tappedGeoPoint)
   {
      boolean handled = false;
      for (GoogleSegmentRendering segment : mOverlays)
      {
         if (!handled)
         {
            handled = segment.commonOnTap(tappedGeoPoint);
         }
      }
      return handled;
   }
//used to add segments on the map
   synchronized public void addSegment(GoogleSegmentRendering segment)
   {
      segment.setBitmapHolder(this);
      mOverlays.add(segment);
   }

   synchronized public void clearSegments()
   {
      for (GoogleSegmentRendering segment : mOverlays)
      {
         segment.closeResources();
      }
      mOverlays.clear();
      reset();
   }

   //set the color of using the setTrackColorMethod
   synchronized public void setTrackColoringMethod(int color, double speed, double height)
   {
      for (GoogleSegmentRendering segment : mOverlays)
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

   @Override
   protected void redrawOffscreen(Canvas asyncBuffer, GoogleInterface mLoggerMap2)
   {
      // TODO Auto-generated method stub
      
   }

 
}
*/