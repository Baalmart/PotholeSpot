package dev.ugasoft.android.gps.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import dev.ugasoft.android.gps.actions.tasks.GpxParser;
import dev.ugasoft.android.gps.actions.tasks.GpxParser.ProgressAdmin;

/**
 * ????
 *
 * @version $Id$
 * @author  Martin Bbaale
 */
public class ProgressFilterInputStream extends FilterInputStream
{
   GpxParser mAsyncTask;
   long progress = 0;
   private ProgressAdmin mProgressAdmin;

   public ProgressFilterInputStream(InputStream is, ProgressAdmin progressAdmin)
   {
      super( is );
      mProgressAdmin = progressAdmin;
   }

   @Override
   public int read() throws IOException
   {
      int read = super.read();
      incrementProgressBy( 1 );
      return read;
   }

   @Override
   public int read( byte[] buffer, int offset, int count ) throws IOException
   {
      int read = super.read( buffer, offset, count );
      incrementProgressBy( read );
      return read;
   }   
   
   private void incrementProgressBy( int bytes )
   {
      if( bytes > 0 )
      {
         mProgressAdmin.addBytesProgress(bytes);
      }
   }
   
}
