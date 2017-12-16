package de.embl.cba.registration;

import de.embl.cba.registration.filter.ImageFilter;
import de.embl.cba.registration.filter.ImageFilterFactory;
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.transformationfinders.TransformationFinder;
import de.embl.cba.registration.transformationfinders.TransformationFinderFactory;
import de.embl.cba.registration.transformationfinders.TransformationFinderParameters;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.log.LogService;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRegistration
        < R extends RealType< R > & NativeType < R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable < T > > {


    final RandomAccessibleInterval< R > inputRAI;

    private final ImageFilter imageFilter;
    private final TransformationFinder transformationFinder;

    private final SequenceAxisSettings sequenceAxisProperties;
    private final TransformableAxesSettings transformableAxesSettings;
    private final FixedAxesSettings fixedAxesSettings;

//    private final Map< String, Object > imageFilterParameters;

    ReferenceRegionTypes referenceRegionType;

    ExecutorService executorService;
    private final boolean showFixedImageSequence;
    private final OutputViewIntervalSizeTypes outputViewIntervalSizeType;

    RandomAccessibleInterval referenceImageSequenceOutput;
    RandomAccessibleInterval transformedOutput;

    Map< Long, T > transformations;


    // TODO: read and write registrations:
    // - https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/XmlHelpers.java
    // - http://scijava.org/javadoc.scijava.org/Fiji/mpicbg/spim/data/registration/XmlIoViewRegistrations.html
    // - http://scijava.org/javadoc.scijava.org/Fiji/mpicbg/spim/data/registration/ViewRegistrations.html
    // - http://scijava.org/javadoc.scijava.org/Fiji/mpicbg/spim/data/registration/ViewRegistration.html
    // - https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/SpimDataExample2.java


    /**
     *
     * @param inputRAI
     * @param axisTypes
     * @param intervalInput
     * @param otherCoordinateInput
     * @param numThreads
     * @param imageFilterType
     * @param imageFilterParameters
     * @param outputViewIntervalSizeType
     * @param showFixedImageSequence
     */
    public ImageRegistration(
            final RandomAccessibleInterval< R > inputRAI,
            final RegistrationAxisTypes[] axisTypes,
            final FinalInterval intervalInput,
            Map< String, Object > imageFilterParameters,
            Map< String, Object > transformationParameters,
            int numThreads,
            final OutputViewIntervalSizeTypes outputViewIntervalSizeType,
            boolean showFixedImageSequence,
            LogService logService )
    {

        PackageLogService.logService = logService;

        this.outputViewIntervalSizeType = outputViewIntervalSizeType;
        this.showFixedImageSequence = showFixedImageSequence;

        this.referenceRegionType = ReferenceRegionTypes.Moving; // TODO: get from GUI

        this.inputRAI = inputRAI;
        int numDimensions = this.inputRAI.numDimensions();

        this.executorService = Executors.newFixedThreadPool( numThreads );

        // Init image filter
        //
        if ( imageFilterParameters.get( ImageFilterParameters.FILTER_TYPE ) != null )
        {
            imageFilterParameters.put( GlobalParameters.LOG_SERVICE, logService );
            imageFilterParameters.put( ImageFilterParameters.NUM_THREADS, numThreads );
            imageFilterParameters.put( ImageFilterParameters.EXECUTOR_SERVICE, executorService );
            this.imageFilter = ImageFilterFactory.create( imageFilterParameters );
        }
        else
        {
            this.imageFilter = null;
        }

        // Init transformation finder
        //
        transformationParameters.put( GlobalParameters.LOG_SERVICE, logService );
        transformationParameters.put( GlobalParameters.EXECUTOR_SERVICE, executorService );
        transformationParameters.put( TransformationFinderParameters.IMAGE_FILTER, imageFilter );

        transformationFinder = TransformationFinderFactory.create( transformationParameters );


        // Configure sequence axis
        //

        int sequenceDimension = Arrays.asList( axisTypes ).indexOf( RegistrationAxisTypes.Sequence );
        long sequenceAxisReferenceCoordinate = intervalInput.min( sequenceDimension );
        sequenceAxisProperties =
                new SequenceAxisSettings(
                        sequenceDimension,
                        intervalInput.min( sequenceDimension ),
                        intervalInput.max( sequenceDimension ),
                        sequenceAxisReferenceCoordinate);

        // Configure transformable axes
        //
        int numTransformableDimensions = Collections.frequency( Arrays.asList( axisTypes ), RegistrationAxisTypes.Transformable);

        int[] transformableDimensions = new int[ numTransformableDimensions ];
        long[] maximalDisplacements = new long[ numTransformableDimensions ];
        long[] referenceIntervalMin = new long[ numTransformableDimensions ];
        long[] referenceIntervalMax = new long[ numTransformableDimensions ];
        long[] transformableDimensionsInputIntervalMin = new long[ numTransformableDimensions ];
        long[] transformableDimensionsInputIntervalMax = new long[ numTransformableDimensions ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( axisTypes[ d ].equals( RegistrationAxisTypes.Transformable ) )
            {
                transformableDimensions[ i ] = d;
                referenceIntervalMin[ i ] = intervalInput.min( d );
                referenceIntervalMax[ i ] = intervalInput.max( d );
                transformableDimensionsInputIntervalMin[ i ] = this.inputRAI.min( d );
                transformableDimensionsInputIntervalMax[ i ] = this.inputRAI.max( d );
                ++i;
            }
        }

        FinalInterval transformableDimensionsReferenceInterval =
                new FinalInterval( referenceIntervalMin, referenceIntervalMax );
        FinalInterval transformableDimensionsFullInterval =
                new FinalInterval( transformableDimensionsInputIntervalMin, transformableDimensionsInputIntervalMax );

        transformableAxesSettings =
                new TransformableAxesSettings(
                        transformableDimensions,
                        transformableDimensionsReferenceInterval,
                        transformableDimensionsFullInterval
                );


        // Configure fixed axes
        //
        int numFixedDimensions = Collections.frequency( Arrays.asList( axisTypes ), RegistrationAxisTypes.Fixed );

        int[] fixedDimensions = new int[ numFixedDimensions ];
        long[] fixedDimensionsReferenceIntervalMin = new long[ numFixedDimensions ];
        long[] fixedDimensionsReferenceIntervalMax = new long[ numFixedDimensions ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( axisTypes[ d ].equals( RegistrationAxisTypes.Fixed ) )
            {
                fixedDimensions[ i ] = d;
                // Stored as an interval, although the code currently only supports one fixed coordinate.
                // However, one could imagine in the future to e.g. average channels or compute
                // average transformations taking information from multiple channels into account...
                fixedDimensionsReferenceIntervalMin[ i ] = intervalInput.min( d );
                fixedDimensionsReferenceIntervalMax[ i ] = intervalInput.max( d );
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
        executorService = Executors.newFixedThreadPool( numThreads );


    }

    public void run()
    {
        RandomAccessibleInterval fixedRAI;
        List< RandomAccessibleInterval < R > > fixedRAIList = new ArrayList<>(  );
        RandomAccessible movingRA;

        T absoluteTransformation = null;
        transformations = new HashMap<>(  );

        for ( long s = sequenceAxisProperties.min; s <= sequenceAxisProperties.max; s += 1 )
        {

            // Get next fixedRAI, transformed and potentially filtered
            //
            fixedRAI = getFixedRAI( s, transformations.get( s ), imageFilter );
            fixedRAIList.add( fixedRAI ); // not really used, just to show the user

            // Get next movingRA, transformed view,  but not filtered.
            // For performance reasons the potential filtering has to happen during the findTransform
            //
            movingRA = getMovingRA( s + 1, transformations.get( s ) );

            // Find transformation
            //
            T relativeTransformation = ( T ) transformationFinder
                    .findTransform(
                            fixedRAI,
                            movingRA,
                            executorService );

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
        referenceImageSequenceOutput = Views.stack( fixedRAIList );

        // Generate actual result, i.e. transform the whole input RAI
        //
        transformedOutput = transformWholeInputRAI( transformations );

    }

    public RandomAccessibleInterval getFixedSequenceOutput(
            ArrayList< Integer > axes )
    {
        for ( int a : transformableAxesSettings.axes )
        {
            axes.add( a );
        }

        axes.add( sequenceAxisProperties.axis );

        return referenceImageSequenceOutput;
    }

    public RandomAccessibleInterval getTransformedOutput(
            ArrayList< Integer > axes )
    {
        for ( int a : transformableAxesSettings.axes )
        {
            axes.add( a );
        }

        axes.add( sequenceAxisProperties.axis );

        for ( int a : fixedAxesSettings.axes )
        {
            axes.add( a );
        }

        return transformedOutput;
    }

    public FinalInterval getTransformableDimensionsOutputInterval()
    {
        if ( outputViewIntervalSizeType == OutputViewIntervalSizeTypes.InputDataSize )
        {
            return transformableAxesSettings.inputInterval;
        }
        else if ( outputViewIntervalSizeType == OutputViewIntervalSizeTypes.ReferenceRegionSize )
        {
            return transformableAxesSettings.referenceInterval;
        }
        else if ( outputViewIntervalSizeType == OutputViewIntervalSizeTypes.UnionSize )
        {
            return getTransformationsUnion( new FinalInterval( inputRAI ) );
        }
        else
        {
            return null;
        }
    }

    private FinalInterval getTransformationsUnion( FinalInterval inputInterval )
    {
        // TODO
        ArrayList< long[] > corner = getCorners( inputInterval );
        return null;
    }

    private ArrayList< long[] > getCorners( FinalInterval interval )
    {
        // init input for recursive corner determination
        //
        ArrayList< long [] > corners = new ArrayList<>(  );
        LinkedHashMap< Integer, String > dimensionMinMaxMap = new LinkedHashMap<>();
        for ( int d = 0; d < interval.numDimensions(); ++d )
            dimensionMinMaxMap.put( d, null );

        setCorners( dimensionMinMaxMap,
                corners, interval );

        return corners;

    }

    private void setCorners( LinkedHashMap< Integer, String > axisMinMaxMap,
                             ArrayList< long [] > corners,
                             FinalInterval interval )
    {
        int n = interval.numDimensions();

        if ( axisMinMaxMap.containsValue( null ) )
        {   // there are still dimensions with undetermined corners
            for ( int d : axisMinMaxMap.keySet() )
            {
                if( axisMinMaxMap.get( d ) == null )
                {
                    axisMinMaxMap.put( d, "min" );
                    setCorners( axisMinMaxMap,
                            corners,
                            interval );

                    axisMinMaxMap.put( d, "max" );
                    setCorners( axisMinMaxMap,
                            corners,
                            interval );

                }
            }
        }
        else
        {
            long [] corner = new long[ interval.numDimensions() ];

            for ( int axis : axisMinMaxMap.keySet() )
            {
                if ( axisMinMaxMap.get( axis ).equals( "min" ) )
                {
                    corner[ axis ] = interval.min( axis );
                }
                else if ( axisMinMaxMap.get( axis ).equals( "max" ) )
                {
                    corner[ axis ] = interval.max( axis );
                }
            }

            corners.add( corner );

        }
    }

    private RandomAccessibleInterval createTransformedInputRAISequence(
            Map< Map< Integer, Long >, RandomAccessibleInterval < R > >  transformedSequenceMap,
            Map< Integer, Long > dimensionCoordinateMap,
            Map< Long, T > transformations )
    {
        if ( dimensionCoordinateMap.containsValue( null ) )
        {
            List < RandomAccessibleInterval< R > > dimensionCoordinateRAIList = new ArrayList<>(  );

            for ( int d : dimensionCoordinateMap.keySet() )
            {
                if ( dimensionCoordinateMap.get( d ) == null )
                {
                    List < RandomAccessibleInterval< R > > sequenceCoordinateRAIList = new ArrayList<>(  );
                    for ( long c = inputRAI.min( d ); c <= inputRAI.max( d ); ++c )
                    {
                        Map< Integer, Long > newFixedDimensions =
                                new LinkedHashMap<>( dimensionCoordinateMap );

                        newFixedDimensions.put( d, c );

                        sequenceCoordinateRAIList.add(
                                createTransformedInputRAISequence(
                                        transformedSequenceMap,
                                        newFixedDimensions,
                                        transformations ) );

                    }
                    dimensionCoordinateRAIList.add(  Views.stack( sequenceCoordinateRAIList ) );
                }
            }

            return Views.stack( dimensionCoordinateRAIList );

        }
        else
        {
            List< RandomAccessibleInterval< R > > transformedRAIList = new ArrayList<>();

            for ( long s = inputRAI.min( sequenceAxisProperties.axis );
                  s <= inputRAI.max( sequenceAxisProperties.axis );
                  ++s )
            {
                if ( transformations.containsKey( s ) )
                {

                    transformedRAIList.add(
                            getTransformedRAI(
                                    s,
                                    transformations.get( s ),
                                    dimensionCoordinateMap,
                                    getTransformableDimensionsOutputInterval()
                                    ) );
                }

            }

            // combine time-series of multiple channels into a channel stack
            RandomAccessibleInterval< R > transformedSequence = Views.stack( transformedRAIList );

            transformedSequenceMap.put( dimensionCoordinateMap, transformedSequence );

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
            rai = getTransformableRAI( s, fixedAxesSettings.getReferenceAxisCoordinateMap() );
            rai = Views.interval( rai, transformableAxesSettings.referenceInterval );
        }
        else if (  transform != null && referenceRegionType == ReferenceRegionTypes.Moving )
        {
            rai = getTransformableRAI( s, fixedAxesSettings.getReferenceAxisCoordinateMap() );
            RandomAccessible ra = ImageRegistrationUtils.getTransformedRA( rai, transform );
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
                        fixedAxesSettings.getReferenceAxisCoordinateMap() );

        RandomAccessible ra;

        if ( transform == null )
        {
            ra = Views.extendMirrorSingle( rai );
        }
        else
        {
            ra = ImageRegistrationUtils.getTransformedRA( rai, transform );
        }

        return ra;

    }

    private RandomAccessibleInterval getTransformedRAI(
            long s,
            InvertibleRealTransform transform,
            Map< Integer, Long > fixedDimensions,
            FinalInterval transformableDimensionsInterval )
    {
        RandomAccessibleInterval rai;

        rai = getTransformableRAI( s, fixedDimensions );

        rai = Views.interval(
                ImageRegistrationUtils.getTransformedRA( rai, transform ),
                transformableDimensionsInterval );

        return rai;
    }

    private RandomAccessibleInterval transformWholeInputRAI(
            Map< Long, T > transformations )
    {

        // For each combination of the fixed axes ( Map< Integer, Long > )
        // generate a transformed RAI sequence
        Map< Map< Integer, Long >, RandomAccessibleInterval < R > >
                transformedSequenceMap = new HashMap<>(  );

        // Fixed axes map.
        // This serves to indicate whether the axis has been transformed already or not (null).
        Map< Integer, Long > fixedDimensions =
                new HashMap<>( fixedAxesSettings.getReferenceAxisCoordinateMap() );

        for ( int d : fixedDimensions.keySet() )
        {
            fixedDimensions.put( d, null );
        }

        RandomAccessibleInterval transformedRAI =
                createTransformedInputRAISequence(
                    transformedSequenceMap,
                    fixedDimensions,
                    transformations );

        // just for logging...
        //
        for ( Map< Integer, Long > fixedCoordinates : transformedSequenceMap.keySet() )
        {
            PackageLogService.logService.info( "-- Transformed sequence at fixed axes:" );
            for ( Integer d : fixedCoordinates.keySet() )
            {
                PackageLogService.logService.info( "Dimension " + d + "; Coordinate " + fixedCoordinates.get( d ) );
            }
        }

        return Views.dropSingletonDimensions( transformedRAI );

    }

    /**
     * Returns a RandomAccessibleInterval which has the dimensionality
     * of number of transformable axes, where the sequence axis s
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

        min[ sequenceAxisProperties.axis ] = s;
        max[ sequenceAxisProperties.axis ] = s;

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

    private class SequenceAxisSettings
    {
        long min;
        long max;
        long ref;
        int axis;

        SequenceAxisSettings( int d, long min, long max, long ref )
        {
            this.axis = d;
            this.min = min;
            this.max = max;
            this.ref = ref;
        }

    }

    private class TransformableAxesSettings
    {
        int[] axes;
        FinalInterval referenceInterval;
        FinalInterval inputInterval;
//        long[] maximalDisplacements;

        TransformableAxesSettings(
                int[] dimensions,
                FinalInterval referenceInterval,
                FinalInterval inputInterval )
//                long[] maximalDisplacements )
        {
            this.axes = dimensions;
            this.referenceInterval = referenceInterval;
            this.inputInterval = inputInterval;
//            this.maximalDisplacements = maximalDisplacements;
        }

    }

    /**
     *  Important: the referenceInterval has the dimensionality of numFixedAxes
     *  maybe this is not good
     */
    private class FixedAxesSettings
    {
        int[] axes;
        FinalInterval referenceInterval;

        FixedAxesSettings(
                int[] dimensions,
                FinalInterval referenceInterval )
        {
            this.axes = dimensions;
            this.referenceInterval = referenceInterval;
        }

        public long min( int d )
        {
            int i = Arrays.asList( axes ).indexOf( d );
            return referenceInterval.min( i );
        }

        public long max( int d )
        {
            int i = Arrays.asList( axes ).indexOf( d );
            return referenceInterval.max( i );
        }

        public HashMap< Integer, Long > getReferenceAxisCoordinateMap()
        {
            HashMap< Integer, Long > map = new HashMap<>(  );

            for ( int i = 0; i < axes.length; ++i )
            {
                map.put( axes[ i ], referenceInterval.min( i ) );
            }

            return map;
        }


    }


    /*
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

            for ( long s = inputRAI.min( sequenceAxisProperties.axis );
                  s <= inputRAI.max( sequenceAxisProperties.axis );
                  ++s )
            {
                if ( transformations.containsKey( s ) )
                {
                    transformedRaiList.add(
                            getTransformedRAI(
                                    s,
                                    transformations.get( s ),
                                    fixedDimensions ) );
                }
            }

            // combine time-series of multiple channels into a channel stack
            RandomAccessibleInterval< R > transformedSequence = Views.stack( transformedRaiList );

            transformedSequenceMap.put( fixedDimensions, transformedSequence );

            return;
        }

    }*/

}

