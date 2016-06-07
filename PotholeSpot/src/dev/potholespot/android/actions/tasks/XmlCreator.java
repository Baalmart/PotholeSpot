package dev.potholespot.android.actions.tasks;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Window;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.db.Pspot.Media;
import dev.potholespot.android.db.Pspot.MediaColumns;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.Waypoints;
import dev.potholespot.android.util.Constants;

public abstract class XmlCreator extends AsyncTask<Void, Integer, Uri>
{
   private final String TAG = "PS.XmlCreator";
   private String mExportDirectoryPath;
   private boolean mNeedsBundling;

   String mChosenName;
   private final ProgressListener mProgressListener;
   protected Context mContext;
   protected Uri mTrackUri;
   String mFileName;
   private String mErrorText;
   private Exception mException;
   private String mTask;
   public ProgressAdmin mProgressAdmin; //this can be accessed by the GpxCreator class since it is public.

   XmlCreator(final Context context, final Uri trackUri, final String chosenFileName, final ProgressListener listener)
   {
      this.mChosenName = chosenFileName;
      this.mContext = context;
      this.mTrackUri = trackUri;
      this.mProgressListener = listener;
      this.mProgressAdmin = new ProgressAdmin();

      final String trackName = extractCleanTrackName();
      this.mFileName = cleanFilename(this.mChosenName, trackName);
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

   private String extractCleanTrackName()
   {
      Cursor trackCursor = null;
      final ContentResolver resolver = this.mContext.getContentResolver();
      String trackName = "Untitled";
      try
      {
         trackCursor = resolver.query(this.mTrackUri, new String[] { TracksColumns.NAME }, null, null, null);
         if (trackCursor.moveToLast())
         {
            trackName = cleanFilename(trackCursor.getString(0), trackName);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
      return trackName;
   }

   /**
    * Calculated the total progress sum expected from a export to file This is the sum of the number of waypoints and media entries times 100. The whole number is doubled when compression is needed.
    */
   public void determineProgressGoal()
   {
      if (this.mProgressListener != null)
      {
         final Uri allWaypointsUri = Uri.withAppendedPath(this.mTrackUri, "waypoints");
         final Uri allMediaUri = Uri.withAppendedPath(this.mTrackUri, "media");
         Cursor cursor = null;
         final ContentResolver resolver = this.mContext.getContentResolver();
         try
         {
            cursor = resolver.query(allWaypointsUri, new String[] { "count(" + Waypoints.TABLE + "." + BaseColumns._ID + ")" }, null, null, null);
            if (cursor.moveToLast())
            {
               this.mProgressAdmin.setWaypointCount(cursor.getInt(0));
            }
            cursor.close();
            cursor = resolver.query(allMediaUri, new String[] { "count(" + Media.TABLE + "." + BaseColumns._ID + ")" }, null, null, null);
            if (cursor.moveToLast())
            {
               this.mProgressAdmin.setMediaCount(cursor.getInt(0));
            }
            cursor.close();
            cursor = resolver.query(allMediaUri, new String[] { "count(" + BaseColumns._ID + ")" }, MediaColumns.URI + " LIKE ? and " + MediaColumns.URI + " NOT LIKE ?", new String[] { "file://%",
                  "%txt" }, null);
            if (cursor.moveToLast())
            {
               this.mProgressAdmin.setCompress(cursor.getInt(0) > 0);
            }
         }
         finally
         {
            if (cursor != null)
            {
               cursor.close();
            }
         }
      }
      else
      {
         Log.w(this.TAG, "Exporting " + this.mTrackUri + " without progress!");
      }
   }

   /**
    * Removes all non-word chars (\W) from the text
    * 
    * @param fileName
    * @param defaultName
    * @return a string larger then 0 with either word chars remaining from the input or the default provided
    */
   public static String cleanFilename(String fileName, final String defaultName)
   {
      if (fileName == null || "".equals(fileName))
      {
         fileName = defaultName;
      }
      else
      {
         fileName = fileName.replaceAll("\\W", "");
         fileName = (fileName.length() > 0) ? fileName : defaultName;
      }
      return fileName;
   }

   /**
    * Includes media into the export directory and returns the relative path of the media
    * 
    * @param inputFilePath
    * @return file path relative to the export dir
    * @throws IOException
    */
   protected String includeMediaFile(final String inputFilePath) throws IOException
   {
      this.mNeedsBundling = true;
      final File source = new File(inputFilePath);
      final File target = new File(this.mExportDirectoryPath + "/" + source.getName());

      //      Log.d( TAG, String.format( "Copy %s to %s", source, target ) ); 
      if (source.exists())
      {
         final FileInputStream fileInputStream = new FileInputStream(source);
         final FileChannel inChannel = fileInputStream.getChannel();
         final FileOutputStream fileOutputStream = new FileOutputStream(target);
         final FileChannel outChannel = fileOutputStream.getChannel();
         try
         {
            inChannel.transferTo(0, inChannel.size(), outChannel);
         }
         finally
         {
            if (inChannel != null)
            {
               inChannel.close();
            }
            if (outChannel != null)
            {
               outChannel.close();
            }
            if (fileInputStream != null)
            {
               fileInputStream.close();
            }
            if (fileOutputStream != null)
            {
               fileOutputStream.close();
            }
         }
      }
      else
      {
         Log.w(this.TAG, "Failed to add file to new XML export. Missing: " + inputFilePath);
      }
      this.mProgressAdmin.addMediaProgress();

      return target.getName();
   }

   /**
    * Just to start failing early
    * 
    * @throws IOException verifying the availability of the SD card
    */
   protected void verifySdCardAvailibility() throws IOException
   {
      final String state = Environment.getExternalStorageState();
      if (!Environment.MEDIA_MOUNTED.equals(state))
      {
         throw new IOException("The ExternalStorage is not mounted, unable to export files for sharing.");
      }
   }

   /**
    * Create a zip of the export directory based on the given filename
    * 
    * @param fileName The directory to be replaced by a zipped file of the same name
    * @param extension
    * @return full path of the build zip file
    * @throws IOException
    */

   protected String bundlingMediaAndXml(final String fileName, final String extension) throws IOException
   {
      String zipFilePath;
      if (fileName.endsWith(".zip") || fileName.endsWith(extension))
      {
         zipFilePath = Constants.getSdCardDirectory(this.mContext) + fileName;
      }
      else
      {
         zipFilePath = Constants.getSdCardDirectory(this.mContext) + fileName + extension;
      }
      final String[] filenames = new File(this.mExportDirectoryPath).list();
      final byte[] buf = new byte[1024];
      ZipOutputStream zos = null;
      try
      {
         zos = new ZipOutputStream(new FileOutputStream(zipFilePath));
         for (final String filename2 : filenames)
         {
            final String entryFilePath = this.mExportDirectoryPath + "/" + filename2;
            final FileInputStream in = new FileInputStream(entryFilePath);
            zos.putNextEntry(new ZipEntry(filename2));
            int len;
            while ((len = in.read(buf)) >= 0)
            {
               zos.write(buf, 0, len);
            }
            zos.closeEntry();
            in.close();
            this.mProgressAdmin.addCompressProgress();
         }
      }
      finally
      {
         if (zos != null)
         {
            zos.close();
         }
      }

      deleteRecursive(new File(this.mExportDirectoryPath));

      return zipFilePath;
   }

   public static boolean deleteRecursive(final File file)
   {
      if (file.isDirectory())
      {
         final String[] children = file.list();
         for (final String element : children)
         {
            final boolean success = deleteRecursive(new File(file, element));
            if (!success)
            {
               return false;
            }
         }
      }
      return file.delete();
   }

   public void setExportDirectoryPath(final String exportDirectoryPath)
   {
      this.mExportDirectoryPath = exportDirectoryPath;
   }

   public String getExportDirectoryPath()
   {
      return this.mExportDirectoryPath;
   }

   public void quickTag(final XmlSerializer serializer, final String ns, String tag, String content) throws IllegalArgumentException, IllegalStateException, IOException
   {
      if (tag == null)
      {
         tag = "";
      }
      if (content == null)
      {
         content = "";
      }
      serializer.text("\n");
      serializer.startTag(ns, tag);
      serializer.text(content);
      serializer.endTag(ns, tag);
   }

   public boolean needsBundling()
   {
      return this.mNeedsBundling;
   }

   public static String convertStreamToString(final InputStream is) throws IOException
   {
      String result = "";
      /*
       * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We iterate until the Reader return -1 which means there's no more data to read. We use the StringWriter
       * class to produce the string.
       */
      if (is != null)
      {
         final Writer writer = new StringWriter();

         final char[] buffer = new char[8192];
         try
         {
            final Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1)
            {
               writer.write(buffer, 0, n);
            }
         }
         finally
         {
            is.close();
         }
         result = writer.toString();
      }
      return result;
   }

   public static InputStream convertStreamToLoggedStream(final String tag, final InputStream is) throws IOException
   {
      String result = "";
      /*
       * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We iterate until the Reader return -1 which means there's no more data to read. We use the StringWriter
       * class to produce the string.
       */
      if (is != null)
      {
         final Writer writer = new StringWriter();

         final char[] buffer = new char[8192];
         try
         {
            final Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1)
            {
               writer.write(buffer, 0, n);
            }
         }
         finally
         {
            is.close();
         }
         result = writer.toString();
      }
      final InputStream in = new ByteArrayInputStream(result.getBytes("UTF-8"));
      return in;
   }

   protected abstract String getContentType();

   protected void handleError(final String task, final Exception e, final String text)
   {
      Log.e(this.TAG, "Unable to save ", e);
      this.mTask = task;
      this.mException = e;
      this.mErrorText = text;
      cancel(false);
      throw new CancellationException(text);
   }

   @Override
   protected void onPreExecute()
   {
      if (this.mProgressListener != null)
      {
         this.mProgressListener.started();
      }
   }

   @Override
   protected void onProgressUpdate(final Integer... progress)
   {
      if (this.mProgressListener != null)
      {
         this.mProgressListener.setProgress(this.mProgressAdmin.getProgress());
      }
   }

   @Override
   protected void onPostExecute(final Uri resultFilename)
   {
      if (this.mProgressListener != null)
      {
         this.mProgressListener.finished(resultFilename);
      }
   }

   @Override
   protected void onCancelled()
   {
      if (this.mProgressListener != null)
      {
         this.mProgressListener.finished(null);
         this.mProgressListener.showError(this.mTask, this.mErrorText, this.mException);
      }
   }

   public class ProgressAdmin
   {
      long lastUpdate;
      private boolean compressCount;
      private boolean compressProgress;
      private boolean uploadCount;
      private boolean uploadProgress;
      private int mediaCount;
      private int mediaProgress;
      private int waypointCount;
      private int waypointProgress;
      private int labelProgress;
      private long photoUploadCount;
      private long photoUploadProgress;

      public void addMediaProgress()
      {
         this.mediaProgress++;
      }

      public void addCompressProgress()
      {
         this.compressProgress = true;
      }

      public void addUploadProgress()
      {
         this.uploadProgress = true;
      }

      public void addPhotoUploadProgress(final long length)
      {
         this.photoUploadProgress += length;
      }

      /**
       * Get the progress on scale 0 ... Window.PROGRESS_END
       * 
       * @return Returns the progress as a int.
       */
      public int getProgress()
      {
         int blocks = 0;
         if (this.waypointCount > 0)
         {
            blocks++;
         }
         if (this.mediaCount > 0)
         {
            blocks++;
         }
         if (this.compressCount)
         {
            blocks++;
         }
         if (this.uploadCount)
         {
            blocks++;
         }
         if (this.photoUploadCount > 0)
         {
            blocks++;
         }
         int progress;
         if (blocks > 0)
         {
            final int blockSize = Window.PROGRESS_END / blocks;
            progress = this.waypointCount > 0 ? blockSize * this.waypointProgress / this.waypointCount : 0;
            progress += this.mediaCount > 0 ? blockSize * this.mediaProgress / this.mediaCount : 0;
            progress += this.compressProgress ? blockSize : 0;
            progress += this.uploadProgress ? blockSize : 0;
            progress += this.photoUploadCount > 0 ? blockSize * this.photoUploadProgress / this.photoUploadCount : 0;
         }
         else
         {
            progress = 0;
         }
         //Log.d( TAG, "Progress updated to "+progress);
         return progress;
      }

      public void setWaypointCount(final int waypoint)
      {
         this.waypointCount = waypoint;
         considerPublishProgress();
      }

      public void setMediaCount(final int media)
      {
         this.mediaCount = media;
         considerPublishProgress();
      }

      public void setCompress(final boolean compress)
      {
         this.compressCount = compress;
         considerPublishProgress();
      }

      public void setUpload(final boolean upload)
      {
         this.uploadCount = upload;
         considerPublishProgress();
      }

      public void setPhotoUpload(final long length)
      {
         this.photoUploadCount += length;
         considerPublishProgress();
      }

      public void addWaypointProgress(final int i)
      {
         this.waypointProgress += i;
         considerPublishProgress();
      }

      public void addLabelProgress(final int i)
      {
         this.labelProgress += i;
         considerPublishProgress();
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
}
