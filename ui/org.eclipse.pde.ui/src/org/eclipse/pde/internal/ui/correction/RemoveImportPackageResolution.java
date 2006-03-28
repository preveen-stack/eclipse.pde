package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveImportPackageResolution extends AbstractManifestMarkerResolution {
	
	private String fPkgName;

	public RemoveImportPackageResolution(int type, String packageName) {
		super(type);
		fPkgName = packageName;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle)model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader)bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null)
			header.removePackage(fPkgName);
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveImportPkgResolution_description, fPkgName);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveImportPkgResolution_label, fPkgName);
	}

}
