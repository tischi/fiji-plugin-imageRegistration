package embl.almf;

import embl.almf.registration.TranslationPhaseCorrelation;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRegistration {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";

    RandomAccessibleInterval input;
    int sequenceDimension;
    Set< Integer > translationDimensions;
    long sequenceStep, sMin, sMax, ds;
    FinalInterval referenceInterval;

    long[] searchRadius;

    String registrationType;
    String registrationMethod;
    String referenceType;

    ExecutorService service;


    ImageRegistration( RandomAccessibleInterval input,
                       int sequenceDimension,
                       Set< Integer > translationDimensions,
                       long[] searchRadius,
                       int numThreads )
    {
        this.input = input;
        this.sequenceDimension = sequenceDimension;
        this.translationDimensions = translationDimensions;
        this.searchRadius = searchRadius;

        // set default values
        this.sMin = input.min( sequenceDimension );
        this.sMax = input.max( sequenceDimension );
        this.ds = 1;
        this.registrationType = TRANSLATION;
        this.registrationMethod = MEAN_SQUARE_DIFFERENCE;
        this.referenceType = MOVING;

        this.service = Executors.newFixedThreadPool( numThreads );

        // TODO: this.referenceInterval = new FinalInterval( input );
    }


    public <T extends RealTransform & Concatenable< T > > void computeTransforms()
    {

        RandomAccessibleInterval fixedRAI = Views.interval( input, referenceInterval );
        RandomAccessible movingRA;

        T relativeTransformation = null;
        T absoluteTransformation = null;

        for ( long s = sMin; s <= sMax; s += ds )
        {
            if ( referenceType.equals( MOVING )
                    && absoluteTransformation != null )
            {
                fixedRAI = getFixedRAI( absoluteTransformation, s );
            }

            movingRA = getMovingRA( absoluteTransformation, s + ds );

            relativeTransformation = (T) TranslationPhaseCorrelation.compute(
                    fixedRAI,
                    movingRA,
                    searchRadius,
                    service );

            absoluteTransformation = (T) relativeTransformation
                            .concatenate( absoluteTransformation );

        }

    }


    private FinalInterval getFullImageInterval( long sequenceCoordinate )
    {
        long[] min = net.imglib2.util.Intervals.minAsLongArray( input );
        long[] max = net.imglib2.util.Intervals.maxAsLongArray( input );

        for ( int d = 0; d < min.length; ++d )
        {
            if ( translationDimensions.contains( d ) )
            {
                // leave as is, because we crop later, after applying current translation
            }
            else if ( d == sequenceDimension )
            {
                // move to next point in the sequence
                min[ d ] = sequenceCoordinate;
                max[ d ] = sequenceCoordinate;
            }
            else
            {
                // neither translation dimension, nor sequence dimension.
                // for example, this could be the 'reference channel'
                // in a multi-channel image
                min[ d ] = referenceInterval.min( d );
                max[ d ] = referenceInterval.max( d );
            }
        }

        return new FinalInterval( min, max );

    }

    private RandomAccessibleInterval getFixedRAI(
            RealTransform transform,
            long sequenceCoordinate )
    {

        RandomAccessible ra = getTransformedRA( transform, sequenceCoordinate );

        // crop out reference region
        RandomAccessibleInterval rai = Views.interval( ra, referenceInterval );

        return rai;

    }

    private RandomAccessible getMovingRA(
            RealTransform transform,
            long sequenceCoordinate )
    {

        RandomAccessible ra = getTransformedRA(
                transform,
                sequenceCoordinate );

        return ra;
    }


    private RandomAccessible getTransformedRA(
            RealTransform transform,
            long sequenceCoordinate )
    {
        // get appropriate view on data at current step
        //
        FinalInterval fullIntervalAtCurrentStep = getFullImageInterval( sequenceCoordinate );
        RandomAccessibleInterval rai = Views.interval( input, fullIntervalAtCurrentStep );

        // apply current inverse translation in order to
        // compensate for the already detected drift
        RealRandomAccessible rra = Views.interpolate(
                Views.extendMirrorSingle( input ),
                new NLinearInterpolatorFactory() );
        rra = RealViews.transform( rra, transform );
        RandomAccessible ra = Views.raster( rra );

        return ra;

    }




    private RandomAccessibleInterval getFixedImageAtIntegerInterval(
            FinalRealInterval realInterval,
            long sequenceCoordinate )
    {

        // Set sequence coordinate
        //
        FinalRealInterval fixedImageRealInterval =
                IntervalUtils.fixDimension(
                        realInterval,
                        sequenceDimension,
                        sequenceCoordinate );

        // Make it an Integer interval for cropping a view
        //
        FinalInterval fixedImageInterval = IntervalUtils.realToInt( fixedImageRealInterval );

        // Create new view
        //
        RandomAccessibleInterval nextFixedImage =
                Views.interval( input, fixedImageInterval );

        return nextFixedImage;

    }

    private RandomAccessible getMovingImageWrong(
            long sequenceCoordinate )
    {

        // construct an interval of the full image
        // at the given sequenceCoordinate
        long[] min = net.imglib2.util.Intervals.minAsLongArray( input );
        long[] max = net.imglib2.util.Intervals.maxAsLongArray( input );

        min[ sequenceDimension ] = sequenceCoordinate;
        max[ sequenceDimension ] = sequenceCoordinate;

        FinalInterval interval = new FinalInterval( min, max );

        // get a view on this image
        //
        RandomAccessibleInterval randomAccessibleInterval =
                Views.interval( input, interval );

        // make it infinite such that we do not have to
        // care about out-of-bounds issues during the
        // search for the best match
        //
        RandomAccessible randomAccessible =
                Views.extendMirrorSingle( randomAccessibleInterval );

        return randomAccessible;

    }

}

