package dev.ugasoft.android.gps.actions.tasks;

import dev.baalmart.potholespot.R;
import dev.ugasoft.android.gps.actions.ShareTrack;
import dev.ugasoft.android.gps.actions.utils.ProgressListener;
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
      ShareTrack.sendFile(mContext, resultFilename, mContext.getString(R.string.email_gpxbody), getContentType());
   }
   
}
