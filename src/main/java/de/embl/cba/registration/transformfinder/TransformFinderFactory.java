package de.embl.cba.registration.transformfinder;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public abstract class TransformFinderFactory< R extends RealType< R > & NativeType< R > > {

    public static TransformFinder create( TransformFinderType type,TransformFinderSettings settings )
    {
        if ( type.equals( TransformFinderType.Translation__PhaseCorrelation ) )
        {
            return new TransformFinderTranslationPhaseCorrelation( settings );
        }

        if ( type.equals( TransformFinderType.Rotation_Translation__PhaseCorrelation ) )
        {
            return new TransformFinderRotationTranslationPhaseCorrelation( settings );
        }

        return null;
    }

}
