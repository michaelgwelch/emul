/**
 * 
 */
package v9t9.emulator.clients.builtin;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.ejs.coffee.core.properties.SettingProperty;

/**
 * @author ejs
 *
 */
public class IconSetting extends SettingProperty implements ISettingDecorator {

	private final URL iconPath;

	/**
	 * @param name
	 * @param storage
	 */
	public IconSetting(String name, String label, String description, Object storage, URL iconPath) {
		super(name, label, description, storage);
		this.iconPath = iconPath;
	}

	/* (non-Javadoc)
	 * @see v9t9.emulator.clients.builtin.ISettingDecorator#getIcon()
	 */
	public ImageDescriptor getIcon() {
		return ImageDescriptor.createFromURL(iconPath);
	}

}
