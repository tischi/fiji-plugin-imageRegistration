package de.embl.cba.registration.axessettings;

public class SequenceAxisSettings
{
    public long min;
    public long max;
    public long ref;
    public int axis;

    public SequenceAxisSettings(int d, long min, long max, long ref )
    {
        this.axis = d;
        this.min = min;
        this.max = max;
        this.ref = ref;
    }

}
