package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.ImageRegistrationUtils;
import de.embl.cba.registration.PackageLogService;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.*;

public class TransformationFinderRotationTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType < R > >
        implements TransformationFinder {

    FinalRealInterval rotationInterval;
    double rotationStep = 2D * Math.PI / 360D; //TODO: make configurable

    private RandomAccessibleInterval fixedRAI;
    private RandomAccessible movingRA;
    private TransformationFinderTranslationPhaseCorrelation transformationFinderTranslationPhaseCorrelation;

    private static final String CROSS_CORRELATION = "CrossCorrelation";
    private static final String TRANSLATIONS = "Translations";
    private static final String ROTATIONS = "Rotations";

    TransformationFinderRotationTranslationPhaseCorrelation(
            Map< String, Object > transformationParameters )
    {

        double[] maximalRotationsDegrees =
                ( double[] ) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_ROTATIONS );
        double[] maxRotations = Arrays.stream( maximalRotationsDegrees )
                .map( x -> 2D * Math.PI * x / 360D ).toArray();
        double[] minRotations = Arrays.stream( maxRotations ).map( x -> -x ).toArray();

        this.rotationInterval = new FinalRealInterval( minRotations, maxRotations );

        // get a transformationFinder for the translations

        Map< String, Object > transformationTranslationParameters
                = new HashMap<>( transformationParameters );

        transformationTranslationParameters.put(
                TransformationFinderParameters.TRANSFORMATION_FINDER_TYPE,
                TransformationFinderType.Translation__PhaseCorrelation );

        this.transformationFinderTranslationPhaseCorrelation
                = new TransformationFinderTranslationPhaseCorrelation( transformationParameters );

    }

    public RealTransform findTransform(
             RandomAccessibleInterval fixedRAI,
             RandomAccessible movingRA )
    {

        PackageLogService.logService.info( "## TransformationFinderRotationTranslationPhaseCorrelation" );

        this.fixedRAI = fixedRAI;
        this.movingRA = movingRA;

        // Recursively loop through all possible rotations and compute best translations
        ArrayList< Result > results = new ArrayList<>(  );
        double[] rotations = new double[ rotationInterval.numDimensions() ];
        Arrays.fill( rotations, Double.MAX_VALUE );

        computeCrossCorrelationAndTranslation( rotations, results );

        // go through list and and find the best transformation

        Result bestResult = new Result();
        bestResult.crossCorrelation = Double.MIN_VALUE;

        for ( Result result : results )
        {
            if ( result.crossCorrelation > bestResult.crossCorrelation )
            {
                bestResult.crossCorrelation = result.crossCorrelation;
                bestResult.rotations = result.rotations;
                bestResult.translations = result.translations;
            }
        }

        // Combine translations and rotations and return result

        PackageLogService.logService.info( "### Result" );

        RealTransform bestTransform;
        if ( bestResult.rotations.length == 1 )
        {
            AffineTransform2D affineTransform2D = new AffineTransform2D();
            affineTransform2D.rotate( -1D * bestResult.rotations[ 0 ] );
            affineTransform2D.translate( bestResult.translations );
            PackageLogService.logService.info( affineTransform2D.toString() );
            bestTransform = affineTransform2D;
        }
        else if ( bestResult.rotations.length == 3)
        {
            AffineTransform3D affineTransform3D = new AffineTransform3D();
            for ( int d = 0; d < bestResult.rotations.length; ++d )
            {
                affineTransform3D.rotate( d, -1D * bestResult.rotations[ d ]);
            }
            affineTransform3D.translate( bestResult.translations );
            PackageLogService.logService.info( affineTransform3D.toString() );
            bestTransform = affineTransform3D;
        }
        else
        {
            bestTransform = null; // TODO: throw error (or even better: this should not happen)
        }

        return bestTransform;
    }

    private void computeCrossCorrelationAndTranslation(
            double[] rotations,
            ArrayList< Result > rotationsTranslationsXCorrList )
    {

        if ( Arrays.asList(  rotations ).contains( Double.MAX_VALUE ) )
        {
            for ( int d = 0; d < rotations.length; ++d )
            {
                if ( rotations[ d ] == Double.MAX_VALUE )
                {
                    for ( double r = rotationInterval.realMin( d );
                          r <= rotationInterval.realMax( d );
                          r += rotationStep )
                    {
                        rotations[ d ] = r;
                        computeCrossCorrelationAndTranslation(
                                rotations,
                                rotationsTranslationsXCorrList );
                    }
                }
            }
        }
        else
        {
            // all rotations are set => compute best translations and add to list

            // rotate
            RandomAccessible< R > rotatedMovingRA;

            if ( rotations.length == 1 )
            {
                AffineTransform2D rotation2D = new AffineTransform2D();
                rotation2D.rotate(  rotations[ 0 ]  );
                rotatedMovingRA = ImageRegistrationUtils.getRAasTransformedRA( movingRA, rotation2D );
            }
            else if ( rotations.length == 3)
            {
                AffineTransform3D rotation3D = new AffineTransform3D();
                for ( int d = 0; d < rotations.length; ++d )
                {
                    rotation3D.rotate( d, rotations[ d ] );
                }
                rotatedMovingRA = ImageRegistrationUtils.getRAasTransformedRA( movingRA, rotation3D );
            }
            else
            {
                // TODO: throw error (or even better: this should not happen)
                rotatedMovingRA = null;
            }

            // find best translations for this rotation
            transformationFinderTranslationPhaseCorrelation.findTransform( fixedRAI, rotatedMovingRA );

            // add result to list
            Result result = new Result();
            result.crossCorrelation = transformationFinderTranslationPhaseCorrelation.getCrossCorrelation();
            result.rotations = rotations.clone();
            result.translations = transformationFinderTranslationPhaseCorrelation.getTranslation();

            rotationsTranslationsXCorrList.add( result );

        }
    }
    
    private class Result
    {
        double[] rotations;
        double[] translations;
        double crossCorrelation;


    }

}
