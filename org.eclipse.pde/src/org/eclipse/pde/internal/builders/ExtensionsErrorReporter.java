package org.eclipse.pde.internal.builders;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class ExtensionsErrorReporter extends XMLErrorReporter {
	
	public ExtensionsErrorReporter(IFile file) {
		super(file);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.XMLErrorReporter#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length)
			throws SAXException {
	}

	public void validateContent(IProgressMonitor monitor) {
		Element element = getDocumentRoot();
		String elementName = element.getNodeName();
		if (!"plugin".equals(elementName) && !"fragment".equals(elementName)) { //$NON-NLS-1$ //$NON-NLS-2$
			reportIllegalElement(element, CompilerFlags.ERROR);
		} else {
			/*int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ATTRIBUTE);
			if (severity != CompilerFlags.IGNORE) {
				NamedNodeMap attrs = element.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++) {
					reportUnknownAttribute(element, attrs.item(i).getNodeName(), severity);
				}
			}*/
			
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (monitor.isCanceled())
					break;
				Element child = (Element)children.item(i);
				String name = child.getNodeName();
				if (name.equals("extension")) { //$NON-NLS-1$
					validateExtension(child);
				} else if (name.equals("extension-point")) { //$NON-NLS-1$
					validateExtensionPoint(child);
				} /*else {
					severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
					if (severity != CompilerFlags.IGNORE)
						reportIllegalElement(child, severity);
				}*/
			}
		}
	}

	protected void validateExtension(Element element) {
		if (!assertAttributeDefined(element, "point", CompilerFlags.ERROR)) //$NON-NLS-1$
			return;
		String pointID = element.getAttribute("point"); //$NON-NLS-1$
		IPluginExtensionPoint point = PDECore.getDefault().findExtensionPoint(pointID);
		if (point == null) {
			int severity = CompilerFlags.getFlag(CompilerFlags.P_UNRESOLVED_EX_POINTS);
			if (severity != CompilerFlags.IGNORE) {
				report(PDE.getFormattedMessage(
					"Builders.Manifest.ex-point", pointID), //$NON-NLS-1$
					getLine(element, "point"), severity); //$NON-NLS-1$
			}
		} else {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			ISchema schema = registry.getSchema(pointID);
			if (schema != null) {
				validateElement(element, schema);
			}
		}
	}
	
	protected void validateElement(Element element, ISchema schema) {
		String elementName = element.getNodeName();
		ISchemaElement schemaElement = schema.findElement(elementName);
		
		ISchemaElement parentSchema = null;
		if (!"extension".equals(elementName)) { //$NON-NLS-1$
			Node parent = element.getParentNode();
			parentSchema = schema.findElement(parent.getNodeName());
		}
		
		if (parentSchema != null) {
			int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
			if (severity != CompilerFlags.IGNORE) {
				HashSet allowedElements = new HashSet();
				computeAllowedElements(parentSchema.getType(), allowedElements);
				if (!allowedElements.contains(elementName)) {
					reportIllegalElement(element, severity);
					return;
				}
			}
			
		}	
		boolean executable = false;
		if (schemaElement == null && parentSchema != null) {
			ISchemaAttribute attr = parentSchema.getAttribute(elementName);
			executable = (attr != null && attr.getKind() == ISchemaAttribute.JAVA);
		}
		
		if (executable) {
			validateJavaAttribute(element, element.getAttributeNode("class")); //$NON-NLS-1$
			return;
		} 		
		validateRequiredExtensionAttributes(element, schemaElement);
		validateExistingExtensionAttributes(element, element.getAttributes(), schemaElement);
		
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			validateElement((Element)children.item(i), schema);
		}
	}
	
	private void computeAllowedElements(ISchemaType type, HashSet elementSet) {
		if (type instanceof ISchemaComplexType) {
			ISchemaComplexType complexType = (ISchemaComplexType) type;
			ISchemaCompositor compositor = complexType.getCompositor();
			if (compositor != null)
				computeAllowedElements(compositor, elementSet);

			ISchemaAttribute[] attrs = complexType.getAttributes();
			for (int i = 0; i < attrs.length; i++) {
				if (attrs[i].getKind() == ISchemaAttribute.JAVA)
					elementSet.add(attrs[i].getName());
			}
		}
	}

	private void computeAllowedElements(ISchemaCompositor compositor,
			HashSet elementSet) {
		ISchemaObject[] children = compositor.getChildren();
		for (int i = 0; i < children.length; i++) {
			ISchemaObject child = children[i];
			if (child instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) child;
				ISchemaElement refElement = (ISchemaElement) ref
						.getReferencedObject();
				if (refElement != null)
					elementSet.add(refElement.getName());
			} else if (child instanceof ISchemaCompositor) {
				computeAllowedElements((ISchemaCompositor) child, elementSet);
			}
		}
	}


	
	private void validateRequiredExtensionAttributes(Element element, ISchemaElement schemaElement) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_NO_REQUIRED_ATT);
		if (severity == CompilerFlags.IGNORE)
			return;
		
		ISchemaAttribute[] attInfos = schemaElement.getAttributes();
		for (int i = 0; i < attInfos.length; i++) {
			ISchemaAttribute attInfo = attInfos[i];
			if (attInfo.getUse() == ISchemaAttribute.REQUIRED) {
				boolean found = element.getAttributeNode(attInfo.getName()) != null;
				if (!found && attInfo.getKind() == ISchemaAttribute.JAVA) {
					NodeList children = element.getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						if (attInfo.getName().equals(children.item(j).getNodeName())) {
							found = true;
							break;
						}
					}
				}
				if (!found) {
					reportMissingRequiredAttribute(element, attInfo.getName(), severity);
				}
			}
		}
	}
	
	private void validateExistingExtensionAttributes(Element element, NamedNodeMap attrs,
			ISchemaElement schemaElement) {
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr)attrs.item(i);
			ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
			if (attInfo == null) {
				HashSet allowedElements = new HashSet();
				computeAllowedElements(schemaElement.getType(), allowedElements);
				if (allowedElements.contains(attr.getName())) {
					validateJavaAttribute(element, attr);
				} else {
					int flag = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ATTRIBUTE);
					if (flag != CompilerFlags.IGNORE)
						reportUnknownAttribute(element, attr.getName(), flag);
				}
			} else {
				validateExtensionAttribute(element, attr, attInfo);
			}
		}
	}

	private void validateExtensionAttribute(Element element, Attr attr, ISchemaAttribute attInfo) {
		if (attInfo.getUse() == ISchemaAttribute.DEPRECATED) {
			report(PDE.getFormattedMessage("Builders.Manifest.deprecated-attribute", attr.getName()), //$NON-NLS-1$
					getLine(element, attr.getName()), CompilerFlags.WARNING);
		}
		
		ISchemaSimpleType type = attInfo.getType();
		ISchemaRestriction restriction = type.getRestriction();
		if (restriction != null) {
			validateRestrictionAttribute(element, attr, restriction);
		}
		
		int kind = attInfo.getKind();
		if (kind == ISchemaAttribute.JAVA) {
			validateJavaAttribute(element, attr);
		} else if (kind == ISchemaAttribute.RESOURCE) {
			validateResourceAttribute(element, attr);
		} else if (type.getName().equals("boolean")) { //$NON-NLS-1$
			validateBoolean(element, attr);
		} 
		
		if (attInfo.isTranslatable()) {
			validateTranslatableString(element, attr);
		}
	}

	protected void validateExtensionPoint(Element element) {
		assertAttributeDefined(element, "id", CompilerFlags.ERROR); //$NON-NLS-1$
		assertAttributeDefined(element, "name", CompilerFlags.ERROR); //$NON-NLS-1$
		
		int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ATTRIBUTE);
		NamedNodeMap attrs = element.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr)attrs.item(i);
			String name = attr.getName();
			if ("name".equals(name)) { //$NON-NLS-1$
				validateTranslatableString(element, attr);
			} else if (!"id".equals(name) && !"schema".equals(name) && severity != CompilerFlags.IGNORE) { //$NON-NLS-1$ //$NON-NLS-2$
				reportUnknownAttribute(element, name, severity);
			}
		}
		
		severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
		if (severity != CompilerFlags.IGNORE) {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
				reportIllegalElement((Element)children.item(i), severity);
		}
	}
	
	protected boolean assertAttributeDefined(Element element, String attrName, int severity) {
		Attr attr = element.getAttributeNode(attrName);
		if (attr == null) {
			reportMissingRequiredAttribute(element, attrName, severity);
			return false;
		}
		return true;
	}
	
	protected void validateTranslatableString(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_NOT_EXTERNALIZED);
		if (severity == CompilerFlags.IGNORE)
			return;
		String value = attr.getValue();
		if (!value.startsWith("%")) { //$NON-NLS-1$
			report(PDE.getFormattedMessage("Builders.Manifest.non-ext-attribute", attr.getName()), getLine(element, attr.getName()), severity); //$NON-NLS-1$
		}	
	}

	protected void validateResourceAttribute(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_RESOURCE);
		if (severity == CompilerFlags.IGNORE)
			return;

		String[] resourcePaths = getResourcePaths(attr.getValue());
		IProject project = fFile.getProject();
		IResource resource = null;
		for (int i = 0; i < resourcePaths.length; i++) {
			resource = project.findMember(resourcePaths[i]);
			if (resource != null)
				break;
		}
		if (resource == null) {
			report(PDE.getFormattedMessage(
							"Builders.Manifest.resource", new String[] { attr.getValue(), attr.getName() }),  //$NON-NLS-1$
							getLine(element,
							attr.getName()), 
							severity);
		}
	}

	protected void validateJavaAttribute(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_CLASS);
		if (severity == CompilerFlags.IGNORE)
			return;

		String value = attr.getValue();
		IJavaProject javaProject = JavaCore.create(fFile.getProject());
		try {
			// be careful: people have the option to use the format:
			// fullqualifiedName:staticMethod
			int index = value.indexOf(":"); //$NON-NLS-1$
			if (index != -1)
				value = value.substring(0, index);

			IType javaType = javaProject.findType(value);
			if (javaType == null) {
				report(PDE.getFormattedMessage("Builders.Manifest.class", //$NON-NLS-1$
						new String[] { value, attr.getName() }), getLine(
						element, attr.getName()), severity);
			} 
		} catch (JavaModelException e) {
		}
	}
	
	protected void validateBoolean(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE);
		if (severity == CompilerFlags.IGNORE)
			return;
		
		String value = attr.getValue();
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) //$NON-NLS-1$ //$NON-NLS-2$
			return;
		
		reportIllegalAttributeValue(element, attr, severity);
	}

	protected void validateRestrictionAttribute(Element element, Attr attr, ISchemaRestriction restriction) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE);
		if (severity != CompilerFlags.IGNORE) {
			Object[] children = restriction.getChildren();
			String value = attr.getValue();
			for (int i = 0; i < children.length; i++) {
				Object child = children[i];
				if (child instanceof ISchemaEnumeration) {
					ISchemaEnumeration enumeration = (ISchemaEnumeration) child;
					if (enumeration.getName().equals(value)) {
						return;
					}
				}
			}
			reportIllegalAttributeValue(element, attr, severity);
		}
	}

	protected void reportEmptyRequiredAttribute(Element element, String attName, int severity) {
		String message = PDE
				.getFormattedMessage(
						"Builders.manifest.nonEmptyRequired", new String[] { attName, element.getNodeName() }); //$NON-NLS-1$			
		report(message, getLine(element, attName), severity);
	}

	protected void reportMissingRequiredAttribute(Element element, String attName, int severity) {
		String message = PDE
				.getFormattedMessage(
						"Builders.manifest.missingRequired", new String[] { attName, element.getNodeName() }); //$NON-NLS-1$			
		report(message, getLine(element), severity);
	}

	protected void reportUnknownAttribute(Element element, String attName, int severity) {
		String message = PDE.getFormattedMessage("Builders.Manifest.attribute", //$NON-NLS-1$
				attName);
		report(message, getLine(element, attName), severity);
	}

	protected void reportIllegalAttributeValue(Element element, Attr attr, int severity) {
		String message = PDE.getFormattedMessage("Builders.Manifest.att-value", //$NON-NLS-1$
				new String[] { attr.getValue(), attr.getName() });
		report(message, getLine(element, attr.getName()), severity);
	}
	
	protected void reportIllegalElement(Element element, int severity) {
		Node parent = element.getParentNode();
		if (parent == null || parent instanceof Document) {
			report(PDE.getResourceString("Builders.Manifest.illegalRoot"), getLine(element), severity); //$NON-NLS-1$
		} else {
			report(PDE.getFormattedMessage(
					"Builders.Manifest.child", new String[] { //$NON-NLS-1$
					element.getNodeName(), parent.getNodeName() }),
					getLine(element), severity);
		}
	}

	private String[] getResourcePaths(String path) {
		if (path.indexOf("$nl$") != -1) { //$NON-NLS-1$
			String language = TargetPlatform.getNL().substring(0, 2);
			String country = TargetPlatform.getNL().substring(3);
			String[] paths = new String[3];
			paths[0] = path
					.replaceAll(
							"\\$nl\\$", "nl" + IPath.SEPARATOR + language + IPath.SEPARATOR + country); //$NON-NLS-1$ //$NON-NLS-2$
			paths[1] = path.replaceAll(
					"\\$nl\\$", "nl" + IPath.SEPARATOR + language); //$NON-NLS-1$ //$NON-NLS-2$
			paths[2] = path.replaceAll("\\$nl\\$", ""); //$NON-NLS-1$ //$NON-NLS-2$
			return paths;
		}
		return new String[] { path };
	}

}
