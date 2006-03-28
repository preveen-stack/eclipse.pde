package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.framework.Constants;

public class RemoveExportPackageResolution extends AbstractManifestMarkerResolution
		implements IMarkerResolution {
	
	String fPackage;

	public RemoveExportPackageResolution(int type, String pkgName) {
		super(type);
		fPackage = pkgName;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle)model.getBundle();
		ExportPackageHeader header = (ExportPackageHeader)bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header != null)
			header.removePackage(fPackage);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_label, fPackage);
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_description, fPackage);
	}

}
