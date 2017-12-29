package de.embl.cba.registration.transformfinder;

import java.util.Map;

public abstract class TransformFinderParameters
{

    public final static String MAXIMAL_TRANSLATIONS = "Maximal translations";
    public final static String MAXIMAL_ROTATIONS = "Maximal rotations";
    public final static String TRANSFORMATION_FINDER_TYPE = "Transformation finder type";
    public final static String IMAGE_FILTER = "Image apply";

    public static TransformFinderType getType( Map< String, Object > map )
    {
        return (TransformFinderType ) map.get( TRANSFORMATION_FINDER_TYPE );
    }

}