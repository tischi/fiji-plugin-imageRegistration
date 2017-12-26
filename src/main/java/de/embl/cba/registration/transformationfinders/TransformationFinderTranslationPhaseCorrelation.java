package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.GlobalParameters;
import de.embl.cba.registration.LogServiceImageRegistration;
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

    double[] maximalTranslations;
    ImageFilter imageFilter;
    ExecutorService service;

    private double[] translation;
    private double crossCorrelation;

    TransformationFinderTranslationPhaseCorrelation( final Map< String, Object > transformationParameters )
    {
        this.maximalTranslations = (double[]) transformationParameters
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

        LogServiceImageRegistration.debug("### TransformationFinderTranslationPhaseCorrelation");

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

        RandomAccessibleInterval filteredFixedRAI = imageFilter.filter( fixedRAI );
        RandomAccessibleInterval filteredMovingRAI = imageFilter.filter( movingRAI );

        // TODO: remove below code once possible!
        filteredFixedRAI = Views.zeroMin( filteredFixedRAI );
        filteredMovingRAI = Views.zeroMin( filteredMovingRAI );

        // copy to RAM for speed
        //
        ImageFilter copyToRAM = new ImageFilterCopyToRAM( null );

        final RandomAccessibleInterval< R >  finalFixedRAI =  copyToRAM.filter( filteredFixedRAI );
        final RandomAccessibleInterval< R >  finalMovingRAI =  copyToRAM.filter( filteredMovingRAI );

        //ImageJFunctions.show( finalFixedRAI );
        //ImageJFunctions.show( finalMovingRAI );

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

        translation = new double[ n ];

        if ( shiftPeak != null || ! Double.isInfinite( shiftPeak.getCrossCorr() ) )
        {
            if ( shiftPeak.getSubpixelShift() == null )
                shiftPeak.getShift().localize( translation );
            else
                shiftPeak.getSubpixelShift().localize( translation );

            for ( double s : translation )
            {
                LogServiceImageRegistration.debug( "translations "+ s );
            }
            LogServiceImageRegistration.debug("x-corr " + shiftPeak.getCrossCorr());

            crossCorrelation = shiftPeak.getCrossCorr();
        }
        else
        {
            LogServiceImageRegistration.debug(
                    "No sensible translations found => returning zero translations.\n" +
                    "Consider increasing the maximal translations range." );

            crossCorrelation = Double.NaN;
        }

        for ( int d = 0; d < translation.length; ++d )
        {
            if ( Math.abs( translation[ d  ] ) > maximalTranslations[ d ] )
            {
                translation[ d ] = maximalTranslations[ d ] * Math.signum( translation[ d ] );
                LogServiceImageRegistration.debug(
                        "Shift was larger than allowed => restricting to allowed range.");

            }
        }

        if ( translation.length == 2 )
        {
            return new Translation2D( translation );
        }
        else if ( translation.length == 3 )
        {
            return new Translation3D( translation );
        }
        else
        {
            // TODO: still bug with concatenate?
            return new Translation( translation );
        }

    }

    public double[] getTranslation()
    {
        return translation;
    }

    public double getCrossCorrelation()
    {
        return crossCorrelation;
    }


}
