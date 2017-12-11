package embl.almf.filter;

import net.imagej.ops.Ops;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import net.preibisch.mvrecon.process.interestpointregistration.pairwise.methods.ransac.RANSAC;

import java.util.Map;

import static embl.almf.filter.ImageFilterParameters.THRESHOLD_VALUE;

public class ImageFilterDogThreshold
        < R extends RealType< R > & NativeType< R > >
        implements ImageFilter< R, BitType > {


    ImageFilter imageFilterDog;
    ImageFilter imageFilterThreshold;

    public ImageFilterDogThreshold( Map< String, Object > parameters )
    {
        this.imageFilterDog = ImageFilterFactory.create( ImageFilterType.DOG, parameters );
        this.imageFilterThreshold = ImageFilterFactory.create( ImageFilterType.THRESHOLD, parameters );
    }

    @Override
    public RandomAccessibleInterval< BitType > filter( RandomAccessibleInterval< R > source )
    {

        RandomAccessibleInterval dog = imageFilterDog.filter( source );
        RandomAccessibleInterval threshold = imageFilterThreshold.filter( dog );

        return threshold;
    }

}
