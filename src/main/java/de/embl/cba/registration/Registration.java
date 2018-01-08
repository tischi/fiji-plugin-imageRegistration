package de.embl.cba.registration;

import de.embl.cba.registration.filter.*;
import de.embl.cba.registration.transformfinder.TransformFinder;
import de.embl.cba.registration.transformfinder.TransformFinderFactory;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.util.MetaImage;
import de.embl.cba.registration.util.Transforms;
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

    private final RandomAccessibleInterval< R > input;
    private final InputViews inputViews;
    private final FilterSequence filterSequence;
    private final TransformFinder transformFinder;
    private final Axes axes;
    private final ReferenceRegionType referenceRegionType;
    private final OutputIntervalType outputIntervalType;
    private Map< Long, T > transformations;
    private Map< Long, String > transformationInfos;

    // output
    private final List< RandomAccessibleInterval< R > > referenceRegions;

    private RandomAccessibleInterval< R > transformedInput;

    public Registration( Settings settings )
    {
        this.input = settings.rai;

        this.axes = settings.axes;

        this.inputViews = new InputViews( input, axes );

        this.outputIntervalType = settings.outputIntervalType;

        this.referenceRegionType = ReferenceRegionType.Moving; // TODO: not used (get from UI)
        this.referenceRegions = new ArrayList<>();

        this.filterSequence = new FilterSequence( settings.filterSettings );

        this.transformFinder = TransformFinderFactory.create( settings.transformSettings.transformFinderType, settings.transformSettings );

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

            addTransform( s + 1, transform, transformFinder.toString() );

        }

        doneIn( startTimeMilliseconds );

    }


    private void showSequenceProgress( long s )
    {
        String message = "Registration";
        // TODO: add memory and time
        int current = (int) (s + 1 - axes.sequenceMin());
        int total = (int) (axes.sequenceMax() - axes.sequenceMin());
        Logger.showStatus( current, total, message );
    }

    public void logTransformations()
    {
        Logger.info( "# Relative transformations between subsequent sequence coordinates" );

        for ( long s : axes.sequenceCoordinates() )
        {
            Logger.info( "Coordinate " + s + ": " + transformationInfos.get( s ) );
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

    private void addTransform( long s, T relativeTransformation, String transformationInfo )
    {
        T absoluteTransformation = ( T ) transformations.get( s - 1 ).copy();
        absoluteTransformation.preConcatenate( relativeTransformation );
        transformations.put( s, ( T ) absoluteTransformation );
        transformationInfos.put( s, transformationInfo );
    }

    private void initializeTransforms()
    {
        transformations = new HashMap<>(  );
        transformations.put( axes.sequenceMin(), (T) Transforms.identityAffineTransformation( axes.numRegistrationDimensions() ) );
        transformationInfos = new HashMap<>(  );
        transformationInfos.put( axes.sequenceMin(), "Reference => no transformation." );
    }

    public MetaImage transformedImage( OutputIntervalType outputIntervalType )
    {
        long start = Logger.start( "# Creating transformed image view..." );

        MetaImage metaImage = new MetaImage();
        metaImage.title = "transformed";
        metaImage.rai = inputViews.transformed( transformations, outputIntervalType );;
        metaImage.axisTypes = axes.inputAxisTypes();
        metaImage.axisOrder = axes.axisOrder( metaImage.axisTypes );
        metaImage.numSpatialDimensions = axes.numSpatialDimensions( metaImage.axisTypes );

        Logger.doneIn( start );
        return metaImage;
    }

    /*
    public Output output()
    {
        long startTime = Logger.start("Creating registered image...");

        Output< R > output = new Output<>();

        output.transformedImgPlus = inputViews.asImgPlus( transformedInput, axes.inputAxisTypes(), "registered" );
        output.transformedAxisOrder = axes.axisOrder( axes.inputAxisTypes() );
        output.transformedNumSpatialDimensions = axes.numSpatialDimensions( axes.transformedOutputAxisTypes() );

        output.referenceImgPlus = inputViews.asImgPlus( referenceRegionSequence(), axes.referenceAxisTypes(), "reference" );
        output.referenceAxisOrder = axes.axisOrder( axes.referenceAxisTypes() );
        output.referenceNumSpatialDimensions = axes.numSpatialDimensions( axes.referenceAxisTypes() );

        Logger.doneIn( startTime );

        return output;
    }*/

    private RandomAccessibleInterval< R > referenceRegionSequence()
    {
        ArrayList< RandomAccessibleInterval< R > > randomAccessibleIntervals = new ArrayList<>(  );

        for ( long s = axes.sequenceMin(); s <= axes.sequenceMax(); s += axes.sequenceIncrement() )
        {
            RandomAccessibleInterval fixed = fixed( s, transformations.get( s ) );
            fixed = filterSequence.apply( fixed );
            randomAccessibleIntervals.add( fixed );
        }

        RandomAccessibleInterval rai = InputViews.stackAndDropSingletons( randomAccessibleIntervals );

        return rai;
    }

    public RandomAccessibleInterval fixed( long s, T transform )
    {
        RandomAccessible ra = transformedHyperSlice( s, transform );

        return Views.interval( ra, axes.registrationAxesReferenceInterval() );
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

