package de.embl.cba.registration.filter;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class ImageFilterFactory < R extends RealType< R > & NativeType< R > > {

    public static ImageFilter create( FilterType filterType, FilterSettings settings )
    {

        switch ( filterType )
        {
            case None: return new ImageFilterNone( settings );
            case Gauss: return new ImageFilterGauss( settings );
            case Threshold: return new ImageFilterThreshold( settings );
            case DifferenceOfGaussian: return new ImageFilterDog( settings );
            case SubSample: return new ImageFilterSubSample( settings );
            case AsArrayImg: return new ImageFilterAsArrayImg( settings );
            case Gradient: return new ImageFilterGradient( settings );
            default: return new ImageFilterNone( settings );
        }

        /*
        if ( filterType.equals( FilterType.Gauss ) )
        {
            return new ImageFilterGauss( settings );
        }

        if ( filterType.equals ( FilterType.None ) )
        {
            return new ImageFilterNone( settings );
        }

        if ( filterType.equals ( FilterType.Threshold ) )
        {
            return new ImageFilterThreshold( settings );
        }

        if ( filterType.equals ( FilterType.DifferenceOfGaussian ) )
        {
            return new ImageFilterDog( settings );
        }

        if ( filterType.equals ( FilterType.SubSample ) )
        {
            return new ImageFilterSubSample( settings );
        }

        if ( filterType.equals ( FilterType.AsArrayImg ) )
        {
            return new ImageFilterAsArrayImg( settings );
        }

        return new ImageFilterNone( settings );
        */

    }

}
