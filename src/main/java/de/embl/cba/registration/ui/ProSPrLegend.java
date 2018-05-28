package de.embl.cba.registration.ui;

import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.util.ArrayList;

public class ProSPrLegend extends JPanel implements ActionListener
{
    public static final String CHANGE_COLOR = "Change color";
    public static final String ADAPT_BRIGHTNESS = "Adapt brightness";
    public static final String REMOVE = "Remove";
    public static final String CANCELLED = "Cancelled";
    protected ArrayList< JButton > buttons;
    JFrame frame;
    final ProSPr prospr;

    public ProSPrLegend( ProSPr prospr )
    {
        this.prospr = prospr;
        buttons = new ArrayList<>(  );
        createGUI();
    }


    public void add( ProSPrDataSource dataSource )
    {
        if ( getButton( dataSource.name ) != null ) return;

        JButton button = createButton( dataSource );

        buttons.add( button );

        refreshGui();

    }



    private JButton createButton( ProSPrDataSource dataSource )
    {

        JButton button = new JButton( padded( dataSource.name ) );
        button.setOpaque( true );
        button.setBackground( dataSource.color );
        button.addActionListener( this );
        button.setAlignmentX( Component.CENTER_ALIGNMENT );
        //Font font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font.ttf"));
        //button.setFont( new Font.createFont( Font.TRUETYPE_FONT ) );

        add( button );

        return ( button );
    }


    private String padded( String string )
    {
        return string;
        //return String.format("%20s", string);
    }


    private void removeButton( String name )
    {
        remove( getButton( name ) );
        refreshGui();
    }

    private void refreshGui()
    {
        this.revalidate();
        this.repaint();
        frame.pack();
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {

        for ( int i = 0; i < buttons.size(); ++i )
        {
            if ( e.getSource() == buttons.get( i ) )
            {
                showActionUI( buttons.get( i ).getText().trim() );
            }
        }

    }

    public void showActionUI( String dataSourceName )
    {
        String action = getActionFromUI();

        switch ( action )
        {
            case REMOVE:
                prospr.hideDataSource( dataSourceName );
                removeButton( dataSourceName );
                break;
            case CHANGE_COLOR:
                changeColorViaUI( dataSourceName );
                break;
            case ADAPT_BRIGHTNESS:
                prospr.setBrightness( dataSourceName );
                break;
            case CANCELLED:
                break;
        }

    }

    private String getActionFromUI()
    {
        GenericDialog genericDialog = new GenericDialog( "Choose action " );

        genericDialog.addChoice( "Action", new String[]
                {
                        CHANGE_COLOR,
                        ADAPT_BRIGHTNESS,
                        REMOVE
                },
                CHANGE_COLOR);

        genericDialog.showDialog();

        if ( genericDialog.wasCanceled() ) return CANCELLED;

        return genericDialog.getNextChoice();
    }

    public void changeColorViaUI( String name )
    {
        Color color = JColorChooser.showDialog( null, "Select color for " + name, null );

        if ( color != null )
        {
            prospr.setDataSourceColor( name, color );
            getButton( name ).setBackground( color );
        }

    }

    private void changeButtonColor( String name )
    {

    }


    private JButton getButton( String name )
    {
        for ( JButton button : buttons )
        {
            if ( button.getText().trim().equals( name ) )
            {
                return  button ;
            }
        }

        return null;
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createGUI( )
    {

        //Create and set up the window.
        frame = new JFrame( "" );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        //Create and set up the content pane.
        setOpaque( true ); //content panes must be opaque
        setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );

        frame.setContentPane( this );

        //Display the window.
        frame.pack();
        frame.setVisible( true );
    }


}