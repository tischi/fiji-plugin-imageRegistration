package embl.almf;

import embl.almf.registration.TranslationPhaseCorrelation;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRegistration
        < R extends RealType< R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable < T > > {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";
    final static String FIXED = "Fixed";

    public final static int SEQUENCE_DIM = 0;
    public final static int TRANSFORMABLE_DIM = 1;
    public final static int FIXED_DIM = 2;

    RandomAccessibleInterval< R > inputRAI;

    private long[] searchRadii;

    SequenceDimension sequenceDimension;
    Map< Integer, long[] > transformableDimensions;
    FinalInterval transformableDimensionsInterval;
    Map< Integer, Long > fixedDimensionsReferenceCoordinates;

    String registrationType;
    String registrationMethod;
    String referenceType;

    ExecutorService service;

    int numThreads;



    private FinalInterval setTransformableDimensionsReferenceInterval()
    {
        int n = transformableDimensions.size();

        long[] min = new long[n];
        long[] max = new long[n];

        int i = 0;
        for ( int d = 0; d < transformableDimensions.size(); ++d )
        {
            min[ i ] = transformableDimensions.get( d )[0];
            max[ i ] = transformableDimensions.get( d )[1];
        }
        return new FinalInterval( min, max );

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
            this.max = min;

        }

    }


    ImageRegistration( RandomAccessibleInterval< R > input )
    {
        this.inputRAI = input;

        // set defaults
        numThreads = 1;
        service = Executors.newFixedThreadPool( numThreads );
        referenceType = MOVING;

    }

    public void setNumThreads( int n )
    {
        numThreads = n;
        service = Executors.newFixedThreadPool( numThreads );
    }

    public void setDimensionTypesAndInterval( int[] dimensionTypes,
                                              FinalInterval interval )
    {

        transformableDimensions = new LinkedHashMap<>();
        fixedDimensionsReferenceCoordinates = new HashMap<>(  );

        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            switch ( dimensionTypes[ d ] )
            {
                case SEQUENCE_DIM:
                    sequenceDimension = new SequenceDimension( d, interval.min( d ), interval.max( d ) );
                    break;
                case TRANSFORMABLE_DIM:
                    transformableDimensions.put( d, new long[]{ interval.min( d ), interval.max( d ) });
                    break;
                case FIXED_DIM:
                    fixedDimensionsReferenceCoordinates.put( d, interval.min( d ) );
                    break;
            }
        }

        setTransformableDimensionsReferenceInterval();

    }

    public void setSearchRadii( long[] searchRadii )
    {
        assert searchRadii.length == transformableDimensions.size();

        this.searchRadii = searchRadii;
    }


    public void computeTransforms()
    {

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;

        T absoluteTransformation = null;

        Map< Long, T > transformations = new HashMap<>(  );

        for ( long s = sequenceDimension.min;
              s <= sequenceDimension.max;
              s += 1 )
        {
            fixedRAI = getFixedRAI( s, absoluteTransformation );
            movingRA = getMovingRA( s + 1, absoluteTransformation );

            //ImageJFunctions.show( fixedRAI );

            T relativeTransformation = ( T ) TranslationPhaseCorrelation
                    .compute( fixedRAI, movingRA, searchRadii, service );

            if ( s != sequenceDimension.min )
            {
                absoluteTransformation.preConcatenate( relativeTransformation );
                int a = 1;
            }
            else
            {
                absoluteTransformation = relativeTransformation;
                int a = 1;
            }

            transformations.put( s + 1, ( T ) absoluteTransformation.copy() );

        }

        RandomAccessibleInterval transformedSeries =
                createTransformedInputRAI( transformations );

        ImageJFunctions.show( transformedSeries );
    }


    // TODO: deal with fixed dimensions
    private RandomAccessibleInterval createTransformedInputRAI(
            Map< Long, T > transformations )
    {

        Map< Map< Integer, Long >, RandomAccessibleInterval < R > >  transformedSequenceMap = new HashMap<>(  );
        Map< Integer, Long > fixedDimensions = new HashMap<>( fixedDimensionsReferenceCoordinates );
        for ( int d : fixedDimensions.keySet() )
        {
            fixedDimensions.put( d, null );
        }

        populateTransformedSeriesList(
                transformedSequenceMap,
                fixedDimensions,
                transformations );

        return null;

    }


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
                        fixedDimensions.put( d, c );
                        populateTransformedSeriesList( transformedSequenceMap, fixedDimensions, transformations );
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

    private RandomAccessibleInterval getFixedRAI(
            long s,
            InvertibleRealTransform transform )
    {
        RandomAccessibleInterval rai;

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
        else
        {
            return null; // should not occur
        }

        // crop out reference region


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
    
}

