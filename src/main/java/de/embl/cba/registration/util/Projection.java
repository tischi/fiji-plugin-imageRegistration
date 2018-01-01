package de.embl.cba.registration.util;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
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
    private FinalInterval projectionInterval;
    private long[] outputDimensions;


    public Projection( RandomAccessibleInterval< T > input, int projectionDimension )
    {
        init( input, projectionDimension, fullProjectionInterval( input ) );
    }

    public Projection( RandomAccessibleInterval< T > input, int projectionDimension, FinalInterval projectionInterval )
    {
        init( input, projectionDimension, projectionInterval );
    }

    private void init( RandomAccessibleInterval< T > input, int projectionDimension, FinalInterval projectionInterval )
    {
        this.numOutputDimensions = input.numDimensions() - 1;

        this.input = input;
        this.inputAccess = input.randomAccess( );

        this.projectionDimension = projectionDimension;
        this.projectionInterval = projectionInterval;

        configureInputAxesExcludingProjectionAxis();
        initializeOutputArrayImg( );
    }

    private FinalInterval fullProjectionInterval( RandomAccessibleInterval< T > input)
    {
        long[] minMax = new long[]{ input.min( projectionDimension ), input.max( projectionDimension ) };
        return Intervals.createMinMax( minMax );
    }




    public RandomAccessibleInterval< T > average( )
    {
        final Cursor< T > outputCursor = Views.iterable( output ).localizingCursor();

        while ( outputCursor.hasNext() )
        {
            outputCursor.fwd( );
            setInputAccess( outputCursor );
            setAverageProjection( outputCursor );
        }

        return output;
    }

    private void initializeOutputArrayImg()
    {
        setOutputDimensions();
        final ImgFactory< T > factory = new ArrayImgFactory< T >();
        output = factory.create( outputDimensions, Views.iterable( input ).firstElement() );
        output = Views.translate( output,  outputOffset() );
    }

    private long[] outputOffset()
    {
        long[] offset = new long[ numOutputDimensions ];
        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            offset[ d ] = input.min( inputAxesExcludingProjectionAxis[ d ] );
        }
        return offset;
    }

    private void setOutputDimensions( )
    {
        outputDimensions = new long[ numOutputDimensions ];

        for ( int d = 0; d < numOutputDimensions; ++d )
        {
            outputDimensions[ d ] = input.dimension( inputAxesExcludingProjectionAxis[ d ] );
        }
    }

    private void setSumProjection( Cursor< T > outputCursor )
    {
        outputCursor.get().setZero();

        for ( long d = projectionInterval.min(0); d <= projectionInterval.max(0 ); ++d )
        {
            inputAccess.setPosition( d, projectionDimension );
            outputCursor.get().add( inputAccess.get() );
        }
    }


    private void setAverageProjection( Cursor< T > outputCursor )
    {
        double average = 0;
        long count = 0;

        for ( long d = projectionInterval.min(0); d <= projectionInterval.max(0 ); ++d )
        {
            inputAccess.setPosition( d, projectionDimension );
            average += inputAccess.get().getRealDouble();
            count++;
        }

        average /= count;

        outputCursor.get().setReal( average );

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
