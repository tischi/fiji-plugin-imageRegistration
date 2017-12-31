package de.embl.cba.registration.util;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.RealSum;
import net.imglib2.view.Views;

public class Projection< T extends RealType< T > & NativeType< T > >
{

    private int[] inputAxesWithoutProjectionAxis;
    private int projectionDimension;
    private RandomAccessibleInterval< T > output;
    private int numOutputDimensions;
    private RandomAccess< T > inputAccess;
    private RandomAccessibleInterval< T > input;
    private long projectionDimensionMin;
    private long projectionDimensionMax;
    private long[] outputDimensions;

    public Projection( RandomAccessibleInterval< T > input, int projectionDimension )
    {
        this( input, projectionDimension, input.min( projectionDimension ), input.max( projectionDimension ) );
    }


    public Projection( RandomAccessibleInterval< T > input, int projectionDimension, long min, long max )
    {
        this.numOutputDimensions = input.numDimensions() - 1;

        this.input = input;
        this.inputAccess = input.randomAccess( );

        this.projectionDimension = projectionDimension;
        this.projectionDimensionMin = min;
        this.projectionDimensionMax = max;

        setInputAxesWithoutProjectionAxis();

        setOutputAsArrayImg( );

    }

    public RandomAccessibleInterval< T > sum()
    {
        computeSumProjection();
        return output;
    }

    private void computeSumProjection()
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd( );
            setInputAccess( outputCursor );
            outputCursor.get().set( getSumProjection( ) );
        }
    }

    private void setOutputAsArrayImg()
    {
        setOutputDimensions();
        final ImgFactory< T > factory = new ArrayImgFactory< T >();
        output = factory.create( outputDimensions, Views.iterable( input ).firstElement() );
    }

    private void setOutputDimensions( )
    {

        outputDimensions = new long[ numOutputDimensions ];

        for ( int d = 0; d < inputAxesWithoutProjectionAxis.length; ++d )
        {
            outputDimensions[ d ] = input.dimension( inputAxesWithoutProjectionAxis[ d ] );
        }

    }

    private T getSumProjection( )
    {
        final RealSum realSum = new RealSum();

        for ( long d = projectionDimensionMin; d <= projectionDimensionMax; ++d )
        {
            inputAccess.setPosition( d, projectionDimension );
            realSum.add( inputAccess.get().getRealDouble() );
        }

        return ( T ) realSum;
    }

    private void setInputAccess( Cursor< T > cursorOutput )
    {
        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            inputAccess.setPosition( cursorOutput.getLongPosition( d ) , inputAxesWithoutProjectionAxis[ d ] );
        }
    }

    private void setInputAxesWithoutProjectionAxis()
    {
        inputAxesWithoutProjectionAxis = new int[ numOutputDimensions ];

        for ( int i = 0; i < numOutputDimensions; i++ )
        {
            if ( i >= projectionDimension )
            {
                inputAxesWithoutProjectionAxis[ i ] = i + 1;
            }
            else
            {
                inputAxesWithoutProjectionAxis[ i ] = i;
            }
        }
    }
}
