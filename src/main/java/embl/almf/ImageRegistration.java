package embl.almf;

import embl.almf.filter.*;
import embl.almf.registration.TranslationPhaseCorrelation;
import ij.IJ;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRegistration
        < R extends RealType< R > & NativeType < R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable < T > > {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";
    final static String FIXED = "Fixed";

    public final static String SEQUENCE_DIMENSION = "Sequence";
    public final static String TRANSFORMABLE_DIMENSION = "Transformable";
    public final static String FIXED_DIMENSION = "Fixed";

    private final ImageFilter imageFilter;

    final RandomAccessibleInterval< R > inputRAI;

    private final long[] searchRadii;

    private final SequenceDimension sequenceDimension;
    private final Map< Integer, long[] > transformableDimensions;
    private FinalInterval transformableDimensionsInterval;
    private final Map< Integer, Long > fixedDimensionsReferenceCoordinates;

    private final Map< String, Object > imageFilterParameters;

    String referenceType;

    ExecutorService service;
    private final boolean showFixedImageSequence;

    private void setTransformableDimensionsReferenceInterval()
    {
        int n = transformableDimensions.size();

        long[] min = new long[n];
        long[] max = new long[n];

        int i = 0;
        for ( int d = 0; d < transformableDimensions.size(); ++d )
        {
            min[ i ] = transformableDimensions.get( d )[0];
            max[ i ] = transformableDimensions.get( d )[1];
            ++i;
        }
        transformableDimensionsInterval = new FinalInterval( min, max );

    }

    private class SequenceDimension
    {
        int dimension;
        long min;
        long max;

        SequenceDimension( int d, long min, long max )
        {
            this.dimension = d;
            this.min = min;
            this.max = max;

        }

    }


    // TODO: read and write: https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/XmlHelpers.java

    ImageRegistration( final RandomAccessibleInterval< R > input,
                       final String[] dimensionTypes,
                       final FinalInterval interval,
                       final long[] searchRadii,
                       int numThreads,
                       final ImageFilterType imageFilterType,
                       final Map< String, Object > imageFilterParameters,
                       boolean showFixedImageSequence )
    {
        this.showFixedImageSequence = showFixedImageSequence;

        referenceType = MOVING; // TODO

        this.inputRAI = input;

        if ( imageFilterType != null )
        {
            this.imageFilterParameters = imageFilterParameters;
            this.imageFilterParameters.put( ImageFilterParameters.NUM_THREADS, numThreads );
            this.imageFilter = ImageFilterFactory.create( imageFilterType, imageFilterParameters );
        }
        else
        {
            this.imageFilter = null;
            this.imageFilterParameters = null;
        }

        service = Executors.newFixedThreadPool( numThreads );

        // set up sequence dimension
        int s = Arrays.asList( dimensionTypes ).indexOf( SEQUENCE_DIMENSION );
        sequenceDimension = new SequenceDimension( s, interval.min( s ), interval.max( s ) );

        // set up other dimensions
        transformableDimensions = new LinkedHashMap<>();
        fixedDimensionsReferenceCoordinates = new HashMap<>(  );

        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            switch ( dimensionTypes[ d ] )
            {
                case TRANSFORMABLE_DIMENSION:
                    transformableDimensions.put( d, new long[]{ interval.min( d ), interval.max( d ) });
                    break;
                case FIXED_DIMENSION:
                    fixedDimensionsReferenceCoordinates.put( d, interval.min( d ) );
                    break;
            }
        }

        setTransformableDimensionsReferenceInterval();

        // set up multi-threading
        service = Executors.newFixedThreadPool( numThreads );

        // set search radii
        this.searchRadii = searchRadii;

    }

    public void run()
    {

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;

        List< RandomAccessibleInterval < R > > fixedRAIList = new ArrayList<>(  );

        T absoluteTransformation = null;

        Map< Long, T > transformations = new HashMap<>(  );

        for ( long s = sequenceDimension.min; s <= sequenceDimension.max; s += 1 )
        {

            fixedRAI = getFixedRAI( s, transformations.get( s ) );
            movingRA = getMovingRA( s + 1, transformations.get( s ) );

            //ImageJFunctions.show( fixedRAI );

            T relativeTransformation = ( T ) TranslationPhaseCorrelation
                    .findTransform(
                            fixedRAI,
                            movingRA,
                            fixedRAIList,
                            searchRadii,
                            imageFilter,
                            service );

            if ( absoluteTransformation != null )
            {
                absoluteTransformation.preConcatenate( relativeTransformation );
            }
            else
            {
                absoluteTransformation = relativeTransformation;
            }

            transformations.put( s + 1, ( T ) absoluteTransformation.copy() );

        }

        // Generate fixedRAI
        //
        RandomAccessibleInterval fixedRAISequence = Views.stack( fixedRAIList );
        ImageJFunctions.show( fixedRAISequence, "reference region" );

        // Generate result
        //
        RandomAccessibleInterval transformedInputRAI =
                transformWholeInputRAI( transformations );

        ImageJFunctions.show( transformedInputRAI, "registered" );
    }

    public RandomAccessibleInterval< R > getFilteredImage()
    {
        /*
        if ( imageFilter != null )
        {
        ThresholdFilterView thresholdFilter = new ThresholdFilterView< R >( imageFilterParameters );

        RandomAccessibleInterval source = getTransformableRAI(
                0, fixedDimensionsReferenceCoordinates );

        RandomAccessibleInterval filtered = imageFilter.filter( source );

        return filtered;
        */
        return null;
    }

    private RandomAccessibleInterval createTransformedRAI(
            Map< Map< Integer, Long >, RandomAccessibleInterval < R > >  transformedSequenceMap,
            Map< Integer, Long > fixedDimensions,
            Map< Long, T > transformations )
    {
        if ( fixedDimensions.containsValue( null ) )
        {
            List < RandomAccessibleInterval< R > > dimensionCoordinateList = new ArrayList<>(  );

            for ( int d : fixedDimensions.keySet() )
            {
                if ( fixedDimensions.get( d ) == null )
                {
                    List < RandomAccessibleInterval< R > > sequenceCoordinateList = new ArrayList<>(  );
                    for ( long c = inputRAI.min( d ); c <= inputRAI.max( d ); ++c )
                    {
                        Map< Integer, Long > newFixedDimensions = new LinkedHashMap<>(fixedDimensions);
                        newFixedDimensions.put( d, c );

                        sequenceCoordinateList.add(
                                createTransformedRAI(
                                        transformedSequenceMap,
                                        newFixedDimensions,
                                        transformations ) );
                    }
                    dimensionCoordinateList.add(  Views.stack( sequenceCoordinateList ) );
                }
            }

            return Views.stack( dimensionCoordinateList );

        }
        else
        {
            List< RandomAccessibleInterval< R > > transformedRaiList = new ArrayList<>();

            for ( long s = inputRAI.min( sequenceDimension.dimension );
                  s <= inputRAI.max( sequenceDimension.dimension );
                  ++s )
            {
                if ( transformations.containsKey( s ) )
                {
                    transformedRaiList.add( getTransformedRAI( s, transformations.get( s ), fixedDimensions ) );
                }

            }

            // combine time-series of multiple channels into a channel stack
            RandomAccessibleInterval< R > transformedSequence = Views.stack( transformedRaiList );

            transformedSequenceMap.put( fixedDimensions, transformedSequence );

            return transformedSequence;
        }

    }

    private RandomAccessibleInterval getFixedRAI(
            long s,
            InvertibleRealTransform transform )
    {
        RandomAccessibleInterval rai = null;

        if ( transform == null || referenceType.equals( FIXED ) )
        {
            rai = getTransformableRAI( s, fixedDimensionsReferenceCoordinates );
            rai = Views.interval( rai, transformableDimensionsInterval );
        }
        else if (  transform != null && referenceType.equals( MOVING ) )
        {
            rai = getTransformableRAI( s, fixedDimensionsReferenceCoordinates );
            RandomAccessible ra = getTransformedRA( rai, transform );
            rai = Views.interval( ra, transformableDimensionsInterval );
        }

        return rai;
    }

    private RandomAccessible getMovingRA(
            long s,
            InvertibleRealTransform transform )
    {

        RandomAccessibleInterval rai = getTransformableRAI( s, fixedDimensionsReferenceCoordinates );
        RandomAccessible ra;

        if ( transform == null )
        {
            ra = Views.extendMirrorSingle( rai );
        }
        else
        {
            ra = getTransformedRA( rai, transform );
        }

        return ra;

    }

    private RandomAccessible getTransformedRA(
            RandomAccessibleInterval rai,
            InvertibleRealTransform transform )
    {

        RealRandomAccessible rra
                = RealViews.transform(
                    Views.interpolate( Views.extendBorder( rai ),
                            new NLinearInterpolatorFactory() ),
                                transform );

        RandomAccessible ra = Views.raster( rra );

        return ra;
    }

    private RandomAccessibleInterval getTransformedRAI(
            long s,
            InvertibleRealTransform transform,
            Map< Integer, Long > fixedDimensions )
    {
        RandomAccessibleInterval rai;

        rai = getTransformableRAI( s, fixedDimensions );
        rai = Views.interval( getTransformedRA( rai, transform ), rai );

        return rai;
    }

    private RandomAccessibleInterval transformWholeInputRAI(
            Map< Long, T > transformations )
    {

        Map< Map< Integer, Long >, RandomAccessibleInterval < R > >  transformedSequenceMap = new HashMap<>(  );
        Map< Integer, Long > fixedDimensions = new HashMap<>( fixedDimensionsReferenceCoordinates );
        for ( int d : fixedDimensions.keySet() )
        {
            fixedDimensions.put( d, null );
        }

        RandomAccessibleInterval transformedRAI = createTransformedRAI(
                transformedSequenceMap,
                fixedDimensions,
                transformations );

        // just for logging...
        for ( Map< Integer, Long > fixedCoordinates : transformedSequenceMap.keySet() )
        {
            IJ.log( "-- Transformed sequence at fixed dimensions:" );
            for ( Integer d : fixedCoordinates.keySet() )
            {
                IJ.log( "Dimension " + d + "; Coordinate " + fixedCoordinates.get( d ) );
            }
        }

        return transformedRAI;

    }

    private RandomAccessibleInterval getTransformableRAI(
            long s,
            Map< Integer, Long > fixedDimensions )
    {

        long[] min = Intervals.minAsLongArray( inputRAI );
        long[] max = Intervals.maxAsLongArray( inputRAI );

        min[ sequenceDimension.dimension ] = s;
        max[ sequenceDimension.dimension ] = s;

        for ( int d : fixedDimensions.keySet() )
        {
            min[ d ] = fixedDimensions.get( d );
            max[ d ] = fixedDimensions.get( d );
        }

        FinalInterval interval = new FinalInterval( min, max );

        RandomAccessibleInterval rai =
                Views.dropSingletonDimensions(
                        Views.interval( inputRAI, interval ) );

        return rai;

    }

    @Deprecated
    private void populateTransformedSeriesList(
            Map< Map< Integer, Long >, RandomAccessibleInterval < R > >  transformedSequenceMap,
            Map< Integer, Long > fixedDimensions,
            Map< Long, T > transformations )
    {
        if ( fixedDimensions.containsValue( null ) )
        {
            for ( int d : fixedDimensions.keySet() )
            {
                if ( fixedDimensions.get( d ) == null )
                {
                    for ( long c = inputRAI.min( d ); c <= inputRAI.max( d ); ++c )
                    {
                        Map< Integer, Long > newFixedDimensions = new LinkedHashMap<>(fixedDimensions);
                        newFixedDimensions.put( d, c );
                        populateTransformedSeriesList( transformedSequenceMap, newFixedDimensions, transformations );
                    }
                }
            }
        }
        else
        {
            List< RandomAccessibleInterval< R > > transformedRaiList = new ArrayList<>();

            for ( long s = inputRAI.min( sequenceDimension.dimension );
                  s <= inputRAI.max( sequenceDimension.dimension );
                  ++s )
            {
                if ( transformations.containsKey( s ) )
                {
                    transformedRaiList.add( getTransformedRAI( s, transformations.get( s ), fixedDimensions ) );
                }
            }

            // combine time-series of multiple channels into a channel stack
            RandomAccessibleInterval< R > transformedSequence = Views.stack( transformedRaiList );

            transformedSequenceMap.put( fixedDimensions, transformedSequence );

            return;
        }

    }

}

