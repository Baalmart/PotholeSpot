package dev.potholespot.android.viewer.map.overlay;

import dev.potholespot.uganda.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.gms.maps.GoogleMap;
//import com.google.android.maps.MapView;
import com.google.android.gms.maps.MapView;
import com.google.android.maps.Projection;

/**
 * Fix for a ClassCastException found on some Google Maps API's implementations.
 * @see <a href="http://www.spectrekking.com/download/FixedMyLocationOverlay.java">www.spectrekking.com</a> 
 * @version $Id$
 */
public class GoogleFixedMyLocationOverlay extends MyLocationOverlay 
{
   private boolean bugged = false;

   private Paint accuracyPaint;
   private Point center;
   private Point left;
   private Drawable drawable;
   private int width;
   private int height;
   private GoogleMap googleMap;

   public GoogleFixedMyLocationOverlay(Context context, com.google.android.maps.MapView mMapView_new) 
   {
      super(context, mMapView_new);
      
   }

   protected void drawMyLocation(Canvas canvas, com.google.android.maps.MapView mapView, Location lastFix, GeoPoint myLoc, long when) {
      if (!bugged) {
         try {
            super.drawMyLocation(canvas, mapView, lastFix, myLoc, when);
         } catch (Exception e) 
         {
            bugged = true;
         }
      }
   
  // googleMap.setMyLocationEnabled(true);

      if (bugged) 
      {
         if (drawable == null) 
         {
            if( accuracyPaint == null )
            {
               accuracyPaint = new Paint();
               accuracyPaint.setAntiAlias(true);
               accuracyPaint.setStrokeWidth(2.0f);
            }
            
            drawable = mapView.getContext().getResources().getDrawable(R.drawable.mylocation);
            width = drawable.getIntrinsicWidth();
            height = drawable.getIntrinsicHeight();
            center = new Point();
            left = new Point();
         }
         Projection projection = mapView.getProjection();
         
         double latitude = lastFix.getLatitude();
         double longitude = lastFix.getLongitude();
         float accuracy = lastFix.getAccuracy();
         
         float[] result = new float[1];

         Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
         float longitudeLineDistance = result[0];

         GeoPoint leftGeo = new GeoPoint((int)(latitude*1e6), (int)((longitude-accuracy/longitudeLineDistance)*1e6));
         
         //Point point1 = googleMap.getProjection().toScreenLocation(leftGeo);
         projection.toPixels(leftGeo, left);
         projection.toPixels(myLoc, center);
         int radius = center.x - left.x;
         
         accuracyPaint.setColor(0xff6666ff);
         accuracyPaint.setStyle(Style.STROKE);
         canvas.drawCircle(center.x, center.y, radius, accuracyPaint);

         accuracyPaint.setColor(0x186666ff);
         accuracyPaint.setStyle(Style.FILL);
         canvas.drawCircle(center.x, center.y, radius, accuracyPaint);
                  
         drawable.setBounds(center.x - width / 2, center.y - height / 2, center.x + width / 2, center.y + height / 2);
         drawable.draw(canvas);
      }
   }

}