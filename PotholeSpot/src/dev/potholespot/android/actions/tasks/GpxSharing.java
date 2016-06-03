package dev.potholespot.android.actions.tasks;

import dev.potholespot.uganda.R;
import dev.potholespot.android.actions.ShareRoute;
import dev.potholespot.android.actions.utils.ProgressListener;
import android.content.Context;
import android.net.Uri;

/**
 * ????
 *
 * @version $Id:$
 * @author Martin Bbaale
 */
public class GpxSharing extends GpxCreator
{


   public GpxSharing(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      ShareRoute.sendFile(mContext, resultFilename, mContext.getString(R.string.email_gpxbody), getContentType());
   }
   
}
