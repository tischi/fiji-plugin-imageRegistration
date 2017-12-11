package embl.almf.gui.wizard;

import net.imagej.Dataset;
import net.imagej.ops.OpService;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.NumberWidget;

import java.util.HashMap;
import java.util.Map;

import static embl.almf.ImageRegistrationParameters.*;

@Plugin(type = Command.class,
        initializer = "initFields")
public class SetSequenceAxisParameters extends DynamicCommand {

    // resolved

    @Parameter
    private Dataset dataset;

    @Parameter
    private String sequenceAxis;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter
    private CommandService commandService2;

    // new



    @Override
    public void run()
    {
        Map< String, Object > parameters = new HashMap<>(  );
        parameters.put( "dataset" , dataset );
        parameters.put( INPUT_SEQUENCE_AXIS , getInfo().getInput( INPUT_SEQUENCE_AXIS ).getValue( this ) );
        parameters.put( INPUT_SEQUENCE_MIN , getInfo().getInput( INPUT_SEQUENCE_MIN ).getValue( this ) );
        parameters.put( INPUT_SEQUENCE_MAX , getInfo().getInput( INPUT_SEQUENCE_MAX ).getValue( this ) );

    }

    protected void initFields()
    {

        int n = dataset.numDimensions();
        int sequenceAxisId = getAxisNamesAsStringList( dataset ).indexOf( sequenceAxis );


        final MutableModuleItem<String> messageItem =
                addInput("Message", String.class);
        messageItem.setPersisted(false);
        messageItem.setVisibility( ItemVisibility.MESSAGE );
        messageItem.setValue(this, "Sequence dimension");

        final MutableModuleItem<Long> minItem =
                addInput( INPUT_SEQUENCE_MIN, Long.class);
        minItem.setPersisted( true );
        minItem.setLabel( INPUT_SEQUENCE_MIN );
        minItem.setWidgetStyle( NumberWidget.SLIDER_STYLE  );
        minItem.setMinimumValue( dataset.min( sequenceAxisId ) );
        minItem.setMaximumValue( dataset.max( sequenceAxisId ) );
        minItem.setValue( this, dataset.min( sequenceAxisId ) );

        final MutableModuleItem<Long> maxItem =
                addInput( INPUT_SEQUENCE_MAX, Long.class);
        maxItem.setPersisted( true );
        maxItem.setLabel( INPUT_SEQUENCE_MAX );
        maxItem.setWidgetStyle( NumberWidget.SLIDER_STYLE  );
        maxItem.setMinimumValue( dataset.min( sequenceAxisId ) );
        maxItem.setMaximumValue( dataset.max( sequenceAxisId ) );
        minItem.setValue( this, dataset.max( sequenceAxisId ) );

    }

}
