package de.embl.cba.registration;

import de.embl.cba.registration.filter.*;
import de.embl.cba.registration.transformfinder.TransformFinder;
import de.embl.cba.registration.transformfinder.TransformFinderFactory;
import de.embl.cba.registration.transformfinder.TransformFinderParameters;
import de.embl.cba.registration.ui.RegistrationParameters;
import net.imagej.Dataset;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.*;

import static de.embl.cba.registration.Logger.*;

public class Registration
        < R extends RealType< R > & NativeType < R >,
                T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable < T > > {

    private final Dataset dataset;
    private final RandomAccessibleInterval< R > input;
    private final InputViews inputViews;
    private final FilterSequence filterSequence;
    private final TransformFinder transformFinder;
    private final Axes axes;
    private final ReferenceRegionType referenceRegionType;
    private final OutputIntervalType outputIntervalType;
    private Map< Long, T > transformations;
    private final List< RandomAccessibleInterval< R > > referenceRegions;
    RandomAccessibleInterval< R > transformedInput;

    public Registration(
            final Dataset dataset,
            RegistrationParameters registrationParameters )
    {

        this.dataset = dataset;
        this.input = ( RandomAccessibleInterval<R> ) dataset;

        this.axes = new Axes( dataset, registrationParameters.registrationAxisTypes, registrationParameters.interval );

        this.inputViews = new InputViews( input, axes );

        this.outputIntervalType = registrationParameters.outputIntervalType;

        this.referenceRegionType = ReferenceRegionType.Moving; // TODO: not used (get from UI)
        this.referenceRegions = new ArrayList<>();

        this.filterSequence = new FilterSequence( registrationParameters.filterParameters );

        this.transformFinder = TransformFinderFactory.create(
                registrationParameters.transformSettings.transformFinderType,
                registrationParameters.transformSettings );

    }

    public void run()
    {
        long startTimeMilliseconds = start( "# Finding transforms..." );

        initializeTransforms();

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;

        for ( long s = axes.sequenceMin(); s < axes.sequenceMax(); s += axes.sequenceIncrement() )
        {
            showSequenceProgress( s );

            fixedRAI = fixedRAI( s );
            movingRA = movingRA( s + 1 );

            T transform = ( T ) transformFinder.findTransform( fixedRAI, movingRA );

            addTransform( s + 1, transform );
            addReferenceRegion( fixedRAI );
        }

        transformedInput = inputViews.transformedInput( transformations, outputIntervalType );

        doneIn( startTimeMilliseconds );

    }

    private void showSequenceProgress( long s )
    {
        String message = "Sequence registration";
        // TODO: add memory and time
        int min = (int) (s - axes.sequenceMin());
        int max = (int) (axes.sequenceMax() - axes.sequenceMin());
        statusService.showStatus( min, max, message );
    }

    private List< RandomAccessibleInterval< R > > initializeFixedRAIList()
    {
        return new ArrayList<>(  );
    }

    private void addReferenceRegion( RandomAccessibleInterval fixedRAI )
    {
        // to show the user for 'debugging'
        referenceRegions.add( filterSequence.apply( fixedRAI ) );
    }

    private void addTransform( long s, T relativeTransformation )
    {
        T absoluteTransformation = ( T ) transformations.get( s - 1 ).copy();
        absoluteTransformation.preConcatenate( relativeTransformation );
        transformations.put( s, ( T ) absoluteTransformation );
    }

    private void initializeTransforms()
    {
        transformations = new HashMap<>(  );
        transformations.put( axes.sequenceMin(), identityTransformation() );
    }

    private T identityTransformation()
    {

        int numTransformableDimensions = axes.numTransformableDimensions();

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

    public Output output()
    {
        Output< R > output = new Output<>();
        output.imgPlus = inputViews.imgPlus( transformedInput, "registered" );
        output.axisTypes = axes.transformableDimensionsAxisTypes();
        output.axisOrder = axes.axisOrderAfterTransformation();
        output.numSpatialDimensions = axes.numSpatialDimensions( output.axisTypes );

        return output;
    }


    public RandomAccessibleInterval fixedRAI( long s )
    {
        RandomAccessibleInterval rai;
        InvertibleRealTransform transform = transformations.get( s );

        if ( referenceRegionType == ReferenceRegionType.Moving )
        {
            rai = inputViews.transformableHyperSlice( s );
            RandomAccessible ra = inputViews.transform( rai, transform );
            rai = Views.interval( ra, axes.transformableAxesReferenceInterval() );
        }
        else
        {
            return null; // TODO
        }

        rai = filterSequence.apply( rai );

        return rai;
    }

    public RandomAccessible movingRA( long s )
    {
        // this sequence point
        RandomAccessibleInterval rai = inputViews.transformableHyperSlice( s );

        // transformed with previous transform at s - 1
        RandomAccessible ra = inputViews.transform( rai, transformations.get( s - 1 ) );

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
