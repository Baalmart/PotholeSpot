package dev.potholespot.android.actions.tasks;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import dev.potholespot.android.db.Pspot.LabelsColumns;
import dev.potholespot.android.db.Pspot.Waypoints;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

public class GpxCreator extends XmlCreator

{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
   public static final String NS_GPX_10 = "http://www.topografix.com/GPX/1/0";
   public static final String NS_OGT_10 = "http://www.ugandasoft.ug";
   public static final SimpleDateFormat ZULU_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   static
   {
      final TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMATER.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }

   private final String TAG = "OGT.GpxCreator";
   private final boolean includeAttachments;
   protected String mName;

   public GpxCreator(final Context context, final Uri trackUri, final String chosenBaseFileName, final boolean attachments, final ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, listener);
      this.includeAttachments = attachments;
   }

   @Override
   protected Uri doInBackground(final Void... params)
   {
      determineProgressGoal();

      final Uri resultFilename = exportGpx();
      return resultFilename;
   }

   protected Uri exportGpx()
   {

      String xmlFilePath;
      if (this.mFileName.endsWith(".gpx") || this.mFileName.endsWith(".xml"))
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(this.mContext) + this.mFileName.substring(0, this.mFileName.length() - 4));

         xmlFilePath = getExportDirectoryPath() + "/" + this.mFileName;
      }
      else
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(this.mContext) + this.mFileName);
         xmlFilePath = getExportDirectoryPath() + "/" + this.mFileName + ".gpx";
      }

      new File(getExportDirectoryPath()).mkdirs();

      String resultFilename = null;
      FileOutputStream fos = null;
      BufferedOutputStream buf = null;
      try
      {
         verifySdCardAvailibility();

         final XmlSerializer serializer = Xml.newSerializer();
         final File xmlFile = new File(xmlFilePath);
         fos = new FileOutputStream(xmlFile);
         buf = new BufferedOutputStream(fos, 8 * 8192);
         serializer.setOutput(buf, "UTF-8");

         serializeTrack(this.mTrackUri, serializer);
         buf.close();
         buf = null;
         fos.close();
         fos = null;

         if (needsBundling())
         {
            resultFilename = bundlingMediaAndXml(xmlFile.getParentFile().getName(), ".zip");
         }
         else
         {
            final File finalFile = new File(Constants.getSdCardDirectory(this.mContext) + xmlFile.getName());
            xmlFile.renameTo(finalFile);
            resultFilename = finalFile.getAbsolutePath();

            XmlCreator.deleteRecursive(xmlFile.getParentFile());
         }

         this.mFileName = new File(resultFilename).getName();
      }
      catch (final FileNotFoundException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_filenotfound);
         handleError(this.mContext.getString(R.string.taskerror_gpx_write), e, text);
      }
      catch (final IllegalArgumentException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_filename);
         handleError(this.mContext.getString(R.string.taskerror_gpx_write), e, text);
      }

      catch (final IllegalStateException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_buildxml);
         handleError(this.mContext.getString(R.string.taskerror_gpx_write), e, text);
      }
      catch (final IOException e)
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + this.mContext.getString(R.string.error_writesdcard);
         handleError(this.mContext.getString(R.string.taskerror_gpx_write), e, text);
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

   private void serializeTrack(final Uri trackUri, final XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      serializer.startDocument("UTF-8", true);

      serializer.setPrefix("xsi", NS_SCHEMA);
      serializer.setPrefix("gpx10", NS_GPX_10);
      serializer.setPrefix("ogt10", NS_OGT_10);
      serializer.text("\n");
      serializer.startTag("", "prim");
      serializer.attribute(null, "version", "1.1");
      serializer.attribute(null, "creator", "ugasoft");
      serializer.attribute(NS_SCHEMA, "schemaLocation", NS_GPX_11 + " http://www.topografix.com/gpx/1/1/gpx.xsd");
      serializer.attribute(null, "xmlns", NS_GPX_11);

      // <metadata/> Big header of the track
      serializeTrackHeader(this.mContext, serializer, trackUri);

      // <wpt/> [0...] Waypoints 
      if (this.includeAttachments)
      {
         serializeWaypoints(this.mContext, serializer, Uri.withAppendedPath(trackUri, "/media"));
      }

      // <trk/> [0...] Label
      /*
       * serializer.text("\n"); serializer.startTag("", "label"); serializer.text("\n");
       */

      serializer.startTag("", "label");
      serializer.text(this.mName);
      /* serializer.endTag("", "name"); */

      // The list of segments in the track
      serializeSegments(serializer, Uri.withAppendedPath(trackUri, "segments"));

      /*
       * serializeTrackPoints(serializer, Uri.withAppendedPath(trackUri, "waypoints")); serializer.text("\n");
       */

      serializer.endTag("", "label");
      /* serializer.endTag("", "label"); */

      serializer.text("\n");
      serializer.endTag("", "prim");
      serializer.endDocument();
   }

   private void serializeTrackHeader(final Context context, final XmlSerializer serializer, final Uri trackUri) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      final ContentResolver resolver = context.getContentResolver();
      Cursor labelCursor = null;

      String databaseName = null;
      try
      {
         /* trackCursor = resolver.query(trackUri, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, null); */
         labelCursor = resolver.query(trackUri, new String[] { BaseColumns._ID, LabelsColumns.NAME, LabelsColumns.CREATION_TIME }, null, null, null);
         if (labelCursor.moveToFirst())
         {
            databaseName = labelCursor.getString(1);
            serializer.text("\n");
            serializer.startTag("", "metadata");
            serializer.text("\n");
            serializer.startTag("", "time");
            final Date time = new Date(labelCursor.getLong(2));
            synchronized (ZULU_DATE_FORMATER)
            {
               serializer.text(ZULU_DATE_FORMATER.format(time));
            }
            serializer.endTag("", "time");
            serializer.text("\n");
            serializer.endTag("", "metadata");
         }
      }
      finally
      {
         if (labelCursor != null)
         {
            labelCursor.close();
         }
      }
      if (this.mName == null)
      {
         this.mName = "Untitled";
      }
      if (databaseName != null && !databaseName.equals(""))
      {
         this.mName = databaseName;
      }
      if (this.mChosenName != null && !this.mChosenName.equals(""))
      {
         this.mName = this.mChosenName;
      }
   }

   private void serializeAccelerationValues(final XmlSerializer serializer, final Uri accelerationValues) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      Cursor xyzCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      try
      {
         xyzCursor = resolver.query(accelerationValues, new String[] { BaseColumns._ID }, null, null, null);
         if (xyzCursor.moveToFirst())
         {
            do
            {
               final Uri acc_points = Uri.withAppendedPath(accelerationValues, xyzCursor.getLong(0) + "/locations");
               serializer.text("\n");
               serializer.startTag("", "acc");
               // serializeAccelerationPoints(serializer, acc_points);
               serializer.text("\n");
               serializer.endTag("", "acc");
            }
            while (xyzCursor.moveToNext());
         }
      }

      finally
      {
         if (xyzCursor != null)
         {
            xyzCursor.close();
         }
      }
   }

   private void serializeSegments(final XmlSerializer serializer, final Uri segments) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
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
               serializer.startTag("", "location");
               serializeTrackPoints(serializer, waypoints);
               serializer.text("\n");
               serializer.endTag("", "location");
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

   private void serializeTrackPoints(final XmlSerializer serializer, final Uri waypoints) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      Cursor waypointsCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(waypoints, new String[] { WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE, WaypointsColumns.TIME, WaypointsColumns.ALTITUDE, BaseColumns._ID,
               WaypointsColumns.SPEED, WaypointsColumns.ACCURACY, WaypointsColumns.BEARING }, null, null, null);
         if (waypointsCursor.moveToFirst())
         {
            do
            {
               this.mProgressAdmin.addWaypointProgress(1);

               serializer.text("\n");
               serializer.startTag("", "pt");
               serializer.attribute(null, "lat", Double.toString(waypointsCursor.getDouble(1)));
               serializer.attribute(null, "lon", Double.toString(waypointsCursor.getDouble(0)));
               /*
                * serializer.text("\n"); serializer.startTag("", "ele"); serializer.text(Double.toString(waypointsCursor.getDouble(3))); serializer.endTag("", "ele"); serializer.text("\n");
                */

               serializer.text("\n");
               serializer.startTag("", "x");
               serializer.text(Double.toString(waypointsCursor.getDouble(3)));
               serializer.endTag("", "x");
               serializer.text("\n");

               serializer.text("\n");
               serializer.startTag("", "y");
               serializer.text(Double.toString(waypointsCursor.getDouble(3)));
               serializer.endTag("", "y");
               serializer.text("\n");

               serializer.text("\n");
               serializer.startTag("", "z");
               serializer.text(Double.toString(waypointsCursor.getDouble(3)));
               serializer.endTag("", "z");
               serializer.text("\n");

               serializer.startTag("", "time");
               final Date time = new Date(waypointsCursor.getLong(2));
               synchronized (ZULU_DATE_FORMATER)
               {
                  serializer.text(ZULU_DATE_FORMATER.format(time));
               }
               serializer.endTag("", "time");
               serializer.text("\n");
               serializer.startTag("", "extensions");

               final double speed = waypointsCursor.getDouble(5);
               final double accuracy = waypointsCursor.getDouble(6);
               final double bearing = waypointsCursor.getDouble(7);
               if (speed > 0.0)
               {
                  quickTag(serializer, NS_GPX_10, "speed", Double.toString(speed));
               }
               if (accuracy > 0.0)
               {
                  quickTag(serializer, NS_OGT_10, "accuracy", Double.toString(accuracy));
               }
               /*
                * if (bearing != 0.0) { quickTag(serializer, NS_GPX_10, "course", Double.toString(bearing)); }
                */
               serializer.endTag("", "extensions");
               serializer.text("\n");
               serializer.endTag("", "pt");
            }
            while (waypointsCursor.moveToNext());
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

   private void serializeWaypoints(final Context context, final XmlSerializer serializer, final Uri media) throws IOException
   {
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      Cursor mediaCursor = null;
      Cursor waypointCursor = null;
      BufferedReader buf = null;
      final ContentResolver resolver = context.getContentResolver();
      try
      {
         mediaCursor = resolver.query(media, new String[] { dev.potholespot.android.db.Pspot.MediaColumns.URI, dev.potholespot.android.db.Pspot.MediaColumns.TRACK,
               dev.potholespot.android.db.Pspot.MediaColumns.SEGMENT, dev.potholespot.android.db.Pspot.MediaColumns.WAYPOINT }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               final Uri waypointUri = Waypoints.buildUri(mediaCursor.getLong(1), mediaCursor.getLong(2), mediaCursor.getLong(3));
               waypointCursor = resolver.query(waypointUri, new String[] { WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE, WaypointsColumns.ALTITUDE, WaypointsColumns.TIME }, null, null, null);
               serializer.text("\n");
               serializer.startTag("", "wpt");
               if (waypointCursor != null && waypointCursor.moveToFirst())
               {
                  serializer.attribute(null, "lat", Double.toString(waypointCursor.getDouble(0)));
                  serializer.attribute(null, "lon", Double.toString(waypointCursor.getDouble(1)));
                  serializer.text("\n");
                  serializer.startTag("", "ele");
                  serializer.text(Double.toString(waypointCursor.getDouble(2)));
                  serializer.endTag("", "ele");
                  serializer.text("\n");
                  serializer.startTag("", "time");
                  final Date time = new Date(waypointCursor.getLong(3));
                  synchronized (ZULU_DATE_FORMATER)
                  {
                     serializer.text(ZULU_DATE_FORMATER.format(time));
                  }
                  serializer.endTag("", "time");
               }
               if (waypointCursor != null)
               {
                  waypointCursor.close();
                  waypointCursor = null;
               }

               final Uri mediaUri = Uri.parse(mediaCursor.getString(0));
               if (mediaUri.getScheme().equals("file"))
               {
                  if (mediaUri.getLastPathSegment().endsWith("3gp"))
                  {
                     final String fileName = includeMediaFile(mediaUri.getLastPathSegment());
                     quickTag(serializer, "", "name", fileName);
                     serializer.startTag("", "link");
                     serializer.attribute(null, "href", fileName);
                     quickTag(serializer, "", "text", fileName);
                     serializer.endTag("", "link");
                  }
                  else if (mediaUri.getLastPathSegment().endsWith("jpg"))
                  {
                     final String mediaPathPrefix = Constants.getSdCardDirectory(this.mContext);
                     final String fileName = includeMediaFile(mediaPathPrefix + mediaUri.getLastPathSegment());
                     quickTag(serializer, "", "name", fileName);
                     serializer.startTag("", "link");
                     serializer.attribute(null, "href", fileName);
                     quickTag(serializer, "", "text", fileName);
                     serializer.endTag("", "link");
                  }
                  else if (mediaUri.getLastPathSegment().endsWith("txt"))
                  {
                     quickTag(serializer, "", "name", mediaUri.getLastPathSegment());
                     serializer.startTag("", "desc");
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
                     serializer.endTag("", "desc");
                  }
               }
               else if (mediaUri.getScheme().equals("content"))
               {
                  if ((Pspot.AUTHORITY + ".string").equals(mediaUri.getAuthority()))
                  {
                     quickTag(serializer, "", "name", mediaUri.getLastPathSegment());
                  }
                  else if (mediaUri.getAuthority().equals("media"))
                  {

                     Cursor mediaItemCursor = null;
                     try
                     {
                        mediaItemCursor = resolver.query(mediaUri, new String[] { MediaColumns.DATA, MediaColumns.DISPLAY_NAME }, null, null, null);
                        if (mediaItemCursor.moveToFirst())
                        {
                           final String fileName = includeMediaFile(mediaItemCursor.getString(0));
                           quickTag(serializer, "", "name", fileName);
                           serializer.startTag("", "link");
                           serializer.attribute(null, "href", fileName);
                           quickTag(serializer, "", "text", mediaItemCursor.getString(1));
                           serializer.endTag("", "link");
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
               serializer.text("\n");
               serializer.endTag("", "wpt");
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
         if (waypointCursor != null)
         {
            waypointCursor.close();
         }
         if (buf != null)
         {
            buf.close();
         }
      }
   }

   @Override
   protected String getContentType()
   {
      return needsBundling() ? "application/zip" : "text/xml";
   }
}