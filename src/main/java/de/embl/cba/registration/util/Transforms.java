package de.embl.cba.registration.util;

import de.embl.cba.registration.transform.Translation1D;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.realtransform.*;

public abstract class Transforms < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{
    public static RealTransform translationAsRealTransform( double[] translation )
    {
        if ( translation.length == 1 ) return new Translation1D( translation );

        if ( translation.length == 2 ) return new Translation2D( translation );

        if ( translation.length == 3 ) return new Translation3D( translation );

        return new Translation( translation );
    }


    public static < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > > RealTransform identityAffineTransformation( int numDimensions )
    {
        if ( numDimensions == 2 )
        {
            return (T) new AffineTransform2D();
        }
        else if ( numDimensions == 3 )
        {
            return (T) new AffineTransform3D();
        }
        else
        {
            return (T) new AffineTransform( numDimensions );
        }
    }
}
