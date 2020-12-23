package com.lzf.flyingsocks.client.gui.swt;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * @author lizifan 695199262@qq.com
 * SWT选择事件监听器 用于适应Lambda表达式
 */
@FunctionalInterface
interface SimpleSelectionListener extends SelectionListener {

    /**
     * @see SelectionListener#widgetSelected(SelectionEvent)
     */
    @Override
    void widgetSelected(SelectionEvent e);

    /**
     * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
     */
    default void widgetDefaultSelected(SelectionEvent e) {
        // NOOP
    }
}
