/**
 * 
 */
package v9t9.common.modules;

import java.net.URI;
import java.net.URL;

/**
 * @author ejs
 *
 */
public interface IModule {
	URI getDatabaseURI();
	
	String getName();
	
	/** Get filename or path to associated image, or <code>null</code> */
	String getImagePath();
	/** Get resolved URL for image, or <code>null</code> */
	URL getImageURL();
	
	MemoryEntryInfo[] getMemoryEntryInfos();
}
