package dev.potholespot.android.actions.tasks;

import dev.baalmart.potholespot.R;
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
      ShareRoute.sendFile(mContext, resultFilename, mContext.getString(R.string.email_kmzbody), getContentType());
   }
   
}
