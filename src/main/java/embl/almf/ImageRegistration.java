package embl.almf;

import net.imglib2.*;
import net.imglib2.realtransform.Translation;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class ImageRegistration {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";

    RandomAccessibleInterval input;
    int sequenceDimension;
    long sequenceStep, sequenceStart, sequenceEnd, sequenceIncrement;
    FinalRealInterval referenceInterval;

    long[] searchRadius;

    String registrationType;
    String registrationMethod;
    String referenceType;

    ImageRegistration( RandomAccessibleInterval input,
                       int sequenceDimension,
                       long[] searchRadius )
    {
        this.input = input;
        this.sequenceDimension = sequenceDimension;
        this.searchRadius = searchRadius;

        // set default values
        this.sequenceStart = input.min( sequenceDimension );
        this.sequenceEnd = input.max( sequenceDimension );
        this.sequenceIncrement = 1;
        this.registrationType = TRANSLATION;
        this.registrationMethod = MEAN_SQUARE_DIFFERENCE;
        this.referenceType = MOVING;

        // TODO: this.referenceInterval = new FinalInterval( input );
    }

    public void setReferenceRealInterval(
            FinalRealInterval interval )
    {
        referenceInterval = interval;
    }

    public void computeTransforms()
    {

        FinalRealInterval fixedRealInterval = referenceInterval;
        Translation relativeTranslation = null;
        Translation absoluteTranslation = null;

        for ( long s = sequenceStart; s <= sequenceEnd; ++s )
        {
            if ( referenceType.equals( MOVING )
                    && relativeTranslation != null )
            {
                // move reference roi along with
                // the drift
                fixedRealInterval
                        = IntervalUtils.translateRealInterval(
                        fixedRealInterval, relativeTranslation );
            }

            RandomAccessibleInterval fixedImage =
                getFixedImage( fixedRealInterval, s );

            RandomAccessible movingImage =
                    getMovingImage( s );

            relativeTranslation = computeTransform(
                    fixedImage,
                    movingImage,
                    searchRadius );

            if ( referenceType.equals( MOVING ) )
            {
                absoluteTranslation = relativeTranslation.concatenate( absoluteTranslation );
            }
            else
            {
                absoluteTranslation = relativeTranslation;
            }

        }

    }

    private RandomAccessibleInterval getFixedImage(
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

    private RandomAccessible getMovingImage(
            long sequenceCoordinate )
    {

        // construct an interval of the full image
        // at the given sequenceCoordinate
        long[] min = Intervals.minAsLongArray( input );
        long[] max = Intervals.maxAsLongArray( input );

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

