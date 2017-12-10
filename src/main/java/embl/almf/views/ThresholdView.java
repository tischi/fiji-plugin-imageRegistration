package embl.almf.views;

import net.imglib2.*;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

public class ThresholdView<T extends RealType<T>> implements RandomAccessible<BooleanType> {

     private final RandomAccessible<T> source;

     private final T threshold;

     public ThresholdView( RandomAccessible<T> source, T threshold)
     {
         this.source = source;
         this.threshold = threshold;
     }

    @Override
    public RandomAccess< BooleanType > randomAccess()
    {
        return new MyRandomAccess();
    }

    @Override
    public RandomAccess< BooleanType > randomAccess( Interval interval )
    {
        return new MyRandomAccess( interval );
    }

    @Override
    public int numDimensions()
    {
        return source.numDimensions();
    }

    private class MyRandomAccess implements RandomAccess<BooleanType>
    {

        private final RandomAccess<T> ra;
        private final BitType val;


        private MyRandomAccess()
        {
            this.ra = source.randomAccess();
            this.val = new BitType();
        }

        private MyRandomAccess( Interval interval)
        {
            this.ra = source.randomAccess( interval );
            this.val = new BitType();
        }


        @Override
        public RandomAccess< BooleanType > copyRandomAccess()
        {
            return new MyRandomAccess();
        }

        @Override
        public void localize( int[] ints )
        {
            ra.localize( ints );
        }

        @Override
        public void localize( long[] longs )
        {
            ra.localize( longs );
        }

        @Override
        public int getIntPosition( int i )
        {
            return ra.getIntPosition( i );
        }

        @Override
        public long getLongPosition( int i )
        {
            return ra.getLongPosition( i );
        }

        @Override
        public void fwd( int i )
        {
            ra.fwd( i );
        }

        @Override
        public void bck( int i )
        {
            ra.bck( i );
        }

        @Override
        public void move( int i, int i1 )
        {
            ra.move( i,i1 );
        }

        @Override
        public void move( long l, int i )
        {
            ra.move( l, i );
        }

        @Override
        public void move( Localizable localizable )
        {
            ra.move( localizable );
        }

        @Override
        public void move( int[] ints )
        {
            ra.move( ints );
        }

        @Override
        public void move( long[] longs )
        {
            ra.move( longs );
        }

        @Override
        public void setPosition( Localizable localizable )
        {
            ra.setPosition( localizable );
        }

        @Override
        public void setPosition( int[] ints )
        {
            ra.setPosition( ints );
        }

        @Override
        public void setPosition( long[] longs )
        {
            ra.setPosition( longs );
        }

        @Override
        public void setPosition( int i, int i1 )
        {
            ra.setPosition( i, i1 );
        }

        @Override
        public void setPosition( long l, int i )
        {
            ra.setPosition( l, i );
        }

        @Override
        public void localize( float[] floats )
        {
            ra.localize( floats );
        }

        @Override
        public void localize( double[] doubles )
        {
            ra.localize( doubles );
        }

        @Override
        public float getFloatPosition( int i )
        {
            return ra.getFloatPosition( i );
        }

        @Override
        public double getDoublePosition( int i )
        {
            return ra.getDoublePosition( i );
        }

        @Override
        public int numDimensions()
        {
            return ra.numDimensions();
        }

        @Override
        public BitType get()
        {
            val.set( ra.get().compareTo( threshold ) >= 0 );
            return val;
        }

        @Override
        public Sampler< BooleanType > copy()
        {
            return new MyRandomAccess();
        }
    }
}
