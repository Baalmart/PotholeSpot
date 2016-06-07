package dev.potholespot.android.actions.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.util.ProgressFilterInputStream;
import dev.potholespot.android.util.UnicodeReader;
import dev.potholespot.uganda.R;

public class GpxParser extends AsyncTask<Uri, Void, Uri>
{
   private static final String LATITUDE_ATRIBUTE = "lat";
   private static final String LONGITUDE_ATTRIBUTE = "lon";
   private static final String TRACK_ELEMENT = "trkpt";
   private static final String SEGMENT_ELEMENT = "trkseg";
   private static final String NAME_ELEMENT = "name";
   private static final String TIME_ELEMENT = "time";
   private static final String ELEVATION_ELEMENT = "ele";
   private static final String COURSE_ELEMENT = "course";
   private static final String ACCURACY_ELEMENT = "accuracy";
   private static final String SPEED_ELEMENT = "speed";
   public static final SimpleDateFormat ZULU_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   public static final SimpleDateFormat ZULU_DATE_FORMAT_MS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
   public static final SimpleDateFormat ZULU_DATE_FORMAT_BC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
   protected static final int DEFAULT_UNKNOWN_FILESIZE = 1024 * 1024 * 10;
   private static final String TAG = "PotholeSpot.GpxParser";
   static
   {
      final TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMAT.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
      ZULU_DATE_FORMAT_MS.setTimeZone(utc);

   }

   private final ContentResolver mContentResolver;
   protected String mErrorDialogMessage;
   protected Exception mErrorDialogException;
   protected Context mContext;
   private final ProgressListener mProgressListener;
   protected ProgressAdmin mProgressAdmin;

   public GpxParser(final Context context, final ProgressListener progressListener)
   {
      this.mContext = context;
      this.mProgressListener = progressListener;
      this.mContentResolver = this.mContext.getContentResolver();
   }

   public void executeOn(final Executor executor)
   {
      if (Build.VERSION.SDK_INT >= 11)
      {
         executeOnExecutor(executor);
      }
      else
      {
         execute();
      }
   }

   public void determineProgressGoal(final Uri importFileUri)
   {
      this.mProgressAdmin = new ProgressAdmin();
      this.mProgressAdmin.setContentLength(DEFAULT_UNKNOWN_FILESIZE);
      if (importFileUri != null && importFileUri.getScheme().equals("file"))
      {
         final File file = new File(importFileUri.getPath());
         this.mProgressAdmin.setContentLength(file.length());
      }
   }

   public Uri importUri(final Uri importFileUri)
   {
      Uri result = null;
      String trackName = null;
      InputStream fis = null;
      if (importFileUri.getScheme().equals("file"))
      {
         trackName = importFileUri.getLastPathSegment();
      }
      try
      {
         fis = this.mContentResolver.openInputStream(importFileUri);
      }
      catch (final IOException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_io));
      }

      result = importTrack(fis, trackName);

      return result;
   }

   /**
    * Read a stream containing GPX XML into the OGT content provider
    * 
    * @param fis opened stream the read from, will be closed after this call
    * @param trackName
    * @return
    */
   public Uri importTrack(final InputStream fis, final String trackName)
   {
      Uri trackUri = null;
      int eventType;
      ContentValues lastPosition = null;
      final Vector<ContentValues> bulk = new Vector<ContentValues>();
      boolean speed = false;
      boolean accuracy = false;
      boolean bearing = false;
      boolean elevation = false;
      boolean name = false;
      boolean time = false;
      final Long importDate = Long.valueOf(new Date().getTime());
      try
      {
         final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         final XmlPullParser xmlParser = factory.newPullParser();

         final ProgressFilterInputStream pfis = new ProgressFilterInputStream(fis, this.mProgressAdmin);
         final BufferedInputStream bis = new BufferedInputStream(pfis);
         final UnicodeReader ur = new UnicodeReader(bis, "UTF-8");
         xmlParser.setInput(ur);

         eventType = xmlParser.getEventType();

         String attributeName;

         Uri segmentUri = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               if (xmlParser.getName().equals(NAME_ELEMENT))
               {
                  name = true;
               }
               else
               {
                  final ContentValues trackContent = new ContentValues();
                  trackContent.put(TracksColumns.NAME, trackName);
                  if (xmlParser.getName().equals("trk") && trackUri == null)
                  {
                     trackUri = startTrack(trackContent);
                  }
                  else if (xmlParser.getName().equals(SEGMENT_ELEMENT))
                  {
                     segmentUri = startSegment(trackUri);
                  }
                  else if (xmlParser.getName().equals(TRACK_ELEMENT))
                  {
                     lastPosition = new ContentValues();
                     for (int i = 0; i < 2; i++)
                     {
                        attributeName = xmlParser.getAttributeName(i);
                        if (attributeName.equals(LATITUDE_ATRIBUTE))
                        {
                           lastPosition.put(WaypointsColumns.LATITUDE, Double.valueOf(xmlParser.getAttributeValue(i)));
                        }
                        else if (attributeName.equals(LONGITUDE_ATTRIBUTE))
                        {
                           lastPosition.put(WaypointsColumns.LONGITUDE, Double.valueOf(xmlParser.getAttributeValue(i)));
                        }
                     }
                  }
                  else if (xmlParser.getName().equals(SPEED_ELEMENT))
                  {
                     speed = true;
                  }
                  else if (xmlParser.getName().equals(ACCURACY_ELEMENT))
                  {
                     accuracy = true;
                  }
                  else if (xmlParser.getName().equals(COURSE_ELEMENT))
                  {
                     bearing = true;
                  }
                  else if (xmlParser.getName().equals(ELEVATION_ELEMENT))
                  {
                     elevation = true;
                  }
                  else if (xmlParser.getName().equals(TIME_ELEMENT))
                  {
                     time = true;
                  }
               }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if (xmlParser.getName().equals(NAME_ELEMENT))
               {
                  name = false;
               }
               else if (xmlParser.getName().equals(SPEED_ELEMENT))
               {
                  speed = false;
               }
               else if (xmlParser.getName().equals(ACCURACY_ELEMENT))
               {
                  accuracy = false;
               }
               else if (xmlParser.getName().equals(COURSE_ELEMENT))
               {
                  bearing = false;
               }
               else if (xmlParser.getName().equals(ELEVATION_ELEMENT))
               {
                  elevation = false;
               }
               else if (xmlParser.getName().equals(TIME_ELEMENT))
               {
                  time = false;
               }
               else if (xmlParser.getName().equals(SEGMENT_ELEMENT))
               {
                  if (segmentUri == null)
                  {
                     segmentUri = startSegment(trackUri);
                  }
                  this.mContentResolver.bulkInsert(Uri.withAppendedPath(segmentUri, "waypoints"), bulk.toArray(new ContentValues[bulk.size()]));
                  bulk.clear();
               }
               else if (xmlParser.getName().equals(TRACK_ELEMENT))
               {
                  if (!lastPosition.containsKey(WaypointsColumns.TIME))
                  {
                     lastPosition.put(WaypointsColumns.TIME, importDate);
                  }
                  if (!lastPosition.containsKey(WaypointsColumns.SPEED))
                  {
                     lastPosition.put(WaypointsColumns.SPEED, 0);
                  }
                  bulk.add(lastPosition);
                  lastPosition = null;
               }
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               final String text = xmlParser.getText();
               if (name)
               {
                  final ContentValues nameValues = new ContentValues();
                  nameValues.put(TracksColumns.NAME, text);
                  if (trackUri == null)
                  {
                     trackUri = startTrack(new ContentValues());
                  }
                  this.mContentResolver.update(trackUri, nameValues, null, null);
               }
               else if (lastPosition != null && speed)
               {
                  lastPosition.put(WaypointsColumns.SPEED, Double.parseDouble(text));
               }
               else if (lastPosition != null && accuracy)
               {
                  lastPosition.put(WaypointsColumns.ACCURACY, Double.parseDouble(text));
               }
               else if (lastPosition != null && bearing)
               {
                  lastPosition.put(WaypointsColumns.BEARING, Double.parseDouble(text));
               }
               else if (lastPosition != null && elevation)
               {
                  lastPosition.put(WaypointsColumns.ALTITUDE, Double.parseDouble(text));
               }
               else if (lastPosition != null && time)
               {
                  lastPosition.put(WaypointsColumns.TIME, parseXmlDateTime(text));
               }
            }
            eventType = xmlParser.next();
         }
      }
      catch (final XmlPullParserException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_xml));
      }
      catch (final IOException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_io));
      }
      finally
      {
         try
         {
            fis.close();
         }
         catch (final IOException e)
         {
            Log.w(TAG, "Failed closing inputstream");
         }
      }
      return trackUri;
   }

   private Uri startSegment(Uri trackUri)
   {
      if (trackUri == null)
      {
         trackUri = startTrack(new ContentValues());
      }
      return this.mContentResolver.insert(Uri.withAppendedPath(trackUri, "segments"), new ContentValues());
   }

   private Uri startTrack(final ContentValues trackContent)
   {
      return this.mContentResolver.insert(Tracks.CONTENT_URI, trackContent);
   }

   public static Long parseXmlDateTime(final String text)
   {
      Long dateTime = 0L;
      try
      {
         if (text == null)
         {
            throw new ParseException("Unable to parse dateTime " + text + " of length ", 0);
         }
         final int length = text.length();
         switch (length)
         {
            case 20:
               synchronized (ZULU_DATE_FORMAT)
               {
                  dateTime = Long.valueOf(ZULU_DATE_FORMAT.parse(text).getTime());
               }
               break;
            case 23:
               synchronized (ZULU_DATE_FORMAT_BC)
               {
                  dateTime = Long.valueOf(ZULU_DATE_FORMAT_BC.parse(text).getTime());
               }
               break;
            case 24:
               synchronized (ZULU_DATE_FORMAT_MS)
               {
                  dateTime = Long.valueOf(ZULU_DATE_FORMAT_MS.parse(text).getTime());
               }
               break;
            default:
               throw new ParseException("Unable to parse dateTime " + text + " of length " + length, 0);
         }
      }
      catch (final ParseException e)
      {
         Log.w(TAG, "Failed to parse a time-date", e);
      }
      return dateTime;
   }

   /**
    * @param e
    * @param text
    */
   protected void handleError(final Exception dialogException, final String dialogErrorMessage)
   {
      Log.e(TAG, "Unable to save ", dialogException);
      this.mErrorDialogException = dialogException;
      this.mErrorDialogMessage = dialogErrorMessage;
      cancel(false);
      throw new CancellationException(dialogErrorMessage);
   }

   @Override
   protected void onPreExecute()
   {
      this.mProgressListener.started();
   }

   @Override
   protected Uri doInBackground(final Uri... params)
   {
      final Uri importUri = params[0];
      determineProgressGoal(importUri);
      final Uri result = importUri(importUri);
      return result;
   }

   @Override
   protected void onProgressUpdate(final Void... values)
   {
      this.mProgressListener.setProgress(this.mProgressAdmin.getProgress());
   }

   @Override
   protected void onPostExecute(final Uri result)
   {
      this.mProgressListener.finished(result);
   }

   @Override
   protected void onCancelled()
   {
      this.mProgressListener.showError(this.mContext.getString(R.string.taskerror_gpx_import), this.mErrorDialogMessage, this.mErrorDialogException);
   }

   public class ProgressAdmin
   {
      private long progressedBytes;
      private long contentLength;
      private int progress;
      private long lastUpdate;

      /**
       * Get the progress.
       * 
       * @return Returns the progress as a int.
       */
      public int getProgress()
      {
         return this.progress;
      }

      public void addBytesProgress(final int addedBytes)
      {
         this.progressedBytes += addedBytes;
         this.progress = (int) (Window.PROGRESS_END * this.progressedBytes / this.contentLength);
         considerPublishProgress();
      }

      public void setContentLength(final long contentLength)
      {
         this.contentLength = contentLength;
      }

      public void considerPublishProgress()
      {
         final long now = new Date().getTime();
         if (now - this.lastUpdate > 1000)
         {
            this.lastUpdate = now;
            publishProgress();
         }
      }
   }
};