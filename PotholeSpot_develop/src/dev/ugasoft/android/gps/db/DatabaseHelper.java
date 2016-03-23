package dev.ugasoft.android.gps.db;

import java.util.ArrayList;
import java.util.Date;

import dev.ugasoft.android.gps.db.Prim.Labels;
import dev.ugasoft.android.gps.db.Prim.LabelsColumns;
import dev.ugasoft.android.gps.db.Prim.Locations;
import dev.ugasoft.android.gps.db.Prim.LocationsColumns;
import dev.ugasoft.android.gps.db.Prim.Media;
import dev.ugasoft.android.gps.db.Prim.MediaColumns;
import dev.ugasoft.android.gps.db.Prim.MetaData;
import dev.ugasoft.android.gps.db.Prim.Segments;
import dev.ugasoft.android.gps.db.Prim.Tracks;
import dev.ugasoft.android.gps.db.Prim.TracksColumns;
import dev.ugasoft.android.gps.db.Prim.Waypoints;
import dev.ugasoft.android.gps.db.Prim.WaypointsColumns;
import dev.ugasoft.android.gps.db.Prim.Xyz;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import dev.ugasoft.android.gps.actions.*;

/**
 * Class to hold bare-metal database operations exposed as functionality blocks
 * To be used by database adapters, like a content provider, that implement a
 * required functionality set
 * 
 * @version $Id$
   @author Martin Bbaale
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
   private Context mContext;
   private final static String TAG = "PRIM.DatabaseHelper";

   public DatabaseHelper(Context context)
   {
      super(context, Prim.DATABASE_NAME, null, Prim.DATABASE_VERSION);
      this.mContext = context;
   }

   /*
    * (non-Javadoc)
    * @see
    * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
    * .SQLiteDatabase)
    */
   @Override
   public void onCreate(SQLiteDatabase db)
   {
      db.execSQL(Labels.CREATE_STATEMENT);
      db.execSQL(Locations.CREATE_STATEMENT);
      db.execSQL(Xyz.CREATE_STATEMENT);
      db.execSQL(Waypoints.CREATE_STATEMENT);
      db.execSQL(Segments.CREATE_STATMENT);
      db.execSQL(Tracks.CREATE_STATEMENT);
      db.execSQL(Media.CREATE_STATEMENT);
      db.execSQL(MetaData.CREATE_STATEMENT);      
     
   }
   
   
  /* public SQLiteDatabase openDB(SQLiteDatabase db) {
      db = this.getWritableDatabase();
      return db;
  }*/
   
   
   /**
    * Will update version 1 through 5 to version 8
    * 
    * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase,
    *      int, int)
    * @see Prim.DATABASE_VERSION
    */
   
   
   @Override
   public void onUpgrade(SQLiteDatabase db, int current, int targetVersion)
   {
      Log.i(TAG, "Upgrading db from " + current + " to " + targetVersion);
      if (current <= 5) // From 1-5 to 6 (these before are the same before) 
      {
         current = 6;
      }
      if (current == 6) // From 6 to 7 ( no changes ) 
      {
         current = 7;
      }
      if (current == 7) // From 7 to 8 ( more waypoints data ) 
      {
         for (String statement : Waypoints.UPGRADE_STATEMENT_7_TO_8)
         {
            db.execSQL(statement);
         }
         current = 8;
      }
      if (current == 8) // From 8 to 9 ( media Uri data ) 
      {
         db.execSQL(Media.CREATE_STATEMENT);
         current = 9;
      }
      if (current == 9) // From 9 to 10 ( metadata ) 
      {
         db.execSQL(MetaData.CREATE_STATEMENT);
         current = 10;
      }
   }

   //vacuuming the database
   public void vacuum()
   {
      new Thread()
      {
         @Override
         public void run()
         {
            SQLiteDatabase sqldb = getWritableDatabase();
            sqldb.execSQL("VACUUM");
         }
      }.start();

   }

   int bulkInsertWaypoint(long trackId, long segmentId, ContentValues[] valuesArray)
   {
      if (trackId < 0 || segmentId < 0)
      {
         throw new IllegalArgumentException("Track and segments may not the less then 0.");
      }
      int inserted = 0;

      SQLiteDatabase sqldb = getWritableDatabase();
      sqldb.beginTransaction();
      try
      {
         for (ContentValues args : valuesArray)
         {
            args.put(Waypoints.SEGMENT, segmentId);

            long id = sqldb.insert(Waypoints.TABLE, null, args);
            if (id >= 0)
            {
               inserted++;
            }
         }
         sqldb.setTransactionSuccessful();

      }
      finally
      {
         if (sqldb.inTransaction())
         {
            sqldb.endTransaction();
         }
      }

      return inserted;
   }
   
   
   
   int bulkInsertLocations(long labelId, ContentValues[] valuesArray)
   {
      if (labelId < 0 )
      {
         throw new IllegalArgumentException("labels may not the less then 0.");
      }
      int inserted = 0;

      SQLiteDatabase sqldb = getWritableDatabase();
      sqldb.beginTransaction();
      try
      {
         for (ContentValues args : valuesArray)
         {
            //args.put(Locations.SEGMENT, segmentId);

            long id = sqldb.insert(Locations.TABLE, null, args);
            if (id >= 0)
            {
               inserted++;
            }
         }
         sqldb.setTransactionSuccessful();

      }
      finally
      {
         if (sqldb.inTransaction())
         {
            sqldb.endTransaction();
         }
      }

      return inserted;
   }
   
   
   

   /**
    * Creates a waypoint under the current track segment with the current time
    * on which the waypoint is reached
    * 
    * @param track track
    * @param segment segment
    * @param latitude latitude
    * @param longitude longitude
    * @param time time
    * @param speed the measured speed
    * @return
    */
   long insertWaypoint(long trackId, long segmentId, Location location)
   {
      if (trackId < 0 || segmentId < 0)
      {
         throw new IllegalArgumentException("Track and segments may not the less then 0.");
      }

      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(WaypointsColumns.SEGMENT, segmentId);
      args.put(WaypointsColumns.TIME, location.getTime());
      args.put(WaypointsColumns.LATITUDE, location.getLatitude());
      args.put(WaypointsColumns.LONGITUDE, location.getLongitude());
      args.put(WaypointsColumns.SPEED, location.getSpeed());
      args.put(WaypointsColumns.ACCURACY, location.getAccuracy());
      args.put(WaypointsColumns.ALTITUDE, location.getAltitude());
      args.put(WaypointsColumns.BEARING, location.getBearing());

      long waypointId = sqldb.insert(Waypoints.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints");
      resolver.notifyChange(notifyUri, null);

      //      Log.d( TAG, "Waypoint stored: "+notifyUri);
      return waypointId;
   }
   
   public long insertLocation(Location location)
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(LocationsColumns.TIME, location.getTime());
      args.put(LocationsColumns.LATITUDE, location.getLatitude());
      args.put(LocationsColumns.LONGITUDE, location.getLongitude());
      args.put(LocationsColumns.SPEED, location.getSpeed());
      args.put(LocationsColumns.ACCURACY, location.getAccuracy());    
      long locationId = sqldb.insert(Locations.TABLE, null, args);
      sqldb.close();
            Log.d( TAG, "Location stored: ");
      return locationId;
   }
   
   public void deleteLocation(long id) {
      SQLiteDatabase db = getWritableDatabase();
      if (db == null) {
          return;
      }
      db.delete("locations", "_id = ?", new String[] { String.valueOf(id) });
      db.close();
  }
   
   

   public void insertLabel(String label, Long labelTime)
   {
      
      SQLiteDatabase sqldb = getWritableDatabase();
     
      try
      {
      ContentValues args = new ContentValues();
      args.put(Labels.CREATION_TIME, labelTime);
      args.put(Labels.NAME, label);
      sqldb.insert(Labels.TABLE, null, args);    
      
          Log.d( TAG, "label stored ");
      }
      
      catch (Exception e) {
         Log.d(TAG, "Error while trying to add post to database");
     } 
      //return labelId;
   }
   
   public void deleteLabel(long id) {
      SQLiteDatabase db = getWritableDatabase();
      if (db == null) {
          return;
      }
      db.delete("labels", "_id = ?", new String[] { String.valueOf(id) });
      db.close();
  }
   
   public long insert_xyz(float x, float y, float z, Location location)
   {
     
      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(Xyz.TIME, location.getTime());
      args.put(Xyz.X, x);
      args.put(Xyz.Y, y);
      args.put(Xyz.Z, z);
      long xyzId = sqldb.insert(Xyz.TABLE, null, args);
      sqldb.close();
          Log.d( TAG, "acceleration value stored ");
      return xyzId;
   }
   
   public void delete_xyz(long id) {
      SQLiteDatabase db = getWritableDatabase();
      if (db == null) {
          return;
      }
      db.delete("xyz", "_id = ?", new String[] { String.valueOf(id) });
      db.close();
  }
  
   /**
    * Return values for a single row with the specified id
    * @param id The unique id for the row o fetch
    * @return All column values are stored as properties in the ContentValues object
    */
   public ContentValues get(long id) 
   {
       SQLiteDatabase db = getReadableDatabase();
       if (db == null) 
       {
           return null;
       }
       ContentValues row = new ContentValues();
       Cursor cur = db.rawQuery("select name, creationtime from labels where _id = ?", new String[] { String.valueOf(id) });
       if (cur.moveToNext()) 
       {
           row.put("name", cur.getString(0));
           row.put("creationtime", cur.getInt(1));
       }
       cur.close();
       db.close();
       return row;
   }
   
  
   public Cursor selectLabelsRecords() 
   {
      SQLiteDatabase database = getReadableDatabase();
      String[] cols = new String[] {Labels._ID, Labels.NAME};  
      Cursor mCursor = database.query(true, Labels.TABLE,cols,null  
               , null, null, null, null, null);  
      if (mCursor != null) {  
        mCursor.moveToFirst();  
      }  
      return mCursor; // iterate to get each value.
   }
  
 
   /**
    * Insert a URI for a given waypoint/segment/track in the media table
    * 
    * @param trackId
    * @param segmentId
    * @param waypointId
    * @param mediaUri
    * @return
    */
   long insertMedia(long trackId, long segmentId, long waypointId, String mediaUri)
   {
      if (trackId < 0 || segmentId < 0 || waypointId < 0)
         
      {
         throw new IllegalArgumentException("Track, segments and waypoint may not the less then 0.");
      }
      
      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(MediaColumns.TRACK, trackId);
      args.put(MediaColumns.SEGMENT, segmentId);
      args.put(MediaColumns.WAYPOINT, waypointId);
      args.put(MediaColumns.URI, mediaUri);

      Log.d( TAG, "Media stored in the datebase: "+mediaUri );

      long mediaId = sqldb.insert(Media.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints/" + waypointId + "/media");
      resolver.notifyChange(notifyUri, null);      
      Log.d( TAG, "Notify: "+notifyUri );      
      resolver.notifyChange(Media.CONTENT_URI, null);
      Log.d( TAG, "Notify: "+Media.CONTENT_URI );

      return mediaId;
   }

   /**
    * Insert a key/value pair as meta-data for a track and optionally narrow the
    * scope by segment or segment/waypoint
    * 
    * @param trackId
    * @param segmentId
    * @param waypointId
    * @param key
    * @param value
    * @return
    */
   long insertOrUpdateMetaData(long trackId, long segmentId, long waypointId, String key, String value)
   {
      long metaDataId = -1;
      if (trackId < 0 && key != null && value != null)
      {
         throw new IllegalArgumentException("Track, key and value must be provided");
      }
      if (waypointId >= 0 && segmentId < 0)
      {
         throw new IllegalArgumentException("Waypoint must have segment");
      }

      ContentValues args = new ContentValues();
      args.put(MetaData.TRACK, trackId);
      args.put(MetaData.SEGMENT, segmentId);
      args.put(MetaData.WAYPOINT, waypointId);
      args.put(MetaData.KEY, key);
      args.put(MetaData.VALUE, value);
      String whereClause = MetaData.TRACK + " = ? AND " + MetaData.SEGMENT + " = ? AND " + MetaData.WAYPOINT + " = ? AND " + MetaData.KEY + " = ?";
      String[] whereArgs = new String[] { Long.toString(trackId), Long.toString(segmentId), Long.toString(waypointId), key };

      SQLiteDatabase sqldb = getWritableDatabase();
      int updated = sqldb.update(MetaData.TABLE, args, whereClause, whereArgs);
      if (updated == 0)
      {
         metaDataId = sqldb.insert(MetaData.TABLE, null, args);
      }
      else
      {
         Cursor c = null;
         try
         {
            c = sqldb.query(MetaData.TABLE, new String[] { MetaData._ID }, whereClause, whereArgs, null, null, null);
            if( c.moveToFirst() )
            {
               metaDataId = c.getLong(0);
            }
         }
         finally
         {
            if (c != null)
            {
               c.close();
            }
         }
      }

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri;
      if (segmentId >= 0 && waypointId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints/" + waypointId + "/metadata");
      }
      else if (segmentId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/metadata");
      }
      else
      {
         notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/metadata");
      }
      resolver.notifyChange(notifyUri, null);
      resolver.notifyChange(MetaData.CONTENT_URI, null);

      return metaDataId;
   }

   /**
    * Deletes a single track and all underlying segments, waypoints, media and
    * metadata
    * 
    * @param trackId
    * @return
    */
   @SuppressWarnings("resource")
   int deleteTrack(long trackId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();
      int affected = 0;
      Cursor cursor = null;
      long segmentId = -1;
      long metadataId = -1;

      try
      {
         sqldb.beginTransaction();
         // Iterate on each segement to delete each
         cursor = sqldb.query(Segments.TABLE, new String[] { Segments._ID }, Segments.TRACK + "= ?", new String[] { String.valueOf(trackId) }, null, null,
               null, null);
         if (cursor.moveToFirst())
         {
            do
            {
               segmentId = cursor.getLong(0);
               affected += deleteSegment(sqldb, trackId, segmentId);
            }
            while (cursor.moveToNext());
         }
         else
         {
            Log.e(TAG, "Did not find the last active segment");
         }
         // Delete the track
         affected += sqldb.delete(Tracks.TABLE, Tracks._ID + "= ?", new String[] { String.valueOf(trackId) });
         // Delete remaining meta-data
         affected += sqldb.delete(MetaData.TABLE, MetaData.TRACK + "= ?", new String[] { String.valueOf(trackId) });

         cursor = sqldb.query(MetaData.TABLE, new String[] { MetaData._ID }, MetaData.TRACK + "= ?", new String[] { String.valueOf(trackId) }, null, null,
               null, null);
         if (cursor.moveToFirst())
         {
            do
            {
               metadataId = cursor.getLong(0);
               affected += deleteMetaData(metadataId);
            }
            while (cursor.moveToNext());
         }

         sqldb.setTransactionSuccessful();
      }
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
         if (sqldb.inTransaction())
         {
            sqldb.endTransaction();
         }
      }

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Tracks.CONTENT_URI, null);
      resolver.notifyChange(ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId), null);

      return affected;
   }

   /**
    * @param mediaId
    * @return
    */
   int deleteMedia(long mediaId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      Cursor cursor = null;
      long trackId = -1;
      long segmentId = -1;
      long waypointId = -1;
      try
      {
         cursor = sqldb.query(Media.TABLE, new String[] { Media.TRACK, Media.SEGMENT, Media.WAYPOINT }, Media._ID + "= ?",
               new String[] { String.valueOf(mediaId) }, null, null, null, null);
         if (cursor.moveToFirst())
         {
            trackId = cursor.getLong(0);
            segmentId = cursor.getLong(0);
            waypointId = cursor.getLong(0);
         }
         else
         {
            Log.e(TAG, "Did not find the media element to delete");
         }
      }
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
      }

      int affected = sqldb.delete(Media.TABLE, Media._ID + "= ?", new String[] { String.valueOf(mediaId) });

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints/" + waypointId + "/media");
      resolver.notifyChange(notifyUri, null);
      notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/media");
      resolver.notifyChange(notifyUri, null);
      notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/media");
      resolver.notifyChange(notifyUri, null);
      resolver.notifyChange(ContentUris.withAppendedId(Media.CONTENT_URI, mediaId), null);

      return affected;
   }

   int deleteMetaData(long metadataId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      Cursor cursor = null;
      long trackId = -1;
      long segmentId = -1;
      long waypointId = -1;
      try
      {
         cursor = sqldb.query(MetaData.TABLE, new String[] { MetaData.TRACK, MetaData.SEGMENT, MetaData.WAYPOINT }, MetaData._ID + "= ?",
               new String[] { String.valueOf(metadataId) }, null, null, null, null);
         if (cursor.moveToFirst())
         {
            trackId = cursor.getLong(0);
            segmentId = cursor.getLong(0);
            waypointId = cursor.getLong(0);
         }
         else
         {
            Log.e(TAG, "Did not find the media element to delete");
         }
      }
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
      }

      int affected = sqldb.delete(MetaData.TABLE, MetaData._ID + "= ?", new String[] { String.valueOf(metadataId) });

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri;
      if (trackId >= 0 && segmentId >= 0 && waypointId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints/" + waypointId + "/media");
         resolver.notifyChange(notifyUri, null);
      }
      if (trackId >= 0 && segmentId >= 0)
      {
         notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/media");
         resolver.notifyChange(notifyUri, null);
      }
      notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/media");
      resolver.notifyChange(notifyUri, null);
      resolver.notifyChange(ContentUris.withAppendedId(Media.CONTENT_URI, metadataId), null);

      return affected;
   }

   /**
    * Delete a segment and all member waypoints
    * 
    * @param sqldb The SQLiteDatabase in question
    * @param trackId The track id of this delete
    * @param segmentId The segment that needs deleting
    * @return
    */
   int deleteSegment(SQLiteDatabase sqldb, long trackId, long segmentId)
   {
      int affected = sqldb.delete(Segments.TABLE, Segments._ID + "= ?", new String[] { String.valueOf(segmentId) });

      // Delete all waypoints from segments
      affected += sqldb.delete(Waypoints.TABLE, Waypoints.SEGMENT + "= ?", new String[] { String.valueOf(segmentId) });
      // Delete all media from segment
      affected += sqldb.delete(Media.TABLE, Media.TRACK + "= ? AND " + Media.SEGMENT + "= ?",
            new String[] { String.valueOf(trackId), String.valueOf(segmentId) });
      // Delete meta-data
      affected += sqldb.delete(MetaData.TABLE, MetaData.TRACK + "= ? AND " + MetaData.SEGMENT + "= ?",
            new String[] { String.valueOf(trackId), String.valueOf(segmentId) });

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId), null);
      resolver.notifyChange(Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments"), null);

      return affected;
   }

   int updateTrack(long trackId, String name)
   {
      int updates;
      String whereclause = Tracks._ID + " = " + trackId;
      ContentValues args = new ContentValues();
      args.put(Tracks.NAME, name);

      // Execute the query.
      SQLiteDatabase mDb = getWritableDatabase();
      updates = mDb.update(Tracks.TABLE, args, whereclause, null);

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId);
      resolver.notifyChange(notifyUri, null);

      return updates;
   }
   
   int updateLabel(long labelId, String name)
   {
      int updates;
      String whereclause = Labels._ID + " = " + labelId;
      ContentValues args = new ContentValues();
      args.put(Labels.NAME, name);

      // Execute the query.
      SQLiteDatabase mDb = getWritableDatabase();
      updates = mDb.update(Labels.TABLE, args, whereclause, null);

      ContentResolver resolver = this.mContext.getContentResolver();
      Uri notifyUri = ContentUris.withAppendedId(Labels.CONTENT_URI, labelId);
      resolver.notifyChange(notifyUri, null);

      return updates;
   }

   /**
    * Insert a key/value pair as meta-data for a track and optionally narrow the
    * scope by segment or segment/waypoint
    * 
    * @param trackId
    * @param segmentId
    * @param waypointId
    * @param key
    * @param value
    * @return
    */
   int updateMetaData(long trackId, long segmentId, long waypointId, long metadataId, String selection, String[] selectionArgs, String value)
   {
      {
         if ((metadataId < 0 && trackId < 0))
         {
            throw new IllegalArgumentException("Track or meta-data id be provided");
         }
         if (trackId >= 0 && (selection == null || !selection.contains("?") || selectionArgs.length != 1))
         {
            throw new IllegalArgumentException("A where clause selection must be provided to select the correct KEY");
         }
         if (trackId >= 0 && waypointId >= 0 && segmentId < 0)
         {
            throw new IllegalArgumentException("Waypoint must have segment");
         }

         SQLiteDatabase sqldb = getWritableDatabase();

         String[] whereParams;
         String whereclause;
         if (metadataId >= 0)
         {
            whereclause = MetaData._ID + " = ? ";
            whereParams = new String[] { Long.toString(metadataId) };
         }
         else
         {
            whereclause = MetaData.TRACK + " = ? AND " + MetaData.SEGMENT + " = ? AND " + MetaData.WAYPOINT + " = ? AND " + MetaData.KEY + " = ? ";
            whereParams = new String[] { Long.toString(trackId), Long.toString(segmentId), Long.toString(waypointId), selectionArgs[0] };
         }
         ContentValues args = new ContentValues();
         args.put(MetaData.VALUE, value);

         int updates = sqldb.update(MetaData.TABLE, args, whereclause, whereParams);

         ContentResolver resolver = this.mContext.getContentResolver();
         Uri notifyUri;
         if (trackId >= 0 && segmentId >= 0 && waypointId >= 0)
         {
            notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/waypoints/" + waypointId + "/metadata");
         }
         else if (trackId >= 0 && segmentId >= 0)
         {
            notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments/" + segmentId + "/metadata");
         }
         else if (trackId >= 0)
         {
            notifyUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/metadata");
         }
         else
         {
            notifyUri = Uri.withAppendedPath(MetaData.CONTENT_URI, "" + metadataId);
         }

         resolver.notifyChange(notifyUri, null);
         resolver.notifyChange(MetaData.CONTENT_URI, null);

         return updates;
      }
   }

   /**
    * Move to a fresh track with a new first segment for this track
    * 
    * @return
    */
   long toNextTrack(String name)
   {
      long currentTime = new Date().getTime();
      ContentValues args = new ContentValues();
      args.put(TracksColumns.NAME, name);
      args.put(TracksColumns.CREATION_TIME, currentTime);

      SQLiteDatabase sqldb = getWritableDatabase();
      long trackId = sqldb.insert(Tracks.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Tracks.CONTENT_URI, null);

      return trackId;
   }
   
   long toNextLabel(String name)
   {
      long currentTime = new Date().getTime();
      ContentValues args = new ContentValues();
      args.put(LabelsColumns.NAME, name);
      args.put(LabelsColumns.CREATION_TIME, currentTime);

      SQLiteDatabase sqldb = getWritableDatabase();
      long labelId = sqldb.insert(Labels.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Labels.CONTENT_URI, null);

      return labelId;
   }

   /**
    * Moves to a fresh segment to which waypoints can be connected
    * 
    * @return
    */
   long toNextSegment(long trackId)
   {
      SQLiteDatabase sqldb = getWritableDatabase();

      ContentValues args = new ContentValues();
      args.put(Segments.TRACK, trackId);
      long segmentId = sqldb.insert(Segments.TABLE, null, args);

      ContentResolver resolver = this.mContext.getContentResolver();
      resolver.notifyChange(Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments"), null);

      return segmentId;
   }
   
   /***
    * 
    * ************methods for the database manager**********
    * 
    * 
    * ************/
   
   
   public ArrayList<Cursor> getData(String Query)
   {
      //get writable database
      SQLiteDatabase sqlDB = this.getWritableDatabase();
      String[] columns = new String[] { "mesage" };
      //an array list of cursor to save two cursors one has results from the query 
      //other cursor stores error message if any errors are triggered
      ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
      MatrixCursor Cursor2= new MatrixCursor(columns);
      alc.add(null);
      alc.add(null);
      
      try
      {
         String maxQuery = Query ;
         //execute the query results will be save in Cursor c
         Cursor c = sqlDB.rawQuery(maxQuery, null);
         
         //add value to cursor2
         Cursor2.addRow(new Object[] { "Success" });
         
         alc.set(1,Cursor2);
         if (null != c && c.getCount() > 0)         
         {            
            alc.set(0,c);
            c.moveToFirst();            
            return alc ;
         }
         
         return alc;
      } 
      
      catch(SQLException sqlEx)
      {
         Log.d("printing exception", sqlEx.getMessage());
         //if any exceptions are triggered save the error message to cursor an return the arraylist
         Cursor2.addRow(new Object[] 
               { ""+sqlEx.getMessage() });
         alc.set(1,Cursor2);
         return alc;
      } 
      
      catch(Exception ex)
      {
         Log.d("printing exception", ex.getMessage());

         //if any exceptions are triggered save the error message to cursor an return the arraylist
         Cursor2.addRow(new Object[] { ""+ex.getMessage() });
         alc.set(1,Cursor2);
         return alc;
      }
         }
   
   public ArrayList<PotholeSpotLabel> getUntaggedLabels(){
	   
	   ArrayList<PotholeSpotLabel> labels = new ArrayList<PotholeSpotLabel>();
	   
	   String query = "SELECT  * FROM " + Labels.TABLE;// need to add a where clause
	   
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(query, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	        	PotholeSpotLabel label = new PotholeSpotLabel();
	        	label.setID((Integer.parseInt(cursor.getString(0))));
	        	label.setXValue(Float.parseFloat(cursor.getString(1)));
	        	label.setYValue(Float.parseFloat(cursor.getString(2)));
	        	label.setYValue(Float.parseFloat(cursor.getString(3)));
	            // Adding contact to list
	        	labels.add(label);
	        } while (cursor.moveToNext());
	    }
	 
	   return labels;
   }
   
   public void UpdateLabelTag(PotholeSpotLabel label){
	   SQLiteDatabase db = this.getWritableDatabase();
	   
	    ContentValues values = new ContentValues();
	    values.put(Xyz.Tag, label.getTag());
	    values.put(Xyz._ID, label.getID());
	 
	    // updating row
	     db.update(Labels.TABLE, values, Xyz._ID + " = ?",
	            new String[] { String.valueOf(label.getID()) });  
   }
   
   }
