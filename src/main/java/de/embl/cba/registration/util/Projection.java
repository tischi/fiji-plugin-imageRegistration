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

    private int[] inputAxesExcludingProjectionAxis;
    private int projectionDimension;
    private RandomAccessibleInterval< T > output;
    private int numOutputDimensions;
    private RandomAccess< T > inputAccess;
    private RandomAccessibleInterval< T > input;
    private long projectionDimensionIntervalMin;
    private long projectionDimensionIntervalMax;
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
        this.projectionDimensionIntervalMin = min;
        this.projectionDimensionIntervalMax = max;

        configureInputAxesExcludingProjectionAxis();
        initializeOutputArrayImg( );

    }

    public RandomAccessibleInterval< T > sum()
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd( );
            setInputAccess( outputCursor );
            setSumProjection( outputCursor );
        }

        return output;
    }

    private void initializeOutputArrayImg()
    {
        setOutputDimensions();
        final ImgFactory< T > factory = new ArrayImgFactory< T >();
        output = factory.create( outputDimensions, Views.iterable( input ).firstElement() );
    }

    private void setOutputDimensions( )
    {
        outputDimensions = new long[ numOutputDimensions ];

        for ( int d = 0; d < inputAxesExcludingProjectionAxis.length; ++d )
        {
            outputDimensions[ d ] = input.dimension( inputAxesExcludingProjectionAxis[ d ] );
        }
    }

    private void setSumProjection( Cursor< T > outputCursor )
    {
        outputCursor.get().setZero();

        for ( long d = projectionDimensionIntervalMin; d <= projectionDimensionIntervalMax; ++d )
        {
            inputAccess.setPosition( d, projectionDimension );
            outputCursor.get().add( inputAccess.get() );
        }
    }

    private void setInputAccess( Cursor< T > cursorOutput )
    {
        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            inputAccess.setPosition( cursorOutput.getLongPosition( d ) , inputAxesExcludingProjectionAxis[ d ] );
        }
    }

    private void configureInputAxesExcludingProjectionAxis()
    {
        inputAxesExcludingProjectionAxis = new int[ numOutputDimensions ];

        for ( int i = 0; i < numOutputDimensions; i++ )
        {
            if ( i >= projectionDimension )
            {
                inputAxesExcludingProjectionAxis[ i ] = i + 1;
            }
            else
            {
                inputAxesExcludingProjectionAxis[ i ] = i;
            }
        }
    }
}
