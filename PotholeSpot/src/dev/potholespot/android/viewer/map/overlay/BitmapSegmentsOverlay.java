package dev.potholespot.android.viewer.map.overlay;

import java.util.LinkedList;
import java.util.List;

import com.mapquest.android.maps.GeoPoint;

import android.graphics.Canvas;
import android.os.Handler;
import dev.potholespot.android.viewer.map.LoggerMap;

public class BitmapSegmentsOverlay extends AsyncOverlay
{
   private static final String TAG = "PS.BitmapSegmentsOverlay";

   List<SegmentRendering> mOverlays;
   Handler mOverlayHandler;

   public BitmapSegmentsOverlay(final LoggerMap loggermap, final Handler handler)
   {
      super(loggermap, handler);
      this.mOverlays = new LinkedList<SegmentRendering>();
      this.mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(final Canvas asyncBuffer, final LoggerMap loggermap)
   {
      for (final SegmentRendering segment : this.mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }

   @Override
   public synchronized void scheduleRecalculation()
   {
      for (final SegmentRendering segment : this.mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack();
      }
   }

   @Override
   synchronized protected boolean commonOnTap(final GeoPoint tappedGeoPoint)
   {
      boolean handled = false;
      for (final SegmentRendering segment : this.mOverlays)
      {
         if (!handled)
         {
            handled = segment.commonOnTap(tappedGeoPoint);
         }
      }
      return handled;
   }

   //used to add segments on the map
   synchronized public void addSegment(final SegmentRendering segment)
   {
      segment.setBitmapHolder(this);
      this.mOverlays.add(segment);
   }

   synchronized public void clearSegments()
   {
      for (final SegmentRendering segment : this.mOverlays)
      {
         segment.closeResources();
      }
      this.mOverlays.clear();
      reset();
   }

   //set the color of using the setTrackColorMethod
   synchronized public void setTrackColoringMethod(final int color, final double speed, final double height)
   {
      for (final SegmentRendering segment : this.mOverlays)
      {
         segment.setTrackColoringMethod(color, speed, height);
      }
      scheduleRecalculation();
   }

   //size of the arrayList
   public int size()
   {
      return this.mOverlays.size();
   }
}
