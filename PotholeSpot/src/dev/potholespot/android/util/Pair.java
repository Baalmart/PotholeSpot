package dev.potholespot.android.util;

/**
 * Container to ease passing around a tuple of two objects. This object provides a sensible implementation of equals(), returning true if equals() is true on each of the contained objects.
 */
public class Pair<F, S>
{
   public final F first;
   public final S second;
   private String toStringOverride;

   /**
    * Constructor for a Pair. If either are null then equals() and hashCode() will throw a NullPointerException.
    * 
    * @param first the first object in the Pair
    * @param second the second object in the pair
    */
   public Pair(final F first, final S second)
   {
      this.first = first;
      this.second = second;
   }

   /**
    * Checks the two objects for equality by delegating to their respective equals() methods.
    * 
    * @param o the Pair to which this one is to be checked for equality
    * @return true if the underlying objects of the Pair are both considered equals()
    */
   @SuppressWarnings("unchecked")
   @Override
   public boolean equals(final Object o)
   {
      if (o == this)
      {
         return true;
      }
      if (!(o instanceof Pair))
      {
         return false;
      }
      final Pair<F, S> other;
      try
      {
         other = (Pair<F, S>) o;
      }
      catch (final ClassCastException e)
      {
         return false;
      }
      return this.first.equals(other.first) && this.second.equals(other.second);
   }

   /**
    * Compute a hash code using the hash codes of the underlying objects
    * 
    * @return a hashcode of the Pair
    */
   @Override
   public int hashCode()
   {
      int result = 17;
      result = 31 * result + this.first.hashCode();
      result = 31 * result + this.second.hashCode();
      return result;
   }

   /**
    * Convenience method for creating an appropriately typed pair.
    * 
    * @param a the first object in the Pair
    * @param b the second object in the pair
    * @return a Pair that is templatized with the types of a and b
    */
   public static <A, B> Pair<A, B> create(final A a, final B b)
   {
      return new Pair<A, B>(a, b);
   }

   public void overrideToString(final String toStringOverride)
   {
      this.toStringOverride = toStringOverride;
   }

   @Override
   public String toString()
   {
      if (this.toStringOverride == null)
      {
         return super.toString();
      }
      else
      {
         return this.toStringOverride;
      }
   }
}
