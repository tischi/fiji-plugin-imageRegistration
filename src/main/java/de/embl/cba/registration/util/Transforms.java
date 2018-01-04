package de.embl.cba.registration.util;

import de.embl.cba.registration.transform.Translation1D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

public class Transforms
{
    public static RealTransform translationAsRealTransform( double[] translation )
    {
        if ( translation.length == 1 ) return new Translation1D( translation );

        if ( translation.length == 2 ) return new Translation2D( translation );

        if ( translation.length == 3 ) return new Translation3D( translation );

        return new Translation( translation );

    }
}
