package embl.almf;

import net.imglib2.*;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Translation;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class ImageRegistration {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";

    RandomAccessibleInterval input;
    int sequenceDimension;
    double sequenceStep;
    FinalInterval sequenceInterval;


    String registrationType = TRANSLATION;
    String registrationMethod = MEAN_SQUARE_DIFFERENCE;
    String referenceType = MOVING;

    ImageRegistration( RandomAccessibleInterval input,
                       int sequenceDimension,
                       FinalInterval sequenceInterval)
    {
        this.input = input;
        this.sequenceDimension = sequenceDimension;
        this.sequenceInterval = sequenceInterval;
    }

    public void computeTransforms()
    {
        RandomAccessibleInterval view =
                Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ 100, 100, 0 } );

        ImagePlus imp2 = ImageJFunctions.wrap( view, "wrapped back cropped view");
        imp2.show();


        for ( int t = 0; t < 1; ++t )
        {
            int tFixed = 0;
            RandomAccessibleInterval fixed = Views.interval(
                    img,
                    new long[]{0, 100, tFixed},
                    new long[]{0, 100, tFixed}
            );
            ImagePlus impFixed = ImageJFunctions.wrap( view, "wrapped back fixed");
            impFixed.show();

            int tMoving = tFixed + 1;
            RandomAccessibleInterval moving = Views.interval(img,
                    new long[]{0, 100, tMoving},
                    new long[]{0, 100, tMoving}
            );

            computeTransform( fixed, moving );
        }

    }


    private RandomAccessibleInterval getNextFixedImage(
            FinalRealInterval lastInterval,
            Translation translation,
            long lastSequenceCoordinate )
    {

        // using the last transformation
        // compute fixed interval
        // at next sequence coordinate
        //
        FinalRealInterval nextRealInterval =
                IntervalUtils.translateRealInterval( lastInterval, translation );

        nextRealInterval = IntervalUtils.increment( nextRealInterval,
                sequenceDimension, sequenceStep );

        FinalInterval nextInterval = IntervalUtils.realToInt( nextRealInterval );


        // using the nextInterval,
        // get the next fixed image
        //
        RandomAccessibleInterval nextFixedImage =
                Views.interval( input, nextInterval );

        return nextFixedImage;

    }

}

