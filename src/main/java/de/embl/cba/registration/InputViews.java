package de.embl.cba.registration;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.*;

public class InputViews
        < R extends RealType< R > & NativeType< R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > > {

    final RandomAccessibleInterval inputRAI;
    final Axes axes;
    Map< Long, T > transformations;
    OutputIntervalType outputIntervalType;
    RandomAccessibleInterval< R > transformedInput;

    public InputViews( RandomAccessibleInterval inputImage,
                       Axes axes )
    {
        this.inputRAI = inputImage;
        this.axes = axes;
    }


    public ImgPlus< R > imgPlus( RandomAccessibleInterval< R > rai, String title )
    {
        assert inputRAI.numDimensions() == rai.numDimensions();

        Dataset dataset = Services.datasetService.create( Views.zeroMin( rai ) );

        // TODO: rearranging the axes does not work, why?
        //AxisType[] axisTypes = axes.axisTypesInputImage().toArray( new AxisType[0] );

        AxisType[] axisTypes = axes.axisTypesAfterTransformation().toArray( new AxisType[0]);
        ImgPlus< R > imgPlus = new ImgPlus( dataset, title, axisTypes );

        return imgPlus;
    }

    public RandomAccessibleInterval< R > referenceInterval( RandomAccessible< R > ra )
    {
        return Views.interval( ra, axes.transformableAxesReferenceInterval() );
    }


    public RandomAccessible< R > transform( RandomAccessibleInterval< R > rai, InvertibleRealTransform transform )
    {
        // TODO: make single lines
        RealRandomAccessible rra
                = RealViews.transform(
                        Views.interpolate( Views.extendBorder( rai ), new NLinearInterpolatorFactory() ),
                                    transform );

        RandomAccessible transformedRA = Views.raster( rra );

        return transformedRA;
    }

    public static RandomAccessible transform( RandomAccessible input, InvertibleRealTransform transform )
    {

        RealRandomAccessible tmp = Views.interpolate( input, new NLinearInterpolatorFactory() );
        tmp = RealViews.transform( tmp, transform );
        RandomAccessible transformed = Views.raster( tmp );

        return transformed;
    }


    public RandomAccessibleInterval transformableHyperSlice( long s )
    {
        return transformableHyperSlice( s, axes.fixedReferenceCoordinates() );
    }

    public RandomAccessibleInterval transformableHyperSlice(
            long s,
            long[] fixedAxesCoordinates )
    {

        // TODO: simplify below
        long[] min = Intervals.minAsLongArray( inputRAI );
        long[] max = Intervals.maxAsLongArray( inputRAI );

        min[ axes.sequenceDimension() ] = s;
        max[ axes.sequenceDimension() ] = s;

        setFixedAxesCoordinates( fixedAxesCoordinates, min, max );

        FinalInterval interval = new FinalInterval( min, max );

        RandomAccessibleInterval rai =
                Views.dropSingletonDimensions(
                        Views.interval( inputRAI, interval ) );

        return rai;

    }

    private void setFixedAxesCoordinates( long[] fixedAxesCoordinates, long[] min, long[] max )
    {
        for ( int i = 0; i < axes.numFixedDimensions( ); ++i )
        {
            int d = axes.fixedDimension( i );
            min[ d ] = fixedAxesCoordinates[ i ];
            max[ d ] = fixedAxesCoordinates[ i ];
        }
    }


    private RandomAccessibleInterval transformedHyperSlice(
            long s,
            InvertibleRealTransform transform,
            long[] fixedDimensions,
            FinalInterval interval )
    {

        RandomAccessibleInterval rai = transformableHyperSlice( s, fixedDimensions );
        RandomAccessible ra = transform( rai, transform );
        rai = Views.interval( ra, interval );

        return rai;
    }

    public RandomAccessibleInterval< R > transformedInput(
            Map< Long, T > transformations,
            OutputIntervalType outputIntervalType)
    {

        this.transformations = transformations;
        this.outputIntervalType = outputIntervalType;

        transformedInput = transformedSequences( initializedFixedCoordinates(), 0 );

        // TODO: below code seems to work but the resulting ImgPlus
        // does not show the right thing using the uiService.show().
        //rearrangeTransformedAxesIntoSameOrderAsInput();

        return transformedInput;
    }

    private long[] initializedFixedCoordinates()
    {
        long[] fixedCoordinates = new long[ axes.numFixedDimensions() ];
        FinalInterval fixedDimensionsInterval = axes.fixedDimensionsInterval();
        for ( int i = 0; i < fixedCoordinates.length; ++i )
        {
            fixedCoordinates[ i ] = fixedDimensionsInterval.min( i );
        }
        return fixedCoordinates;
    }

    private RandomAccessibleInterval< R > transformedSequences(
            long[] fixedCoordinates,
            int loopingDimension )
    {

        ArrayList< RandomAccessibleInterval<R> > transformedSequenceList = new ArrayList<>(  );

        long min = axes.fixedDimensionsInterval().min( loopingDimension );
        long max = axes.fixedDimensionsInterval().max( loopingDimension );

        for ( long coordinate = min; coordinate <= max; ++coordinate )
        {
            RandomAccessibleInterval transformedSequence;

            fixedCoordinates[ loopingDimension ] = coordinate;

            if ( loopingDimension == fixedCoordinates.length - 1 )
            {
                transformedSequence = transformedSequence( fixedCoordinates );
            }
            else
            {
                transformedSequence = transformedSequences( fixedCoordinates, ++loopingDimension );
            }

            transformedSequenceList.add( transformedSequence );
        }

        RandomAccessibleInterval rai = stackAndDropSingletons( transformedSequenceList );

        // Services.uiService.show( rai ); // TODO: does not do the dimensionality right. why?

        return rai;
    }

    private RandomAccessibleInterval< R > transformedSequence( long[] fixedCoordinates )
    {
        ArrayList< RandomAccessibleInterval<R> > transformedList = new ArrayList<>(  );

        long min = axes.sequenceMin();
        long max = axes.sequenceMax();

        for ( long s = min; s <= max; ++s )
        {
            if ( transformations.containsKey( s ) )
            {
                RandomAccessibleInterval transformed =
                        transformedHyperSlice(
                                s,
                                transformations.get( s ),
                                fixedCoordinates,
                                axes.transformableDimensionsOutputInterval( outputIntervalType ) );

                transformedList.add ( transformed );
            }
        }

        RandomAccessibleInterval rai = stackAndDropSingletons( transformedList );

        // Services.uiService.show( rai );

        return rai;

    }

    private void rearrangeTransformedAxesIntoSameOrderAsInput( )
    {
        // TODO: This code assumes that axistypes within one dataset are unique; is this true?

        ArrayList< AxisType > transformedAxisTypes = axes.axisTypesAfterTransformation();
        ArrayList< AxisType > inputAxisTypes = axes.axisTypesInputImage();

        for ( int inputDimension = 0; inputDimension < inputAxisTypes.size(); ++inputDimension )
        {
            int transformedDimension = transformedAxisTypes.indexOf( inputAxisTypes.get( inputDimension ) );
            Collections.swap( transformedAxisTypes, inputDimension, transformedDimension );
            transformedInput = Views.permute( transformedInput, inputDimension, transformedDimension );
        }

    }

    private RandomAccessibleInterval stackAndDropSingletons( ArrayList< RandomAccessibleInterval< R > > transformedList )
    {
        RandomAccessibleInterval rai = Views.stack( transformedList );
        rai = Views.dropSingletonDimensions( rai );
        return rai;
    }


}
