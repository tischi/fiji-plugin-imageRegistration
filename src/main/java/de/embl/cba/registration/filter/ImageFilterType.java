package de.embl.cba.registration.filter;

public enum ImageFilterType
{
    Threshold,
    Gauss,
    DifferenceOfGaussian,  // actually a DoG
    EnhanceEdgesAndThreshold;  // actually a DoG

}
