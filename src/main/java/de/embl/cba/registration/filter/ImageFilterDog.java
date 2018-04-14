package de.embl.cba.registration.filter;

import de.embl.cba.registration.Services;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Map;
import java.util.concurrent.ExecutorService;


public class ImageFilterDog< R extends RealType< R > & NativeType< R > > implements ImageFilter< R, FloatType > {

    private final double[] sigmaSmaller;
    private final double[] sigmaLarger;

    public ImageFilterDog( FilterSettings settings )
    {

        sigmaSmaller = settings.gaussSigma;

        sigmaLarger = new double[ sigmaSmaller.length ];
        for ( int i = 0; i < sigmaSmaller.length; ++i )
        {
            sigmaLarger[ i ] = 2.0D * sigmaSmaller[ i ];
        }

    }

    @Override
    public RandomAccessibleInterval< FloatType > filter( RandomAccessibleInterval< R > source )
    {
        // toArrayImg target image with same offset as source image
        //
        RandomAccessibleInterval< FloatType > output =
                Views.translate( ArrayImgs.floats( Intervals.dimensionsAsLongArray( source ) ), Intervals.minAsLongArray( source ) );

        RandomAccessible< R > extendedSource = Views.extendBorder( source );

        // they subtract smaller from larger => needed to invert smaller and larger
        DifferenceOfGaussian.DoG(
                sigmaLarger,
                sigmaSmaller,
                extendedSource,
                output,
                Services.executorService );

        return output;
    }
}


