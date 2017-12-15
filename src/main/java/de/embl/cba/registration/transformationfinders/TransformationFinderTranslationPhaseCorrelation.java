package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.filter.ImageFilter;
import ij.IJ;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.realtransform.RealTransform;

import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;

import java.util.*;
import java.util.concurrent.ExecutorService;


public class TransformationFinderTranslationPhaseCorrelation implements TransformationFinder {

    Double[] maximalTranslations;
    ImageFilter imageFilter;

    TransformationFinderTranslationPhaseCorrelation(
            Map< String, Object > transformationParameters,
            ImageFilter imageFilter )
    {
        this.maximalTranslations =
                (Double[]) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_TRANSLATIONS);

        this.imageFilter = imageFilter;
    }

    public < R extends RealType< R > & NativeType< R > > RealTransform findTransform(
            RandomAccessibleInterval fixedRAI,
            RandomAccessible movingRA,
            ExecutorService service)
    {
        final int n = fixedRAI.numDimensions();

        final int numPeaksToCheck = 5;

        // TODO: find a good strategy for the minOverlap!

        long minOverlap = 1;
        for ( int d = 0; d < n; ++d )
        {
            minOverlap *= ( fixedRAI.dimension( d ) - maximalTranslations[ d ] );
        }

        final int extensionValue = 0; //(int) searchRadius[ 0 ];
        final boolean doSubpixel = true;
        final boolean interpolateCrossCorrelation = true;

        final int[] extension = new int[ n ];
        Arrays.fill( extension, extensionValue );

        RandomAccessibleInterval movingRAI = Views.interval( movingRA, fixedRAI );

        // potentially filter the moving image (the fixed one is already filtered)
        //
        RandomAccessibleInterval filteredFixedRAI = fixedRAI;
        RandomAccessibleInterval filteredMovingRAI = movingRAI;

        if ( imageFilter != null )
        {
            filteredMovingRAI = imageFilter.filter( movingRAI );
        }


        // TODO: remove below code once possible!
        filteredFixedRAI = Views.zeroMin( filteredFixedRAI );
        filteredMovingRAI = Views.zeroMin( filteredMovingRAI );


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
