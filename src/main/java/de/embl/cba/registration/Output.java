package de.embl.cba.registration;

import bdv.util.AxisOrder;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;

public class Output < R extends RealType< R > & NativeType< R > >
{
    public ImgPlus< R > transformedImgPlus;
    public ImgPlus< R > referenceImgPlus;
    public AxisOrder transformedAxisOrder;
    public AxisOrder referenceAxisOrder;
    public long transformedNumSpatialDimensions;
    public long referenceNumSpatialDimensions;
}
