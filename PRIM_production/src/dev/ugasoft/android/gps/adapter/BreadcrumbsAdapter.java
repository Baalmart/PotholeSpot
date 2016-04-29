package dev.ugasoft.android.gps.adapter;

import java.util.LinkedList;
import java.util.List;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.actions.tasks.GpxParser;
import dev.ugasoft.android.gps.breadcrumbs.BreadcrumbsService;
import dev.ugasoft.android.gps.breadcrumbs.BreadcrumbsTracks;
import dev.ugasoft.android.gps.util.Constants;
import dev.ugasoft.android.gps.util.Pair;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Organizes Breadcrumbs tasks based on demands on the BaseAdapter functions
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class BreadcrumbsAdapter extends BaseAdapter
{
   private static final String TAG = "OGT.BreadcrumbsAdapter";

   public static final boolean DEBUG = false;

   private Activity mContext;
   private LayoutInflater mInflater;
   private BreadcrumbsService mService;
   private List<Pair<Integer, Integer>> breadcrumbItems = new LinkedList<Pair<Integer, Integer>>();

   public BreadcrumbsAdapter(Activity ctx, BreadcrumbsService service)
   {
      super();
      mContext = ctx;
      mService = service;
      mInflater = LayoutInflater.from(mContext);
   }

   public void setService(BreadcrumbsService service)
   {
      mService = service;
      updateItemList();
   }

   /**
    * Reloads the current list of known breadcrumb listview items
    * 
    */
   public void updateItemList()
   {
      mContext.runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               if (mService != null)
               {
                  breadcrumbItems = mService.getAllItems();
                  notifyDataSetChanged();
               }
            }
         });
   }

   /**
    * @see android.widget.Adapter#getCount()
    */
   @Override
   public int getCount()
   {
      if (mService != null)
      {
         if (mService.isAuthorized())
         {
            return breadcrumbItems.size();
         }
         else
         {
            return 1;
         }
      }
      else
      {
         return 0;
      }

   }

   /**
    * @see android.widget.Adapter#getItem(int)
    */
   @Override
   public Object getItem(int position)
   {
      if (mService.isAuthorized())
      {
         return breadcrumbItems.get(position);
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT;
      }

   }

   /**
    * @see android.widget.Adapter#getItemId(int)
    */
   @Override
   public long getItemId(int position)
   {
      return position;
   }

   /**
    * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
    */
   @Override
   public View getView(int position, View convertView, ViewGroup parent)
   {
      View view = null;
      if (mService.isAuthorized())
      {
         int type = getItemViewType(position);
         if (convertView == null)
         {
            switch (type)
            {
               case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_bundle, null);
                  break;
               case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_track, null);
                  break;
               default:
                  view = new TextView(null);
                  break;
            }
         }
         else
         {
            view = convertView;
         }
         Pair<Integer, Integer> item = breadcrumbItems.get(position);
         mService.willDisplayItem(item);
         String name;
         switch (type)
         {
            case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
               name = mService.getValueForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view.findViewById(R.id.listitem_name)).setText(name);
               break;
            case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
               TextView nameView = (TextView) view.findViewById(R.id.listitem_name);
               TextView dateView = (TextView) view.findViewById(R.id.listitem_from);

               nameView.setText(mService.getValueForItem(item, BreadcrumbsTracks.NAME));
               String dateString = mService.getValueForItem(item, BreadcrumbsTracks.ENDTIME);
               if (dateString != null)
               {
                  Long date = GpxParser.parseXmlDateTime(dateString);
                  dateView.setText(date.toString());
               }
               break;
            default:
               view = new TextView(null);
               break;
         }
      }
      else
      {
         if (convertView == null)
         {
            view = mInflater.inflate(R.layout.breadcrumbs_connect, null);
         }
         else
         {
            view = convertView;
         }
         ((TextView) view).setText(R.string.breadcrumbs_connect);
      }
      return view;
   }

   @Override
   public int getViewTypeCount()
   {
      int types = 4;
      return types;
   }

   @Override
   public int getItemViewType(int position)
   {
      if (mService.isAuthorized())
      {
         Pair<Integer, Integer> item = breadcrumbItems.get(position);
         return item.first;
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
      }
   }

   @Override
   public boolean areAllItemsEnabled()
   {
      return false;
   };

   @Override
   public boolean isEnabled(int position)
   {
      int itemViewType = getItemViewType(position);
      return itemViewType == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE || itemViewType == Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
   }
}
