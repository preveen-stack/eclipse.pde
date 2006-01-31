package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

public class XMLContentMergeViewerCreator implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		return new XMLContentMergeViewer(parent, config);
	}
}
