package de.embl.cba.registration;

import de.embl.cba.registration.filter.*;
import de.embl.cba.registration.transformfinder.TransformFinder;
import de.embl.cba.registration.transformfinder.TransformFinderFactory;
import de.embl.cba.registration.ui.Settings;
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
            Settings settings )
    {

        this.dataset = dataset;
        this.input = ( RandomAccessibleInterval<R> ) dataset;

        this.axes = settings.axes;

        this.inputViews = new InputViews( input, axes );

        this.outputIntervalType = settings.outputIntervalType;

        this.referenceRegionType = ReferenceRegionType.Moving; // TODO: not used (get from UI)
        this.referenceRegions = new ArrayList<>();

        this.filterSequence = new FilterSequence( settings.filterSettings );

        this.transformFinder = TransformFinderFactory.create(
                settings.transformSettings.transformFinderType,
                settings.transformSettings );

    }

    public void run()
    {
        long startTimeMilliseconds = start( "# Finding transforms..." );

        initializeTransforms();

        for ( long s = axes.sequenceMin(); s < axes.sequenceMax(); s += axes.sequenceIncrement() )
        {
            showSequenceProgress( s );

            RandomAccessibleInterval fixed = fixed( s, transformations.get( s ) );
            RandomAccessible moving = moving( s + 1, transformations.get( s ) );

            T transform = ( T ) transformFinder.findTransform( fixed, moving, filterSequence );

            addTransform( s + 1, transform );
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
        Logger.showStatus( min, max, message );

    }

    public void logTransformations()
    {
        Logger.info( "Transformations" );

        for ( long s : axes.sequenceCoordinates() )
        {
            Logger.info( "" + s + ": " + transformations.get( s ).toString() );
        }

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

        output.transformedImgPlus = inputViews.asImgPlus( transformedInput, axes.inputAxisTypes(), "registered" );
        output.transformedAxisOrder = axes.axisOrder( axes.inputAxisTypes() );
        //output.transformedAxisOrder = axes.axisOrder( axes.transformedAxisTypes() );
        output.transformedNumSpatialDimensions = axes.numSpatialDimensions( axes.transformedAxisTypes() );

        output.referenceImgPlus = inputViews.asImgPlus( referenceRegionSequence(), axes.referenceAxisTypes(), "reference" );
        //output.referenceAxisOrder = axes.axisOrder( axes.referenceAxisTypes() );
        output.referenceNumSpatialDimensions = axes.numSpatialDimensions( axes.referenceAxisTypes() );

        return output;
    }

    private RandomAccessibleInterval< R > referenceRegionSequence()
    {
        ArrayList< RandomAccessibleInterval< R > > randomAccessibleIntervals = new ArrayList<>(  );

        for ( long s = axes.sequenceMin(); s < axes.sequenceMax(); s += axes.sequenceIncrement() )
        {
            RandomAccessibleInterval fixed = fixed( s, transformations.get( s ) );
            fixed = filterSequence.apply( fixed );
            randomAccessibleIntervals.add( fixed );
        }

        return InputViews.stackAndDropSingletons( randomAccessibleIntervals );
    }

    public RandomAccessibleInterval fixed( long s, T transform )
    {
        RandomAccessible ra = transformedHyperSlice( s, transform );

        return Views.interval( ra, axes.transformableAxesReferenceInterval() );
    }

    public RandomAccessible moving( long s, T transform )
    {
        return transformedHyperSlice( s, transform );
    }

    private RandomAccessible transformedHyperSlice( long s, InvertibleRealTransform transform )
    {
        RandomAccessibleInterval rai = inputViews.transformableReferenceHyperSlice( s );

        return inputViews.transform( rai, transform );
    }


}

