package embl.almf;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ImageRegistrationParameters {

    public final static String SEQUENCE_AXIS = "Sequence dimension";
    public final static String SEQUENCE_MIN = "Sequence min";
    public final static String SEQUENCE_MAX = "Sequence max";

    public static ArrayList< String > getAxisTypesAsStringList( Dataset dataset )
    {
        ArrayList< String > axisTypes = new ArrayList<>(  );
        for (int d = 0; d < dataset.numDimensions(); d++)
        {
            axisTypes.add( dataset.axis( d ).type().toString() );
        }

        return axisTypes;

    }

}
