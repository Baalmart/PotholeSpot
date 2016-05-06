package dev.potholespot.android.db;

import android.content.ContentUris;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;

/**
 * The Prim provider stores all static information about PRIM.
 * 
 * @author Martin Bbaale

 */

public final class Pspot
{
   /** The authority of this provider: nl.sogeti.android.gpstracker */
   public static final String AUTHORITY = "dev.baalmart.potholespot";
   /** The content:// style Uri for this provider, content://nl.sogeti.android.gpstracker */
   public static final Uri CONTENT_URI = Uri.parse( "content://" + Pspot.AUTHORITY );
   /** The name of the database file */
   static final String DATABASE_NAME = "PotholeSpot.db";
   /** The version of the database schema */
   static final int DATABASE_VERSION = 10;

   /**
    * This table contains routes...

    */
   
   
   public static final class Labels extends LabelsColumns implements android.provider.BaseColumns
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single track. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.track";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.track";
      /** The content:// style URL for this provider, content://nl.sogeti.android.gpstracker/tracks */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Pspot.AUTHORITY + "/" + Labels.TABLE );

      /** The name of this table */
      public static final String TABLE = "labels";
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + Labels.TABLE + "(" + " " + Labels._ID           + " " + Labels._ID_TYPE + 
                                          "," + " " + Labels.NAME          + " " + Labels.NAME_TYPE + 
                                          "," + " " + Labels.CREATION_TIME + " " + Labels.CREATION_TIME_TYPE + 
                                          "," + " " + Labels.LONGITUDE + " " + Labels.LONGITUDE_TYPE +
                                          "," + " " + Labels.LATITUDE + " " + Labels.LATITUDE_TYPE +
                                          "," + " " + Labels.SPEED + " " + Labels.SPEED_TYPE +
                                          "," + " " + Labels.X + " " + Labels.X_TYPE + 
                                          "," + " " + Labels.Y + " " + Labels.Y_TYPE +
                                          "," + " " + Labels.Z + " " + Labels.Z_TYPE +
                                          ");";
   }
      
   public static final class Xyz extends XYZColumns implements android.provider.BaseColumns
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single track. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.prim.xyz";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.prim.xyz";
      /** The content:// style URL for this provider, content://com.prim/xyz */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Pspot.AUTHORITY + "/" + Xyz.TABLE );

      /** The name of this table */
      public static final String TABLE = "xyz";
      
      //this is the create statement to be used by the Database helper when setting up the database
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + Xyz.TABLE + "(" + " " + Xyz._ID           + " " + Xyz._ID_TYPE +                                          
                                          "," + " " + Xyz.TIME + " "  + Xyz.TIME_TYPE + 
                                          "," + " " + Xyz.SPEED + " " + Xyz.SPEED_TYPE +
                                          "," + " " + Xyz.X          + " " + Xyz.X_TYPE +
                                          "," + " " + Xyz.Y          + " " + Xyz.Y_TYPE +
                                          "," + " " + Xyz.Z          + " " + Xyz.Z_TYPE +
                                          ");";      
      /**
       * Build an Xyz Uri like:
       * 
       * @param labelId
       * @param xyzId
       * @param waypointId
       * 
       * @return
       */
      
      
      public static Uri buildUri(long labelId, long waypointId , long xyzId)
      
      {
         Builder builder = Labels.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, labelId);
        
         builder.appendPath(Locations.TABLE);
         ContentUris.appendId(builder, waypointId);
         
         builder.appendPath(Xyz.TABLE);
         ContentUris.appendId(builder, xyzId);
         
         return builder.build();
      }
   
     }
   

   public static final class Tracks extends TracksColumns implements android.provider.BaseColumns
   {
      /** The MIME type of a CONTENT_URI subdirectory of a single track. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.track";
      /** The MIME type of CONTENT_URI providing a directory of tracks. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.track";
      /** The content:// style URL for this provider, content://nl.sogeti.android.gpstracker/tracks */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Pspot.AUTHORITY + "/" + Tracks.TABLE );

      /** The name of this table */
      public static final String TABLE = "tracks";
      static final String CREATE_STATEMENT = 
         "CREATE TABLE " + Tracks.TABLE + "(" + " " + Tracks._ID           + " " + Tracks._ID_TYPE + 
                                          "," + " " + Tracks.NAME          + " " + Tracks.NAME_TYPE + 
                                          "," + " " + Tracks.CREATION_TIME + " " + Tracks.CREATION_TIME_TYPE + 
                                          ");";
   }
 
   
   
   
   
   
   
   
   /**
    * This table contains segments.

    */
   
   public static final class Segments extends SegmentsColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single segment. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.segment";
      /** The MIME type of CONTENT_URI providing a directory of segments. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.segment";

      /** The name of this table, segments */
      public static final String TABLE = "segments";
      static final String CREATE_STATMENT = 
         "CREATE TABLE " + Segments.TABLE + "(" + " " + Segments._ID   + " " + Segments._ID_TYPE + 
                                            "," + " " + Segments.TRACK + " " + Segments.TRACK_TYPE + 
                                            ");";
   }

   /**
    * This table contains waypoints.

    */
   public static final class Waypoints extends WaypointsColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single waypoint. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.waypoint";
      /** The MIME type of CONTENT_URI providing a directory of waypoints. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.waypoint";
      
      /** The name of this table, waypoints */
      public static final String TABLE = "waypoints";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Waypoints.TABLE + 
      "(" + " " + BaseColumns._ID + " " + WaypointsColumns._ID_TYPE + 
      "," + " " + WaypointsColumns.LATITUDE  + " " + WaypointsColumns.LATITUDE_TYPE + 
      "," + " " + WaypointsColumns.LONGITUDE + " " + WaypointsColumns.LONGITUDE_TYPE + 
      "," + " " + WaypointsColumns.TIME      + " " + WaypointsColumns.TIME_TYPE + 
      "," + " " + WaypointsColumns.SPEED     + " " + WaypointsColumns.SPEED + 
      "," + " " + WaypointsColumns.SEGMENT   + " " + WaypointsColumns.SEGMENT_TYPE + 
      "," + " " + WaypointsColumns.ACCURACY  + " " + WaypointsColumns.ACCURACY_TYPE + 
      "," + " " + WaypointsColumns.ALTITUDE  + " " + WaypointsColumns.ALTITUDE_TYPE + 
      "," + " " + WaypointsColumns.BEARING   + " " + WaypointsColumns.BEARING_TYPE + 
      ");";
      
      static final String[] UPGRADE_STATEMENT_7_TO_8 = 
         {
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.ACCURACY + " " + WaypointsColumns.ACCURACY_TYPE +";",
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.ALTITUDE + " " + WaypointsColumns.ALTITUDE_TYPE +";",
            "ALTER TABLE " + Waypoints.TABLE + " ADD COLUMN " + WaypointsColumns.BEARING  + " " + WaypointsColumns.BEARING_TYPE +";"
         };

      /**
       * Build a waypoint Uri like:
       * 
       * @param trackId
       * @param segmentId
       * @param waypointId
       * 
       * @return
       */
      public static Uri buildUri(long trackId, long segmentId, long waypointId)
      {
         Builder builder = Tracks.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, trackId);
         builder.appendPath(Segments.TABLE);
         ContentUris.appendId(builder, segmentId);
         builder.appendPath(Waypoints.TABLE);
         ContentUris.appendId(builder, waypointId);
         
         return builder.build();
      }
   }
   
   
   
   public static final class Locations extends LocationsColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single waypoint. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.waypoint";
      /** The MIME type of CONTENT_URI providing a directory of waypoints. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.waypoint";
      
      /** The name of this table, waypoints */
      public static final String TABLE = "locations";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Locations.TABLE + 
      "(" + " " + BaseColumns._ID + " " + LocationsColumns._ID_TYPE + 
      "," + " " + LocationsColumns.LATITUDE  + " " + LocationsColumns.LATITUDE_TYPE + 
      "," + " " + LocationsColumns.LONGITUDE + " " + LocationsColumns.LONGITUDE_TYPE + 
      "," + " " + LocationsColumns.TIME      + " " + LocationsColumns.TIME_TYPE + 
      "," + " " + LocationsColumns.SPEED     + " " + LocationsColumns.SPEED + 
      "," + " " + LocationsColumns.SEGMENT   + " " + LocationsColumns.SEGMENT_TYPE + 
      "," + " " + LocationsColumns.ACCURACY  + " " + LocationsColumns.ACCURACY_TYPE + 
      "," + " " + LocationsColumns.ALTITUDE  + " " + LocationsColumns.ALTITUDE_TYPE + 
      "," + " " + LocationsColumns.BEARING   + " " + LocationsColumns.BEARING_TYPE + 
      ");";
      
      static final String[] UPGRADE_STATEMENT_7_TO_8 = 
         {
            "ALTER TABLE " + Locations.TABLE + " ADD COLUMN " + LocationsColumns.ACCURACY + " " + LocationsColumns.ACCURACY_TYPE +";",
            "ALTER TABLE " + Locations.TABLE + " ADD COLUMN " + LocationsColumns.ALTITUDE + " " + LocationsColumns.ALTITUDE_TYPE +";",
            "ALTER TABLE " + Locations.TABLE + " ADD COLUMN " + LocationsColumns.BEARING  + " " + LocationsColumns.BEARING_TYPE +";"
         };

      /**
       * Build a location Uri like:
       * 
       * @param labelId
       * @param xyzId
       * @param locationId
       * 
       * @return
       */
      
      
      public static Uri buildUri(long labelId, long xyzId, long locationId)
      {
         Builder builder = Labels.CONTENT_URI.buildUpon();
         ContentUris.appendId(builder, labelId);
         builder.appendPath(Xyz.TABLE);
         ContentUris.appendId(builder, xyzId);
         builder.appendPath(Locations.TABLE);
         ContentUris.appendId(builder, locationId);
         
         return builder.build();
      }
   }
   
  
   /**
 
    */
   public static final class Media extends MediaColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single media entry. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.media";
      /** The MIME type of CONTENT_URI providing a directory of media entry. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.media";
      
      /** The name of this table */
      public static final String TABLE = "media";
      static final String CREATE_STATEMENT = "CREATE TABLE " + Media.TABLE + 
      "(" + " " + BaseColumns._ID       + " " + MediaColumns._ID_TYPE + 
      "," + " " + MediaColumns.TRACK    + " " + MediaColumns.TRACK_TYPE + 
      "," + " " + MediaColumns.SEGMENT  + " " + MediaColumns.SEGMENT_TYPE + 
      "," + " " + MediaColumns.WAYPOINT + " " + MediaColumns.WAYPOINT_TYPE + 
      "," + " " + MediaColumns.URI      + " " + MediaColumns.URI_TYPE + 
      ");";
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Pspot.AUTHORITY + "/" + Media.TABLE );
   }
   
   /**
    * This table contains media URI's.
    * 

    */
   public static final class MetaData extends MetaDataColumns implements android.provider.BaseColumns
   {

      /** The MIME type of a CONTENT_URI subdirectory of a single metadata entry. */
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.metadata";
      /** The MIME type of CONTENT_URI providing a directory of media entry. */
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.metadata";
      
      /** The name of this table */
      public static final String TABLE = "metadata";
      static final String CREATE_STATEMENT = "CREATE TABLE " + MetaData.TABLE + 
      "(" + " " + BaseColumns._ID          + " " + MetaDataColumns._ID_TYPE + 
      "," + " " + MetaDataColumns.TRACK    + " " + MetaDataColumns.TRACK_TYPE + 
      "," + " " + MetaDataColumns.SEGMENT  + " " + MetaDataColumns.SEGMENT_TYPE + 
      "," + " " + MetaDataColumns.WAYPOINT + " " + MetaDataColumns.WAYPOINT_TYPE + 
      "," + " " + MetaDataColumns.KEY      + " " + MetaDataColumns.KEY_TYPE + 
      "," + " " + MetaDataColumns.VALUE    + " " + MetaDataColumns.VALUE_TYPE + 
      ");";
      /**
       * content://nl.sogeti.android.gpstracker/metadata
       */
      public static final Uri CONTENT_URI = Uri.parse( "content://" + Pspot.AUTHORITY + "/" + MetaData.TABLE );
   }
   
   /**
    * Columns from the tracks table.
    * 
 
    */
   public static class TracksColumns
   {
      public static final String NAME          = "name";
      public static final String CREATION_TIME = "creationtime";
      static final String CREATION_TIME_TYPE   = "INTEGER NOT NULL";
      static final String NAME_TYPE            = "TEXT";
      static final String _ID_TYPE             = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   /**
    * Columns from the tracks table.
    *  
    */
   
   public static class LabelsColumns
   {
      public static final String NAME          = "name";
      public static final String CREATION_TIME = "creationtime";
      public static final String X ="x";
      public static final String Y ="y";
      public static final String Z ="z"; 
      public static final String SPEED = "speed";
      /** The latitude */
      public static final String LATITUDE = "latitude";
      /** The longitude */
      public static final String LONGITUDE = "longitude";
      
      static final String CREATION_TIME_TYPE   = "INTEGER NOT NULL";
      static final String NAME_TYPE            = "TEXT";
      static final String _ID_TYPE             = "INTEGER PRIMARY KEY AUTOINCREMENT";
      static final String X_TYPE = "REAL NOT NULL";
      static final String Y_TYPE = "REAL NOT NULL";
      static final String Z_TYPE = "REAL NOT NULL";
      static final String SPEED_TYPE     = "REAL NOT NULL";
      static final String LATITUDE_TYPE  = "REAL NOT NULL";
      static final String LONGITUDE_TYPE = "REAL NOT NULL";
   }
   
   public static class XYZColumns
   
   {   
      public static final String SPEED = "speed";
      public static final String X ="x";
      public static final String Y ="y";
      public static final String Z ="z";   
      static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT"; 
      /** The recorded time */
      public static final String TIME = "time";
      static final String TIME_TYPE      = "INTEGER NOT NULL";
      static final String SPEED_TYPE     = "REAL NOT NULL";
      static final String X_TYPE = "REAL NOT NULL";
      static final String Y_TYPE = "REAL NOT NULL";
      static final String Z_TYPE = "REAL NOT NULL";
   }   
     
  
   /**
    * Columns from the segments table.
    * 

    */
   public static class SegmentsColumns
   {
      /** The track _id to which this segment belongs */
      public static final String TRACK = "track";     
      static final String TRACK_TYPE   = "INTEGER NOT NULL";
      static final String _ID_TYPE     = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }

   /**
    * Columns from the waypoints table.

    */
   public static class WaypointsColumns
   {

      /** The latitude */
      public static final String LATITUDE = "latitude";
      /** The longitude */
      public static final String LONGITUDE = "longitude";
      /** The recorded time */
      public static final String TIME = "time";
      /** The speed in meters per second */
      public static final String SPEED = "speed";
      /** The segment _id to which this segment belongs */
      public static final String SEGMENT = "tracksegment";
      /** The accuracy of the fix */
      public static final String ACCURACY = "accuracy";
      /** The altitude */
      public static final String ALTITUDE = "altitude";
      /** the bearing of the fix */
      public static final String BEARING = "bearing";

      static final String LATITUDE_TYPE  = "REAL NOT NULL";
      static final String LONGITUDE_TYPE = "REAL NOT NULL";
      static final String TIME_TYPE      = "INTEGER NOT NULL";
      static final String SPEED_TYPE     = "REAL NOT NULL";
      static final String SEGMENT_TYPE   = "INTEGER NOT NULL";
      static final String ACCURACY_TYPE  = "REAL";
      static final String ALTITUDE_TYPE  = "REAL";
      static final String BEARING_TYPE   = "REAL";
      static final String _ID_TYPE       = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   
   
   public static class LocationsColumns
   {

      /** The latitude */
      public static final String LATITUDE = "latitude";
      /** The longitude */
      public static final String LONGITUDE = "longitude";
      /** The recorded time */
      public static final String TIME = "time";
      /** The speed in meters per second */
      public static final String SPEED = "speed";
      /** The segment _id to which this segment belongs */
      public static final String SEGMENT = "tracksegment";
      /** The accuracy of the fix */
      public static final String ACCURACY = "accuracy";
      /** The altitude */
      public static final String ALTITUDE = "altitude";
      /** the bearing of the fix */
      public static final String BEARING = "bearing";

      static final String LATITUDE_TYPE  = "REAL NOT NULL";
      static final String LONGITUDE_TYPE = "REAL NOT NULL";
      static final String TIME_TYPE      = "INTEGER NOT NULL";
      static final String SPEED_TYPE     = "REAL NOT NULL";
      static final String SEGMENT_TYPE   = "INTEGER NOT NULL";
      static final String ACCURACY_TYPE  = "REAL";
      static final String ALTITUDE_TYPE  = "REAL";
      static final String BEARING_TYPE   = "REAL";
      static final String _ID_TYPE       = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   
   /**
    * Columns from the media table.

    */
   public static class MediaColumns
   {
      /** The track _id to which this segment belongs */
      public static final String TRACK    = "track";     
      static final String TRACK_TYPE      = "INTEGER NOT NULL";
      public static final String SEGMENT  = "segment";     
      static final String SEGMENT_TYPE    = "INTEGER NOT NULL";
      public static final String WAYPOINT = "waypoint";     
      static final String WAYPOINT_TYPE   = "INTEGER NOT NULL";
      public static final String URI      = "uri";     
      static final String URI_TYPE        = "TEXT";
      static final String _ID_TYPE        = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
   
   /**
    * Columns from the media table.
    * 

    */
   public static class MetaDataColumns
   {
      /** The track _id to which this segment belongs */
      public static final String TRACK    = "track";     
      static final String TRACK_TYPE      = "INTEGER NOT NULL";
      public static final String SEGMENT  = "segment";     
      static final String SEGMENT_TYPE    = "INTEGER";
      public static final String WAYPOINT = "waypoint";     
      static final String WAYPOINT_TYPE   = "INTEGER";
      public static final String KEY      = "key";          
      static final String KEY_TYPE        = "TEXT NOT NULL";
      public static final String VALUE    = "value";        
      static final String VALUE_TYPE      = "TEXT NOT NULL";
      static final String _ID_TYPE        = "INTEGER PRIMARY KEY AUTOINCREMENT";
   }
}
