package de.embl.cba.registration;

import de.embl.cba.registration.axessettings.AxesSettings;
import de.embl.cba.registration.filter.ImageFilter;
import de.embl.cba.registration.filter.ImageFilterFactory;
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.transformationfinders.TransformationFinder;
import de.embl.cba.registration.transformationfinders.TransformationFinderFactory;
import de.embl.cba.registration.transformationfinders.TransformationFinderParameters;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.img.Img;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.log.LogService;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.embl.cba.registration.LogServiceImageRegistration.*;

public class Registration
        < R extends RealType< R > & NativeType < R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable < T > > {

    final RandomAccessibleInterval< R > input;
    RandomAccessibleInterval< R > outputRAI;

    private final ImageFilter imageFilter;
    private final TransformationFinder transformFinder;

    private final AxesSettings axesSettings;

//    private final Map< String, Object > imageFilterParameters;

    ReferenceRegionType referenceRegionType;

    ExecutorService executorService;
    private final boolean showFixedImageSequence;
    private final OutputIntervalType outputViewIntervalSizeType;

    Map< Long, T > transformations;

    final Dataset dataset;
    final DatasetService datasetService;

    final InputImageViews inputImageViews;
    private final List< RandomAccessibleInterval< R > > fixedRAIList;


    public Registration(
            final Dataset dataset,
            final DatasetService datasetService,
            final RegistrationAxisTypes[] registrationAxisTypes,
            final FinalInterval registrationAxesInterval,
            Map<String, Object> imageFilterParameters,
            Map<String, Object> transformationParameters,
            int numThreads,
            final OutputIntervalType outputViewIntervalSizeType,
            boolean showFixedImageSequence,
            LogService logService )
    {

        LogServiceImageRegistration.logService = logService;

        this.dataset = dataset;
        this.datasetService = datasetService;
        this.input = ( RandomAccessibleInterval<R> ) dataset;
        this.axesSettings = new AxesSettings( dataset, registrationAxisTypes, registrationAxesInterval );
        this.inputImageViews = new InputImageViews( input, axesSettings, datasetService );
        this.outputViewIntervalSizeType = outputViewIntervalSizeType;
        this.showFixedImageSequence = showFixedImageSequence;
        this.referenceRegionType = ReferenceRegionType.Moving; // TODO: get from GUI
        this.fixedRAIList = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool( numThreads );

        imageFilterParameters.put( GlobalParameters.LOG_SERVICE, logService );
        imageFilterParameters.put( ImageFilterParameters.NUM_THREADS, numThreads );
        imageFilterParameters.put( ImageFilterParameters.EXECUTOR_SERVICE, executorService );
        this.imageFilter = ImageFilterFactory.create( imageFilterParameters );

        transformationParameters.put( GlobalParameters.LOG_SERVICE, logService );
        transformationParameters.put( GlobalParameters.EXECUTOR_SERVICE, executorService );
        transformationParameters.put( TransformationFinderParameters.IMAGE_FILTER, imageFilter );
        this.transformFinder = TransformationFinderFactory.create( transformationParameters );

    }

    public void run()
    {
        long startTimeMilliseconds = start( "# Finding transforms..." );

        initializeTransforms();

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;
        for ( long s = axesSettings.sequenceMin(); s < axesSettings.sequenceMax(); s += axesSettings.sequenceIncrement() )
        {
            showStatus( s );

            fixedRAI = fixedRAI( s );
            movingRA = movingRA( s + 1 );

            T transform = ( T ) transformFinder.findTransform( fixedRAI, movingRA );

            addTransform( s, transform );
            addFixedRAI( fixedRAI );
        }

        outputRAI = inputImageViews.transformedInput( transformations );

        doneInDuration( startTimeMilliseconds );

    }

    private List< RandomAccessibleInterval< R > > initializeFixedRAIList()
    {
        return new ArrayList<>(  );
    }

    private void addFixedRAI( RandomAccessibleInterval fixedRAI )
    {
        // keep just for debugging
        fixedRAIList.add( imageFilter.filter( fixedRAI ) );
    }

    private void addTransform( long s, T relativeTransformation )
    {
        T absoluteTransformation = ( T ) transformations.get( s ).copy();
        absoluteTransformation.preConcatenate( relativeTransformation );
        transformations.put( s + 1, ( T ) absoluteTransformation );
    }

    private void initializeTransforms()
    {
        transformations = new HashMap<>(  );
        transformations.put( axesSettings.sequenceMin(), identityTransformation() );
    }

    private void showStatus( long s )
    {
        statusService.showStatus( (int) (s - sequenceAxisProperties.min),
                (int) (sequenceAxisProperties.max -  - sequenceAxisProperties.min),
                "Image sequence registration" );
    }

    private T identityTransformation()
    {

        int numTransformableDimensions = axesSettings.numTransformableDimensions();

        if ( numTransformableDimensions == 2 )
        {
            return (T) new AffineTransform2D();
        }
        else if ( numTransformableDimensions == 3 )
        {
            return (T) new AffineTransform3D();
        }
        else
        {
            return (T) new AffineTransform( numTransformableDimensions );
        }

    }

    private AxisType[] getTransformedAxes()
    {
        AxisType[] transformedAxisTypes = new AxisType[ dataset.numDimensions() ];
        int i = 0;

        for ( int a : transformableAxesSettings.axes )
        {
            transformedAxisTypes[ i++ ] = dataset.axis( a ).type();
        }

        transformedAxisTypes[ i++ ] = dataset.axis( sequenceAxisProperties.axis ).type();

        for ( int a : fixedAxesSettings.axes )
        {
            transformedAxisTypes[ i++ ] = dataset.axis( a ).type();
        }

        return transformedAxisTypes;
    }

    public Img getTransformedImg( )
    {



        return inputImageViews.asImg( outputRAI );
    }

    public FinalInterval getTransformableDimensionsOutputInterval()
    {
        if ( outputViewIntervalSizeType == OutputIntervalType.InputDataSize )
        {
            return transformableAxesSettings.inputInterval;
        }
        else if ( outputViewIntervalSizeType == OutputIntervalType.ReferenceRegionSize )
        {
            return transformableAxesSettings.referenceInterval;
        }
        else if ( outputViewIntervalSizeType == OutputIntervalType.UnionSize )
        {
            return getTransformationsUnion( new FinalInterval(input) );
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
        // initUI input for recursive corner determination
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



    public RandomAccessibleInterval fixedRAI( long s )
    {
        RandomAccessibleInterval rai;
        InvertibleRealTransform transform = transformations.get( s );

        if ( referenceRegionType == ReferenceRegionType.Moving )
        {
            rai = inputImageViews.transformableHyperSlice( s );
            RandomAccessible ra = inputImageViews.transform( rai, transform );
            rai = Views.interval( ra, axesSettings.transformableAxesReferenceInterval() );
        }
        else
        {
            return null; // TODO
        }

        rai = imageFilter.filter( rai );

        return rai;
    }

    public RandomAccessible movingRA( long s )
    {
        // this sequence point
        RandomAccessibleInterval rai = inputImageViews.transformableHyperSlice( s );

        // transformed with previous transform at s - 1
        RandomAccessible ra = inputImageViews.transform( rai, transformations.get( s - 1 ) );

        return ra;
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
                    for ( long c = input.min( d ); c <= input.max( d ); ++c )
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

            for ( long s = input.min( sequenceAxisProperties.axis );
                  s <= input.max( sequenceAxisProperties.axis );
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
            RandomAccessibleInterval< R > transformedInput = Views.stack( transformedRaiList );

            transformedSequenceMap.put( fixedDimensions, transformedInput );

            return;
        }

    }*/

}

