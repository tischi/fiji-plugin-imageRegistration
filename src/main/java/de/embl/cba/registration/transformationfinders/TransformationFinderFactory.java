package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.TransformationType;
import de.embl.cba.registration.filter.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

import static de.embl.cba.registration.transformationfinders.TransformationFinderParameters.*;


public abstract class TransformationFinderFactory < R extends RealType< R > & NativeType< R > > {

    public static TransformationFinder create(
            Map< String, Object > transformationParameters,
            ImageFilter imageFilter )
    {
        TransformationType transformationType
                = (TransformationType) transformationParameters.get(
                        TRANSFORMATION_FINDER_TYPE );

        if ( transformationType.equals( TransformationType.Translation ) )
        {
            return new TransformationFinderTranslationPhaseCorrelation(
                    transformationParameters, imageFilter );
        }
        else
        {
            // TODO: throw an error
            return null;
        }

    }

}
