package dev.potholespot.android.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;
import dev.potholespot.android.actions.utils.GraphCanvas;
import dev.potholespot.android.actions.utils.StatisticsCalulator;
import dev.potholespot.android.actions.utils.StatisticsDelegate;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.util.UnitsI18n;
import dev.potholespot.android.viewer.RouteList;
import dev.potholespot.uganda.R;

public class Statistics extends Activity implements StatisticsDelegate
{

   private static final int DIALOG_GRAPHTYPE = 3;
   private static final int MENU_GRAPHTYPE = 11;
   private static final int MENU_TRACKLIST = 12;
   private static final int MENU_SHARE = 41;
   private static final String TRACKURI = "TRACKURI";
   private static final String TAG = "OGT.Statistics";

   private static final int SWIPE_MIN_DISTANCE = 120;
   private static final int SWIPE_MAX_OFF_PATH = 250;
   private static final int SWIPE_THRESHOLD_VELOCITY = 200;

   private Uri mTrackUri = null;
   private boolean calculating;
   private TextView overallavgSpeedView;
   private TextView avgSpeedView;
   private TextView distanceView;
   private TextView endtimeView;
   private TextView starttimeView;
   private TextView maxSpeedView;
   private TextView waypointsView;
   private TextView mAscensionView;
   private TextView mElapsedTimeView;

   private UnitsI18n mUnits;
   private GraphCanvas mGraphTimeSpeed;

   private ViewFlipper mViewFlipper;
   private Animation mSlideLeftIn;
   private Animation mSlideLeftOut;
   private Animation mSlideRightIn;
   private Animation mSlideRightOut;
   private GestureDetector mGestureDetector;
   private GraphCanvas mGraphDistanceSpeed;
   private GraphCanvas mGraphTimeAltitude;
   private GraphCanvas mGraphDistanceAltitude;

   private final ContentObserver mTrackObserver = new ContentObserver(new Handler())
      {

         @Override
         public void onChange(final boolean selfUpdate)
         {
            if (!Statistics.this.calculating)
            {
               Statistics.this.drawTrackingStatistics();
            }
         }
      };
   private final OnClickListener mGraphControlListener = new View.OnClickListener()
      {
         @Override
         public void onClick(final View v)
         {
            final int id = v.getId();
            switch (id)
            {
               case R.id.graphtype_timespeed:
                  Statistics.this.mViewFlipper.setDisplayedChild(0);
                  break;
               case R.id.graphtype_distancespeed:
                  Statistics.this.mViewFlipper.setDisplayedChild(1);
                  break;
               case R.id.graphtype_timealtitude:
                  Statistics.this.mViewFlipper.setDisplayedChild(2);
                  break;
               case R.id.graphtype_distancealtitude:
                  Statistics.this.mViewFlipper.setDisplayedChild(3);
                  break;
               default:
                  break;
            }
            dismissDialog(DIALOG_GRAPHTYPE);
         }
      };

   class MyGestureDetector extends SimpleOnGestureListener
   {
      @Override
      public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY)
      {
         if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
         {
            return false;
         }
         // right to left swipe
         if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
         {
            Statistics.this.mViewFlipper.setInAnimation(Statistics.this.mSlideLeftIn);
            Statistics.this.mViewFlipper.setOutAnimation(Statistics.this.mSlideLeftOut);
            Statistics.this.mViewFlipper.showNext();
         }
         else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
         {
            Statistics.this.mViewFlipper.setInAnimation(Statistics.this.mSlideRightIn);
            Statistics.this.mViewFlipper.setOutAnimation(Statistics.this.mSlideRightOut);
            Statistics.this.mViewFlipper.showPrevious();
         }
         return false;
      }
   }

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(final Bundle load)
   {
      super.onCreate(load);
      this.mUnits = new UnitsI18n(this, new UnitsI18n.UnitsChangeListener()
         {
            @Override
            public void onUnitsChange()
            {
               drawTrackingStatistics();
            }
         });
      setContentView(R.layout.statistics);

      this.mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
      this.mViewFlipper.setDrawingCacheEnabled(true);
      this.mSlideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
      this.mSlideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
      this.mSlideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
      this.mSlideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

      this.mGraphTimeSpeed = (GraphCanvas) this.mViewFlipper.getChildAt(0);
      this.mGraphDistanceSpeed = (GraphCanvas) this.mViewFlipper.getChildAt(1);
      this.mGraphTimeAltitude = (GraphCanvas) this.mViewFlipper.getChildAt(2);
      this.mGraphDistanceAltitude = (GraphCanvas) this.mViewFlipper.getChildAt(3);

      this.mGraphTimeSpeed.setType(GraphCanvas.TIMESPEEDGRAPH);
      this.mGraphDistanceSpeed.setType(GraphCanvas.DISTANCESPEEDGRAPH);
      this.mGraphTimeAltitude.setType(GraphCanvas.TIMEALTITUDEGRAPH);
      this.mGraphDistanceAltitude.setType(GraphCanvas.DISTANCEALTITUDEGRAPH);

      this.mGestureDetector = new GestureDetector(new MyGestureDetector());

      this.maxSpeedView = (TextView) findViewById(R.id.stat_maximumspeed);
      this.mAscensionView = (TextView) findViewById(R.id.stat_ascension);
      this.mElapsedTimeView = (TextView) findViewById(R.id.stat_elapsedtime);
      this.overallavgSpeedView = (TextView) findViewById(R.id.stat_overallaveragespeed);
      this.avgSpeedView = (TextView) findViewById(R.id.stat_averagespeed);
      this.distanceView = (TextView) findViewById(R.id.stat_distance);
      this.starttimeView = (TextView) findViewById(R.id.stat_starttime);
      this.endtimeView = (TextView) findViewById(R.id.stat_endtime);
      this.waypointsView = (TextView) findViewById(R.id.stat_waypoints);

      if (load != null && load.containsKey(TRACKURI))
      {
         this.mTrackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, load.getString(TRACKURI));
      }
      else
      {
         this.mTrackUri = getIntent().getData();
      }
   }

   @Override
   protected void onRestoreInstanceState(final Bundle load)
   {
      if (load != null)
      {
         super.onRestoreInstanceState(load);
      }
      if (load != null && load.containsKey(TRACKURI))
      {
         this.mTrackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, load.getString(TRACKURI));
      }
      if (load != null && load.containsKey("FLIP"))
      {
         this.mViewFlipper.setDisplayedChild(load.getInt("FLIP"));
      }
   }

   @Override
   protected void onSaveInstanceState(final Bundle save)
   {
      super.onSaveInstanceState(save);
      save.putString(TRACKURI, this.mTrackUri.getLastPathSegment());
      save.putInt("FLIP", this.mViewFlipper.getDisplayedChild());
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPause()
    */
   @Override
   protected void onPause()
   {
      super.onPause();
      this.mViewFlipper.stopFlipping();
      this.mGraphTimeSpeed.clearData();
      this.mGraphDistanceSpeed.clearData();
      this.mGraphTimeAltitude.clearData();
      this.mGraphDistanceAltitude.clearData();
      final ContentResolver resolver = getContentResolver();
      resolver.unregisterContentObserver(this.mTrackObserver);
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onResume()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      drawTrackingStatistics();

      final ContentResolver resolver = getContentResolver();
      resolver.registerContentObserver(this.mTrackUri, true, this.mTrackObserver);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu)
   {
      final boolean result = super.onCreateOptionsMenu(menu);
      menu.add(Menu.NONE, MENU_GRAPHTYPE, Menu.NONE, R.string.menu_graphtype).setIcon(R.drawable.ic_menu_picture).setAlphabeticShortcut('t');

      menu.add(Menu.NONE, MENU_TRACKLIST, Menu.NONE, R.string.menu_tracklist).setIcon(R.drawable.ic_menu_show_list).setAlphabeticShortcut('l');

      menu.add(Menu.NONE, MENU_SHARE, Menu.NONE, R.string.menu_shareTrack).setIcon(R.drawable.ic_menu_share).setAlphabeticShortcut('s');

      return result;
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item)
   {
      boolean handled = false;
      Intent intent;
      switch (item.getItemId())
      {
         case MENU_GRAPHTYPE:
            showDialog(DIALOG_GRAPHTYPE);
            handled = true;
            break;
         case MENU_TRACKLIST:
            intent = new Intent(this, RouteList.class);
            intent.putExtra(BaseColumns._ID, this.mTrackUri.getLastPathSegment());
            startActivityForResult(intent, MENU_TRACKLIST);
            break;
         case MENU_SHARE:
            intent = new Intent(Intent.ACTION_RUN);
            intent.setDataAndType(this.mTrackUri, Tracks.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final Bitmap bm = this.mViewFlipper.getDrawingCache();
            final Uri screenStreamUri = ShareRoute.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.share_track)), MENU_SHARE);
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   @Override
   public boolean onTouchEvent(final MotionEvent event)
   {
      if (this.mGestureDetector.onTouchEvent(event))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
      switch (requestCode)
      {
         case MENU_TRACKLIST:
            if (resultCode == RESULT_OK)
            {
               this.mTrackUri = intent.getData();
               drawTrackingStatistics();
            }
            break;
         case MENU_SHARE:
            ShareRoute.clearScreenBitmap();
            break;
         default:
            Log.w(TAG, "Unknown activity result request code");
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_GRAPHTYPE:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.graphtype, null);
            builder.setTitle(R.string.dialog_graphtype_title).setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(R.string.btn_cancel, null).setView(view);
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_GRAPHTYPE:
            final Button speedtime = (Button) dialog.findViewById(R.id.graphtype_timespeed);
            final Button speeddistance = (Button) dialog.findViewById(R.id.graphtype_distancespeed);
            final Button altitudetime = (Button) dialog.findViewById(R.id.graphtype_timealtitude);
            final Button altitudedistance = (Button) dialog.findViewById(R.id.graphtype_distancealtitude);
            speedtime.setOnClickListener(this.mGraphControlListener);
            speeddistance.setOnClickListener(this.mGraphControlListener);
            altitudetime.setOnClickListener(this.mGraphControlListener);
            altitudedistance.setOnClickListener(this.mGraphControlListener);
         default:
            break;
      }
      super.onPrepareDialog(id, dialog);
   }

   private void drawTrackingStatistics()
   {
      this.calculating = true;
      final StatisticsCalulator calculator = new StatisticsCalulator(this, this.mUnits, this);
      calculator.execute(this.mTrackUri);
   }

   @Override
   public void finishedCalculations(final StatisticsCalulator calculated)
   {
      this.mGraphTimeSpeed.setData(this.mTrackUri, calculated);
      this.mGraphDistanceSpeed.setData(this.mTrackUri, calculated);
      this.mGraphTimeAltitude.setData(this.mTrackUri, calculated);
      this.mGraphDistanceAltitude.setData(this.mTrackUri, calculated);

      this.mViewFlipper.postInvalidate();

      this.maxSpeedView.setText(calculated.getMaxSpeedText());
      this.mElapsedTimeView.setText(calculated.getDurationText());
      this.mAscensionView.setText(calculated.getAscensionText());
      this.overallavgSpeedView.setText(calculated.getOverallavgSpeedText());
      this.avgSpeedView.setText(calculated.getAvgSpeedText());
      this.distanceView.setText(calculated.getDistanceText());
      this.starttimeView.setText(Long.toString(calculated.getStarttime()));
      this.endtimeView.setText(Long.toString(calculated.getEndtime()));
      final String titleFormat = getString(R.string.stat_title);
      setTitle(String.format(titleFormat, calculated.getTracknameText()));
      this.waypointsView.setText(calculated.getWaypointsText());

      this.calculating = false;
   }
}
