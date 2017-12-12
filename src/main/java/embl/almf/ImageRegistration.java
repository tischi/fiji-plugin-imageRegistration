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


    final RandomAccessibleInterval< R > inputRAI;

    private final ImageFilter imageFilter;


    private final SequenceAxisSettings sequenceAxisProperties;
    private final TransformableAxesSettings transformableAxesSettings;
    private final FixedAxesSettings fixedAxesSettings;

    private final Map< String, Object > imageFilterParameters;

    ReferenceRegionTypes referenceRegionType;

    ExecutorService service;
    private final boolean showFixedImageSequence;


    private class SequenceAxisSettings
    {
        int dimension;
        long min;
        long max;
        long ref;

        SequenceAxisSettings( int d, long min, long max, long ref )
        {
            this.dimension = d;
            this.min = min;
            this.max = max;
            this.ref = ref;
        }

    }

    private class TransformableAxesSettings
    {
        int[] dimensions;
        FinalInterval referenceInterval;
        long[] maximalDisplacements;

        TransformableAxesSettings(
                int[] dimensions,
                FinalInterval referenceInterval,
                long[] maximalDisplacements )
        {
            this.dimensions = dimensions;
            this.referenceInterval = referenceInterval;
            this.maximalDisplacements = maximalDisplacements;
        }

    }

    /**
     *  Important: the referenceInterval has the dimensionality of numFixedAxes
     *  maybe this is not good
     */
    private class FixedAxesSettings
    {
        int[] dimensions;
        FinalInterval referenceInterval;

        FixedAxesSettings(
                int[] dimensions,
                FinalInterval referenceInterval )
        {
            this.dimensions = dimensions;
            this.referenceInterval = referenceInterval;
        }

        public long min( int d )
        {
            int i = Arrays.asList( dimensions ).indexOf( d );
            return referenceInterval.min( i );
        }

        public long max( int d )
        {
            int i = Arrays.asList( dimensions ).indexOf( d );
            return referenceInterval.max( i );
        }

        public HashMap< Integer, Long > getDimensionCoordinateMap()
        {
            HashMap< Integer, Long > map = new HashMap<>(  );

            for ( int i = 0; i < dimensions.length; ++i )
            {
                map.put( dimensions[ i ], referenceInterval.min( i ) );
            }

            return map;
        }


    }


    // TODO: read and write registrations:
    // - https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/XmlHelpers.java
    // - http://scijava.org/javadoc.scijava.org/Fiji/mpicbg/spim/data/registration/XmlIoViewRegistrations.html
    // - http://scijava.org/javadoc.scijava.org/Fiji/mpicbg/spim/data/registration/ViewRegistrations.html
    // - http://scijava.org/javadoc.scijava.org/Fiji/mpicbg/spim/data/registration/ViewRegistration.html
    // - https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/SpimDataExample2.java

    public ImageRegistration(
            final RandomAccessibleInterval< R > input,
            final AxisTypes[] axisTypes,
            final FinalInterval intervalInput,
            final long[] otherCoordinateInput,
            int numThreads,
            final ImageFilterType imageFilterType,
            final Map< String, Object > imageFilterParameters,
            boolean showFixedImageSequence )
    {
        this.showFixedImageSequence = showFixedImageSequence;

        referenceRegionType = ReferenceRegionTypes.Moving; // TODO: get from GUI

        this.inputRAI = input;
        int numDimensions = inputRAI.numDimensions();

        this.service = Executors.newFixedThreadPool( numThreads );

        if ( imageFilterType != null )
        {
            this.imageFilter = ImageFilterFactory.create( imageFilterType, imageFilterParameters );
            this.imageFilterParameters = imageFilterParameters;
            this.imageFilterParameters.put( ImageFilterParameters.NUM_THREADS, numThreads );
            this.imageFilterParameters.put( ImageFilterParameters.EXECUTOR_SERVICE, service );
        }
        else
        {
            this.imageFilter = null;
            this.imageFilterParameters = null;
        }

        // Configure sequence axis
        //
        int sequenceDimension = Arrays.asList( axisTypes ).indexOf( AxisTypes.SEQUENCE_DIMENSION );
        sequenceAxisProperties =
                new SequenceAxisSettings(
                        sequenceDimension,
                        intervalInput.min( sequenceDimension ),
                        intervalInput.max( sequenceDimension ),
                        otherCoordinateInput[ sequenceDimension ]);


        // Configure transformable axes
        //
        int numTransformableDimensions = Collections.frequency( Arrays.asList( axisTypes ), AxisTypes.TRANSFORMABLE_DIMENSION);

        int[] transformableDimensions = new int[ numTransformableDimensions ];
        long[] maximalDisplacements = new long[ numTransformableDimensions ];
        long[] referenceIntervalMin = new long[ numTransformableDimensions ];
        long[] referenceIntervalMax = new long[ numTransformableDimensions ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( axisTypes[ d ].equals( AxisTypes.TRANSFORMABLE_DIMENSION ) )
            {
                transformableDimensions[ i ] = d;
                referenceIntervalMin[ i ] = intervalInput.min( d );
                referenceIntervalMax[ i ] = intervalInput.max( d );
                maximalDisplacements[ i ] = otherCoordinateInput[ d ];
                ++i;
            }
        }
        FinalInterval referenceInterval = new FinalInterval( referenceIntervalMin, referenceIntervalMax );
        transformableAxesSettings =
                new TransformableAxesSettings(
                        transformableDimensions,
                        referenceInterval,
                        maximalDisplacements
                );


        // Configure fixed axes
        //
        int numFixedDimensions = Collections.frequency( Arrays.asList( axisTypes ), AxisTypes.FIXED_DIMENSION );

        int[] fixedDimensions = new int[ numFixedDimensions ];
        long[] fixedDimensionsReferenceIntervalMin = new long[ numFixedDimensions ];
        long[] fixedDimensionsReferenceIntervalMax = new long[ numFixedDimensions ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( axisTypes[ d ].equals( AxisTypes.FIXED_DIMENSION ) )
            {
                fixedDimensions[ i ] = d;
                // Stored as an interval, although the code currently only supports one fixed coordinate.
                // However, one could imagine in the future to e.g. average channels or compute
                // some combined registration taking information from multiple channels into account...
                fixedDimensionsReferenceIntervalMin[ i ] = otherCoordinateInput[ d ];
                fixedDimensionsReferenceIntervalMax[ i ] = otherCoordinateInput[ d ];
                ++i;
            }
        }
        FinalInterval fixedDimensionsReferenceInterval =
                new FinalInterval(
                        fixedDimensionsReferenceIntervalMin,
                        fixedDimensionsReferenceIntervalMax );

        fixedAxesSettings =
                new FixedAxesSettings(
                        fixedDimensions,
                        fixedDimensionsReferenceInterval
                );


        // Configure multi-threading
        //
        service = Executors.newFixedThreadPool( numThreads );

    }

    public void run()
    {
        RandomAccessibleInterval fixedRAI;
        List< RandomAccessibleInterval < R > > fixedRAIList = new ArrayList<>(  );
        RandomAccessible movingRA;


        T absoluteTransformation = null;

        Map< Long, T > transformations = new HashMap<>(  );

        for ( long s = sequenceAxisProperties.min; s <= sequenceAxisProperties.max; s += 1 )
        {

            // Get next fixedRAI, transformed and potentially filtered
            //
            fixedRAI = getFixedRAI( s, transformations.get( s ), imageFilter );
            fixedRAIList.add( fixedRAI ); // not used, just to show the user

            // Get next movingRA, transformed view,  but not filtered.
            // For performance reasons the potential filtering has to happen during the findTransform
            //
            movingRA = getMovingRA( s + 1, transformations.get( s ) );

            // Find transformation
            //
            T relativeTransformation = ( T ) TranslationPhaseCorrelation
                    .findTransform(
                            fixedRAI,
                            movingRA,
                            transformableAxesSettings.maximalDisplacements,
                            imageFilter,
                            service );

            // Store transformation
            //
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

        // Generate fixedRAI output sequence for the user to check if everything worked well
        //
        RandomAccessibleInterval fixedRAISequence = Views.stack( fixedRAIList );
        ImageJFunctions.show( fixedRAISequence, "reference region" );

        // Generate actual result, i.e. transform the whole input RAI
        //
        RandomAccessibleInterval transformedInputRAI =
                transformWholeInputRAI( transformations );

        ImageJFunctions.show( transformedInputRAI, "registered" );
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

            for ( long s = inputRAI.min( sequenceAxisProperties.dimension );
                  s <= inputRAI.max( sequenceAxisProperties.dimension );
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
            InvertibleRealTransform transform,
            ImageFilter imageFilter )
    {
        RandomAccessibleInterval rai = null;

        if ( transform == null || referenceRegionType == ReferenceRegionTypes.Fixed )
        {
            rai = getTransformableRAI( s, fixedAxesSettings.getDimensionCoordinateMap() );
            rai = Views.interval( rai, transformableAxesSettings.referenceInterval );
        }
        else if (  transform != null && referenceRegionType == ReferenceRegionTypes.Moving )
        {
            rai = getTransformableRAI( s, fixedAxesSettings.getDimensionCoordinateMap() );
            RandomAccessible ra = getTransformedRA( rai, transform );
            rai = Views.interval( ra, transformableAxesSettings.referenceInterval );
        }

        if ( imageFilter != null )
        {
            rai = imageFilter.filter( rai );
        }

        return rai;
    }

    private RandomAccessible getMovingRA(
            long s,
            InvertibleRealTransform transform )
    {

        RandomAccessibleInterval rai =
                getTransformableRAI(
                        s,
                        fixedAxesSettings.getDimensionCoordinateMap() );

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


        // For each combination of the fixed dimensions ( Map< Integer, Long > )
        // generate a transformed RAI sequence
        Map< Map< Integer, Long >, RandomAccessibleInterval < R > >  transformedSequenceMap = new HashMap<>(  );

        // Fixed dimensions map.
        // This serves to indicate whether the dimension has been transformed already or not (null).
        Map< Integer, Long > fixedDimensions = new HashMap<>( fixedAxesSettings.getDimensionCoordinateMap() );
        for ( int d : fixedDimensions.keySet() )
        {
            fixedDimensions.put( d, null );
        }

        RandomAccessibleInterval transformedRAI =
                createTransformedRAI(
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

    /**
     * Returns a RandomAccessibleInterval which has the dimensionality
     * of number of transformable dimensions, where the sequence dimension s
     * and the fixedDimensions will be set (and dropped) as specified in the input.
     * @param s
     * @param fixedDimensions
     * @return
     */
    private RandomAccessibleInterval getTransformableRAI(
            long s,
            Map< Integer, Long > fixedAxesDimensionCoordinateMap )
    {

        long[] min = Intervals.minAsLongArray( inputRAI );
        long[] max = Intervals.maxAsLongArray( inputRAI );

        min[ sequenceAxisProperties.dimension ] = s;
        max[ sequenceAxisProperties.dimension ] = s;

        for ( int d : fixedAxesDimensionCoordinateMap.keySet() )
        {
            min[ d ] = fixedAxesDimensionCoordinateMap.get( d );
            max[ d ] = fixedAxesDimensionCoordinateMap.get( d );
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

            for ( long s = inputRAI.min( sequenceAxisProperties.dimension );
                  s <= inputRAI.max( sequenceAxisProperties.dimension );
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

