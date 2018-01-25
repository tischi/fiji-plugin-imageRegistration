package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.InputViews;
import de.embl.cba.registration.Logger;
import de.embl.cba.registration.filter.FilterSequence;
import net.imglib2.FinalInterval;
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

    FinalInterval rotationInterval;

    //TODO: make rotationStep configurable
    //TODO: maybe first look, e.g., every 5 degree and then finer, e.g. 1 degree.
    int rotationStep = 1;

    private RandomAccessibleInterval fixed;
    private RandomAccessible moving;
    private TransformFinderTranslationPhaseCorrelation translationFinder;

    private FilterSequence filterSequence;
    private ArrayList< RotationAndTransformation > rotationAndTransformationList;

    private Long UNSET_ROTATION = Long.MAX_VALUE;
    private RotationAndTransformation bestRotationAndTransformation;
    private RealTransform bestTransform;

    TransformFinderRotationTranslationPhaseCorrelation( TransformSettings settings )
    {
        configureRotationRanges( settings );

        this.translationFinder = new TransformFinderTranslationPhaseCorrelation( settings );
    }

    public RealTransform findTransform( RandomAccessibleInterval fixedRAI, RandomAccessible movingRA, FilterSequence filterSequence )
    {

        Logger.debug( "## TransformFinderRotationTranslationPhaseCorrelation" );

        this.fixed = fixedRAI;
        this.moving = movingRA;
        this.filterSequence = filterSequence;

        ArrayList< Long > rotation = initialize();;
        determineBestTranslationForEachRotation( rotation );
        setBestTransform();

        return bestTransform;

    }

    private void configureRotationRanges( TransformSettings settings )
    {
        double[] maximalRotationsDegrees = settings.maximalRotations;
        long[] maxRotations = Arrays.stream( maximalRotationsDegrees ).mapToLong( x -> ( long ) x ).toArray();
        long[] minRotations = Arrays.stream( maximalRotationsDegrees ).mapToLong( x -> ( long ) - x ).toArray();
        this.rotationInterval = new FinalInterval( minRotations, maxRotations );
    }

    private RealTransform createAffineTransform( RotationAndTransformation rotationAndTransformation )
    {

        if ( rotationAndTransformation.rotation.size() == 1 )
        {
            return createAffineTransform2D( rotationAndTransformation );
        }

        if ( rotationAndTransformation.rotation.size() == 3)
        {
            return createAffineTransform3D( rotationAndTransformation );
        }

        return null;

    }

    private RealTransform createAffineTransform3D( RotationAndTransformation rotationAndTransformation )
    {
        RealTransform affineTransform;
        AffineTransform3D affineTransform3D = new AffineTransform3D();
        for ( int d = 0; d < rotationAndTransformation.rotation.size(); ++d )
        {
            affineTransform3D.rotate( d, asRadian( rotationAndTransformation.rotation.get( d ) ) );
        }
        affineTransform3D.translate( rotationAndTransformation.translation );
        affineTransform = affineTransform3D;
        return affineTransform;
    }

    private RealTransform createAffineTransform2D( RotationAndTransformation rotationAndTransformation )
    {
        RealTransform affineTransform;
        AffineTransform2D affineTransform2D = new AffineTransform2D();
        affineTransform2D.rotate( asRadian( rotationAndTransformation.rotation.get( 0 ) ) );
        affineTransform2D.translate( rotationAndTransformation.translation );
        affineTransform = affineTransform2D;
        return affineTransform;
    }

    private ArrayList< Long > initialize()
    {
        rotationAndTransformationList = new ArrayList<>(  );

        ArrayList< Long > rotation = new ArrayList<>();
        for ( int d = 0; d < rotationInterval.numDimensions(); ++d )
        {
            rotation.add( UNSET_ROTATION );
        }

        return rotation;

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

    private void determineBestTranslationForEachRotation( ArrayList< Long > rotation )
    {
        if ( rotation.contains( UNSET_ROTATION ) )
        {
            int d = rotation.indexOf( UNSET_ROTATION );
            for ( long r = rotationInterval.min( d ); r <= rotationInterval.max( d ); r += rotationStep )
            {
                rotation.set( d, r );
                determineBestTranslationForEachRotation( new ArrayList<>( rotation ) );
            }
        }
        else
        {

            RandomAccessible moving = rotateMoving( rotation );

            /*  FOR DEBUGGING
            if ( rotation.get ( 0 ) == 0 && rotation.get( 1 ) == 0 && rotation.get( 2 ) == -10 )
            {
                Services.ij.ui().show( Views.interval( moving, fixed ) );
                Services.ij.ui().show( Views.interval( fixed, fixed ) );
            }
            */

            translationFinder.findTransform( fixed, moving, filterSequence );
            addRotationAndTranslation( rotation, translationFinder );
        }
    }

    private void addRotationAndTranslation( ArrayList< Long > rotation, TransformFinderTranslationPhaseCorrelation translationFinder )
    {
        RotationAndTransformation rotationAndTransformation = new RotationAndTransformation();
        rotationAndTransformation.phaseCorrelation = translationFinder.phaseCorrelation();
        rotationAndTransformation.rotation = new ArrayList<>( rotation );
        rotationAndTransformation.translation = translationFinder.translation();
        rotationAndTransformationList.add( rotationAndTransformation );
    }

    private RandomAccessible< R > rotateMoving( ArrayList< Long > rotation )
    {

        if ( rotation.size() == 1 )
        {
            AffineTransform2D rotation2D = new AffineTransform2D();
            rotation2D.rotate(  asRadian( rotation.get( 0 ) ) );
            return InputViews.transformExtendingOutOfBoundsPixels( moving, rotation2D );
        }

        if ( rotation.size() == 3)
        {
            AffineTransform3D rotation3D = new AffineTransform3D();
            for ( int d = 0; d < rotation.size(); ++d )
            {
                rotation3D.rotate( d, asRadian( rotation.get( d ) ) );
            }
            return InputViews.transformExtendingOutOfBoundsPixels( moving, rotation3D );
        }

        return null;

    }

    private double asRadian( long rotation )
    {
        return ( Math.PI / 180.D ) * rotation;
    }

    public String asString()
    {
        if ( bestRotationAndTransformation == null ) return "No transformation determined.";

        return bestRotationAndTransformation.toString();
    }

    private class RotationAndTransformation
    {
        ArrayList< Long > rotation;
        double[] translation;
        double phaseCorrelation;

        public String toString()
        {
            String rotationString = rotation.stream()
                    .map( Object::toString )
                    .collect( Collectors.joining(", ") );

            String translationString = Arrays.stream( translation )
                    .mapToObj( Double::toString )
                    .collect( Collectors.joining(", ") );

            String out = "";
            out += "Rotation: " + rotationString;
            out += "; Translation: " + translationString;
            out += "; Phase-correlation: " + phaseCorrelation;

            return out;
        }
    }

}
