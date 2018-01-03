package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.InputViews;
import de.embl.cba.registration.Logger;
import de.embl.cba.registration.filter.FilterSequence;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.*;
import java.util.stream.Collectors;

public class TransformFinderRotationTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType < R > >
        implements TransformFinder
{

    FinalRealInterval rotationInterval;

    //TODO: make rotationStep configurable
    //TODO: maybe first look, e.g., every 5 degree and then finer, e.g. 1 degree.
    double rotationStep = 2D * Math.PI / 360D;

    private RandomAccessibleInterval fixed;
    private RandomAccessible moving;
    private TransformFinderTranslationPhaseCorrelation translationFinder;

    private FilterSequence filterSequence;
    private ArrayList< RotationAndTransformation > rotationAndTransformationList;

    private Double UNSET_ROTATION = Double.MAX_VALUE;
    private RotationAndTransformation bestRotationAndTransformation;
    private RealTransform bestTransform;
    private ArrayList< Double > rotation;

    TransformFinderRotationTranslationPhaseCorrelation( TransformSettings settings )
    {
        configureRotations( settings );

        this.translationFinder = new TransformFinderTranslationPhaseCorrelation( settings );
    }

    private void configureRotations( TransformSettings settings )
    {
        double[] maximalRotationsDegrees = settings.maximalRotations;
        double[] maxRotations = Arrays.stream( maximalRotationsDegrees )
                .map( x -> 2D * Math.PI * x / 360D ).toArray();
        double[] minRotations = Arrays.stream( maxRotations ).map( x -> -x ).toArray();

        this.rotationInterval = new FinalRealInterval( minRotations, maxRotations );
    }

    public RealTransform findTransform(
            RandomAccessibleInterval fixedRAI,
            RandomAccessible movingRA,
            FilterSequence filterSequence )
    {

        Logger.debug( "## TransformFinderRotationTranslationPhaseCorrelation" );

        this.fixed = fixedRAI;
        this.moving = movingRA;
        this.filterSequence = filterSequence;

        initialize();
        determineBestTranslationForEachRotation( rotation );
        setBestTransform();

        return bestTransform;

    }

    private RealTransform createAffineTransform( RotationAndTransformation rotationAndTransformation )
    {
        RealTransform affineTransform = null;

        if ( rotationAndTransformation.rotation.size() == 1 )
        {
            AffineTransform2D affineTransform2D = new AffineTransform2D();
            affineTransform2D.rotate( rotationAndTransformation.rotation.get( 0 ) );
            affineTransform2D.translate( rotationAndTransformation.translation );
            affineTransform = affineTransform2D;
        }
        else if ( rotationAndTransformation.rotation.size() == 3)
        {
            AffineTransform3D affineTransform3D = new AffineTransform3D();
            for ( int d = 0; d < rotationAndTransformation.rotation.size(); ++d )
            {
                affineTransform3D.rotate( d, rotationAndTransformation.rotation.get( d ) );
            }
            affineTransform3D.translate( rotationAndTransformation.translation );
            affineTransform = affineTransform3D;
        }
        else
        {
            affineTransform = null; // TODO: throw error (or even better: this should not happen)
        }

        return affineTransform;
    }

    private void initialize()
    {
        rotationAndTransformationList = new ArrayList<>(  );

        rotation = new ArrayList<>();
        for ( int d = 0; d < rotationInterval.numDimensions(); ++d )
        {
            rotation.add( Double.MAX_VALUE );
        }

    }

    private void setBestTransform( )
    {

        bestRotationAndTransformation = new RotationAndTransformation();
        bestRotationAndTransformation.phaseCorrelation = Double.MIN_VALUE;

        for ( RotationAndTransformation rotationAndTransformation : rotationAndTransformationList )
        {
            if ( rotationAndTransformation.phaseCorrelation > bestRotationAndTransformation.phaseCorrelation )
            {
                bestRotationAndTransformation.phaseCorrelation = rotationAndTransformation.phaseCorrelation;
                bestRotationAndTransformation.rotation = rotationAndTransformation.rotation;
                bestRotationAndTransformation.translation = rotationAndTransformation.translation;
            }
        }

        bestTransform = createAffineTransform( bestRotationAndTransformation );

    }

    private void determineBestTranslationForEachRotation( ArrayList< Double > rotation )
    {
        if ( rotation.contains( UNSET_ROTATION ) )
        {
            int d = rotation.indexOf( UNSET_ROTATION );
            for ( double r = rotationInterval.realMin( d ); r <= rotationInterval.realMax( d ); r += rotationStep )
            {
                rotation.set( d, r );
                determineBestTranslationForEachRotation( rotation );
            }
            return;
        }
        else
        {
            translationFinder.findTransform( fixed, rotateMoving( rotation ), filterSequence );
            addRotationAndTranslation( rotation, translationFinder );
        }
    }

    private void addRotationAndTranslation( ArrayList< Double > rotation, TransformFinderTranslationPhaseCorrelation translationFinder )
    {
        RotationAndTransformation rotationAndTransformation = new RotationAndTransformation();
        rotationAndTransformation.phaseCorrelation = translationFinder.phaseCorrelation();
        rotationAndTransformation.rotation = ( ArrayList< Double > ) rotation.clone();
        rotationAndTransformation.translation = translationFinder.translation();
        rotationAndTransformationList.add( rotationAndTransformation );
    }

    private RandomAccessible< R > rotateMoving( ArrayList< Double > rotation )
    {
        RandomAccessible< R > rotatedMoving;

        if ( rotation.size() == 1 )
        {
            AffineTransform2D rotation2D = new AffineTransform2D();
            rotation2D.rotate(  rotation.get( 0 )  );
            rotatedMoving = InputViews.transform( moving, rotation2D );
        }
        else if ( rotation.size() == 3)
        {
            AffineTransform3D rotation3D = new AffineTransform3D();
            for ( int d = 0; d < rotation.size(); ++d )
            {
                rotation3D.rotate( d, rotation.get( d ) );
            }
            rotatedMoving = InputViews.transform( moving, rotation3D );
        }
        else
        {
            rotatedMoving = null; // TODO: throw error (or even better: this should not happen)
        }

        return rotatedMoving;

    }

    public String toString()
    {
        if ( bestRotationAndTransformation == null ) return "No transformation determined.";

        return bestRotationAndTransformation.toString();
    }

    private class RotationAndTransformation
    {
        ArrayList< Double > rotation;
        double[] translation;
        double phaseCorrelation;

        @Override
        public String toString()
        {
            String rotationString = rotation.stream()
                    .map( x -> 360D * x / (2 * Math.PI) )
                    .map( Object::toString )
                    .collect( Collectors.joining(", ") );

            String translationString = Arrays.stream( translation )
                    .mapToObj( Double::toString )
                    .collect( Collectors.joining(", ") );

            String out = "";
            out += "Rotation: " + rotationString;
            out += "; Translation: " + translationString;
            out += "; Phase-correlation: " + phaseCorrelation;
            out += "\n";

            return out;
        }
    }

}
