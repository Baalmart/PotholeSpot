package dev.ugasoft.android.gps.actions.tasks;

import dev.baalmart.gps.R;
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
public class KmzSharing extends KmzCreator
{

   public KmzSharing(Context context, Uri trackUri, String chosenFileName, ProgressListener listener)
   {
      super(context, trackUri, chosenFileName, listener);
   }

   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      ShareTrack.sendFile(mContext, resultFilename, mContext.getString(R.string.email_kmzbody), getContentType());
   }
   
}
