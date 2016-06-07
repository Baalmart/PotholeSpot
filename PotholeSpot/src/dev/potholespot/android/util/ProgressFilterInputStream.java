package dev.potholespot.android.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import dev.potholespot.android.actions.tasks.GpxParser;
import dev.potholespot.android.actions.tasks.GpxParser.ProgressAdmin;

/**
 * ????
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class ProgressFilterInputStream extends FilterInputStream
{
   GpxParser mAsyncTask;
   long progress = 0;
   private final ProgressAdmin mProgressAdmin;

   public ProgressFilterInputStream(final InputStream is, final ProgressAdmin progressAdmin)
   {
      super(is);
      this.mProgressAdmin = progressAdmin;
   }

   @Override
   public int read() throws IOException
   {
      final int read = super.read();
      incrementProgressBy(1);
      return read;
   }

   @Override
   public int read(final byte[] buffer, final int offset, final int count) throws IOException
   {
      final int read = super.read(buffer, offset, count);
      incrementProgressBy(read);
      return read;
   }

   private void incrementProgressBy(final int bytes)
   {
      if (bytes > 0)
      {
         this.mProgressAdmin.addBytesProgress(bytes);
      }
   }

}
