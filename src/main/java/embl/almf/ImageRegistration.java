package embl.almf;

import embl.almf.registration.TranslationPhaseCorrelation;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRegistration {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";
    final static String FIXED = "Fixed";


    ArrayList< Integer > transformableDimensions;
    FinalInterval transformableDimensionsInterval;

    Map< Integer, Integer > fixedDimensions;

    int sequenceDimension;
    long sMin, sMax, sRef, ds;

    RandomAccessibleInterval input;

    long[] searchRadius;

    String registrationType;
    String registrationMethod;
    String referenceType;

    ExecutorService service;


    ImageRegistration( RandomAccessibleInterval input,
                       int sequenceDimension,
                       Set< Integer > translationDimensions,
                       long[] searchRadius,
                       FinalInterval referenceInterval,
                       int numThreads )
    {
        this.input = input;
        this.sequenceDimension = sequenceDimension;
        this.transformableDimensions = translationDimensions;
        this.searchRadius = searchRadius;

        // set default values
        this.sMin = input.min( sequenceDimension );
        this.sMax = input.max( sequenceDimension );
        this.ds = 1;
        this.registrationType = TRANSLATION;
        this.registrationMethod = MEAN_SQUARE_DIFFERENCE;
        this.referenceType = MOVING;
        this.transformableDimensionsInterval = referenceInterval;

        this.service = Executors.newFixedThreadPool( numThreads );

    }


    public <T extends InvertibleRealTransform & Concatenable< T > > void computeTransforms()
    {

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;

        T relativeTransformation = null;
        T absoluteTransformation = null;

        for ( long s = sMin; s <= sMax; s += ds )
        {
            fixedRAI = getFixedRAI( s, absoluteTransformation );


            movingRA = getMovingRA( absoluteTransformation, s + ds );

            relativeTransformation = (T) TranslationPhaseCorrelation
                    .compute(
                        fixedRAI,
                        movingRA,
                        searchRadius,
                        service );

            if ( s != sMin )
            {
                absoluteTransformation = ( T ) relativeTransformation
                        .concatenate( absoluteTransformation );
            }
            else
            {
                absoluteTransformation = relativeTransformation;
            }

        }

    }


    private RandomAccessibleInterval getTransformableDimensionsRAI( long s )
    {
        long[] min = Intervals.minAsLongArray( input );
        long[] max = Intervals.maxAsLongArray( input );

        min[ sequenceDimension ] = s;
        max[ sequenceDimension ] = s;

        for ( int d : fixedDimensions.keySet() )
        {
            min[ d ] = fixedDimensions.get( d );
            max[ d ] = fixedDimensions.get( d );
        }

        for ( int d : transformableDimensions )
        {
            // leave as is
        }

        FinalInterval interval = new FinalInterval( min, max );

        RandomAccessibleInterval rai =
                Views.dropSingletonDimensions(
                    Views.interval( input, interval )
                );

        return rai;

    }

    private RandomAccessibleInterval getFixedRAI(
            long s,
            InvertibleRealTransform transform )
    {

        RandomAccessibleInterval rai = getTransformableDimensionsRAI( s );

        if ( s == sRef || referenceType.equals( FIXED ) )
        {
            // do nothing
        }
        else if ( s != sRef && referenceType.equals( MOVING ) )
        {
            rai = getTransformableDimensionsRAI( s );
            RandomAccessible ra = getTransformedRA( rai, transform );
            rai = Views.interval( ra, transformableDimensionsInterval );
        }
        else
        {
            return null; // should not occur
        }

        // crop out reference region
        rai = Views.interval( rai, transformableDimensionsInterval );

        return rai;
    }

    private RandomAccessible getMovingRA(
            long s,
            InvertibleRealTransform transform )
    {

        RandomAccessibleInterval rai = getTransformableDimensionsRAI( s );
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
                    Views.interpolate( Views.extendMirrorSingle( rai ),
                            new NLinearInterpolatorFactory() ),
                                transform );

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

