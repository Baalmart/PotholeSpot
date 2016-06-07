package dev.potholespot.android.db;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.LiveFolders;
import android.util.Log;
import dev.potholespot.android.db.Pspot.Labels;
import dev.potholespot.android.db.Pspot.LabelsColumns;
import dev.potholespot.android.db.Pspot.Locations;
import dev.potholespot.android.db.Pspot.LocationsColumns;
import dev.potholespot.android.db.Pspot.Media;
import dev.potholespot.android.db.Pspot.MediaColumns;
import dev.potholespot.android.db.Pspot.MetaData;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.android.db.Pspot.Segments;
import dev.potholespot.android.db.Pspot.SegmentsColumns;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.Waypoints;
import dev.potholespot.android.db.Pspot.WaypointsColumns;

/**
 * Goal of this Content Provider is to make the PotholeSpot information uniformly available to this application and even other applications. The PotholeSpot database can hold, labels, locations,
 * acceleration values, tracks, segments or waypoints
 * <p>
 * A track is an actual route taken from start to finish. All the GPS locations collected are waypoints. Waypoints taken in sequence without loss of GPS-signal are considered connected and are grouped
 * in segments. A route is build up out of 1 or more segments.
 * <p>
 * For example:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks</code> is the URI that returns all the stored tracks or starts a new track on insert
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2</code> is the URI string that would return a single result row, the track with ID = 23.
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments</code> is the URI that returns all the stored segments of a track with ID = 2 or starts a new segment on insert
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/waypoints</code> is the URI that returns all the stored waypoints of a track with ID = 2
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments</code> is the URI that returns all the stored segments of a track with ID = 2
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/3</code> is the URI string that would return a single result row, the segment with ID = 3 of a track with ID = 2 .
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/1/waypoints</code> is the URI that returns all the waypoints of a segment 1 of track 2.
 * <p>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/1/waypoints/52</code> is the URI string that would return a single result row, the waypoint with ID = 52
 * <p>
 * Media is stored under a waypoint and may be queried as:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/3/waypoints/22/media</code>
 * <p>
 * All media for a segment can be queried with:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/3/media</code>
 * <p>
 * All media for a track can be queried with:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/media</code>
 * <p>
 * The whole set of collected media may be queried as:<br>
 * <code>content://nl.sogeti.android.gpstracker/media</code>
 * <p>
 * A single media is stored with an ID, for instance ID = 12:<br>
 * <code>content://nl.sogeti.android.gpstracker/media/12</code>
 * <p>
 * The whole set of collected media may be queried as:<br>
 * <code>content://nl.sogeti.android.gpstracker/media</code>
 * <p>
 * Meta-data regarding a single waypoint may be queried as:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/3/waypoints/22/metadata</code>
 * <p>
 * Meta-data regarding a single segment as whole may be queried as:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/segments/3/metadata</code> Note: This does not include meta-data of waypoints.
 * <p>
 * Meta-data regarding a single track as a whole may be queried as:<br>
 * <code>content://nl.sogeti.android.gpstracker/tracks/2/metadata</code> Note: This does not include meta-data of waypoints or segments.
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class PspotProvider extends ContentProvider
{

   private static final String TAG = "PRIM.PrimProvider";

   /* Action types as numbers for using the UriMatcher */
   private static final int TRACKS = 1;
   private static final int TRACK_ID = 2;
   private static final int TRACK_MEDIA = 3;
   private static final int TRACK_WAYPOINTS = 4;
   private static final int SEGMENTS = 5;
   private static final int SEGMENT_ID = 6;
   private static final int SEGMENT_MEDIA = 7;
   private static final int WAYPOINTS = 8;
   private static final int WAYPOINT_ID = 9;
   private static final int WAYPOINT_MEDIA = 10;
   private static final int SEARCH_SUGGEST_ID = 11;
   private static final int LIVE_FOLDERS = 12;
   private static final int MEDIA = 13;
   private static final int MEDIA_ID = 14;
   private static final int TRACK_METADATA = 15;
   private static final int SEGMENT_METADATA = 16;
   private static final int WAYPOINT_METADATA = 17;
   private static final int METADATA = 18;
   private static final int METADATA_ID = 19;
   private static final int LOCATIONS = 20;
   private static final int LABELS = 21;
   private static final int LOCATION_ID = 22;
   private static final int LABEL_ID = 23;
   private static final int XYZ = 24;
   private static final int XYZ_ID = 25;

   private static final String[] SUGGEST_PROJECTION = new String[] { Tracks._ID, Tracks.NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
         "datetime(" + Tracks.CREATION_TIME + "/1000, 'unixepoch') as " + SearchManager.SUGGEST_COLUMN_TEXT_2, Tracks._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID

   };
   private static final String[] LIVE_PROJECTION = new String[] { Tracks._ID + " AS " + LiveFolders._ID, Tracks.NAME + " AS " + LiveFolders.NAME,
         "datetime(" + Tracks.CREATION_TIME + "/1000, 'unixepoch') as " + LiveFolders.DESCRIPTION };

   private static UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

   /**
    * Although it is documented that in addURI(null, path, 0) "path" should be an absolute path this does not seem to work. A relative path gets the jobs done and matches an absolute path.
    */
   static
   {
      PspotProvider.sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks", PspotProvider.TRACKS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#", PspotProvider.TRACK_ID);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "labels", PspotProvider.LABELS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "labels/#", PspotProvider.LABEL_ID);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/media", PspotProvider.TRACK_MEDIA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/metadata", PspotProvider.TRACK_METADATA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/waypoints", PspotProvider.TRACK_WAYPOINTS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments", PspotProvider.SEGMENTS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#", PspotProvider.SEGMENT_ID);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/media", PspotProvider.SEGMENT_MEDIA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/metadata", PspotProvider.SEGMENT_METADATA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/waypoints", PspotProvider.WAYPOINTS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/waypoints/#", PspotProvider.WAYPOINT_ID);

      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/locations", PspotProvider.LOCATIONS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/locations/#", PspotProvider.LOCATION_ID);

      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/waypoints/#/media", PspotProvider.WAYPOINT_MEDIA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "tracks/#/segments/#/waypoints/#/metadata", PspotProvider.WAYPOINT_METADATA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "media", PspotProvider.MEDIA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "media/#", PspotProvider.MEDIA_ID);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "metadata", PspotProvider.METADATA);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "metadata/#", PspotProvider.METADATA_ID);

      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "live_folders/tracks", PspotProvider.LIVE_FOLDERS);
      PspotProvider.sURIMatcher.addURI(Pspot.AUTHORITY, "search_suggest_query", PspotProvider.SEARCH_SUGGEST_ID);

   }

   private DatabaseHelper mDbHelper;

   /**
    * (non-Javadoc)
    * 
    * @see android.content.ContentProvider#onCreate()
    */
   @Override
   public boolean onCreate()
   {

      if (this.mDbHelper == null)
      {
         this.mDbHelper = new DatabaseHelper(getContext());
      }
      return true;
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.content.ContentProvider#getType(android.net.Uri)
    */
   @Override
   public String getType(final Uri uri)
   {
      final int match = PspotProvider.sURIMatcher.match(uri);
      String mime = null;
      switch (match)
      {
         case TRACKS:
            mime = Tracks.CONTENT_TYPE;
            break;
         case TRACK_ID:
            mime = Tracks.CONTENT_ITEM_TYPE;
            break;

         case LABELS:
            mime = Labels.CONTENT_TYPE;
            break;

         case LABEL_ID:
            mime = Labels.CONTENT_ITEM_TYPE;
            break;

         case SEGMENTS:
            mime = Segments.CONTENT_TYPE;
            break;
         case SEGMENT_ID:
            mime = Segments.CONTENT_ITEM_TYPE;
            break;
         case WAYPOINTS:
            mime = Waypoints.CONTENT_TYPE;
            break;
         case WAYPOINT_ID:
            mime = Waypoints.CONTENT_ITEM_TYPE;
            break;

         case MEDIA_ID:
         case TRACK_MEDIA:
         case SEGMENT_MEDIA:
         case WAYPOINT_MEDIA:
            mime = Media.CONTENT_ITEM_TYPE;
            break;
         case METADATA_ID:
         case TRACK_METADATA:
         case SEGMENT_METADATA:
         case WAYPOINT_METADATA:
            mime = MetaData.CONTENT_ITEM_TYPE;
            break;
         case UriMatcher.NO_MATCH:
         default:
            Log.w(TAG, "There is not MIME type defined for URI " + uri);
            break;
      }
      return mime;
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues) data insertions into the database
    */
   @Override
   public Uri insert(final Uri uri, final ContentValues values)
   {
      Log.d(TAG, "insert on " + uri);
      Uri insertedUri = null;
      final int match = PspotProvider.sURIMatcher.match(uri);
      List<String> pathSegments = null;
      long trackId = -1;
      long segmentId = -1;
      long waypointId = -1;
      long locationId = -1;
      long labelId = -1;
      final long xyzId = -1;

      long mediaId = -1;
      String key;
      String value;
      switch (match)
      {
         case WAYPOINTS:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            final Location loc = new Location(TAG);
            final Double latitude = values.getAsDouble(WaypointsColumns.LATITUDE);
            final Double longitude = values.getAsDouble(WaypointsColumns.LONGITUDE);
            Long time = values.getAsLong(WaypointsColumns.TIME);
            Float speed = values.getAsFloat(WaypointsColumns.SPEED);
            if (time == null)
            {
               time = System.currentTimeMillis();
            }
            if (speed == null)
            {
               speed = 0f;
            }
            loc.setLatitude(latitude);
            loc.setLongitude(longitude);
            loc.setTime(time);
            loc.setSpeed(speed);

            if (values.containsKey(WaypointsColumns.ACCURACY))
            {
               loc.setAccuracy(values.getAsFloat(WaypointsColumns.ACCURACY));
            }

            if (values.containsKey(WaypointsColumns.ALTITUDE))
            {
               loc.setAltitude(values.getAsDouble(WaypointsColumns.ALTITUDE));

            }

            if (values.containsKey(WaypointsColumns.BEARING))
            {
               loc.setBearing(values.getAsFloat(WaypointsColumns.BEARING));
            }

            waypointId = this.mDbHelper.insertWaypoint(trackId, segmentId, loc);
            Log.d(TAG, "Have inserted to segment " + segmentId + " with waypoint " + waypointId);
            insertedUri = ContentUris.withAppendedId(uri, waypointId);
            break;

         case LOCATIONS:
            pathSegments = uri.getPathSegments();
            labelId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            final Location location = new Location(TAG);
            final Double latitude_loc = values.getAsDouble(LocationsColumns.LATITUDE);
            final Double longitude_loc = values.getAsDouble(LocationsColumns.LONGITUDE);
            Long time_loc = values.getAsLong(LocationsColumns.TIME);
            final Float speed_loc = values.getAsFloat(LocationsColumns.SPEED);

            if (time_loc == null)
            {
               time_loc = System.currentTimeMillis();
            }

            if (speed_loc == null)
            {
               speed = 0f;
            }
            location.setLatitude(latitude_loc);
            location.setLongitude(longitude_loc);
            location.setTime(time_loc);
            location.setSpeed(speed_loc);

            if (values.containsKey(LocationsColumns.ACCURACY))
            {
               location.setAccuracy(values.getAsFloat(LocationsColumns.ACCURACY));
            }

            if (values.containsKey(LocationsColumns.ALTITUDE))
            {
               location.setAltitude(values.getAsDouble(LocationsColumns.ALTITUDE));
            }

            if (values.containsKey(LocationsColumns.BEARING))
            {
               location.setBearing(values.getAsFloat(LocationsColumns.BEARING));
            }

            locationId = this.mDbHelper.insertLocation(location);

            Log.d(TAG, "Have inserted a location basing on " + labelId + " with location " + locationId);
            insertedUri = ContentUris.withAppendedId(uri, locationId);
            break;

         /*
          * case XYZ: pathSegments = uri.getPathSegments(); labelId = Long.parseLong( pathSegments.get( 1 ) ); Long time_xyz = values.getAsLong( Locations.TIME ); Float speed_xyz = values.getAsFloat(
          * Locations.SPEED ); if( time_xyz == null ) { time_xyz = System.currentTimeMillis(); } if( speed_xyz == null ) { speed = 0f; } Log.d( TAG,
          * "Have inserted a location basing on "+labelId+" with location "+xyzId ); insertedUri = ContentUris.withAppendedId( uri, xyzId ); break;
          */

         case WAYPOINT_MEDIA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            waypointId = Long.parseLong(pathSegments.get(5));
            final String mediaUri = values.getAsString(MediaColumns.URI);
            mediaId = this.mDbHelper.insertMedia(trackId, segmentId, waypointId, mediaUri);
            insertedUri = ContentUris.withAppendedId(Media.CONTENT_URI, mediaId);
            break;
         case SEGMENTS:
            pathSegments = uri.getPathSegments();
            trackId = Integer.parseInt(pathSegments.get(1));
            segmentId = this.mDbHelper.toNextSegment(trackId);
            insertedUri = ContentUris.withAppendedId(uri, segmentId);
            break;
         case TRACKS:
            final String name = (values == null) ? "" : values.getAsString(TracksColumns.NAME);
            trackId = this.mDbHelper.toNextTrack(name);
            insertedUri = ContentUris.withAppendedId(uri, trackId);
            break;

         case LABELS:
            final String nLabel = (values == null) ? "" : values.getAsString(LabelsColumns.NAME);
            labelId = this.mDbHelper.toNextLabel(nLabel);
            insertedUri = ContentUris.withAppendedId(uri, labelId);
            break;

         case TRACK_METADATA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            key = values.getAsString(MetaDataColumns.KEY);
            value = values.getAsString(MetaDataColumns.VALUE);
            mediaId = this.mDbHelper.insertOrUpdateMetaData(trackId, -1L, -1L, key, value);
            insertedUri = ContentUris.withAppendedId(MetaData.CONTENT_URI, mediaId);
            break;
         case SEGMENT_METADATA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            key = values.getAsString(MetaDataColumns.KEY);
            value = values.getAsString(MetaDataColumns.VALUE);
            mediaId = this.mDbHelper.insertOrUpdateMetaData(trackId, segmentId, -1L, key, value);
            insertedUri = ContentUris.withAppendedId(MetaData.CONTENT_URI, mediaId);
            break;
         case WAYPOINT_METADATA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            waypointId = Long.parseLong(pathSegments.get(5));
            key = values.getAsString(MetaDataColumns.KEY);
            value = values.getAsString(MetaDataColumns.VALUE);
            mediaId = this.mDbHelper.insertOrUpdateMetaData(trackId, segmentId, waypointId, key, value);
            insertedUri = ContentUris.withAppendedId(MetaData.CONTENT_URI, mediaId);
            break;
         default:
            Log.e(PspotProvider.TAG, "Unable to match the insert URI: " + uri.toString());
            insertedUri = null;
            break;
      }
      return insertedUri;
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
    */
   @Override
   public Cursor query(final Uri uri, String[] projection, String selection, String[] selectionArgs, final String sortOrder)
   {
      //      Log.d( TAG, "Query on Uri:"+uri ); 

      final int match = PspotProvider.sURIMatcher.match(uri);

      String tableName = null;
      String innerSelection = "1";
      String[] innerSelectionArgs = new String[] {};
      String sortorder = sortOrder;
      final List<String> pathSegments = uri.getPathSegments();
      switch (match)
      {
         case TRACKS:
            tableName = Tracks.TABLE;
            break;

         case TRACK_ID:
            tableName = Tracks.TABLE;
            innerSelection = BaseColumns._ID + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;

         case LABELS:
            tableName = Labels.TABLE;
            break;

         case LABEL_ID:
            tableName = Labels.TABLE;
            innerSelection = BaseColumns._ID + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;

         case SEGMENTS:
            tableName = Segments.TABLE;
            innerSelection = SegmentsColumns.TRACK + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;

         case SEGMENT_ID:
            tableName = Segments.TABLE;
            innerSelection = SegmentsColumns.TRACK + " = ?  and " + BaseColumns._ID + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1), pathSegments.get(3) };
            break;
         case WAYPOINTS:
            tableName = Waypoints.TABLE;
            innerSelection = WaypointsColumns.SEGMENT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(3) };
            break;
         case WAYPOINT_ID:
            tableName = Waypoints.TABLE;
            innerSelection = WaypointsColumns.SEGMENT + " =  ?  and " + BaseColumns._ID + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(3), pathSegments.get(5) };
            break;

         case LOCATIONS:
            tableName = Locations.TABLE;
            innerSelection = LocationsColumns.SEGMENT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(3) };
            break;

         case LOCATION_ID:
            tableName = Locations.TABLE;
            innerSelection = LocationsColumns.SEGMENT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(3) };
            break;

         case TRACK_WAYPOINTS:
            tableName = Waypoints.TABLE + " INNER JOIN " + Segments.TABLE + " ON " + Segments.TABLE + "." + BaseColumns._ID + "==" + WaypointsColumns.SEGMENT;
            innerSelection = SegmentsColumns.TRACK + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;
         case PspotProvider.MEDIA:
            tableName = Media.TABLE;
            break;
         case PspotProvider.MEDIA_ID:
            tableName = Media.TABLE;
            innerSelection = BaseColumns._ID + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;
         case TRACK_MEDIA:
            tableName = Media.TABLE;
            innerSelection = MediaColumns.TRACK + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;
         case SEGMENT_MEDIA:
            tableName = Media.TABLE;
            innerSelection = MediaColumns.TRACK + " = ? and " + MediaColumns.SEGMENT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1), pathSegments.get(3) };
            break;
         case WAYPOINT_MEDIA:
            tableName = Media.TABLE;
            innerSelection = MediaColumns.TRACK + " = ?  and " + MediaColumns.SEGMENT + " = ? and " + MediaColumns.WAYPOINT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1), pathSegments.get(3), pathSegments.get(5) };
            break;
         case TRACK_METADATA:
            tableName = MetaData.TABLE;
            innerSelection = MetaDataColumns.TRACK + " = ? and " + MetaDataColumns.SEGMENT + " = ? and " + MetaDataColumns.WAYPOINT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1), "-1", "-1" };
            break;
         case SEGMENT_METADATA:
            tableName = MetaData.TABLE;
            innerSelection = MetaDataColumns.TRACK + " = ? and " + MetaDataColumns.SEGMENT + " = ? and " + MetaDataColumns.WAYPOINT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1), pathSegments.get(3), "-1" };
            break;
         case WAYPOINT_METADATA:
            tableName = MetaData.TABLE;
            innerSelection = MetaDataColumns.TRACK + " = ? and " + MetaDataColumns.SEGMENT + " = ? and " + MetaDataColumns.WAYPOINT + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1), pathSegments.get(3), pathSegments.get(5) };
            break;
         case PspotProvider.METADATA:
            tableName = MetaData.TABLE;
            break;
         case PspotProvider.METADATA_ID:
            tableName = MetaData.TABLE;
            innerSelection = BaseColumns._ID + " = ? ";
            innerSelectionArgs = new String[] { pathSegments.get(1) };
            break;
         case SEARCH_SUGGEST_ID:
            tableName = Tracks.TABLE;
            if (selectionArgs[0] == null || selectionArgs[0].equals(""))
            {
               selection = null;
               selectionArgs = null;
               sortorder = TracksColumns.CREATION_TIME + " desc";
            }
            else
            {
               selectionArgs[0] = "%" + selectionArgs[0] + "%";
            }
            projection = SUGGEST_PROJECTION;
            break;
         case LIVE_FOLDERS:
            tableName = Tracks.TABLE;
            projection = LIVE_PROJECTION;
            sortorder = TracksColumns.CREATION_TIME + " desc";
            break;
         default:
            Log.e(PspotProvider.TAG, "Unable to come to an action in the query uri: " + uri.toString());
            return null;
      }

      // SQLiteQueryBuilder is a helper class that creates the
      // proper SQL syntax for us.
      final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

      // Set the table we're querying.
      qBuilder.setTables(tableName);

      if (selection == null)
      {
         selection = innerSelection;
      }
      else
      {
         selection = "( " + innerSelection + " ) and " + selection;
      }
      final LinkedList<String> allArgs = new LinkedList<String>();
      if (selectionArgs == null)
      {
         allArgs.addAll(Arrays.asList(innerSelectionArgs));
      }
      else
      {
         allArgs.addAll(Arrays.asList(innerSelectionArgs));
         allArgs.addAll(Arrays.asList(selectionArgs));
      }
      selectionArgs = allArgs.toArray(innerSelectionArgs);

      // Make the query.
      final SQLiteDatabase mDb = this.mDbHelper.getWritableDatabase();
      final Cursor c = qBuilder.query(mDb, projection, selection, selectionArgs, null, null, sortorder);
      c.setNotificationUri(getContext().getContentResolver(), uri);
      return c;
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
    */
   @Override
   public int update(final Uri uri, final ContentValues givenValues, final String selection, final String[] selectionArgs)
   {
      int updates = -1;
      long trackId;
      long segmentId;
      long waypointId;
      long metaDataId;
      List<String> pathSegments;

      final int match = PspotProvider.sURIMatcher.match(uri);
      String value;
      switch (match)
      {
         case TRACK_ID:
            trackId = new Long(uri.getLastPathSegment()).longValue();
            final String name = givenValues.getAsString(TracksColumns.NAME);
            updates = this.mDbHelper.updateTrack(trackId, name);
            break;

         case LABEL_ID:
            trackId = new Long(uri.getLastPathSegment()).longValue();
            final String nLabel = givenValues.getAsString(LabelsColumns.NAME);
            updates = this.mDbHelper.updateTrack(trackId, nLabel);
            break;

         case TRACK_METADATA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            value = givenValues.getAsString(MetaDataColumns.VALUE);
            updates = this.mDbHelper.updateMetaData(trackId, -1L, -1L, -1L, selection, selectionArgs, value);
            break;
         case SEGMENT_METADATA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            value = givenValues.getAsString(MetaDataColumns.VALUE);
            updates = this.mDbHelper.updateMetaData(trackId, segmentId, -1L, -1L, selection, selectionArgs, value);
            break;
         case WAYPOINT_METADATA:
            pathSegments = uri.getPathSegments();
            trackId = Long.parseLong(pathSegments.get(1));
            segmentId = Long.parseLong(pathSegments.get(3));
            waypointId = Long.parseLong(pathSegments.get(5));
            value = givenValues.getAsString(MetaDataColumns.VALUE);
            updates = this.mDbHelper.updateMetaData(trackId, segmentId, waypointId, -1L, selection, selectionArgs, value);
            break;
         case METADATA_ID:
            pathSegments = uri.getPathSegments();
            metaDataId = Long.parseLong(pathSegments.get(1));
            value = givenValues.getAsString(MetaDataColumns.VALUE);
            updates = this.mDbHelper.updateMetaData(-1L, -1L, -1L, metaDataId, selection, selectionArgs, value);
            break;
         default:
            Log.e(PspotProvider.TAG, "Unable to come to an action in the query uri" + uri.toString());
            return -1;
      }

      return updates;
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
    */
   @Override
   public int delete(final Uri uri, final String selection, final String[] selectionArgs)
   {
      final int match = PspotProvider.sURIMatcher.match(uri);
      int affected = 0;
      switch (match)
      {
         case PspotProvider.TRACK_ID:
            affected = this.mDbHelper.deleteTrack(new Long(uri.getLastPathSegment()).longValue());
            break;
         case PspotProvider.MEDIA_ID:
            affected = this.mDbHelper.deleteMedia(new Long(uri.getLastPathSegment()).longValue());
            break;
         case PspotProvider.METADATA_ID:
            affected = this.mDbHelper.deleteMetaData(new Long(uri.getLastPathSegment()).longValue());
            break;
         default:
            affected = 0;
            break;
      }
      return affected;
   }

   @Override
   public int bulkInsert(final Uri uri, final ContentValues[] valuesArray)
   {
      int inserted = 0;
      final int match = PspotProvider.sURIMatcher.match(uri);
      switch (match)
      {
         case WAYPOINTS:
            final List<String> pathSegments = uri.getPathSegments();
            final int trackId = Integer.parseInt(pathSegments.get(1));
            final int segmentId = Integer.parseInt(pathSegments.get(3));
            inserted = this.mDbHelper.bulkInsertWaypoint(trackId, segmentId, valuesArray);
            break;

         case LOCATIONS:
            final List<String> pathSegmentsL = uri.getPathSegments();
            final int labelId = Integer.parseInt(pathSegmentsL.get(1));
            inserted = this.mDbHelper.bulkInsertLocations(labelId, valuesArray);
            break;

         default:
            inserted = super.bulkInsert(uri, valuesArray);
            break;
      }
      return inserted;
   }

}
