package embl.almf.filter;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static embl.almf.filter.ImageFilterParameters.*;


public class ImageFilterDog
    < R extends RealType< R > & NativeType< R > >
    implements ImageFilter< R, FloatType > {

    private Map< String, Object > parameters;

    public ImageFilterDog( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    @Override
    public RandomAccessibleInterval< FloatType > filter(
            RandomAccessibleInterval< R > source )
    {
        double[] sigmaSmaller = (double []) parameters.get( DOG_SIGMA_SMALLER );
        double[] sigmaLarger = (double []) parameters.get( DOG_SIGMA_LARGER );
        ExecutorService service = (ExecutorService) parameters.get( EXECUTOR_SERVICE );

        // create target image with same offset as source image
        //
        RandomAccessibleInterval< FloatType > output =
                Views.translate(
                    ArrayImgs.floats( Intervals.dimensionsAsLongArray( source ) ),
                            Intervals.minAsLongArray( source ) );

        RandomAccessible< R > extendedSource = Views.extendBorder( source );

        DifferenceOfGaussian.DoG(
                sigmaSmaller,
                sigmaLarger,
                extendedSource,
                output,
                service );

        return output;
    }
}


