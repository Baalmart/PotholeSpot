package dev.potholespot.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import dev.potholespot.uganda.R;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class SlidingIndicatorView extends View
{
   private static final String TAG = "OGT.SlidingIndicatorView";
   private float mMinimum = 0;
   private float mMaximum = 100;
   private float mValue = 0;
   private Drawable mIndicator;
   private int mIntrinsicHeight;

   public SlidingIndicatorView(final Context context)
   {
      super(context);
   }

   public SlidingIndicatorView(final Context context, final AttributeSet attrs)
   {
      super(context, attrs);
   }

   public SlidingIndicatorView(final Context context, final AttributeSet attrs, final int defStyle)
   {
      super(context, attrs, defStyle);
   }

   @Override
   protected void onDraw(final Canvas canvas)
   {
      super.onDraw(canvas);

      if (this.mIndicator == null)
      {
         this.mIndicator = getResources().getDrawable(R.drawable.stip);
         this.mIntrinsicHeight = this.mIndicator.getIntrinsicHeight();
         this.mIndicator.setBounds(0, 0, getWidth(), this.mIntrinsicHeight);
      }
      final int height = getHeight();
      final float scale = Math.abs(this.mValue / (this.mMaximum - this.mMinimum));
      final float y = height - height * scale;
      final float translate = y - this.mIntrinsicHeight;
      canvas.save();
      canvas.translate(0, translate);
      this.mIndicator.draw(canvas);
      canvas.restore();
   }

   public float getMin()
   {
      return this.mMinimum;
   }

   public void setMin(final float min)
   {
      if (this.mMaximum - this.mMinimum == 0)
      {
         Log.w(TAG, "Minimum and maximum difference must be greater then 0");
         return;
      }
      this.mMinimum = min;
   }

   public float getMax()
   {
      return this.mMaximum;
   }

   public void setMax(final float max)
   {
      if (this.mMaximum - this.mMinimum == 0)
      {
         Log.w(TAG, "Minimum and maximum difference must be greater then 0");
         return;
      }
      this.mMaximum = max;
   }

   public float getValue()
   {
      return this.mValue;
   }

   public void setValue(final float value)
   {
      this.mValue = value;
   }
}
