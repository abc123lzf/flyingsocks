/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client.gui.swt;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/1/25 18:24
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class TreeDemo {

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Tree Example");

        final Text text = new Text(shell, SWT.BORDER);
        text.setBounds(0, 270, 290, 25);

        final Tree tree = new Tree(shell, SWT.BORDER | SWT.V_SCROLL
                | SWT.H_SCROLL);
        tree.setSize(290, 260);
        shell.setSize(300, 330);

        for (int loopIndex0 = 0; loopIndex0 < 10; loopIndex0++) {
            TreeItem treeItem0 = new TreeItem(tree, 0);
            treeItem0.setText("Level 0 Item " + loopIndex0);
            for (int loopIndex1 = 0; loopIndex1 < 10; loopIndex1++) {
                TreeItem treeItem1 = new TreeItem(treeItem0, 0);
                treeItem1.setText("Level 1 Item " + loopIndex1);
                for (int loopIndex2 = 0; loopIndex2 < 10; loopIndex2++) {
                    TreeItem treeItem2 = new TreeItem(treeItem1, 0);
                    treeItem2.setText("Level 2 Item " + loopIndex2);
                }
            }
        }

        tree.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (event.detail == SWT.CHECK) {
                    text.setText(event.item + " was checked.");
                } else {
                    text.setText(event.item + " was selected");
                }
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }
}