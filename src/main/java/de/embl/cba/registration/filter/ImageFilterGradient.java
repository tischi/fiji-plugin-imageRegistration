package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gradient.PartialDerivative;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Map;

public class ImageFilterGradient< R extends RealType< R > & NativeType< R > > implements ImageFilter< R, FloatType >
{

    private Map< String, Object > parameters;
    int d;
    double[] sigma;
    ImageFilterGauss gauss;

    public ImageFilterGradient( FilterSettings settings )
    {
        d = settings.gradientAxis;
        gauss = new ImageFilterGauss( settings );
    }

    @Override
    public RandomAccessibleInterval< FloatType > filter( RandomAccessibleInterval< R > source )
    {

        RandomAccessibleInterval< FloatType > gaussFilteredSource = gauss.filter( source );

        RandomAccessibleInterval< FloatType > target = getTargetImage( source );

        PartialDerivative.gradientCentralDifference( Views.extendBorder( gaussFilteredSource ), target, d );

        return target;
    }

    private RandomAccessibleInterval< FloatType > getTargetImage( RandomAccessibleInterval< R > source )
    {
        final ImgFactory< FloatType > factory = new ArrayImgFactory< >();
        return Views.translate(
                factory.create( source, new FloatType(  ) ),
                Intervals.minAsLongArray( source ) );
    }
}
