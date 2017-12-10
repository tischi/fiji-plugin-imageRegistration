package embl.almf.filter;

import embl.almf.views.ThresholdView;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public class ThresholdFilterView<T extends RealType<T> > {

    private Map< String, Object > parameters;

    public ThresholdFilterView( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    public RandomAccessible< T > filter( RandomAccessible< T > input )
    {
        T threshold = ( T ) parameters.get( ImageFilterParameters.THRESHOLD_VALUE );

        return new ThresholdView ( input, threshold );
    }

}
