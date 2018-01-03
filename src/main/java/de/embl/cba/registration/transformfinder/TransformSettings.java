package de.embl.cba.registration.transformfinder;

public class TransformSettings
{
    public final static String MAXIMAL_TRANSLATIONS = "Maximal translation";
    public final static String MAXIMAL_ROTATIONS = "Maximal rotation";
    public final static String TRANSFORMATION_FINDER_TYPE = "Transformation finder type";

    public double[] maximalTranslations;
    public double[] maximalRotations;
    public TransformFinderType transformFinderType;
}
