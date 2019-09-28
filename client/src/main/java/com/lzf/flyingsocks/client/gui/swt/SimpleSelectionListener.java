package com.lzf.flyingsocks.client.gui.swt;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

@FunctionalInterface
interface SimpleSelectionListener extends SelectionListener {

    @Override
    void widgetSelected(SelectionEvent e);


    default void widgetDefaultSelected(SelectionEvent e) { }
}
