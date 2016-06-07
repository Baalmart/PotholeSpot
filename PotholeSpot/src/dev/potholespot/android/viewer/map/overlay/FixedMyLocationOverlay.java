package dev.potholespot.android.viewer.map.overlay;

import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import dev.potholespot.uganda.R;

/**
 * Fix for a ClassCastException found on some Google Maps API's implementations.
 * 
 * @see <a href="http://www.spectrekking.com/download/FixedMyLocationOverlay.java">www.spectrekking.com</a>
 * @version $Id$
 */
public class FixedMyLocationOverlay extends MyLocationOverlay
{
   private boolean bugged = false;

   private Paint accuracyPaint;
   private Point center;
   private Point left;
   private Drawable drawable;
   private int width;
   private int height;

   public FixedMyLocationOverlay(final Context context, final MapView mapView)
   {
      super(context, mapView);
   }

   @Override
   protected void drawMyLocation(final Canvas canvas, final MapView mapView, final Location lastFix, final GeoPoint myLoc, final long when)
   {
      if (!this.bugged)
      {
         try
         {
            super.drawMyLocation(canvas, mapView, lastFix, myLoc, when);
         }
         catch (final Exception e)
         {
            this.bugged = true;
         }
      }

      if (this.bugged)
      {
         if (this.drawable == null)
         {
            if (this.accuracyPaint == null)
            {
               this.accuracyPaint = new Paint();
               this.accuracyPaint.setAntiAlias(true);
               this.accuracyPaint.setStrokeWidth(2.0f);
            }

            this.drawable = mapView.getContext().getResources().getDrawable(R.drawable.mylocation);
            this.width = this.drawable.getIntrinsicWidth();
            this.height = this.drawable.getIntrinsicHeight();
            this.center = new Point();
            this.left = new Point();
         }
         final Projection projection = mapView.getProjection();

         final double latitude = lastFix.getLatitude();
         final double longitude = lastFix.getLongitude();
         final float accuracy = lastFix.getAccuracy();

         final float[] result = new float[1];

         Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
         final float longitudeLineDistance = result[0];

         final GeoPoint leftGeo = new GeoPoint((int) (latitude * 1e6), (int) ((longitude - accuracy / longitudeLineDistance) * 1e6));
         projection.toPixels(leftGeo, this.left);
         projection.toPixels(myLoc, this.center);
         final int radius = this.center.x - this.left.x;

         this.accuracyPaint.setColor(0xff6666ff);
         this.accuracyPaint.setStyle(Style.STROKE);
         canvas.drawCircle(this.center.x, this.center.y, radius, this.accuracyPaint);

         this.accuracyPaint.setColor(0x186666ff);
         this.accuracyPaint.setStyle(Style.FILL);
         canvas.drawCircle(this.center.x, this.center.y, radius, this.accuracyPaint);

         this.drawable.setBounds(this.center.x - this.width / 2, this.center.y - this.height / 2, this.center.x + this.width / 2, this.center.y + this.height / 2);
         this.drawable.draw(canvas);
      }
   }

}