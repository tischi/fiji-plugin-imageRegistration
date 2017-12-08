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
    TransformableDimensions transformableDimensions;
    FixedDimensions fixedDimensions;

    String registrationType;
    String registrationMethod;
    String referenceType;

    ExecutorService service;

    int numThreads;

    private class TransformableDimensions
    {
        public ArrayList< Integer > dimensions;
        public ArrayList< Long > refMin;
        public ArrayList< Long > refMax;

        FinalInterval referenceInterval;

        TransformableDimensions()
        {
            this.dimensions = new ArrayList<>(  );
            this.refMin = new ArrayList<>(  );
            this.refMax = new ArrayList<>(  );

        }

        FinalInterval getReferenceInterval()
        {
            return new FinalInterval( refMin.stream().mapToLong( l -> l).toArray(), refMax.stream().mapToLong( l -> l).toArray() );
        }
    }


    Map< Integer, Long > fixedDimensionsRef;


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
        this.numDimensions = input.numDimensions();

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

        FixedDimensions fixedDimensions = new FixedDimensions();
        TransformableDimensions transformableDimensions = new TransformableDimensions();

        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            switch ( dimensionTypes[ d ] )
            {
                case SEQUENCE_DIM:
                    sequenceDimension = new SequenceDimension( d, interval.min( d ), interval.max( d ) );
                    break;
                case TRANSFORMABLE_DIM:
                    transformableDimensions.dimensions.add( d );
                    transformableDimensions.refMin.add( interval.min( d ) );
                    transformableDimensions.refMax.add( interval.min( d ) );
                    break;
                case FIXED_DIM:
                    fixedDimensions.dimensions.add( d );
                    fixedDimensions.refCoordinates.add( interval.min( d ) );
                    break;
            }
        }

    }

    public void setSearchRadii( long[] searchRadii )
    {
        assert searchRadii.length == transformableDimensionsReferenceInterval.numDimensions();

        this.searchRadii = searchRadii;
    }


    public void computeTransforms()
    {

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;

        T absoluteTransformation = null;

        Map< Long, T > transformations = new HashMap<>(  );

        for ( long s = interval.min( sequenceDim );
              s <= interval.max( sequenceDim );
              s += ds )
        {
            fixedRAI = getFixedRAI( s, absoluteTransformation );
            movingRA = getMovingRA( s, absoluteTransformation );

            //ImageJFunctions.show( fixedRAI );

            T relativeTransformation = ( T ) TranslationPhaseCorrelation
                    .compute( fixedRAI, movingRA, searchRadii, service );

            if ( s != interval.min( sequenceDim ) )
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
                createTransformedSeries( transformations );

        ImageJFunctions.show( transformedSeries );
    }


    // TODO: deal with fixed dimensions
    private RandomAccessibleInterval createTransformedSeries(
            Map< Long, T > transformations )
    {

        RandomAccessibleInterval< R > transformedRAI = null;


        List< RandomAccessibleInterval< R > > transformedFixedDims = new ArrayList<>();

        for ( int f = 0; f < fixedDimensionsRef.size(); ++f )
        {

        }
        for ( int d : fixedDimensionsRef.keySet() ) // e.g. channel, let's ignore for now another fixed dimension.
        {
            Map< Integer, Long > fixedDimensions = new HashMap<>( fixedDimensionsRef );

            for( int d2 )

            getTransformedSequence( d, fixedDimensions.get( d ), transformations );

            List< RandomAccessibleInterval< R > > transformedSequences = new ArrayList<>();

            for ( long i = inputRAI.min( d ); i <= inputRAI.max( d ); ++i )
            {

                for ( long s = inputRAI.min( sequenceDim ); s < inputRAI.max( sequenceDim ); ++s )
                { // e.g. time loop
                    if ( transformations.containsKey( s ) )
                    {
                        transformed.add( getTransformedRAI( s, transformations.get( s ) ) );
                    }
                    else
                    {
                        transformed.add( getTransformableRAI( s ) );
                    }
                }

                // e.g. make time-series of this channel

                // e.g. add time series of this channel to a list (of multiple channels)
                transformedFixedDimSequences.add( transformedFixedDimSequence );
            }

            // combine time-series of multiple channels into a channel stack
            RandomAccessibleInterval< R > transformedFixedDim = Views.stack( transformedFixedDimSequences );

            // e.g. if there was another "fixed dimension", similar to channel,
            // this would be added here
            transformedFixedDims.add( transformedFixedDim );
        }

        // now that we transformed all fixed dims, we can construct the complete result
        transformedRAI = Views.stack( transformedFixedDims );

        return transformedRAI;

    }


    private void createTransformedSeriesList(
            ArrayList< RandomAccessibleInterval < R > > transformedSeriesList,
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
                        createTransformedSeriesList( transformedSeriesList, fixedDimensions, transformations );
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

            transformedSeriesList.add( transformedSequence );

            return;
        }

    }

    private RandomAccessibleInterval getTransformedSequence(
            int fixedDim,
            long fixedDimCoordinate,
            Map< Long, T > transformations )
    {

        List< RandomAccessibleInterval< R > > transformed = new ArrayList<>();

        for ( long s = inputRAI.min( sequenceDim ); s < inputRAI.max( sequenceDim ); ++s )
        { // e.g. time loop
            if ( transformations.containsKey( s ) )
            {
                transformed.add( getTransformedRAI( s, transformations.get( s ) ) );
            }
            else
            {
                transformed.add( getTransformableRAI( s ) );
            }
        }

        RandomAccessibleInterval< R > transformedFixedDimSequence
                = Views.stack( transformed );

        return transformedFixedDimSequence;
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

        if ( s == sRef || referenceType.equals( FIXED ) )
        {
            rai = getTransformableRAI( s, fixedDimsReferenceCoordinates );
            rai = Views.interval( rai, transformableDimensionsReferenceInterval );
        }
        else if ( s != sRef && referenceType.equals( MOVING ) )
        {
            rai = getTransformableRAI( s, fixedDimsReferenceCoordinates );
            RandomAccessible ra = getTransformedRA( rai, transform );
            rai = Views.interval( ra, transformableDimensionsReferenceInterval );
        }
        else
        {
            return null; // should not occur
        }

        // crop out reference region


        return rai;
    }

    public FinalInterval getTransformableDimensionsIntervals()
    {
        int n = getNumTransformableDimensions();

        long[] min = new long[n];
        long[] max = new long[n];

        int i = 0;
        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            if ( dimensionTypes[d] == TRANSFORMABLE_DIM )
            {
                min[ i ] = interval.min( d );
                max[ i ] = interval.max( d );
                i++;
            }
        }

        return new FinalInterval( min, max );

    }

    private RandomAccessible getMovingRA(
            long s,
            InvertibleRealTransform transform )
    {

        RandomAccessibleInterval rai = getTransformableRAI( s + ds );
        RandomAccessible ra;

        if ( s == sRef  )
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

    private int getNumTransformableDimensions( )
    {
        int n = 0;
        for ( int dimensionType : dimensionTypes )
        {
            if ( dimensionType == TRANSFORMABLE_DIM )
            {
               n++;
            }
        }

        return n;
    }

}

