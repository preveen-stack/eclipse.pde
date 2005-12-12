package org.eclipse.pde.internal.ui.editor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class EditorUtilities {
	
	public static final int[][] F_ICON_DIMENSIONS = new int[][] {
		{16, 16}, {32, 32}, {48, 48}, {64, 64}, {128, 128}
	};
	public static final int F_EXACTIMAGE = 0;
	public static final int F_MAXIMAGE = 1;
	public static final int F_ICOIMAGE = 2;
	
	static class MessageSeverity {
		String fMessage;
		int fSeverity = -1;
		MessageSeverity(String message) {
			fMessage = message;
		}
		MessageSeverity(String message, int severity) {
			this(message);
			fSeverity = severity;
		}
		String getMessage() { return fMessage; }
		int getSeverity() { return fSeverity; }
	}
	
	
	/*
	 * dimensions must be atleast of size 2
	 * dimensions[0] = width of image
	 * dimensions[1] = height of image
	 * dimensions[2] = width2
	 * dimensions[3] = height2
	 */
	public static boolean isValidImage(IEditorValidationProvider provider, String definingPluginId, int[] dimensions, int checkType) {
		String imagePath = provider.getProviderValue();
		String message = null;
		int severity = -1;
		if (imagePath.length() == 0) {// ignore empty strings
			return true;
		}
		IPath path = getFullPath(new Path(imagePath), definingPluginId);
		try {
			URL url = new URL(path.toString());
			ImageLoader loader = new ImageLoader();
			InputStream stream = url.openStream();
			ImageData[] idata = loader.load(stream);
			stream.close();
			if (idata.length == 0) {
				message = NLS.bind(PDEUIMessages.EditorUtilities_noImageData, imagePath);
			} else {
				MessageSeverity ms = null;
				switch (checkType) {
				case F_EXACTIMAGE:
					ms = getMS_exactImageSize(idata[0], dimensions);
					break;
				case F_MAXIMAGE:
					ms = getMS_maxImageSize(idata[0], dimensions);
					break;
				case F_ICOIMAGE:
					ms = getMS_icoImage(idata);
				}
				if (ms != null) {
					message = ms.getMessage();
					severity = ms.getSeverity();
				}
			}
		} catch (SWTException e) {
			message = PDEUIMessages.EditorUtilities_pathNotValidImage;
		} catch (MalformedURLException e) {
			message = PDEUIMessages.EditorUtilities_invalidFilePath;
		} catch (IOException e) {
			message = PDEUIMessages.EditorUtilities_invalidFilePath;
		}	
		
		if (message != null) {
			String fieldName = null;
			if (provider instanceof FormEntry)
				fieldName = ((FormEntry)provider).getLabelText();
			if (fieldName != null)
				fieldName = " " + fieldName + " " + imagePath; //$NON-NLS-1$ //$NON-NLS-2$
			else
				fieldName = ""; //$NON-NLS-1$
			provider.getValidator().setMessage(fieldName + " " + message); //$NON-NLS-1$
			if (severity >= 0)
				provider.getValidator().setSeverity(severity);
		}
		
		return message == null;
	}
	
	private static MessageSeverity getMS_icoImage(ImageData[] imagedata) {
		if (imagedata.length < 6)
			return new MessageSeverity(PDEUIMessages.EditorUtilities_icoError);
		return null;
	}

	private static MessageSeverity getMS_exactImageSize(ImageData imagedata, int[] sizes) {
		if (sizes.length < 2)
			return null;
		int width = imagedata.width;
		int height = imagedata.height;
		if (width != sizes[0] || height != sizes[1])
			return new MessageSeverity(
					NLS.bind(PDEUIMessages.EditorUtilities_incorrectSize,
							getSizeString(width, height),
							getSizeString(sizes[0], sizes[1])));
		return null;
	}
	
	private static MessageSeverity getMS_maxImageSize(ImageData imagedata, int[] sizes) {
		if (sizes.length < 2)
			return null;
		int width = imagedata.width;
		int height = imagedata.height;
		if (width > sizes[0] || height > sizes[1]) {
			return new MessageSeverity(
					NLS.bind(PDEUIMessages.EditorUtilities_imageTooLarge,
							getSizeString(width, height),
							getSizeString(sizes[0], sizes[1])));
		} else if (sizes.length > 2) {
			if (width > sizes[2] || height > sizes[3])
				return new MessageSeverity(
						NLS.bind(PDEUIMessages.EditorUtilities_imageTooLargeInfo, getSizeString(sizes[2], sizes[3])),
						IMessageProvider.INFORMATION);
		}
		return null;
	}
	
	private static String getSizeString(int width, int height) {
		return width + " x " + height; //$NON-NLS-1$
	}
	
	private static IPath getFullPath(IPath path, String definingPluginId) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(path);
		if (resource != null)
			return new Path("file:" + resource.getLocation().toString());
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(definingPluginId);
		if (model != null && model.getInstallLocation() != null) {
			IPath newPath = new Path(model.getInstallLocation()).append(path);
			IContainer container = root.getContainerForLocation(newPath);
			if (container != null)
				return container.getFullPath();
			File file = new File(model.getInstallLocation());
			if (file.isFile() && CoreUtility.jarContainsResource(file, path.toString(), false))
				return new Path("jar:file:" + file.getAbsolutePath() + "!/" + path.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			return new Path("file:" + newPath.toString());
		}
		return path;
	}
	
	private static IResource getImageResource(String value, boolean showWarning, String definingPluginId) {
		if (value == null)
			return null;
		IPath path = new Path(value);
		if (path.isEmpty()){
			if (showWarning)
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); // 
			return null;
		}
		if (!path.isAbsolute()) {
			path = getFullPath(path, definingPluginId);
		}
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		return root.findMember(path);
	}
	
	public static void openImage(String value, String definingPluginId) {
		IResource resource = getImageResource(value, true, definingPluginId);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.AboutSection_open, PDEUIMessages.AboutSection_warning); // 
		} catch (PartInitException e) {
		}		
	}
}
