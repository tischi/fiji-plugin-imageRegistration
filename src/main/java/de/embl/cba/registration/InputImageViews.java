package de.embl.cba.registration;

import de.embl.cba.registration.axessettings.AxesSettings;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.Img;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.*;

public class InputImageViews
        < R extends RealType< R > & NativeType< R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > > {

    final RandomAccessibleInterval inputRAI;
    final AxesSettings axesSettings;
    private DatasetService datasetService;
    Map< Long, T > transformations;
    OutputIntervalType outputIntervalType;

    public InputImageViews( RandomAccessibleInterval inputImage,
                            AxesSettings axesSettings,
                            DatasetService datasetService )
    {
        this.inputRAI = inputImage;
        this.axesSettings = axesSettings;
        this.datasetService = datasetService;
    }


    public Img< R > asImgPlus( RandomAccessibleInterval< R > rai )
    {
        assert inputRAI.numDimensions() == rai.numDimensions();

        Dataset dataset = datasetService.create( Views.zeroMin( rai ) );

        ImgPlus img = new ImgPlus( dataset, "transformedSequences", inputImgAxisTypes() );

        // TODO: better to return as Dataset? E.g. for KNIME?

        return img;
    }

    public AxisType[] inputImgAxisTypes()
    {
        return null;
    }

    public RandomAccessibleInterval< R > referenceInterval( RandomAccessible< R > ra )
    {
        return Views.interval( ra, axesSettings.transformableAxesReferenceInterval() );
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
        return transformableHyperSlice( s, axesSettings.fixedReferenceCoordinates() );
    }

    public RandomAccessibleInterval transformableHyperSlice(
            long s,
            long[] fixedAxesCoordinates )
    {

        // TODO: simplify below
        long[] min = Intervals.minAsLongArray( inputRAI );
        long[] max = Intervals.maxAsLongArray( inputRAI );

        min[ axesSettings.sequenceDimension() ] = s;
        max[ axesSettings.sequenceDimension() ] = s;

        setFixedAxesCoordinates( fixedAxesCoordinates, min, max );

        FinalInterval interval = new FinalInterval( min, max );

        RandomAccessibleInterval rai =
                Views.dropSingletonDimensions(
                        Views.interval( inputRAI, interval ) );

        return rai;

    }

    private void setFixedAxesCoordinates( long[] fixedAxesCoordinates, long[] min, long[] max )
    {
        for ( int i = 0; i < axesSettings.numFixedDimensions( ); ++i )
        {
            int d = axesSettings.fixedDimension( i );
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

        long[] fixedCoordinates = initializedFixedCoordinates();
        int loopingIndex = 0;

        RandomAccessibleInterval< R > transformedInput
                = transformedSequences( fixedCoordinates, loopingIndex );

        // TODO: fix dimension order such that it is the same as in input image

        return transformedInput;
    }

    private long[] initializedFixedCoordinates()
    {
        long[] fixedCoordinates = new long[ axesSettings.numFixedDimensions() ];
        FinalInterval fixedDimensionsInterval = axesSettings.fixedDimensionsInterval();
        for ( int i = 0; i < fixedCoordinates.length; ++i )
        {
            fixedCoordinates[ i ] = fixedDimensionsInterval.min( i );
        }
        return fixedCoordinates;
    }

    private RandomAccessibleInterval<R> transformedSequences(
            long[] fixedCoordinates,
            int loopingDimension )
    {

        ArrayList< RandomAccessibleInterval<R> > transformedSequenceList = new ArrayList<>(  );

        long min = axesSettings.fixedDimensionsInterval().min( loopingDimension );
        long max = axesSettings.fixedDimensionsInterval().max( loopingDimension );

        for ( long coordinate = min; coordinate < max; ++coordinate )
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

        return stackAndDropSingletons( transformedSequenceList );
    }


    private RandomAccessibleInterval<R> transformedSequence( long[] fixedCoordinates )
    {
        ArrayList< RandomAccessibleInterval<R> > transformedList = new ArrayList<>(  );

        long min = axesSettings.sequenceMin();
        long max = axesSettings.sequenceMax();

        for (long s = min; s <= max; ++s )
        {
            if ( transformations.containsKey( s ) )
            {
                RandomAccessibleInterval transformed =
                        transformedHyperSlice(
                                s,
                                transformations.get( s ),
                                fixedCoordinates,
                                axesSettings.transformableDimensionsOutputInterval( outputIntervalType ) );

                transformedList.add ( transformed );
            }
        }

        return stackAndDropSingletons( transformedList );

    }

    private RandomAccessibleInterval stackAndDropSingletons( ArrayList< RandomAccessibleInterval< R > > transformedList )
    {
        RandomAccessibleInterval rai = Views.stack( transformedList );
        rai = Views.dropSingletonDimensions( rai );
        return rai;
    }


}
