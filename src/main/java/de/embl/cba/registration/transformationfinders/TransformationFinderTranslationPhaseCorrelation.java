package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.GlobalParameters;
import de.embl.cba.registration.PackageLogService;
import de.embl.cba.registration.filter.ImageFilter;
import de.embl.cba.registration.filter.ImageFilterCopyToRAM;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.realtransform.RealTransform;

import net.imglib2.realtransform.Translation;
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


public class TransformationFinderTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType< R > >
        implements TransformationFinder {

    Double[] maximalTranslations;
    ImageFilter imageFilter;
    ExecutorService service;

    TransformationFinderTranslationPhaseCorrelation( final Map< String, Object > transformationParameters )
    {
        this.maximalTranslations = (Double[]) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_TRANSLATIONS );

        this.service = (ExecutorService) transformationParameters
                        .get( GlobalParameters.EXECUTOR_SERVICE );

        this.imageFilter = ( ImageFilter ) transformationParameters
                        .get( TransformationFinderParameters.IMAGE_FILTER );

    }

    public RealTransform findTransform(
             RandomAccessibleInterval fixedRAI,
             RandomAccessible movingRA )
    {

        PackageLogService.logService.info("### TransformationFinderTranslationPhaseCorrelation");

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
        // TODO: why filter the fixed one outside?

        RandomAccessibleInterval filteredFixedRAI = fixedRAI;
        RandomAccessibleInterval filteredMovingRAI = movingRAI;

        if ( imageFilter != null )
        {
            filteredMovingRAI = imageFilter.filter( movingRAI );
        }

        // TODO: remove below code once possible!
        filteredFixedRAI = Views.zeroMin( filteredFixedRAI );
        filteredMovingRAI = Views.zeroMin( filteredMovingRAI );

        // copy to RAM for speed
        //
        ImageFilter copyToRAM = new ImageFilterCopyToRAM( null );

        final RandomAccessibleInterval< R >  finalFixedRAI =  copyToRAM.filter( filteredFixedRAI );
        final RandomAccessibleInterval< R >  finalMovingRAI =  copyToRAM.filter( filteredMovingRAI );

        // compute best shift
        //
        final RandomAccessibleInterval< FloatType > pcm =
                PhaseCorrelation2.calculatePCM(
                        finalFixedRAI,
                        finalMovingRAI,
                        extension,
                        new ArrayImgFactory< FloatType >(),
                        new FloatType(),
                        new ArrayImgFactory< ComplexFloatType >(),
                        new ComplexFloatType(),
                        service );

        final PhaseCorrelationPeak2 shiftPeak =
                PhaseCorrelation2.getShift(
                        pcm,
                        finalFixedRAI,
                        finalMovingRAI,
                        numPeaksToCheck,
                        minOverlap,
                        doSubpixel,
                        interpolateCrossCorrelation,
                        service );

        //System.out.println( "Actual overlap of best shift is: " + shiftPeak.getnPixel() )

        final double[] shift = new double[ n ];

        if ( shiftPeak != null || ! Double.isInfinite( shiftPeak.getCrossCorr() ) )
        {
            if ( shiftPeak.getSubpixelShift() == null )
                shiftPeak.getShift().localize( shift );
            else
                shiftPeak.getSubpixelShift().localize( shift );

            for ( double s : shift )
            {
                PackageLogService.logService.info( "translation "+ s );
            }
            PackageLogService.logService.info("x-corr " + shiftPeak.getCrossCorr());
        }
        else
        {
            PackageLogService.logService.info(
                    "No sensible translation found => returning zero translation.\n" +
                    "Consider increasing the maximal translation range." );
        }

        for ( int d = 0; d < shift.length; ++d )
        {
            if ( Math.abs( shift[ d  ] ) > maximalTranslations[ d ] )
            {
                shift[ d ] = maximalTranslations[ d ] * Math.signum( shift[ d ] );
                PackageLogService.logService.info(
                        "Shift was larger than allowed => restricting to allowed range.");

            }
        }

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
            // TODO: still bug with concatenate?
            return new Translation( shift );
        }

    }

}
