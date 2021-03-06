package de.embl.cba.registration.utils;

import de.embl.cba.registration.transform.Translation1D;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import static de.embl.cba.registration.utils.Constants.XYZ;

public abstract class Transforms < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{
    public static RealTransform translationAsRealTransform( double[] translation )
    {
        if ( translation.length == 1 ) return new Translation1D( translation );

        if ( translation.length == 2 ) return new Translation2D( translation );

        if ( translation.length == 3 ) return new Translation3D( translation );

        return new Translation( translation );
    }


    public static < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
    RealTransform createIdentityAffineTransformation( int numDimensions )
    {
        if ( numDimensions == 2 )
        {
            return (T) new AffineTransform2D();
        }
        else if ( numDimensions == 3 )
        {
            return (T) new AffineTransform3D();
        }
        else
        {
            return (T) new AffineTransform( numDimensions );
        }
    }


    public static < T extends RealType< T > >
    RandomAccessibleInterval view( RandomAccessibleInterval< T > rai,
								   InvertibleRealTransform combinedTransform )
	{
		final RandomAccessible transformedRA = getTransformedRaView( rai, combinedTransform );
		final FinalInterval transformedInterval = createTransformedInterval( rai, combinedTransform );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, transformedInterval );

		return transformedIntervalView;

	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createArrayCopy( RandomAccessibleInterval< T > rai )
	{
		RandomAccessibleInterval< T > copy = new ArrayImgFactory( rai.randomAccess().get() ).create( rai );
		copy = adjustOrigin( rai, copy );

		final Cursor< T > out = Views.iterable( copy ).localizingCursor();
		final RandomAccess< T > in = rai.randomAccess();

		while( out.hasNext() )
		{
			out.fwd();
			in.setPosition( out );
			out.get().set( in.get() );
		}

		return copy;
	}

	public static < T extends RealType< T > > RandomAccessibleInterval< T > adjustOrigin( RandomAccessibleInterval< T > rai, RandomAccessibleInterval< T > copy )
	{
		long[] offset = new long[ rai.numDimensions() ];
		rai.min( offset );
		copy = Views.translate( copy, offset );
		return copy;
	}

	public static < T extends RealType< T > >
	RandomAccessible getTransformedRaView( RandomAccessibleInterval< T > rai, InvertibleRealTransform combinedTransform )
	{
		RealRandomAccessible rra = Views.interpolate( Views.extendZero( rai ), new NLinearInterpolatorFactory() );
		rra = RealViews.transform( rra, combinedTransform );
		return Views.raster( rra );
	}

    public static < T extends RealType< T > >
	FinalInterval createTransformedInterval( RandomAccessibleInterval< T > rai, InvertibleRealTransform transform )
	{
		final FinalInterval transformedInterval;

		if ( transform instanceof  AffineTransform3D )
		{
			FinalRealInterval transformedRealInterval = ( ( AffineTransform3D ) transform ).estimateBounds( rai );
			transformedInterval = toInterval( transformedRealInterval );
		}
		else if ( transform instanceof Scale )
		{
			transformedInterval = createScaledInterval( rai, ( Scale ) transform );
		}
		else
		{
			transformedInterval = null;
		}

		return transformedInterval;
	}

	public static < T extends RealType< T > >
	FinalInterval createScaledInterval( RandomAccessibleInterval< T > rai, Scale scale )
	{
		long[] min = new long[ 3 ];
		long[] max = new long[ 3 ];
		rai.min( min );
		rai.max( max );

		for ( int d : XYZ )
		{
			min[ d ] *= scale.getScale( d );
			max[ d ] *= scale.getScale( d );
		}

		return new FinalInterval( min, max );
	}

	public static FinalInterval toInterval( FinalRealInterval realInterval )
	{
		double[] realMin = new double[ 3 ];
		double[] realMax = new double[ 3 ];
		realInterval.realMin( realMin );
		realInterval.realMax( realMax );

		long[] min = new long[ 3 ];
		long[] max = new long[ 3 ];

		for ( int d : XYZ )
		{
			min[ d ] = (long) realMin[ d ];
			max[ d ] = (long) realMax[ d ];
		}

		return new FinalInterval( min, max );
	}
}
