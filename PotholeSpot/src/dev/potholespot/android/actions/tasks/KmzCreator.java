package dev.potholespot.android.actions.tasks;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.Xml;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.db.Pspot;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

/**
 * Create a KMZ version of a stored track
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class KmzCreator extends XmlCreator
{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_KML_22 = "http://www.opengis.net/kml/2.2";
   public static final SimpleDateFormat ZULU_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   static
   {
      final TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMATER.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }

   private final String TAG = "OGT.KmzCreator";

   public KmzCreator(final Context context, final Uri trackUri, final String chosenFileName, final ProgressListener listener)
   {
      super(context, trackUri, chosenFileName, listener);
   }

   @Override
   protected Uri doInBackground(final Void... params)
   {
      determineProgressGoal();

      final Uri resultFilename = exportKml();
      return resultFilename;
   }

   private Uri exportKml()
   {
      if (this.mFileName.endsWith(".kmz") || this.mFileName.endsWith(".zip"))
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(this.mContext) + this.mFileName.substring(0, this.mFileName.length() - 4));
      }
      else
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(this.mContext) + this.mFileName);
      }

      new File(getExportDirectoryPath()).mkdirs();
      final String xmlFilePath = getExportDirectoryPath() + "/doc.kml";

      String resultFilename = null;
      FileOutputStream fos = null;
      BufferedOutputStream buf = null;
      try
      {
         verifySdCardAvailibility();

         final XmlSerializer serializer = Xml.newSerializer();
         final File xmlFile = new File(xmlFilePath);
         fos = new FileOutputStream(xmlFile);
         buf = new BufferedOutputStream(fos, 8192);
         serializer.setOutput(buf, "UTF-8");

         serializeTrack(this.mTrackUri, this.mFileName, serializer);
         buf.close();
         buf = null;
         fos.close();
         fos = null;

         resultFilename = bundlingMediaAndXml(xmlFile.getParentFile().getName(), ".kmz");
         this.mFileName = new File(resultFilename).getName();
      }
      catch (final IllegalArgumentException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_filename);
         handleError(this.mContext.getString(R.string.taskerror_kmz_write), e, text);
      }
      catch (final IllegalStateException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_buildxml);
         handleError(this.mContext.getString(R.string.taskerror_kmz_write), e, text);
      }
      catch (final IOException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_writesdcard);
         handleError(this.mContext.getString(R.string.taskerror_kmz_write), e, text);
      }
      finally
      {
         if (buf != null)
         {
            try
            {
               buf.close();
            }
            catch (final IOException e)
            {
               Log.e(this.TAG, "Failed to close buf after completion, ignoring.", e);
            }
         }
         if (fos != null)
         {
            try
            {
               fos.close();
            }
            catch (final IOException e)
            {
               Log.e(this.TAG, "Failed to close fos after completion, ignoring.", e);
            }
         }
      }
      return Uri.fromFile(new File(resultFilename));
   }

   private void serializeTrack(final Uri trackUri, final String trackName, final XmlSerializer serializer) throws IOException
   {
      serializer.startDocument("UTF-8", true);
      serializer.setPrefix("xsi", NS_SCHEMA);
      serializer.setPrefix("kml", NS_KML_22);
      serializer.startTag("", "kml");
      serializer.attribute(NS_SCHEMA, "schemaLocation", NS_KML_22 + " http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd");
      serializer.attribute(null, "xmlns", NS_KML_22);

      serializer.text("\n");
      serializer.startTag("", "Document");
      serializer.text("\n");
      quickTag(serializer, "", "name", trackName);

      /* from <name/> upto <Folder/> */
      serializeTrackHeader(serializer, trackUri);

      serializer.text("\n");
      serializer.endTag("", "Document");

      serializer.endTag("", "kml");
      serializer.endDocument();
   }

   private String serializeTrackHeader(final XmlSerializer serializer, final Uri trackUri) throws IOException
   {
      final ContentResolver resolver = this.mContext.getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query(trackUri, new String[] { TracksColumns.NAME }, null, null, null);
         if (trackCursor.moveToFirst())
         {
            serializer.text("\n");
            serializer.startTag("", "Style");
            serializer.attribute(null, "id", "lineStyle");
            serializer.startTag("", "LineStyle");
            serializer.text("\n");
            serializer.startTag("", "color");
            serializer.text("99ffac59");
            serializer.endTag("", "color");
            serializer.text("\n");
            serializer.startTag("", "width");
            serializer.text("6");
            serializer.endTag("", "width");
            serializer.text("\n");
            serializer.endTag("", "LineStyle");
            serializer.text("\n");
            serializer.endTag("", "Style");
            serializer.text("\n");
            serializer.startTag("", "Folder");
            name = trackCursor.getString(0);
            serializer.text("\n");
            quickTag(serializer, "", "name", name);
            serializer.text("\n");
            serializer.startTag("", "open");
            serializer.text("1");
            serializer.endTag("", "open");
            serializer.text("\n");

            serializeSegments(serializer, Uri.withAppendedPath(trackUri, "segments"));

            serializer.text("\n");
            serializer.endTag("", "Folder");
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
      return name;
   }

   /**
    * <pre>
    * &lt;Folder>
    *    &lt;Placemark>
    *      serializeSegmentToTimespan()
    *      &lt;LineString>
    *         serializeWaypoints()
    *      &lt;/LineString>
    *    &lt;/Placemark>
    *    &lt;Placemark/>
    *    &lt;Placemark/>
    * &lt;/Folder>
    * </pre>
    * 
    * @param serializer
    * @param segments
    * @throws IOException
    */
   private void serializeSegments(final XmlSerializer serializer, final Uri segments) throws IOException
   {
      Cursor segmentCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      try
      {
         segmentCursor = resolver.query(segments, new String[] { BaseColumns._ID }, null, null, null);
         if (segmentCursor.moveToFirst())
         {
            do
            {
               final Uri waypoints = Uri.withAppendedPath(segments, segmentCursor.getLong(0) + "/waypoints");
               serializer.text("\n");
               serializer.startTag("", "Folder");
               serializer.text("\n");
               serializer.startTag("", "name");
               serializer.text(String.format("Segment %d", 1 + segmentCursor.getPosition()));
               serializer.endTag("", "name");
               serializer.text("\n");
               serializer.startTag("", "open");
               serializer.text("1");
               serializer.endTag("", "open");

               /* Single <TimeSpan/> element */
               serializeSegmentToTimespan(serializer, waypoints);

               serializer.text("\n");
               serializer.startTag("", "Placemark");
               serializer.text("\n");
               serializer.startTag("", "name");
               serializer.text("Path");
               serializer.endTag("", "name");
               serializer.text("\n");
               serializer.startTag("", "styleUrl");
               serializer.text("#lineStyle");
               serializer.endTag("", "styleUrl");
               serializer.text("\n");
               serializer.startTag("", "LineString");
               serializer.text("\n");
               serializer.startTag("", "tessellate");
               serializer.text("0");
               serializer.endTag("", "tessellate");
               serializer.text("\n");
               serializer.startTag("", "altitudeMode");
               serializer.text("clampToGround");
               serializer.endTag("", "altitudeMode");

               /* Single <coordinates/> element */
               serializeWaypoints(serializer, waypoints);

               serializer.text("\n");
               serializer.endTag("", "LineString");
               serializer.text("\n");
               serializer.endTag("", "Placemark");

               serializeWaypointDescription(serializer, Uri.withAppendedPath(segments, "/" + segmentCursor.getLong(0) + "/media"));

               serializer.text("\n");
               serializer.endTag("", "Folder");

            }
            while (segmentCursor.moveToNext());

         }
      }
      finally
      {
         if (segmentCursor != null)
         {
            segmentCursor.close();
         }
      }
   }

   /**
    * &lt;TimeSpan>&lt;begin>...&lt;/begin>&lt;end>...&lt;/end>&lt;/TimeSpan>
    * 
    * @param serializer
    * @param waypoints
    * @throws IOException
    */
   private void serializeSegmentToTimespan(final XmlSerializer serializer, final Uri waypoints) throws IOException
   {
      Cursor waypointsCursor = null;
      Date segmentStartTime = null;
      Date segmentEndTime = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(waypoints, new String[] { WaypointsColumns.TIME }, null, null, null);

         if (waypointsCursor.moveToFirst())
         {
            segmentStartTime = new Date(waypointsCursor.getLong(0));
            if (waypointsCursor.moveToLast())
            {
               segmentEndTime = new Date(waypointsCursor.getLong(0));

               serializer.text("\n");
               serializer.startTag("", "TimeSpan");
               serializer.text("\n");
               serializer.startTag("", "begin");
               synchronized (ZULU_DATE_FORMATER)
               {
                  serializer.text(ZULU_DATE_FORMATER.format(segmentStartTime));
                  serializer.endTag("", "begin");
                  serializer.text("\n");
                  serializer.startTag("", "end");
                  serializer.text(ZULU_DATE_FORMATER.format(segmentEndTime));
               }
               serializer.endTag("", "end");
               serializer.text("\n");
               serializer.endTag("", "TimeSpan");
            }
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }
   }

   /**
    * &lt;coordinates>...&lt;/coordinates>
    * 
    * @param serializer
    * @param waypoints
    * @throws IOException
    */
   private void serializeWaypoints(final XmlSerializer serializer, final Uri waypoints) throws IOException
   {
      Cursor waypointsCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(waypoints, new String[] { WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE, WaypointsColumns.ALTITUDE }, null, null, null);
         if (waypointsCursor.moveToFirst())
         {
            serializer.text("\n");
            serializer.startTag("", "coordinates");
            do
            {
               this.mProgressAdmin.addWaypointProgress(1);
               // Single Coordinate tuple
               serializeCoordinates(serializer, waypointsCursor);
               serializer.text(" ");
            }
            while (waypointsCursor.moveToNext());

            serializer.endTag("", "coordinates");
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }

   }

   /**
    * lon,lat,alt tuple without trailing spaces
    * 
    * @param serializer
    * @param waypointsCursor
    * @throws IOException
    */
   private void serializeCoordinates(final XmlSerializer serializer, final Cursor waypointsCursor) throws IOException
   {
      serializer.text(Double.toString(waypointsCursor.getDouble(0)));
      serializer.text(",");
      serializer.text(Double.toString(waypointsCursor.getDouble(1)));
      serializer.text(",");
      serializer.text(Double.toString(waypointsCursor.getDouble(2)));
   }

   private void serializeWaypointDescription(final XmlSerializer serializer, final Uri media) throws IOException
   {
      final String mediaPathPrefix = Constants.getSdCardDirectory(this.mContext);
      Cursor mediaCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      BufferedReader buf = null;
      try
      {
         mediaCursor = resolver.query(media, new String[] { dev.potholespot.android.db.Pspot.MediaColumns.URI, dev.potholespot.android.db.Pspot.MediaColumns.TRACK,
               dev.potholespot.android.db.Pspot.MediaColumns.SEGMENT, dev.potholespot.android.db.Pspot.MediaColumns.WAYPOINT }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               final Uri mediaUri = Uri.parse(mediaCursor.getString(0));
               final Uri singleWaypointUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mediaCursor.getLong(1) + "/segments/" + mediaCursor.getLong(2) + "/waypoints/" + mediaCursor.getLong(3));
               final String lastPathSegment = mediaUri.getLastPathSegment();
               if (mediaUri.getScheme().equals("file"))
               {
                  if (lastPathSegment.endsWith("3gp"))
                  {
                     final String includedMediaFile = includeMediaFile(lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "description");
                     final String kmlAudioUnsupported = this.mContext.getString(R.string.kmlVideoUnsupported);
                     serializer.text(String.format(kmlAudioUnsupported, includedMediaFile));
                     serializer.endTag("", "description");
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
                  else if (lastPathSegment.endsWith("jpg"))
                  {
                     final String includedMediaFile = includeMediaFile(mediaPathPrefix + lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializer.text("\n");
                     quickTag(serializer, "", "description", "<img src=\"" + includedMediaFile + "\" width=\"500px\"/><br/>" + lastPathSegment);
                     serializer.text("\n");
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
                  else if (lastPathSegment.endsWith("txt"))
                  {
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "description");
                     if (buf != null)
                     {
                        buf.close();
                     }
                     buf = new BufferedReader(new FileReader(mediaUri.getEncodedPath()));
                     String line;
                     while ((line = buf.readLine()) != null)
                     {
                        serializer.text(line);
                        serializer.text("\n");
                     }
                     serializer.endTag("", "description");
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
               }
               else if (mediaUri.getScheme().equals("content"))
               {
                  if (mediaUri.getAuthority().equals(Pspot.AUTHORITY + ".string"))
                  {
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
                  else if (mediaUri.getAuthority().equals("media"))
                  {
                     Cursor mediaItemCursor = null;
                     try
                     {
                        mediaItemCursor = resolver.query(mediaUri, new String[] { MediaColumns.DATA, MediaColumns.DISPLAY_NAME }, null, null, null);
                        if (mediaItemCursor.moveToFirst())
                        {
                           final String includedMediaFile = includeMediaFile(mediaItemCursor.getString(0));
                           serializer.text("\n");
                           serializer.startTag("", "Placemark");
                           serializer.text("\n");
                           quickTag(serializer, "", "name", mediaItemCursor.getString(1));
                           serializer.text("\n");
                           serializer.startTag("", "description");
                           final String kmlAudioUnsupported = this.mContext.getString(R.string.kmlAudioUnsupported);
                           serializer.text(String.format(kmlAudioUnsupported, includedMediaFile));
                           serializer.endTag("", "description");
                           serializeMediaPoint(serializer, singleWaypointUri);
                           serializer.text("\n");
                           serializer.endTag("", "Placemark");
                        }
                     }
                     finally
                     {
                        if (mediaItemCursor != null)
                        {
                           mediaItemCursor.close();
                        }
                     }
                  }
               }
            }
            while (mediaCursor.moveToNext());
         }
      }
      finally
      {
         if (mediaCursor != null)
         {
            mediaCursor.close();
         }
         if (buf != null)
         {
            buf.close();
         }
      }
   }

   /**
    * &lt;Point>...&lt;/Point> &lt;shape>rectangle&lt;/shape>
    * 
    * @param serializer
    * @param singleWaypointUri
    * @throws IllegalArgumentException
    * @throws IllegalStateException
    * @throws IOException
    */
   private void serializeMediaPoint(final XmlSerializer serializer, final Uri singleWaypointUri) throws IllegalArgumentException, IllegalStateException, IOException
   {
      Cursor waypointsCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(singleWaypointUri, new String[] { WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE, WaypointsColumns.ALTITUDE }, null, null, null);
         if (waypointsCursor.moveToFirst())
         {
            serializer.text("\n");
            serializer.startTag("", "Point");
            serializer.text("\n");
            serializer.startTag("", "coordinates");
            serializeCoordinates(serializer, waypointsCursor);
            serializer.endTag("", "coordinates");
            serializer.text("\n");
            serializer.endTag("", "Point");
            serializer.text("\n");
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }
   }

   @Override
   protected String getContentType()
   {
      return "application/vnd.google-earth.kmz";
   }

   @Override
   public boolean needsBundling()
   {
      return true;
   }

}