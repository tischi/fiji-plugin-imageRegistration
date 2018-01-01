package de.embl.cba.registration.filter;

import java.util.ArrayList;

public class FilterSettings
{
    public final static String FILTER_TYPE = "Image apply type";
    public final static String SEQUENCE = "Image apply sequence";
    public final static String GAUSS_SIGMA = "Sigma";
    public final static String DOG_SIGMA_SMALLER = "Smaller sigma";
    public final static String DOG_SIGMA_LARGER = "Larger sigma";
    public final static String THRESHOLD_MIN_VALUE = "Threshold min";
    public final static String THRESHOLD_MAX_VALUE = "Threshold max";
    public final static String SUB_SAMPLING = "Subsampling";

    public FilterType filterType;
    public ArrayList< FilterType > filterTypes;
    public double[] gaussSigma;
    public double[] gaussSigmaSmaller;
    public double[] gaussSigmaLarger;
    public double thresholdMin;
    public double thresholdMax;
    public long[] subSampling;
}
