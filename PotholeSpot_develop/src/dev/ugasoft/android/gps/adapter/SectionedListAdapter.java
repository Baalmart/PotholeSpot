package dev.ugasoft.android.gps.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.util.Constants;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

/**
 * Combines multiple Adapters into a sectioned ListAdapter 
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class SectionedListAdapter extends BaseAdapter
{
   @SuppressWarnings("unused")
   private static final String TAG = "OGT.SectionedListAdapter";
   private Map<String, BaseAdapter> mSections;
   private ArrayAdapter<String> mHeaders;

   public SectionedListAdapter(Context ctx)
   {
      mHeaders = new ArrayAdapter<String>(ctx, R.layout.section_header);
      mSections = new LinkedHashMap<String, BaseAdapter>();
   }

   public void addSection(String name, BaseAdapter adapter)
   {
      mHeaders.add(name);
      mSections.put(name, adapter);
   }

   @Override
   public void registerDataSetObserver(DataSetObserver observer)
   {
      super.registerDataSetObserver(observer);
      for( Adapter adapter : mSections.values() )
      {
         adapter.registerDataSetObserver(observer);
      }
   }
   
   @Override
   public void unregisterDataSetObserver(DataSetObserver observer)
   {
      super.unregisterDataSetObserver(observer);
      for( Adapter adapter : mSections.values() )
      {
         adapter.unregisterDataSetObserver(observer);
      }
   }
   
   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getCount()
    */
   @Override
   public int getCount()
   {
      int count = 0;
      for (Adapter adapter : mSections.values())
      {
         count += adapter.getCount() + 1;
      }
      return count;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getItem(int)
    */
   @Override
   public Object getItem(int position)
   {
      int countDown = position;
      Adapter adapter;
      for (String section : mSections.keySet())
      {
         adapter = mSections.get(section);
         if (countDown == 0)
         {
            return section;
         }
         countDown--;
         
         if (countDown < adapter.getCount())
         {
            return adapter.getItem(countDown);
         }
         countDown -= adapter.getCount();
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getItemId(int)
    */
   @Override
   public long getItemId(int position)
   {
      int countDown = position;
      Adapter adapter;
      for (String section : mSections.keySet())
      {
         adapter = mSections.get(section);
         if (countDown == 0)
         {
            return position;
         }
         countDown--;
         
         if (countDown < adapter.getCount())
         {
            long id = adapter.getItemId(countDown);
            return id;
         }
         countDown -= adapter.getCount();
      }
      return -1;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getView(int, android.view.View,
    * android.view.ViewGroup)
    */
   @Override
   public View getView(final int position, View convertView, ViewGroup parent)
   {
      int sectionNumber = 0;
      int countDown = position;
      for (String section : mSections.keySet())
      {
         Adapter adapter = mSections.get(section);
         int size = adapter.getCount() + 1;

         // check if position inside this section
         if (countDown == 0)
         {
            return mHeaders.getView(sectionNumber, convertView, parent);
         }
         if (countDown < size)
         {
            return adapter.getView(countDown - 1, convertView, parent);
         }

         // otherwise jump into next section
         countDown -= size;
         sectionNumber++;
      }
      return null;
   }

   @Override
   public int getViewTypeCount()
   {
      int types = 1;
      for (Adapter section : mSections.values())
      {
         types += section.getViewTypeCount();
      }
      return types;
   }

   @Override
   public int getItemViewType(int position)
   {
      int type = 1;
      Adapter adapter;
      int countDown = position;
      for (String section : mSections.keySet())
      {
         adapter = mSections.get(section);
         int size = adapter.getCount() + 1;

         if (countDown == 0)
         {
            return Constants.SECTIONED_HEADER_ITEM_VIEW_TYPE;
         }
         else if (countDown < size)
         {
            return type + adapter.getItemViewType(countDown - 1);
         }
         countDown -= size;
         type += adapter.getViewTypeCount();
      }
      return ListAdapter.IGNORE_ITEM_VIEW_TYPE;
   }

   @Override
   public boolean areAllItemsEnabled()
   {
      return false;
   };

   @Override
   public boolean isEnabled(int position)
   {
      if( getItemViewType(position) == Constants.SECTIONED_HEADER_ITEM_VIEW_TYPE )
      {
         return false;
      }
      else
      {
         int countDown = position;
         for (String section : mSections.keySet())
         {
            BaseAdapter adapter = mSections.get(section);
            countDown--;
            int size = adapter.getCount() ;

            if (countDown < size)
            {
              return adapter.isEnabled(countDown);
            }
            // otherwise jump into next section
            countDown -= size;
         }
      }
      return false  ;
   }
}
