package dev.potholespot.android.adapter;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import dev.potholespot.android.actions.tasks.GpxParser;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService;
import dev.potholespot.android.breadcrumbs.BreadcrumbsTracks;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.Pair;
import dev.potholespot.uganda.R;

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

   private final Activity mContext;
   private final LayoutInflater mInflater;
   private BreadcrumbsService mService;
   private List<Pair<Integer, Integer>> breadcrumbItems = new LinkedList<Pair<Integer, Integer>>();

   public BreadcrumbsAdapter(final Activity ctx, final BreadcrumbsService service)
   {
      super();
      this.mContext = ctx;
      this.mService = service;
      this.mInflater = LayoutInflater.from(this.mContext);
   }

   public void setService(final BreadcrumbsService service)
   {
      this.mService = service;
      updateItemList();
   }

   /**
    * Reloads the current list of known breadcrumb listview items
    */
   public void updateItemList()
   {
      this.mContext.runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               if (BreadcrumbsAdapter.this.mService != null)
               {
                  BreadcrumbsAdapter.this.breadcrumbItems = BreadcrumbsAdapter.this.mService.getAllItems();
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
      if (this.mService != null)
      {
         if (this.mService.isAuthorized())
         {
            return this.breadcrumbItems.size();
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
   public Object getItem(final int position)
   {
      if (this.mService.isAuthorized())
      {
         return this.breadcrumbItems.get(position);
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
   public long getItemId(final int position)
   {
      return position;
   }

   /**
    * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
    */
   @Override
   public View getView(final int position, final View convertView, final ViewGroup parent)
   {
      View view = null;
      if (this.mService.isAuthorized())
      {
         final int type = getItemViewType(position);
         if (convertView == null)
         {
            switch (type)
            {
               case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
                  view = this.mInflater.inflate(R.layout.breadcrumbs_bundle, null);
                  break;
               case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
                  view = this.mInflater.inflate(R.layout.breadcrumbs_track, null);
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
         final Pair<Integer, Integer> item = this.breadcrumbItems.get(position);
         this.mService.willDisplayItem(item);
         String name;
         switch (type)
         {
            case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
               name = this.mService.getValueForItem(item, BreadcrumbsTracks.NAME);
               ((TextView) view.findViewById(R.id.listitem_name)).setText(name);
               break;
            case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
               final TextView nameView = (TextView) view.findViewById(R.id.listitem_name);
               final TextView dateView = (TextView) view.findViewById(R.id.listitem_from);

               nameView.setText(this.mService.getValueForItem(item, BreadcrumbsTracks.NAME));
               final String dateString = this.mService.getValueForItem(item, BreadcrumbsTracks.ENDTIME);
               if (dateString != null)
               {
                  final Long date = GpxParser.parseXmlDateTime(dateString);
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
            view = this.mInflater.inflate(R.layout.breadcrumbs_connect, null);
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
      final int types = 4;
      return types;
   }

   @Override
   public int getItemViewType(final int position)
   {
      if (this.mService.isAuthorized())
      {
         final Pair<Integer, Integer> item = this.breadcrumbItems.get(position);
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
   public boolean isEnabled(final int position)
   {
      final int itemViewType = getItemViewType(position);
      return itemViewType == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE || itemViewType == Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
   }
}
