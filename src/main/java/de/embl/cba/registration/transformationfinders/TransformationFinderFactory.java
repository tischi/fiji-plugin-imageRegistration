package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.filter.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

import static de.embl.cba.registration.transformationfinders.TransformationFinderParameters.*;


public abstract class TransformationFinderFactory < R extends RealType< R > & NativeType< R > > {

    public static TransformationFinder create( Map< String, Object > transformationParameters )
    {
        TransformationFinderType transformationFinderType
                = (TransformationFinderType) transformationParameters.get(
                        TRANSFORMATION_FINDER_TYPE );

        if ( transformationFinderType.equals(
                TransformationFinderType.Translation__PhaseCorrelation ) )
        {
            return new TransformationFinderTranslationPhaseCorrelation( transformationParameters );
        }
        if ( transformationFinderType.equals(
                TransformationFinderType.Rotation_Translation__PhaseCorrelation ) )
        {
            return new TransformationFinderRotationTranslationPhaseCorrelation( transformationParameters );
        }
        else
        {
            return null;
        }

    }

}
