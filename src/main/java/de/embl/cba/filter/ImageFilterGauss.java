package de.embl.cba.filter;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;

import java.util.Map;

import net.imglib2.algorithm.gauss3.*;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class ImageFilterGauss
        < R extends RealType< R > & NativeType < R > >
        implements ImageFilter< R, FloatType > {

    private Map< String, Object > parameters;

    public ImageFilterGauss( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    @Override
    public RandomAccessibleInterval< FloatType > filter(
            RandomAccessibleInterval< R > source )
    {
        double[] sigmas = (double []) parameters.get( ImageFilterParameters.GAUSS_SIGMA );

        // create target image with same offset as source image
        //
        final ImgFactory< FloatType > factory = new ArrayImgFactory< >();
        RandomAccessibleInterval< FloatType > target =
                Views.translate(
                        factory.create( source, new FloatType(  ) ),
                        Intervals.minAsLongArray( source ) );

        RandomAccessible< R > extendedSource = Views.extendBorder( source );

        try
        {
            Gauss3.gauss( sigmas, extendedSource, target );
        }
        catch ( IncompatibleTypeException e )
        {
            e.printStackTrace();
        }

        return target;
    }
}
