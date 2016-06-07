package dev.potholespot.android.viewer.map.overlay;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import com.mapquest.android.maps.GeoPoint;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;
import dev.potholespot.android.db.Pspot;
import dev.potholespot.android.db.Pspot.MediaColumns;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.util.UnitsI18n;
import dev.potholespot.android.viewer.map.LoggerMap;
import dev.potholespot.uganda.R;

/**
 * Creates an overlay that can draw a single segment of connected waypoints
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class SegmentRendering
{
   public static final int MIDDLE_SEGMENT = 0;
   public static final int FIRST_SEGMENT = 1;
   public static final int LAST_SEGMENT = 2;
   public static final int DRAW_GREEN = 0;
   public static final int DRAW_RED = 1;
   public static final int DRAW_MEASURED = 2;
   public static final int DRAW_CALCULATED = 3;
   public static final int DRAW_DOTS = 4;
   public static final int DRAW_HEIGHT = 5;
   private static final String TAG = "PS.SegmentRendering";
   private static final float MINIMUM_PX_DISTANCE = 15;

   private static SparseArray<Bitmap> sBitmapCache = new SparseArray<Bitmap>();;

   private int mTrackColoringMethod = DRAW_CALCULATED;

   private final ContentResolver mResolver;
   private final LoggerMap mLoggerMap;

   private int mPlacement = SegmentRendering.MIDDLE_SEGMENT;
   private final Uri mWaypointsUri;
   private final Uri mMediaUri;
   private double mAvgSpeed;
   private double mAvgHeight;
   private GeoPoint mGeoTopLeft;
   private GeoPoint mGeoBottumRight;

   private Vector<DotVO> mDotPath;
   private Vector<DotVO> mDotPathCalculation;
   private Path mCalculatedPath;
   private Point mCalculatedStart;
   private Point mCalculatedStop;
   private Path mPathCalculation;
   private Shader mShader;
   private Vector<MediaVO> mMediaPath;
   private Vector<MediaVO> mMediaPathCalculation;

   private GeoPoint mStartPoint;
   private GeoPoint mEndPoint;
   private final Point mPrevDrawnScreenPoint;
   private final Point mScreenPointBackup;
   private final Point mScreenPoint;
   private final Point mMediaScreenPoint;
   private int mStepSize = -1;
   private Location mLocation;
   private Location mPrevLocation;
   private Cursor mWaypointsCursor;
   private Cursor mMediaCursor;
   private final Uri mSegmentUri;
   private int mWaypointCount = -1;
   private int mWidth;
   private int mHeight;
   private GeoPoint mPrevGeoPoint;
   private int mCurrentColor;
   private final Paint dotpaint;
   private final Paint radiusPaint;
   private final Paint routePaint;
   private final Paint defaultPaint;
   private boolean mRequeryFlag;
   private final Handler mHandler;
   private static Bitmap sStartBitmap;
   private static Bitmap sStopBitmap;
   private AsyncOverlay mAsyncOverlay;

   private final ContentObserver mTrackSegmentsObserver;

   private final Runnable mMediaCalculator = new Runnable()
      {
         @Override
         public void run()
         {
            SegmentRendering.this.calculateMediaAsync();
         }
      };

   private final Runnable mTrackCalculator = new Runnable()
      {
         @Override
         public void run()
         {
            SegmentRendering.this.calculateTrackAsync();
         }
      };

   /**
    * Constructor: create a new TrackingOverlay.
    * 
    * @param loggermap
    * @param segmentUri
    * @param color
    * @param avgSpeed
    * @param handler
    */

   public SegmentRendering(final LoggerMap loggermap, final Uri segmentUri, final int color, final double avgSpeed, final double avgHeight, final Handler handler)
   {
      super();
      this.mHandler = handler;
      this.mLoggerMap = loggermap;
      this.mTrackColoringMethod = color;
      this.mAvgSpeed = avgSpeed;
      this.mAvgHeight = avgHeight;
      this.mSegmentUri = segmentUri;
      this.mMediaUri = Uri.withAppendedPath(this.mSegmentUri, "media");
      this.mWaypointsUri = Uri.withAppendedPath(this.mSegmentUri, "waypoints");
      this.mResolver = this.mLoggerMap.getActivity().getContentResolver();
      this.mRequeryFlag = true;
      this.mCurrentColor = Color.rgb(255, 0, 0);

      this.dotpaint = new Paint();
      this.radiusPaint = new Paint();
      this.radiusPaint.setColor(Color.YELLOW);
      this.radiusPaint.setAlpha(100);
      this.routePaint = new Paint();
      this.routePaint.setStyle(Paint.Style.STROKE);
      this.routePaint.setStrokeWidth(6);
      this.routePaint.setAntiAlias(true);
      this.routePaint.setPathEffect(new CornerPathEffect(10));
      this.defaultPaint = new Paint();
      this.mScreenPoint = new Point();
      this.mMediaScreenPoint = new Point();
      this.mScreenPointBackup = new Point();
      this.mPrevDrawnScreenPoint = new Point();

      this.mDotPath = new Vector<DotVO>();
      this.mDotPathCalculation = new Vector<DotVO>();
      this.mCalculatedPath = new Path();
      this.mPathCalculation = new Path();
      this.mMediaPath = new Vector<MediaVO>();
      this.mMediaPathCalculation = new Vector<MediaVO>();

      this.mTrackSegmentsObserver = new ContentObserver(new Handler())
         {

            @Override
            public void onChange(final boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  SegmentRendering.this.mRequeryFlag = true;
               }
               else
               {
                  Log.w(TAG, "mTrackSegmentsObserver skipping change on " + SegmentRendering.this.mSegmentUri);
               }
            }
         };
      openResources();
   }

   public void closeResources()
   {
      this.mResolver.unregisterContentObserver(this.mTrackSegmentsObserver);
      this.mHandler.removeCallbacks(this.mMediaCalculator);
      this.mHandler.removeCallbacks(this.mTrackCalculator);
      this.mHandler.postAtFrontOfQueue(new Runnable()
         {
            @Override
            public void run()
            {
               if (SegmentRendering.this.mWaypointsCursor != null)
               {
                  SegmentRendering.this.mWaypointsCursor.close();
                  SegmentRendering.this.mWaypointsCursor = null;
               }
               if (SegmentRendering.this.mMediaCursor != null)
               {
                  SegmentRendering.this.mMediaCursor.close();
                  SegmentRendering.this.mMediaCursor = null;
               }
            }
         });
      SegmentRendering.sStopBitmap = null;
      SegmentRendering.sStartBitmap = null;
   }

   public void openResources()
   {
      this.mResolver.registerContentObserver(this.mWaypointsUri, false, this.mTrackSegmentsObserver);
   }

   /**
    * Private draw method called by both the draw from Google Overlay and the OSM Overlay
    * 
    * @param canvas
    */
   public void draw(final Canvas canvas)
   {
      switch (this.mTrackColoringMethod)
      {
         case DRAW_HEIGHT:
         case DRAW_CALCULATED:
         case DRAW_MEASURED:
         case DRAW_RED:
         case DRAW_GREEN:
            drawPath(canvas);
            break;
         case DRAW_DOTS:
            drawDots(canvas);
            break;
      }
      drawStartStopCircles(canvas);
      drawMedia(canvas);

      this.mWidth = canvas.getWidth();
      this.mHeight = canvas.getHeight();
   }

   public void calculateTrack()
   {
      this.mHandler.removeCallbacks(this.mTrackCalculator);
      this.mHandler.post(this.mTrackCalculator);
   }

   /**
    * Either the Path or the Dots are calculated based on he current track coloring method
    */
   private synchronized void calculateTrackAsync()
   {
      this.mGeoTopLeft = this.mLoggerMap.fromPixels(0, 0);
      this.mGeoBottumRight = this.mLoggerMap.fromPixels(this.mWidth, this.mHeight);

      calculateStepSize();

      this.mScreenPoint.x = -1;
      this.mScreenPoint.y = -1;
      this.mPrevDrawnScreenPoint.x = -1;
      this.mPrevDrawnScreenPoint.y = -1;

      switch (this.mTrackColoringMethod)
      {
         case DRAW_HEIGHT:
         case DRAW_CALCULATED:
         case DRAW_MEASURED:
         case DRAW_RED:
         case DRAW_GREEN:
            calculatePath();
            synchronized (this.mCalculatedPath) // Switch the fresh path with the old Path object
            {
               final Path oldPath = this.mCalculatedPath;
               this.mCalculatedPath = this.mPathCalculation;
               this.mPathCalculation = oldPath;
            }
            break;
         case DRAW_DOTS:
            calculateDots();
            synchronized (this.mDotPath) // Switch the fresh path with the old Path object
            {
               final Vector<DotVO> oldDotPath = this.mDotPath;
               this.mDotPath = this.mDotPathCalculation;
               this.mDotPathCalculation = oldDotPath;
            }
            break;
      }
      calculateStartStopCircles();
      this.mAsyncOverlay.onDateOverlayChanged();
   }

   /**
    * Calculated the new contents of segment in the mDotPathCalculation
    */
   private void calculatePath()
   {
      this.mDotPathCalculation.clear();
      this.mPathCalculation.rewind();

      this.mShader = null;

      GeoPoint geoPoint;
      this.mPrevLocation = null;

      if (this.mWaypointsCursor == null)
      {
         this.mWaypointsCursor = this.mResolver.query(this.mWaypointsUri, new String[] { WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE, WaypointsColumns.SPEED, WaypointsColumns.TIME,
               WaypointsColumns.ACCURACY, WaypointsColumns.ALTITUDE }, null, null, null);
         this.mRequeryFlag = false;
      }
      if (this.mRequeryFlag)
      {
         this.mWaypointsCursor.requery();
         this.mRequeryFlag = false;
      }
      if (this.mLoggerMap.hasProjection() && this.mWaypointsCursor.moveToFirst())
      {
         // Start point of the segments, possible a dot
         this.mStartPoint = extractGeoPoint();
         this.mPrevGeoPoint = this.mStartPoint;
         this.mLocation = new Location(this.getClass().getName());
         this.mLocation.setLatitude(this.mWaypointsCursor.getDouble(0));
         this.mLocation.setLongitude(this.mWaypointsCursor.getDouble(1));
         this.mLocation.setTime(this.mWaypointsCursor.getLong(3));

         moveToGeoPoint(this.mStartPoint);

         do
         {
            geoPoint = extractGeoPoint();
            // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
            if (geoPoint.getLatitudeE6() == 0 || geoPoint.getLongitudeE6() == 0)
            {
               continue;
            }

            double speed = -1d;
            switch (this.mTrackColoringMethod)
            {
               case DRAW_GREEN:
               case DRAW_RED:
                  plainLineToGeoPoint(geoPoint);
                  break;
               case DRAW_MEASURED:
                  speedLineToGeoPoint(geoPoint, this.mWaypointsCursor.getDouble(2));
                  break;
               case DRAW_CALCULATED:
                  this.mPrevLocation = this.mLocation;
                  this.mLocation = new Location(this.getClass().getName());
                  this.mLocation.setLatitude(this.mWaypointsCursor.getDouble(0));
                  this.mLocation.setLongitude(this.mWaypointsCursor.getDouble(1));
                  this.mLocation.setTime(this.mWaypointsCursor.getLong(3));
                  speed = calculateSpeedBetweenLocations(this.mPrevLocation, this.mLocation);
                  speedLineToGeoPoint(geoPoint, speed);
                  break;
               case DRAW_HEIGHT:
                  heightLineToGeoPoint(geoPoint, this.mWaypointsCursor.getDouble(5));
                  break;
               default:
                  Log.w(TAG, "Unknown coloring method");
                  break;
            }
         }
         while (moveToNextWayPoint());

         this.mEndPoint = extractGeoPoint(); // End point of the segments, possible a dot

      }
      //      Log.d( TAG, "transformSegmentToPath stop: points "+mCalculatedPoints+" from "+moves+" moves" );
   }

   /**
    * @param canvas
    * @param mapView
    * @param shadow
    * @see SegmentRendering#draw(Canvas, MapView, boolean)
    */
   private void calculateDots()
   {
      this.mPathCalculation.reset();
      this.mDotPathCalculation.clear();

      if (this.mWaypointsCursor == null)
      {
         this.mWaypointsCursor = this.mResolver.query(this.mWaypointsUri, new String[] { WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE, WaypointsColumns.SPEED, WaypointsColumns.TIME,
               WaypointsColumns.ACCURACY }, null, null, null);
      }
      if (this.mRequeryFlag)
      {
         this.mWaypointsCursor.requery();
         this.mRequeryFlag = false;
      }
      if (this.mLoggerMap.hasProjection() && this.mWaypointsCursor.moveToFirst())
      {
         GeoPoint geoPoint;

         this.mStartPoint = extractGeoPoint();
         this.mPrevGeoPoint = this.mStartPoint;

         do
         {
            geoPoint = extractGeoPoint();
            // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
            if (geoPoint.getLatitudeE6() == 0 || geoPoint.getLongitudeE6() == 0)
            {
               continue;
            }
            setScreenPoint(geoPoint);

            final float distance = (float) distanceInPoints(this.mPrevDrawnScreenPoint, this.mScreenPoint);
            if (distance > MINIMUM_PX_DISTANCE)
            {
               final DotVO dotVO = new DotVO();
               dotVO.x = this.mScreenPoint.x;
               dotVO.y = this.mScreenPoint.y;
               dotVO.speed = this.mWaypointsCursor.getLong(2);
               dotVO.time = this.mWaypointsCursor.getLong(3);
               dotVO.radius = this.mLoggerMap.metersToEquatorPixels(this.mWaypointsCursor.getFloat(4));
               this.mDotPathCalculation.add(dotVO);

               this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
               this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
            }
         }
         while (moveToNextWayPoint());

         this.mEndPoint = extractGeoPoint();
         final DotVO pointVO = new DotVO();
         pointVO.x = this.mScreenPoint.x;
         pointVO.y = this.mScreenPoint.y;
         pointVO.speed = this.mWaypointsCursor.getLong(2);
         pointVO.time = this.mWaypointsCursor.getLong(3);
         pointVO.radius = this.mLoggerMap.metersToEquatorPixels(this.mWaypointsCursor.getFloat(4));
         this.mDotPathCalculation.add(pointVO);
      }
   }

   public void calculateMedia()
   {
      this.mHandler.removeCallbacks(this.mMediaCalculator);
      this.mHandler.post(this.mMediaCalculator);
   }

   public synchronized void calculateMediaAsync()
   {
      this.mMediaPathCalculation.clear();
      if (this.mMediaCursor == null)
      {
         this.mMediaCursor = this.mResolver.query(this.mMediaUri, new String[] { MediaColumns.WAYPOINT, MediaColumns.URI }, null, null, null);
      }
      else
      {
         this.mMediaCursor.requery();
      }
      if (this.mLoggerMap.hasProjection() && this.mMediaCursor.moveToFirst())
      {
         GeoPoint lastPoint = null;
         int wiggle = 0;
         do
         {
            final MediaVO mediaVO = new MediaVO();
            mediaVO.waypointId = this.mMediaCursor.getLong(0);
            mediaVO.uri = Uri.parse(this.mMediaCursor.getString(1));

            final Uri mediaWaypoint = ContentUris.withAppendedId(this.mWaypointsUri, mediaVO.waypointId);
            Cursor waypointCursor = null;
            try
            {
               waypointCursor = this.mResolver.query(mediaWaypoint, new String[] { WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE }, null, null, null);
               if (waypointCursor != null && waypointCursor.moveToFirst())
               {
                  final int microLatitude = (int) (waypointCursor.getDouble(0) * 1E6d);
                  final int microLongitude = (int) (waypointCursor.getDouble(1) * 1E6d);
                  mediaVO.geopoint = new GeoPoint(microLatitude, microLongitude);
               }
            }
            finally
            {
               if (waypointCursor != null)
               {
                  waypointCursor.close();
               }
            }
            if (isGeoPointOnScreen(mediaVO.geopoint))
            {
               this.mLoggerMap.toPixels(mediaVO.geopoint, this.mMediaScreenPoint);
               if (mediaVO.geopoint.equals(lastPoint))
               {
                  wiggle += 4;
               }
               else
               {
                  wiggle = 0;
               }
               mediaVO.bitmapKey = getResourceForMedia(this.mLoggerMap.getActivity().getResources(), mediaVO.uri);
               mediaVO.w = sBitmapCache.get(mediaVO.bitmapKey).getWidth();
               mediaVO.h = sBitmapCache.get(mediaVO.bitmapKey).getHeight();
               final int left = (mediaVO.w * 3) / 7 + wiggle;
               final int up = (mediaVO.h * 6) / 7 - wiggle;
               mediaVO.x = this.mMediaScreenPoint.x - left;
               mediaVO.y = this.mMediaScreenPoint.y - up;

               lastPoint = mediaVO.geopoint;
            }
            this.mMediaPathCalculation.add(mediaVO);
         }
         while (this.mMediaCursor.moveToNext());
      }

      synchronized (this.mMediaPath) // Switch the fresh path with the old Path object
      {
         final Vector<MediaVO> oldmMediaPath = this.mMediaPath;
         this.mMediaPath = this.mMediaPathCalculation;
         this.mMediaPathCalculation = oldmMediaPath;
      }
      if (this.mMediaPathCalculation.size() != this.mMediaPath.size())
      {
         this.mAsyncOverlay.onDateOverlayChanged();
      }
   }

   private void calculateStartStopCircles()
   {
      if ((this.mPlacement == FIRST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT) && this.mStartPoint != null)
      {
         if (sStartBitmap == null)
         {
            sStartBitmap = BitmapFactory.decodeResource(this.mLoggerMap.getActivity().getResources(), R.drawable.stip);
         }
         if (this.mCalculatedStart == null)
         {
            this.mCalculatedStart = new Point();
         }
         this.mLoggerMap.toPixels(this.mStartPoint, this.mCalculatedStart);

      }
      if ((this.mPlacement == LAST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT) && this.mEndPoint != null)
      {
         if (sStopBitmap == null)
         {
            sStopBitmap = BitmapFactory.decodeResource(this.mLoggerMap.getActivity().getResources(), R.drawable.stip2);
         }
         if (this.mCalculatedStop == null)
         {
            this.mCalculatedStop = new Point();
         }
         this.mLoggerMap.toPixels(this.mEndPoint, this.mCalculatedStop);
      }
   }

   /**
    * @param canvas
    * @see SegmentRendering#draw(Canvas, MapView, boolean)
    */
   private void drawPath(final Canvas canvas)
   {
      switch (this.mTrackColoringMethod)
      {
         case DRAW_HEIGHT:
         case DRAW_CALCULATED:
         case DRAW_MEASURED:
            this.routePaint.setShader(this.mShader);
            break;
         case DRAW_RED:
            this.routePaint.setShader(null);
            this.routePaint.setColor(Color.RED);
            break;
         case DRAW_GREEN:
            this.routePaint.setShader(null);
            this.routePaint.setColor(Color.GREEN);
            break;
         default:
            this.routePaint.setShader(null);
            this.routePaint.setColor(Color.YELLOW);
            break;
      }
      synchronized (this.mCalculatedPath)
      {
         canvas.drawPath(this.mCalculatedPath, this.routePaint);
      }
   }

   private void drawDots(final Canvas canvas)
   {
      synchronized (this.mDotPath)
      {
         if (sStopBitmap == null)
         {
            sStopBitmap = BitmapFactory.decodeResource(this.mLoggerMap.getActivity().getResources(), R.drawable.stip2);
         }
         for (final DotVO dotVO : this.mDotPath)
         {
            canvas.drawBitmap(sStopBitmap, dotVO.x - 8, dotVO.y - 8, this.dotpaint);
            if (dotVO.radius > 8f)
            {
               canvas.drawCircle(dotVO.x, dotVO.y, dotVO.radius, this.radiusPaint);
            }
         }
      }
   }

   private void drawMedia(final Canvas canvas)
   {
      synchronized (this.mMediaPath)
      {
         for (final MediaVO mediaVO : this.mMediaPath)
         {
            if (mediaVO.bitmapKey != null)
            {
               Log.d(TAG, "Draw bitmap at (" + mediaVO.x + ", " + mediaVO.y + ") on " + canvas);
               canvas.drawBitmap(sBitmapCache.get(mediaVO.bitmapKey), mediaVO.x, mediaVO.y, this.defaultPaint);
            }
         }
      }
   }

   private void drawStartStopCircles(final Canvas canvas)
   {
      if (this.mCalculatedStart != null)
      {
         canvas.drawBitmap(sStartBitmap, this.mCalculatedStart.x - 8, this.mCalculatedStart.y - 8, this.defaultPaint);
      }
      if (this.mCalculatedStop != null)
      {
         canvas.drawBitmap(sStopBitmap, this.mCalculatedStop.x - 5, this.mCalculatedStop.y - 5, this.defaultPaint);
      }
   }

   private Integer getResourceForMedia(final Resources resources, final Uri uri)
   {
      int drawable = 0;
      if (uri.getScheme().equals("file"))
      {
         if (uri.getLastPathSegment().endsWith("3gp"))
         {
            drawable = R.drawable.media_film;
         }
         else if (uri.getLastPathSegment().endsWith("jpg"))
         {
            drawable = R.drawable.media_camera;
         }
         else if (uri.getLastPathSegment().endsWith("txt"))
         {
            drawable = R.drawable.media_notepad;
         }
      }
      else if (uri.getScheme().equals("content"))
      {
         if (uri.getAuthority().equals(Pspot.AUTHORITY + ".string"))
         {
            drawable = R.drawable.media_mark;
         }
         else if (uri.getAuthority().equals("media"))
         {
            drawable = R.drawable.media_speech;
         }
      }
      Bitmap bitmap = null;
      final int bitmapKey = drawable;
      synchronized (sBitmapCache)
      {
         if (sBitmapCache.get(bitmapKey) == null)
         {
            bitmap = BitmapFactory.decodeResource(resources, drawable);
            sBitmapCache.put(bitmapKey, bitmap);

         }
         bitmap = sBitmapCache.get(bitmapKey);
      }
      return bitmapKey;
   }

   /**
    * Set the mPlace to the specified value.
    * 
    * @see SegmentRendering.FIRST
    * @see SegmentRendering.MIDDLE
    * @see SegmentRendering.LAST
    * @param place The placement of this segment in the line.
    */
   public void addPlacement(final int place)
   {
      this.mPlacement += place;
   }

   public boolean isLast()
   {
      return (this.mPlacement >= LAST_SEGMENT);
   }

   public long getSegmentId()
   {
      return Long.parseLong(this.mSegmentUri.getLastPathSegment());
   }

   /**
    * Set the beginnging to the next contour of the line to the give GeoPoint
    * 
    * @param geoPoint
    */
   private void moveToGeoPoint(final GeoPoint geoPoint)
   {
      setScreenPoint(geoPoint);

      if (this.mPathCalculation != null)
      {
         this.mPathCalculation.moveTo(this.mScreenPoint.x, this.mScreenPoint.y);
         this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
         this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
      }
   }

   /**
    * Line to point without shaders
    * 
    * @param geoPoint
    */
   private void plainLineToGeoPoint(final GeoPoint geoPoint)
   {
      shaderLineToGeoPoint(geoPoint, 0, 0);
   }

   /**
    * Line to point with speed
    * 
    * @param geoPoint
    * @param height
    */
   private void heightLineToGeoPoint(final GeoPoint geoPoint, final double height)
   {
      shaderLineToGeoPoint(geoPoint, height, this.mAvgHeight);
   }

   /**
    * Line to point with speed
    * 
    * @param geoPoint
    * @param speed
    */
   private void speedLineToGeoPoint(final GeoPoint geoPoint, final double speed)
   {
      shaderLineToGeoPoint(geoPoint, speed, this.mAvgSpeed);
   }

   private void shaderLineToGeoPoint(final GeoPoint geoPoint, final double value, final double average)
   {
      setScreenPoint(geoPoint);

      //      Log.d( TAG, "Draw line to " + geoPoint+" with speed "+speed );

      if (value > 0)
      {
         final int greenfactor = (int) Math.min((127 * value) / average, 255);
         final int redfactor = 255 - greenfactor;
         this.mCurrentColor = Color.rgb(redfactor, greenfactor, 0);
      }
      else
      {
         final int greenfactor = Color.green(this.mCurrentColor);
         final int redfactor = Color.red(this.mCurrentColor);
         this.mCurrentColor = Color.argb(128, redfactor, greenfactor, 0);
      }

      final float distance = (float) distanceInPoints(this.mPrevDrawnScreenPoint, this.mScreenPoint);
      if (distance > MINIMUM_PX_DISTANCE)
      {
         //         Log.d( TAG, "Circle between " + mPrevDrawnScreenPoint+" and "+mScreenPoint );
         final int x_circle = (this.mPrevDrawnScreenPoint.x + this.mScreenPoint.x) / 2;
         final int y_circle = (this.mPrevDrawnScreenPoint.y + this.mScreenPoint.y) / 2;
         final float radius_factor = 0.4f;
         final Shader lastShader = new RadialGradient(x_circle, y_circle, distance, new int[] { this.mCurrentColor, this.mCurrentColor, Color.TRANSPARENT }, new float[] { 0, radius_factor, 0.6f },
               TileMode.CLAMP);
         //            Paint debug = new Paint();
         //            debug.setStyle( Paint.Style.FILL_AND_STROKE );
         //            this.mDebugCanvas.drawCircle(
         //                  x_circle,
         //                  y_circle, 
         //                  distance*radius_factor/2, 
         //                  debug );
         //            this.mDebugCanvas.drawCircle(
         //                  x_circle,
         //                  y_circle, 
         //                  distance*radius_factor, 
         //                  debug );
         //            if( distance > 100 )
         //            {
         //               Log.d( TAG, "Created shader for speed " + speed + " on " + x_circle + "," + y_circle );
         //            }
         if (this.mShader != null)
         {
            this.mShader = new ComposeShader(this.mShader, lastShader, Mode.DST_OVER);
         }
         else
         {
            this.mShader = lastShader;
         }
         this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
         this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
      }

      this.mPathCalculation.lineTo(this.mScreenPoint.x, this.mScreenPoint.y);
   }

   /**
    * Use to update location/point state when calculating the line
    * 
    * @param geoPoint
    */
   private void setScreenPoint(final GeoPoint geoPoint)
   {
      this.mScreenPointBackup.x = this.mScreenPoint.x;
      this.mScreenPointBackup.y = this.mScreenPoint.x;

      this.mLoggerMap.toPixels(geoPoint, this.mScreenPoint);
   }

   /**
    * Move to a next waypoint, for on screen this are the points with mStepSize % position == 0 to avoid jittering in the rendering or the points on the either side of the screen edge.
    * 
    * @return if a next waypoint is pointed to with the mWaypointsCursor
    */
   private boolean moveToNextWayPoint()
   {
      boolean cursorReady = true;
      final boolean onscreen = isGeoPointOnScreen(extractGeoPoint());
      if (this.mWaypointsCursor.isLast()) // End of the line, cant move onward
      {
         cursorReady = false;
      }
      else if (onscreen) // Are on screen
      {
         cursorReady = moveOnScreenWaypoint();
      }
      else
      // Are off screen => accelerate
      {
         final int acceleratedStepsize = this.mStepSize * (this.mWaypointCount / 1000 + 6);
         cursorReady = moveOffscreenWaypoint(acceleratedStepsize);
      }
      return cursorReady;
   }

   /**
    * Move the cursor to the next waypoint modulo of the step size or less if the screen edge is reached
    * 
    * @param trackCursor
    * @return
    */
   private boolean moveOnScreenWaypoint()
   {
      final int nextPosition = this.mStepSize * (this.mWaypointsCursor.getPosition() / this.mStepSize) + this.mStepSize;
      if (this.mWaypointsCursor.moveToPosition(nextPosition))
      {
         if (isGeoPointOnScreen(extractGeoPoint())) // Remained on screen
         {
            return true; // Cursor is pointing to somewhere
         }
         else
         {
            this.mWaypointsCursor.move(-1 * this.mStepSize); // Step back
            boolean nowOnScreen = true; // onto the screen
            while (nowOnScreen) // while on the screen 
            {
               this.mWaypointsCursor.moveToNext(); // inch forward to the edge
               nowOnScreen = isGeoPointOnScreen(extractGeoPoint());
            }
            return true; // with a cursor point to somewhere
         }
      }
      else
      {
         return this.mWaypointsCursor.moveToLast(); // No full step can be taken, move to last
      }
   }

   /**
    * Previous path GeoPoint was off screen and the next one will be to or the first on screen when the path reaches the projection.
    * 
    * @return
    */
   private boolean moveOffscreenWaypoint(final int flexStepsize)
   {
      while (this.mWaypointsCursor.move(flexStepsize))
      {
         if (this.mWaypointsCursor.isLast())
         {
            return true;
         }
         final GeoPoint evalPoint = extractGeoPoint();
         // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
         if (evalPoint.getLatitudeE6() == 0 || evalPoint.getLongitudeE6() == 0)
         {
            continue;
         }
         //         Log.d( TAG, String.format( "Evaluate point number %d ", mWaypointsCursor.getPosition() ) );
         if (possibleScreenPass(this.mPrevGeoPoint, evalPoint))
         {
            this.mPrevGeoPoint = evalPoint;
            if (flexStepsize == 1) // Just stumbled over a border
            {
               return true;
            }
            else
            {
               this.mWaypointsCursor.move(-1 * flexStepsize); // Take 1 step back
               return moveOffscreenWaypoint(flexStepsize / 2); // Continue at halve accelerated speed
            }
         }
         else
         {
            moveToGeoPoint(evalPoint);
            this.mPrevGeoPoint = evalPoint;
         }

      }
      return this.mWaypointsCursor.moveToLast();
   }

   /**
    * If a segment contains more then 500 waypoints and is zoomed out more then twice then some waypoints will not be used to render the line, this speeding things along.
    */
   private void calculateStepSize()
   {
      Cursor waypointsCursor = null;
      if (this.mRequeryFlag || this.mStepSize < 1 || this.mWaypointCount < 0)
      {
         try
         {
            waypointsCursor = this.mResolver.query(this.mWaypointsUri, new String[] { BaseColumns._ID }, null, null, null);
            this.mWaypointCount = waypointsCursor.getCount();
         }
         finally
         {
            if (waypointsCursor != null)
            {
               waypointsCursor.close();
            }
         }
      }
      if (this.mWaypointCount < 250)
      {
         this.mStepSize = 1;
      }
      else
      {
         final int zoomLevel = this.mLoggerMap.getZoomLevel();
         final int maxZoomLevel = this.mLoggerMap.getMaxZoomLevel();
         if (zoomLevel >= maxZoomLevel - 2)
         {
            this.mStepSize = 1;
         }
         else
         {
            this.mStepSize = maxZoomLevel - zoomLevel;
         }
      }
   }

   /**
    * Is a given GeoPoint in the current projection of the map.
    * 
    * @param eval
    * @return
    */
   protected boolean isGeoPointOnScreen(final GeoPoint geopoint)
   {
      boolean onscreen = geopoint != null;
      if (geopoint != null && this.mGeoTopLeft != null && this.mGeoBottumRight != null)
      {
         onscreen = onscreen && this.mGeoTopLeft.getLatitudeE6() > geopoint.getLatitudeE6();
         onscreen = onscreen && this.mGeoBottumRight.getLatitudeE6() < geopoint.getLatitudeE6();
         if (this.mGeoTopLeft.getLongitudeE6() < this.mGeoBottumRight.getLongitudeE6())
         {
            onscreen = onscreen && this.mGeoTopLeft.getLongitudeE6() < geopoint.getLongitudeE6();
            onscreen = onscreen && this.mGeoBottumRight.getLongitudeE6() > geopoint.getLongitudeE6();
         }
         else
         {
            onscreen = onscreen && (this.mGeoTopLeft.getLongitudeE6() < geopoint.getLongitudeE6() || this.mGeoBottumRight.getLongitudeE6() > geopoint.getLongitudeE6());
         }
      }
      return onscreen;
   }

   /**
    * Is a given coordinates are on the screen
    * 
    * @param eval
    * @return
    */
   protected boolean isOnScreen(final int x, final int y)
   {
      final boolean onscreen = x > 0 && y > 0 && x < this.mWidth && y < this.mHeight;
      return onscreen;
   }

   /**
    * Calculates in which segment opposited to the projecting a geo point resides
    * 
    * @param p1
    * @return
    */
   private int toSegment(final GeoPoint p1)
   {
      //      Log.d( TAG, String.format( "Comparing %s to points TL %s and BR %s", p1, mTopLeft, mBottumRight )); 
      int nr;
      if (p1.getLongitudeE6() < this.mGeoTopLeft.getLongitudeE6()) // left
      {
         nr = 1;
      }
      else if (p1.getLongitudeE6() > this.mGeoBottumRight.getLongitudeE6()) // right
      {
         nr = 3;
      }
      else
      // middle
      {
         nr = 2;
      }

      if (p1.getLatitudeE6() > this.mGeoTopLeft.getLatitudeE6()) // top
      {
         nr = nr + 0;
      }
      else if (p1.getLatitudeE6() < this.mGeoBottumRight.getLatitudeE6()) // bottom
      {
         nr = nr + 6;
      }
      else
      // middle
      {
         nr = nr + 3;
      }
      return nr;
   }

   private boolean possibleScreenPass(final GeoPoint fromGeo, final GeoPoint toGeo)
   {
      boolean safe = true;
      if (fromGeo != null && toGeo != null)
      {
         final int from = toSegment(fromGeo);
         final int to = toSegment(toGeo);

         switch (from)
         {
            case 1:
               safe = to == 1 || to == 2 || to == 3 || to == 4 || to == 7;
               break;
            case 2:
               safe = to == 1 || to == 2 || to == 3;
               break;
            case 3:
               safe = to == 1 || to == 2 || to == 3 || to == 6 || to == 9;
               break;
            case 4:
               safe = to == 1 || to == 4 || to == 7;
               break;
            case 5:
               safe = false;
               break;
            case 6:
               safe = to == 3 || to == 6 || to == 9;
               break;
            case 7:
               safe = to == 1 || to == 4 || to == 7 || to == 8 || to == 9;
               break;
            case 8:
               safe = to == 7 || to == 8 || to == 9;
               break;
            case 9:
               safe = to == 3 || to == 6 || to == 7 || to == 8 || to == 9;
               break;
            default:
               safe = false;
               break;
         }
         //            Log.d( TAG, String.format( "From %d to %d is safe: %s", from, to, safe ) );
      }
      return !safe;
   }

   public void setTrackColoringMethod(final int coloring, final double avgspeed, final double avgHeight)
   {
      if (this.mTrackColoringMethod != coloring)
      {
         this.mTrackColoringMethod = coloring;
         calculateTrack();
      }
      this.mAvgSpeed = avgspeed;
      this.mAvgHeight = avgHeight;
   }

   /**
    * For the current waypoint cursor returns the GeoPoint
    * 
    * @return
    */
   private GeoPoint extractGeoPoint()
   {
      final int microLatitude = (int) (this.mWaypointsCursor.getDouble(0) * 1E6d);
      final int microLongitude = (int) (this.mWaypointsCursor.getDouble(1) * 1E6d);
      return new GeoPoint(microLatitude, microLongitude);
   }

   /**
    * @param startLocation
    * @param endLocation
    * @return speed in m/s between 2 locations
    */
   private static double calculateSpeedBetweenLocations(final Location startLocation, final Location endLocation)
   {
      double speed = -1d;
      if (startLocation != null && endLocation != null)
      {
         final float distance = startLocation.distanceTo(endLocation);
         final float seconds = (endLocation.getTime() - startLocation.getTime()) / 1000f;
         speed = distance / seconds;
         //         Log.d( TAG, "Found a speed of "+speed+ " over a distance of "+ distance+" in a time of "+seconds);
      }
      if (speed > 0)
      {
         return speed;
      }
      else
      {
         return -1d;
      }
   }

   public static int extendPoint(final int x1, final int x2)
   {
      final int diff = x2 - x1;
      final int next = x2 + diff;
      return next;
   }

   private static double distanceInPoints(final Point start, final Point end)
   {
      final int x = Math.abs(end.x - start.x);
      final int y = Math.abs(end.y - start.y);
      return Math.sqrt(x * x + y * y);
   }

   private boolean handleMediaTapList(final List<Uri> tappedUri)
   {
      if (tappedUri.size() == 1)
      {
         return handleMedia(this.mLoggerMap.getActivity(), tappedUri.get(0));
      }
      else
      {
         final BaseAdapter adapter = new MediaAdapter(this.mLoggerMap.getActivity(), tappedUri);
         this.mLoggerMap.showMediaDialog(adapter);
         return true;
      }
   }

   public static boolean handleMedia(final Context ctx, Uri mediaUri)
   {
      if (mediaUri.getScheme().equals("file"))
      {
         final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
         if (mediaUri.getLastPathSegment().endsWith("3gp"))
         {
            intent.setDataAndType(mediaUri, "video/3gpp");
            ctx.startActivity(intent);
            return true;
         }
         else if (mediaUri.getLastPathSegment().endsWith("jpg"))
         {
            //<scheme>://<authority><absolute path>
            final Uri.Builder builder = new Uri.Builder();
            mediaUri = builder.scheme(mediaUri.getScheme()).authority(mediaUri.getAuthority()).path(mediaUri.getPath()).build();
            intent.setDataAndType(mediaUri, "image/jpeg");
            ctx.startActivity(intent);
            return true;
         }
         else if (mediaUri.getLastPathSegment().endsWith("txt"))
         {
            intent.setDataAndType(mediaUri, "text/plain");
            ctx.startActivity(intent);
            return true;
         }
      }
      else if (mediaUri.getScheme().equals("content"))
      {
         if (mediaUri.getAuthority().equals(Pspot.AUTHORITY + ".string"))
         {
            final String text = mediaUri.getLastPathSegment();
            final Toast toast = Toast.makeText(ctx, text, Toast.LENGTH_LONG);
            toast.show();
            return true;
         }
         else if (mediaUri.getAuthority().equals("media"))
         {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, mediaUri));
            return true;
         }
      }
      return false;
   }

   public boolean commonOnTap(final GeoPoint tappedGeoPoint)
   {
      final List<Uri> tappedUri = new Vector<Uri>();

      final Point tappedPoint = new Point();
      this.mLoggerMap.toPixels(tappedGeoPoint, tappedPoint);
      for (final MediaVO media : this.mMediaPath)
      {
         if (media.x < tappedPoint.x && tappedPoint.x < media.x + media.w && media.y < tappedPoint.y && tappedPoint.y < media.y + media.h)
         {
            tappedUri.add(media.uri);
         }
      }
      if (tappedUri.size() > 0)
      {
         return handleMediaTapList(tappedUri);
      }
      else
      {
         if (this.mTrackColoringMethod == DRAW_DOTS)
         {
            DotVO tapped = null;
            synchronized (this.mDotPath) // Switch the fresh path with the old Path object
            {
               final int w = 25;
               for (final DotVO dot : this.mDotPath)
               {
                  //                  Log.d( TAG, "Compare ("+dot.x+","+dot.y+") with tap ("+tappedPoint.x+","+tappedPoint.y+")" );
                  if (dot.x - w < tappedPoint.x && tappedPoint.x < dot.x + w && dot.y - w < tappedPoint.y && tappedPoint.y < dot.y + w)
                  {
                     if (tapped == null)
                     {
                        tapped = dot;
                     }
                     else
                     {
                        tapped = dot.distanceTo(tappedPoint) < tapped.distanceTo(tappedPoint) ? dot : tapped;
                     }
                  }
               }
            }
            if (tapped != null)
            {
               final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this.mLoggerMap.getActivity().getApplicationContext());
               final String timetxt = timeFormat.format(new Date(tapped.time));
               final UnitsI18n units = new UnitsI18n(this.mLoggerMap.getActivity(), null);
               final double speed = units.conversionFromMetersPerSecond(tapped.speed);
               final String speedtxt = String.format("%.1f %s", speed, units.getSpeedUnit());
               final String text = this.mLoggerMap.getActivity().getString(R.string.time_and_speed, timetxt, speedtxt);
               final Toast toast = Toast.makeText(this.mLoggerMap.getActivity(), text, Toast.LENGTH_SHORT);
               toast.show();
            }
         }
         return false;
      }
   }

   private static class MediaVO
   {
      @Override
      public String toString()
      {
         return "MediaVO [bitmapKey=" + this.bitmapKey + ", uri=" + this.uri + ", geopoint=" + this.geopoint + ", x=" + this.x + ", y=" + this.y + ", w=" + this.w + ", h=" + this.h + ", waypointId="
               + this.waypointId + "]";
      }

      public Integer bitmapKey;
      public Uri uri;
      public GeoPoint geopoint;
      public int x;
      public int y;
      public int w;
      public int h;
      public long waypointId;
   }

   private static class DotVO
   {
      public long time;
      public long speed;
      public int x;
      public int y;
      public float radius;

      public int distanceTo(final Point tappedPoint)
      {
         return Math.abs(tappedPoint.x - this.x) + Math.abs(tappedPoint.y - this.y);
      }
   }

   private class MediaAdapter extends BaseAdapter
   {
      private final Context mContext;
      private final List<Uri> mTappedUri;
      private final int itemBackground;

      public MediaAdapter(final Context ctx, final List<Uri> tappedUri)
      {
         this.mContext = ctx;
         this.mTappedUri = tappedUri;
         final TypedArray a = this.mContext.obtainStyledAttributes(R.styleable.gallery);
         this.itemBackground = a.getResourceId(R.styleable.gallery_android_galleryItemBackground, 0);
         a.recycle();

      }

      @Override
      public int getCount()
      {
         return this.mTappedUri.size();
      }

      @Override
      public Object getItem(final int position)
      {
         return this.mTappedUri.get(position);
      }

      @Override
      public long getItemId(final int position)
      {
         return position;
      }

      @Override
      public View getView(final int position, final View convertView, final ViewGroup parent)
      {
         final ImageView imageView = new ImageView(this.mContext);
         imageView.setImageBitmap(sBitmapCache.get(getResourceForMedia(SegmentRendering.this.mLoggerMap.getActivity().getResources(), this.mTappedUri.get(position))));
         imageView.setScaleType(ImageView.ScaleType.FIT_XY);
         imageView.setBackgroundResource(this.itemBackground);
         return imageView;

      }
   }

   public void setBitmapHolder(final AsyncOverlay bitmapOverlay)
   {
      this.mAsyncOverlay = bitmapOverlay;
   }
}
