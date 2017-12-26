package de.embl.cba.registration.axessettings;

import net.imglib2.FinalInterval;

public class TransformableAxesSettings
{
    public int[] axes;
    public FinalInterval referenceInterval;
    public FinalInterval inputInterval;

    public TransformableAxesSettings(
            int[] axes,
            FinalInterval referenceInterval,
            FinalInterval inputInterval )
    {
        this.axes = axes;
        this.referenceInterval = referenceInterval;
        this.inputInterval = inputInterval;
    }

    public int numDimensions()
    {
        return axes.length;
    }


}
