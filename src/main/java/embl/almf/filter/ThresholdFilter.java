package embl.almf.filter;

import embl.almf.ImageRegistration;
import embl.almf.views.ThresholdView;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public class ThresholdFilter<T extends RealType<T> > {

    private Map< String, Object > parameters;

    public ThresholdFilter( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    public RandomAccessible< T > filter( RandomAccessible< T > input )
    {
        T threshold = ( T ) parameters.get( ImageRegistration.FILTER_THRESHOLD_VALUE );

        return new ThresholdView ( input, threshold );
    }

}
