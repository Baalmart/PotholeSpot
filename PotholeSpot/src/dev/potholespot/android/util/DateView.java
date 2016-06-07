package dev.potholespot.android.util;

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * An implementation for the XML element DateView that alters the textview in the formating of the text when displaying a date in ms from 1970.
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class DateView extends TextView
{
   private Date mDate;

   /**
    * Constructor: create a new DateView.
    * 
    * @param context
    */
   public DateView(final Context context)
   {
      super(context);
   }

   /**
    * Constructor: create a new DateView.
    * 
    * @param context
    * @param attrs
    */
   public DateView(final Context context, final AttributeSet attrs)
   {
      super(context, attrs);
   }

   /**
    * Constructor: create a new DateView.
    * 
    * @param context
    * @param attrs
    * @param defStyle
    */
   public DateView(final Context context, final AttributeSet attrs, final int defStyle)
   {
      super(context, attrs, defStyle);
   }

   /*
    * (non-Javadoc)
    * @see android.widget.TextView#setText(java.lang.CharSequence, android.widget.TextView.BufferType)
    */
   @Override
   public void setText(final CharSequence charSeq, final BufferType type)
   {
      // Behavior for the graphical editor
      if (isInEditMode())
      {
         super.setText(charSeq, type);
         return;
      }

      long longVal;
      if (charSeq.length() == 0)
      {
         longVal = 0l;
      }
      else
      {
         try
         {
            longVal = Long.parseLong(charSeq.toString());
         }
         catch (final NumberFormatException e)
         {
            longVal = 0l;
         }
      }
      this.mDate = new Date(longVal);

      final DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getContext().getApplicationContext());
      final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext().getApplicationContext());
      final String text = timeFormat.format(this.mDate) + " " + dateFormat.format(this.mDate);
      super.setText(text, type);
   }

}
