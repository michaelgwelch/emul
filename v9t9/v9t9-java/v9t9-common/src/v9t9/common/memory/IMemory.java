/**
 * 
 */
package v9t9.common.memory;

import ejs.base.properties.IPersistable;

/**
 * @author ejs
 *
 */
public interface IMemory extends IPersistable {

	IMemoryEntryFactory getMemoryEntryFactory();
	void setMemoryEntryFactory(IMemoryEntryFactory factory);
	
	void addListener(IMemoryListener listener);

	void removeListener(IMemoryListener listener);

	void notifyListenersOfPhysicalChange(IMemoryEntry entry);

	void notifyListenersOfLogicalChange(IMemoryEntry entry);

	void addDomain(String key, IMemoryDomain domain);

	IMemoryDomain getDomain(String key);

	void addAndMap(IMemoryEntry entry);

	void removeAndUnmap(IMemoryEntry entry);

	void setModel(IMemoryModel model);
	
	IMemoryModel getModel();

	IMemoryDomain[] getDomains();

	/**
	 * 
	 */
	void save();

	/**
	 * 
	 */
	void clear();

}