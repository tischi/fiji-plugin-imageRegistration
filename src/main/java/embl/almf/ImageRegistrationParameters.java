package embl.almf;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ImageRegistrationParameters {

    public final static String INPUT_SEQUENCE_AXIS = "sequenceAxis";
    public final static String INPUT_SEQUENCE_MIN = "sequenceMin";
    public final static String INPUT_SEQUENCE_MAX = "sequenceMax";

    public static ArrayList< String > getAxisNamesAsStringList( Dataset dataset )
    {
        ArrayList< String > axisTypes = new ArrayList<>(  );
        for (int d = 0; d < dataset.numDimensions(); d++)
        {
            axisTypes.add( dataset.axis( d ).type().toString() );
        }

        return axisTypes;

    }

}
