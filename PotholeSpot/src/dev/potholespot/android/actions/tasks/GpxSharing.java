package dev.potholespot.android.actions.tasks;

import android.content.Context;
import android.net.Uri;
import dev.potholespot.android.actions.ShareRoute;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.uganda.R;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class GpxSharing extends GpxCreator
{

   public GpxSharing(final Context context, final Uri trackUri, final String chosenBaseFileName, final boolean attachments, final ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected void onPostExecute(final Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      ShareRoute.sendFile(this.mContext, resultFilename, this.mContext.getString(R.string.email_gpxbody), getContentType());
   }

}
