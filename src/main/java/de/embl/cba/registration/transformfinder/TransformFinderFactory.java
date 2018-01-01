package de.embl.cba.registration.transformfinder;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class TransformFinderFactory< R extends RealType< R > & NativeType< R > > {

    public static TransformFinder create( TransformFinderType type,TransformSettings settings )
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
