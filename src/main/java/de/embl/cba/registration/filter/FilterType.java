package de.embl.cba.registration.filter;

public enum FilterType
{
    None,
    Threshold,
    Gauss,
    DifferenceOfGaussian,
    DifferenceOfGaussianAndThreshold,
    ThresholdAndDifferenceOfGaussian,
    ThresholdAndGradient,
    SubSample,
    AsArrayImg,
    Gradient
}
