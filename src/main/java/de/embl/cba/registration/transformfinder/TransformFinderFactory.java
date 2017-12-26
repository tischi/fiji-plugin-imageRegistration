package de.embl.cba.registration.transformfinder;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

import static de.embl.cba.registration.transformfinder.TransformFinderParameters.*;


public abstract class TransformFinderFactory< R extends RealType< R > & NativeType< R > > {

    public static TransformFinder create( Map< String, Object > transformationParameters )
    {
        TransformationFinderType transformationFinderType
                = (TransformationFinderType) transformationParameters.get(
                        TRANSFORMATION_FINDER_TYPE );

        if ( transformationFinderType.equals(
                TransformationFinderType.Translation__PhaseCorrelation ) )
        {
            return new TransformFinderTranslationPhaseCorrelation( transformationParameters );
        }
        if ( transformationFinderType.equals(
                TransformationFinderType.Rotation_Translation__PhaseCorrelation ) )
        {
            return new TransformFinderRotationTranslationPhaseCorrelation( transformationParameters );
        }
        else
        {
            return null;
        }

    }

}
