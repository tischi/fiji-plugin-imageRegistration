package de.embl.cba;

import de.embl.cba.filter.ImageFilter;
import de.embl.cba.filter.ImageFilterFactory;
import de.embl.cba.filter.ImageFilterParameters;
import de.embl.cba.filter.ImageFilterType;
import de.embl.cba.registration.TranslationPhaseCorrelation;
import ij.IJ;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.transform.integer.BoundingBox;
import net.imglib2.transform.integer.BoundingBoxTransform;
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

    public ImageRegistration(
            final RandomAccessibleInterval< R > input,
            final RegistrationAxisTypes[] axisTypes,
            final FinalInterval intervalInput,
            final long[] otherCoordinateInput,
            int numThreads,
            final ImageFilterType imageFilterType,
            final Map< String, Object > imageFilterParameters,
            final OutputViewIntervalSizeTypes outputViewIntervalSizeType,
            boolean showFixedImageSequence )
    {
        this.outputViewIntervalSizeType = outputViewIntervalSizeType;
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
        int sequenceDimension = Arrays.asList( axisTypes ).indexOf( RegistrationAxisTypes.Sequence );
        sequenceAxisProperties =
                new SequenceAxisSettings(
                        sequenceDimension,
                        intervalInput.min( sequenceDimension ),
                        intervalInput.max( sequenceDimension ),
                        otherCoordinateInput[ sequenceDimension ]);


        // Configure transformable axes
        //
        int numTransformableDimensions = Collections.frequency( Arrays.asList( axisTypes ), RegistrationAxisTypes.Transformable);

        int[] transformableDimensions = new int[ numTransformableDimensions ];
        long[] maximalDisplacements = new long[ numTransformableDimensions ];
        long[] referenceIntervalMin = new long[ numTransformableDimensions ];
        long[] referenceIntervalMax = new long[ numTransformableDimensions ];
        long[] inputIntervalMin = new long[ numTransformableDimensions ];
        long[] inputIntervalMax = new long[ numTransformableDimensions ];

        for ( int d = 0, i = 0; d < numDimensions; ++d )
        {
            if ( axisTypes[ d ].equals( RegistrationAxisTypes.Transformable ) )
            {
                transformableDimensions[ i ] = d;
                referenceIntervalMin[ i ] = intervalInput.min( d );
                referenceIntervalMax[ i ] = intervalInput.max( d );
                inputIntervalMin[ i ] = inputRAI.min( d );
                inputIntervalMax[ i ] = inputRAI.max( d );
                maximalDisplacements[ i ] = otherCoordinateInput[ d ];
                ++i;
            }
        }

        FinalInterval referenceInterval = new FinalInterval( referenceIntervalMin, referenceIntervalMax );
        FinalInterval inputInterval = new FinalInterval( inputIntervalMin, inputIntervalMax );

        transformableAxesSettings =
                new TransformableAxesSettings(
                        transformableDimensions,
                        referenceInterval,
                        inputInterval,
                        maximalDisplacements
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
        transformations = new HashMap<>(  );

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
        referenceImageSequenceOutput = Views.stack( fixedRAIList );

        // Generate actual result, i.e. transform the whole input RAI
        //
        transformedOutput = transformWholeInputRAI( transformations );

    }

    public RandomAccessibleInterval getFixedSequenceOutput(
            ArrayList< Integer > axes
    )
    {
        for ( int a : transformableAxesSettings.axes )
        {
            axes.add( a );
        }

        axes.add( sequenceAxisProperties.axis );

        return referenceImageSequenceOutput;
    }

    public RandomAccessibleInterval getTransformedOutput(
            ArrayList< Integer > axes
    )
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
            for ( long s = sequenceAxisProperties.min; s <= sequenceAxisProperties.max; ++s )
            {

                /*
                for ( int d = 0; d < transformableAxesSettings.axes.length; ++d )
                {

                    transformableAxesSettings.inputInterval

                }
                BoundingBox boundingBox = new BoundingBox( new long[3], new long[3] );
                boundingBox.getInterval();

                BoundingBox boundingBox = new BoundingBox( );

                                getTransformableRAI( s,
                                        fixedAxesSettings.getReferenceAxisCoordinateMap() )
                        );

                double[] transformedCorner1 = new double[ transformableAxesSettings.axes.length ];

                transformations.get( s ).apply( boundingBox.corner1, transformedCorner1 );
                */

            }
            return null;
        }
    }


    private void setCorners( LinkedHashMap< Integer, String > dimensionMinMaxMap,
                             ArrayList< long [] > corners,
                             FinalInterval interval )
    {
        int n = interval.numDimensions();

        if ( dimensionMinMaxMap.containsValue( null ) )
        {   // there are still dimensions with undetermined corners
            for ( int d : dimensionMinMaxMap.keySet() )
            {
                if( dimensionMinMaxMap.get( d ) == null )
                {
                    dimensionMinMaxMap.put( d, "min" );
                    setCorners( dimensionMinMaxMap,
                            corners,
                            interval );

                    dimensionMinMaxMap.put( d, "max" );
                    setCorners( dimensionMinMaxMap,
                            corners,
                            interval );

                }
            }
        }
        else
        {
            long [] corner = new long[ interval.numDimensions() ];

            Set axes = dimensionMinMaxMap.keySet();

            for ( int i = 0; i < interval.numDimensions(); ++i )
            {
                int d = axes.get( i );

                if ( dimensionMinMaxMap.get( d).equals( "min" ) )
                {
                    corner[ d ] = interval.min( d );
                }
                else if ( dimensionMinMaxMap.get( d).equals( "max" ) )
                {
                    corner[ d ] = interval.max( d );
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
                        fixedAxesSettings.getReferenceAxisCoordinateMap() );

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
            Map< Integer, Long > fixedDimensions,
            FinalInterval transformableDimensionsInterval )
    {
        RandomAccessibleInterval rai;

        rai = getTransformableRAI( s, fixedDimensions );

        rai = Views.interval(
                getTransformedRA( rai, transform ),
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
            IJ.log( "-- Transformed sequence at fixed axes:" );
            for ( Integer d : fixedCoordinates.keySet() )
            {
                IJ.log( "Dimension " + d + "; Coordinate " + fixedCoordinates.get( d ) );
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
        long[] maximalDisplacements;

        TransformableAxesSettings(
                int[] dimensions,
                FinalInterval referenceInterval,
                FinalInterval inputInterval,
                long[] maximalDisplacements )
        {
            this.axes = dimensions;
            this.referenceInterval = referenceInterval;
            this.inputInterval = inputInterval;
            this.maximalDisplacements = maximalDisplacements;
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

    private FinalInterval getInputRAITransformableDimensionsInterval()
    {

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

