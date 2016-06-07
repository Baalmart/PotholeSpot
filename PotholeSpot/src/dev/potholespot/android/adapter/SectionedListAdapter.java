package dev.potholespot.android.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

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
   private final Map<String, BaseAdapter> mSections;
   private final ArrayAdapter<String> mHeaders;

   public SectionedListAdapter(final Context ctx)
   {
      this.mHeaders = new ArrayAdapter<String>(ctx, R.layout.section_header);
      this.mSections = new LinkedHashMap<String, BaseAdapter>();
   }

   public void addSection(final String name, final BaseAdapter adapter)
   {
      this.mHeaders.add(name);
      this.mSections.put(name, adapter);
   }

   @Override
   public void registerDataSetObserver(final DataSetObserver observer)
   {
      super.registerDataSetObserver(observer);
      for (final Adapter adapter : this.mSections.values())
      {
         adapter.registerDataSetObserver(observer);
      }
   }

   @Override
   public void unregisterDataSetObserver(final DataSetObserver observer)
   {
      super.unregisterDataSetObserver(observer);
      for (final Adapter adapter : this.mSections.values())
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
      for (final Adapter adapter : this.mSections.values())
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
   public Object getItem(final int position)
   {
      int countDown = position;
      Adapter adapter;
      for (final String section : this.mSections.keySet())
      {
         adapter = this.mSections.get(section);
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
   public long getItemId(final int position)
   {
      int countDown = position;
      Adapter adapter;
      for (final String section : this.mSections.keySet())
      {
         adapter = this.mSections.get(section);
         if (countDown == 0)
         {
            return position;
         }
         countDown--;

         if (countDown < adapter.getCount())
         {
            final long id = adapter.getItemId(countDown);
            return id;
         }
         countDown -= adapter.getCount();
      }
      return -1;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
    */
   @Override
   public View getView(final int position, final View convertView, final ViewGroup parent)
   {
      int sectionNumber = 0;
      int countDown = position;
      for (final String section : this.mSections.keySet())
      {
         final Adapter adapter = this.mSections.get(section);
         final int size = adapter.getCount() + 1;

         // check if position inside this section
         if (countDown == 0)
         {
            return this.mHeaders.getView(sectionNumber, convertView, parent);
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
      for (final Adapter section : this.mSections.values())
      {
         types += section.getViewTypeCount();
      }
      return types;
   }

   @Override
   public int getItemViewType(final int position)
   {
      int type = 1;
      Adapter adapter;
      int countDown = position;
      for (final String section : this.mSections.keySet())
      {
         adapter = this.mSections.get(section);
         final int size = adapter.getCount() + 1;

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
      return Adapter.IGNORE_ITEM_VIEW_TYPE;
   }

   @Override
   public boolean areAllItemsEnabled()
   {
      return false;
   };

   @Override
   public boolean isEnabled(final int position)
   {
      if (getItemViewType(position) == Constants.SECTIONED_HEADER_ITEM_VIEW_TYPE)
      {
         return false;
      }
      else
      {
         int countDown = position;
         for (final String section : this.mSections.keySet())
         {
            final BaseAdapter adapter = this.mSections.get(section);
            countDown--;
            final int size = adapter.getCount();

            if (countDown < size)
            {
               return adapter.isEnabled(countDown);
            }
            // otherwise jump into next section
            countDown -= size;
         }
      }
      return false;
   }
}
