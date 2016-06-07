package dev.potholespot.android.adapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import dev.potholespot.android.db.Pspot.MetaData;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.Pair;
import dev.potholespot.uganda.R;

/**
 * Model containing agregrated data retrieved from the GoBreadcrumbs.com API
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class BreadcrumbsTracks extends Observable
{
   public static final String DESCRIPTION = "DESCRIPTION";

   public static final String NAME = "NAME";

   public static final String ENDTIME = "ENDTIME";

   public static final String TRACK_ID = "BREADCRUMBS_TRACK_ID";

   public static final String BUNDLE_ID = "BREADCRUMBS_BUNDLE_ID";

   public static final String ACTIVITY_ID = "BREADCRUMBS_ACTIVITY_ID";

   public static final String DIFFICULTY = "DIFFICULTY";

   public static final String STARTTIME = "STARTTIME";

   public static final String ISPUBLIC = "ISPUBLIC";

   public static final String RATING = "RATING";

   public static final String LATITUDE = "LATITUDE";

   public static final String LONGITUDE = "LONGITUDE";

   public static final String TOTALDISTANCE = "TOTALDISTANCE";

   public static final String TOTALTIME = "TOTALTIME";

   private static final String TAG = "OGT.BreadcrumbsTracks";

   private static final Integer CACHE_VERSION = Integer.valueOf(8);
   private static final String BREADCRUMSB_BUNDLES_CACHE_FILE = "breadcrumbs_bundles_cache.data";
   private static final String BREADCRUMSB_ACTIVITY_CACHE_FILE = "breadcrumbs_activity_cache.data";
   /**
    * Time in milliseconds that a persisted breadcrumbs cache is used without a refresh
    */
   private static final long CACHE_TIMEOUT = 1000 * 60;//1000*60*10 ;

   /**
    * Mapping from bundleId to a list of trackIds
    */
   private static HashMap<Integer, List<Integer>> sBundlesWithTracks;
   /**
    * Map from activityId to a dictionary containing keys like NAME
    */
   private static HashMap<Integer, Map<String, String>> sActivityMappings;

   /**
    * Map from bundleId to a dictionary containing keys like NAME and DESCRIPTION
    */
   private static HashMap<Integer, Map<String, String>> sBundleMappings;

   /**
    * Map from trackId to a dictionary containing keys like NAME, ISPUBLIC, DESCRIPTION and more
    */
   private static HashMap<Integer, Map<String, String>> sTrackMappings;
   /**
    * Cache of OGT Tracks that have a Breadcrumbs track id stored in the meta-data table
    */
   private Map<Long, Integer> mSyncedTracks = null;

   private static Set<Pair<Integer, Integer>> sScheduledTracksLoading;

   static
   {
      BreadcrumbsTracks.initCacheVariables();
   }

   private static void initCacheVariables()
   {
      sBundlesWithTracks = new HashMap<Integer, List<Integer>>();
      sActivityMappings = new HashMap<Integer, Map<String, String>>();
      sBundleMappings = new HashMap<Integer, Map<String, String>>();
      sTrackMappings = new HashMap<Integer, Map<String, String>>();
      sScheduledTracksLoading = new HashSet<Pair<Integer, Integer>>();
   }

   private final ContentResolver mResolver;

   /**
    * Constructor: create a new BreadcrumbsTracks.
    * 
    * @param resolver Content resolver to obtain local Breadcrumbs references
    */
   public BreadcrumbsTracks(final ContentResolver resolver)
   {
      this.mResolver = resolver;
   }

   public void addActivity(final int activityId, final String activityName)
   {
      if (BreadcrumbsAdapter.DEBUG)
      {
         Log.d(TAG, "addActivity(Integer " + activityId + " String " + activityName + ")");
      }
      if (sActivityMappings.get(activityId) == null)
      {
         sActivityMappings.put(activityId, new HashMap<String, String>());
      }
      sActivityMappings.get(activityId).put(NAME, activityName);
      setChanged();
      notifyObservers();
   }

   /**
    * Add bundle to the track list
    * 
    * @param activityId
    * @param bundleId
    * @param bundleName
    * @param bundleDescription
    */
   public void addBundle(final int bundleId, final String bundleName, final String bundleDescription)
   {
      if (BreadcrumbsAdapter.DEBUG)
      {
         Log.d(TAG, "addBundle(Integer " + bundleId + ", String " + bundleName + ", String " + bundleDescription + ")");
      }
      if (sBundleMappings.get(bundleId) == null)
      {
         sBundleMappings.put(bundleId, new HashMap<String, String>());
      }
      if (sBundlesWithTracks.get(bundleId) == null)
      {
         sBundlesWithTracks.put(bundleId, new ArrayList<Integer>());
      }
      sBundleMappings.get(bundleId).put(NAME, bundleName);
      sBundleMappings.get(bundleId).put(DESCRIPTION, bundleDescription);
      setChanged();
      notifyObservers();
   }

   /**
    * Add track to tracklist
    * 
    * @param trackId
    * @param trackName
    * @param bundleId
    * @param trackDescription
    * @param difficulty
    * @param startTime
    * @param endTime
    * @param isPublic
    * @param lat
    * @param lng
    * @param totalDistance
    * @param totalTime
    * @param trackRating
    */
   public void addTrack(final int trackId, final String trackName, final int bundleId, final String trackDescription, final String difficulty, final String startTime, final String endTime,
         final String isPublic, final Float lat, final Float lng, final Float totalDistance, final Integer totalTime, final String trackRating)
   {
      if (BreadcrumbsAdapter.DEBUG)
      {
         Log.d(TAG, "addTrack(Integer " + trackId + ", String " + trackName + ", Integer " + bundleId + "...");
      }
      if (sBundlesWithTracks.get(bundleId) == null)
      {
         sBundlesWithTracks.put(bundleId, new ArrayList<Integer>());
      }
      if (!sBundlesWithTracks.get(bundleId).contains(trackId))
      {
         sBundlesWithTracks.get(bundleId).add(trackId);
         sScheduledTracksLoading.remove(Pair.create(Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE, trackId));
      }

      if (sTrackMappings.get(trackId) == null)
      {
         sTrackMappings.put(trackId, new HashMap<String, String>());
      }
      putForTrack(trackId, NAME, trackName);
      putForTrack(trackId, ISPUBLIC, isPublic);
      putForTrack(trackId, STARTTIME, startTime);
      putForTrack(trackId, ENDTIME, endTime);
      putForTrack(trackId, DESCRIPTION, trackDescription);
      putForTrack(trackId, DIFFICULTY, difficulty);
      putForTrack(trackId, RATING, trackRating);
      putForTrack(trackId, LATITUDE, lat);
      putForTrack(trackId, LONGITUDE, lng);
      putForTrack(trackId, TOTALDISTANCE, totalDistance);
      putForTrack(trackId, TOTALTIME, totalTime);
      notifyObservers();
   }

   public void addSyncedTrack(final Long trackId, final int bcTrackId)
   {
      if (this.mSyncedTracks == null)
      {
         isLocalTrackOnline(-1l);
      }
      this.mSyncedTracks.put(trackId, bcTrackId);
      setChanged();
      notifyObservers();
   }

   public void addTracksLoadingScheduled(final Pair<Integer, Integer> item)
   {
      sScheduledTracksLoading.add(item);
      setChanged();
      notifyObservers();
   }

   /**
    * Cleans old bundles based a set of all bundles
    * 
    * @param newBundleIds
    */
   public void setAllBundleIds(final Set<Integer> currentBundleIds)
   {
      final Set<Integer> keySet = sBundlesWithTracks.keySet();
      for (final Integer oldBundleId : keySet)
      {
         if (!currentBundleIds.contains(oldBundleId))
         {
            removeBundle(oldBundleId);
         }
      }
   }

   public void setAllTracksForBundleId(final int mBundleId, final Set<Integer> updatedbcTracksIdList)
   {
      final List<Integer> trackIdList = sBundlesWithTracks.get(mBundleId);
      for (int location = 0; location < trackIdList.size(); location++)
      {
         final Integer oldTrackId = trackIdList.get(location);
         if (!updatedbcTracksIdList.contains(oldTrackId))
         {
            removeTrack(mBundleId, oldTrackId);
         }
      }
      setChanged();
      notifyObservers();
   }

   private void putForTrack(final int trackId, final String key, final Object value)
   {
      if (value != null)
      {
         sTrackMappings.get(trackId).put(key, value.toString());
      }
      setChanged();
      notifyObservers();
   }

   /**
    * Remove a bundle
    * 
    * @param deletedId
    */
   public void removeBundle(final int deletedId)
   {
      sBundleMappings.remove(deletedId);
      sBundlesWithTracks.remove(deletedId);
      setChanged();
      notifyObservers();
   }

   /**
    * Remove a track
    * 
    * @param deletedId
    */
   public void removeTrack(final int bundleId, final int trackId)
   {
      sTrackMappings.remove(trackId);
      if (sBundlesWithTracks.get(bundleId) != null)
      {
         sBundlesWithTracks.get(bundleId).remove(trackId);
      }
      setChanged();
      notifyObservers();

      this.mResolver.delete(MetaData.CONTENT_URI, MetaDataColumns.TRACK + " = ? AND " + MetaDataColumns.KEY + " = ? ", new String[] { Integer.toString(trackId), TRACK_ID });
      if (this.mSyncedTracks != null && this.mSyncedTracks.containsKey(trackId))
      {
         this.mSyncedTracks.remove(trackId);
      }
   }

   public int positions()
   {
      int size = 0;
      for (int index = 0; index < sBundlesWithTracks.size(); index++)
      {
         // One row for the Bundle header
         size += 1;
         final List<Integer> trackIds = sBundlesWithTracks.get(index);
         final int bundleSize = trackIds != null ? trackIds.size() : 0;
         // One row per track in each bundle
         size += bundleSize;
      }
      return size;
   }

   public Integer getBundleIdForTrackId(final int trackId)
   {
      for (final Integer bundleId : sBundlesWithTracks.keySet())
      {
         final List<Integer> trackIds = sBundlesWithTracks.get(bundleId);
         if (trackIds.contains(trackId))
         {
            return bundleId;
         }
      }
      return null;
   }

   /**
    * @param position list postition 0...n
    * @return a pair of a TYPE and an ID
    */
   public Pair<Integer, Integer> getItemForPosition(final int position)
   {
      int countdown = position;
      for (final Integer bundleId : sBundlesWithTracks.keySet())
      {
         if (countdown == 0)
         {
            return Pair.create(Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE, bundleId);
         }
         countdown--;

         final int bundleSize = sBundlesWithTracks.get(bundleId) != null ? sBundlesWithTracks.get(bundleId).size() : 0;
         if (countdown < bundleSize)
         {
            final Integer trackId = sBundlesWithTracks.get(bundleId).get(countdown);
            return Pair.create(Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE, trackId);
         }
         countdown -= bundleSize;
      }
      return null;
   }

   public String getValueForItem(final Pair<Integer, Integer> item, final String key)
   {
      String value = null;
      switch (item.first)
      {
         case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
            value = sBundleMappings.get(item.second).get(key);
            break;
         case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
            value = sTrackMappings.get(item.second).get(key);
            break;
         default:
            value = null;
            break;
      }
      return value;
   }

   public SpinnerAdapter getActivityAdapter(final Context ctx)
   {
      final List<String> activities = new Vector<String>();
      for (final Integer activityId : sActivityMappings.keySet())
      {
         String name = sActivityMappings.get(activityId).get(NAME);
         name = name != null ? name : "";
         activities.add(name);
      }
      Collections.sort(activities);
      final ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, activities);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public SpinnerAdapter getBundleAdapter(final Context ctx)
   {
      final List<String> bundles = new Vector<String>();
      for (final Integer bundleId : sBundlesWithTracks.keySet())
      {
         bundles.add(sBundleMappings.get(bundleId).get(NAME));
      }
      Collections.sort(bundles);
      if (!bundles.contains(ctx.getString(R.string.app_name)))
      {
         bundles.add(ctx.getString(R.string.app_name));
      }
      final ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, bundles);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public static int getIdForActivity(final String selectedItem)
   {
      if (selectedItem == null)
      {
         return -1;
      }
      for (final Integer activityId : sActivityMappings.keySet())
      {
         final Map<String, String> mapping = sActivityMappings.get(activityId);
         if (mapping != null && selectedItem.equals(mapping.get(NAME)))
         {
            return activityId;
         }
      }
      return -1;
   }

   public static int getIdForBundle(final int activityId, final String selectedItem)
   {
      for (final Integer bundleId : sBundlesWithTracks.keySet())
      {
         if (selectedItem.equals(sBundleMappings.get(bundleId).get(NAME)))
         {
            return bundleId;
         }
      }
      return -1;
   }

   private boolean isLocalTrackOnline(final Long qtrackId)
   {
      if (this.mSyncedTracks == null)
      {
         this.mSyncedTracks = new HashMap<Long, Integer>();
         Cursor cursor = null;
         try
         {
            cursor = this.mResolver.query(MetaData.CONTENT_URI, new String[] { MetaDataColumns.TRACK, MetaDataColumns.VALUE }, MetaDataColumns.KEY + " = ? ", new String[] { TRACK_ID }, null);
            if (cursor.moveToFirst())
            {
               do
               {
                  final Long trackId = cursor.getLong(0);
                  try
                  {
                     final Integer bcTrackId = Integer.valueOf(cursor.getString(1));
                     this.mSyncedTracks.put(trackId, bcTrackId);
                  }
                  catch (final NumberFormatException e)
                  {
                     Log.w(TAG, "Illigal value stored as track id", e);
                  }
               }
               while (cursor.moveToNext());
            }
         }
         finally
         {
            if (cursor != null)
            {
               cursor.close();
            }
         }
         setChanged();
         notifyObservers();
      }
      final boolean synced = this.mSyncedTracks.containsKey(qtrackId);
      return synced;
   }

   public boolean isLocalTrackSynced(final Long qtrackId)
   {
      final boolean uploaded = isLocalTrackOnline(qtrackId);
      final Integer trackId = this.mSyncedTracks.get(qtrackId);
      final boolean synced = trackId != null && sTrackMappings.get(trackId) != null;
      return uploaded && synced;
   }

   public boolean areTracksLoaded(final Pair<Integer, Integer> item)
   {
      return sBundlesWithTracks.get(item.second) != null && item.first == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE;
   }

   public boolean areTracksLoadingScheduled(final Pair<Integer, Integer> item)
   {
      return sScheduledTracksLoading.contains(item);
   }

   /**
    * Read the static breadcrumbs data from private file
    * 
    * @param ctx
    * @return is refresh is needed
    */
   @SuppressWarnings("unchecked")
   public boolean readCache(final Context ctx)
   {
      FileInputStream fis = null;
      ObjectInputStream ois = null;
      Date bundlesPersisted = null, activitiesPersisted = null;
      Object[] cache;
      synchronized (BREADCRUMSB_BUNDLES_CACHE_FILE)
      {
         try
         {
            fis = ctx.openFileInput(BREADCRUMSB_BUNDLES_CACHE_FILE);
            ois = new ObjectInputStream(fis);

            cache = (Object[]) ois.readObject();
            // new Object[] { CACHE_VERSION, new Date(), sActivitiesWithBundles, sBundlesWithTracks, sBundleMappings, sTrackMappings };
            if (cache[0] instanceof Integer && CACHE_VERSION.equals(cache[0]))
            {
               bundlesPersisted = (Date) cache[1];
               final HashMap<Integer, List<Integer>> bundles = (HashMap<Integer, List<Integer>>) cache[2];
               final HashMap<Integer, Map<String, String>> bundlemappings = (HashMap<Integer, Map<String, String>>) cache[3];
               final HashMap<Integer, Map<String, String>> trackmappings = (HashMap<Integer, Map<String, String>>) cache[4];
               sBundlesWithTracks = bundles != null ? bundles : sBundlesWithTracks;
               sBundleMappings = bundlemappings != null ? bundlemappings : sBundleMappings;
               sTrackMappings = trackmappings != null ? trackmappings : sTrackMappings;
            }
            else
            {
               clearPersistentCache(ctx);
            }

            fis = ctx.openFileInput(BREADCRUMSB_ACTIVITY_CACHE_FILE);
            ois = new ObjectInputStream(fis);
            cache = (Object[]) ois.readObject();
            // new Object[] { CACHE_VERSION, new Date(), sActivityMappings }; 
            if (cache[0] instanceof Integer && CACHE_VERSION.equals(cache[0]))
            {
               activitiesPersisted = (Date) cache[1];
               final HashMap<Integer, Map<String, String>> activitymappings = (HashMap<Integer, Map<String, String>>) cache[2];
               sActivityMappings = activitymappings != null ? activitymappings : sActivityMappings;
            }
            else
            {
               clearPersistentCache(ctx);
            }
         }
         catch (final OptionalDataException e)
         {
            clearPersistentCache(ctx);
            Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
         }
         catch (final ClassNotFoundException e)
         {
            clearPersistentCache(ctx);
            Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
         }
         catch (final IOException e)
         {
            clearPersistentCache(ctx);
            Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
         }
         catch (final ClassCastException e)
         {
            clearPersistentCache(ctx);
            Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
         }
         catch (final ArrayIndexOutOfBoundsException e)
         {
            clearPersistentCache(ctx);
            Log.w(TAG, "Unable to read persisted breadcrumbs cache", e);
         }
         finally
         {
            if (fis != null)
            {
               try
               {
                  fis.close();
               }
               catch (final IOException e)
               {
                  Log.w(TAG, "Error closing file stream after reading cache", e);
               }
            }
            if (ois != null)
            {
               try
               {
                  ois.close();
               }
               catch (final IOException e)
               {
                  Log.w(TAG, "Error closing object stream after reading cache", e);
               }
            }
         }
      }
      setChanged();
      notifyObservers();

      boolean refreshNeeded = false;
      refreshNeeded = refreshNeeded || bundlesPersisted == null || activitiesPersisted == null;
      refreshNeeded = refreshNeeded || (activitiesPersisted.getTime() < new Date().getTime() - CACHE_TIMEOUT * 10);
      refreshNeeded = refreshNeeded || (bundlesPersisted.getTime() < new Date().getTime() - CACHE_TIMEOUT);

      return refreshNeeded;
   }

   public void persistCache(final Context ctx)
   {

      FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      Object[] cache;
      synchronized (BREADCRUMSB_BUNDLES_CACHE_FILE)
      {
         try
         {
            fos = ctx.openFileOutput(BREADCRUMSB_BUNDLES_CACHE_FILE, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            cache = new Object[] { CACHE_VERSION, new Date(), sBundlesWithTracks, sBundleMappings, sTrackMappings };
            oos.writeObject(cache);

            fos = ctx.openFileOutput(BREADCRUMSB_ACTIVITY_CACHE_FILE, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            cache = new Object[] { CACHE_VERSION, new Date(), sActivityMappings };
            oos.writeObject(cache);

         }
         catch (final FileNotFoundException e)
         {
            Log.e(TAG, "Error in file stream during persist cache", e);
         }
         catch (final IOException e)
         {
            Log.e(TAG, "Error in object stream during persist cache", e);
         }
         finally
         {
            if (fos != null)
            {
               try
               {
                  fos.close();
               }
               catch (final IOException e)
               {
                  Log.w(TAG, "Error closing file stream after writing cache", e);
               }
            }
            if (oos != null)
            {
               try
               {
                  oos.close();
               }
               catch (final IOException e)
               {
                  Log.w(TAG, "Error closing object stream after writing cache", e);
               }
            }
         }
      }
   }

   public void clearAllCache(final Context ctx)
   {
      BreadcrumbsTracks.initCacheVariables();
      setChanged();
      clearPersistentCache(ctx);
      notifyObservers();
   }

   public void clearPersistentCache(final Context ctx)
   {
      Log.w(TAG, "Deleting old Breadcrumbs cache files");
      synchronized (BREADCRUMSB_BUNDLES_CACHE_FILE)
      {
         ctx.deleteFile(BREADCRUMSB_ACTIVITY_CACHE_FILE);
         ctx.deleteFile(BREADCRUMSB_BUNDLES_CACHE_FILE);
      }
   }

   @Override
   public String toString()
   {
      return "BreadcrumbsTracks [mActivityMappings=" + sActivityMappings + ", mBundleMappings=" + sBundleMappings + ", mTrackMappings=" + sTrackMappings + ", mActivities=" + sActivityMappings
            + ", mBundles=" + sBundlesWithTracks + "]";
   }
}
