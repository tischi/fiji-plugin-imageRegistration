package embl.almf.registration;

import embl.almf.IntervalUtils;
import embl.almf.filter.ImageFilter;
import ij.IJ;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Translation;

import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class TranslationPhaseCorrelation {


    public static < R extends RealType< R > & NativeType< R > > RealTransform findTransform(
            RandomAccessibleInterval fixedRAI,
            RandomAccessible movingRA,
            List< RandomAccessibleInterval < R > > fixedRAIList,
            long[] searchRadii,
            ImageFilter imageFilter,
            ExecutorService service)
    {
        final int n = fixedRAI.numDimensions();

        final int numPeaksToCheck = 5;

        long minOverlap = 1;

        for ( int d = 0; d < fixedRAI.numDimensions(); ++d )
        {
            minOverlap *= fixedRAI.dimension( d ) / 5;
        }

        final int extensionValue = 10;
        final boolean doSubpixel = true;
        final boolean interpolateCrossCorrelation = true;

        final int[] extension = new int[ fixedRAI.numDimensions() ];
        Arrays.fill( extension, extensionValue );

        FinalInterval movingInterval =
                IntervalUtils.expand( fixedRAI, searchRadii );
        RandomAccessibleInterval movingRAI = Views.interval( movingRA, movingInterval );

        // potentially filter the input images
        //
        RandomAccessibleInterval filteredFixedRAI = fixedRAI;
        RandomAccessibleInterval filteredMovingRAI = movingRAI;

        if ( imageFilter != null )
        {
            filteredFixedRAI = imageFilter.filter( fixedRAI );
            filteredMovingRAI = imageFilter.filter( movingRAI );
        }

        if ( fixedRAIList != null )
        {
            fixedRAIList.add( filteredFixedRAI );
        }

        //ImageJFunctions.show( fixedRAI );
        //ImageJFunctions.show( movingRAI );

        // compute best shift
        //
        final RandomAccessibleInterval< FloatType > pcm =
                PhaseCorrelation2.calculatePCM(
                        filteredFixedRAI,
                        filteredMovingRAI,
                        extension,
                        new ArrayImgFactory< FloatType >(),
                        new FloatType(),
                        new ArrayImgFactory< ComplexFloatType >(),
                        new ComplexFloatType(),
                        service );

        final PhaseCorrelationPeak2 shiftPeak =
                PhaseCorrelation2.getShift(
                        pcm,
                        filteredFixedRAI,
                        filteredMovingRAI,
                        numPeaksToCheck,
                        minOverlap,
                        doSubpixel,
                        interpolateCrossCorrelation,
                        service );

        //System.out.println( "Actual overlap of best shift is: " + shiftPeak.getnPixel() )

        // the best peak is horrible or no peaks were found at all, return null
        if ( shiftPeak == null || Double.isInfinite( shiftPeak.getCrossCorr() ) )
            return null;

        final double[] shift = new double[ n ];

        if ( shiftPeak.getSubpixelShift() == null )
            shiftPeak.getShift().localize( shift );
        else
            shiftPeak.getSubpixelShift().localize( shift );

        IJ.log("--");
        for ( double s : shift )
        {
            IJ.log( ""+ s );
        }
        IJ.log("" + shiftPeak.getCrossCorr());


        // TODO: replace with N-D ?!
        if ( shift.length == 2 )
        {
            return new Translation2D( shift );
        }
        else if ( shift.length == 3 )
        {
            return new Translation3D( shift );
        }
        else
        {
            return null;
        }

        // TODO: below does not work; why?
        //Translation translation = new Translation( shift );
        //return translation;

    }

}
