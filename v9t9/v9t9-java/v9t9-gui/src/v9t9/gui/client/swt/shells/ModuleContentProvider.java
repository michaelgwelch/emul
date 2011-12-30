/**
 * 
 */
package v9t9.gui.client.swt.shells;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeNodeContentProvider;

import v9t9.common.files.IPathFileLocator;
import v9t9.common.machine.IMachine;
import v9t9.common.memory.MemoryEntryInfo;
import v9t9.common.memory.StoredMemoryEntryInfo;
import v9t9.common.modules.IModule;
import v9t9.gui.client.swt.shells.ModuleSelector.ErrorTreeNode;
import v9t9.gui.client.swt.shells.ModuleSelector.InfoTreeNode;
import ejs.base.properties.IProperty;
import ejs.base.utils.HexUtils;
import ejs.base.utils.Pair;

/**
 * @author ejs
 *
 */
public class ModuleContentProvider extends TreeNodeContentProvider {

	private final IMachine machine;
	private final IPathFileLocator pathFileLocator;
	
	
	public ModuleContentProvider(IMachine machine) {
		this.machine = machine;
		this.pathFileLocator = machine.getPathFileLocator();
	}

	/**
	 * @param module2
	 * @return
	 */
	public Object createModuleContent(IModule module) {
		List<TreeNode> kids = new ArrayList<TreeNode>();

		
		TreeNode moduleDatabase = new TreeNode(new Pair<String, String>(
				"Module Defined By", module.getDatabaseURI().toString()));
		
		kids.add(moduleDatabase);

		TreeNode memInfoNode = new TreeNode(module);
		MemoryEntryInfo[] infos = module.getMemoryEntryInfos();
		
		List<TreeNode> memNodes = new ArrayList<TreeNode>();
		for (MemoryEntryInfo info : infos) {
			if (!info.isBanked()) {
				addMemoryInfoNode(memNodes, info,
						info.getName(), info.getFilename(), info.getOffset());
			} else {
				addMemoryInfoNode(memNodes, info, 
						info.getName() + " (bank 0)", 
						info.getFilename(), 
						info.getOffset());

				addMemoryInfoNode(memNodes, info, 
						info.getName() + " (bank 1)", 
						info.getFilename2(), 
						info.getOffset2());
			}
		}
		memInfoNode.setChildren(memNodes.toArray(new TreeNode[memNodes.size()]));
		kids.add(memInfoNode);

		for (IProperty prop : pathFileLocator.getSearchPathProperties()) {
			kids.add(makeTreeNode(prop));
		}
		

		return (TreeNode[]) kids.toArray(new TreeNode[kids.size()]);
	}

	protected void addMemoryInfoNode(
			List<TreeNode> memNodes, MemoryEntryInfo info,
			String name, String filename, int offset) {
		StoredMemoryEntryInfo storedInfo;
		try {
			storedInfo = StoredMemoryEntryInfo.resolveStoredMemoryEntryInfo(
					pathFileLocator, machine.getSettings(), 
					machine.getMemory(), info, 
					name, filename, offset);
			memNodes.add(makeTreeNode(storedInfo));
		} catch (IOException e) {
			TreeNode errorNode = new ErrorTreeNode(new Pair<String, String>(filename,
					e instanceof FileNotFoundException ? "File not found on search paths" : e.getMessage()));
			TreeNode[] kids = new TreeNode[] {
					makeTreeNode(info),
					};
			errorNode.setChildren(kids);
			memNodes.add(errorNode);
		}
	}
	

	private TreeNode makeTreeNode(StoredMemoryEntryInfo info) {
		TreeNode node = new TreeNode(info);
		TreeNode[] kids = new TreeNode[3];
		kids[0] = new TreeNode(new Pair<String, String>("Location", info.uri.toString()));
		kids[1] = new TreeNode(new Pair<String, String>("File Size", ""+info.filesize));
		kids[2] = makeTreeNode(info.info);
		node.setChildren(kids);
		return node;
	}


	private TreeNode makeTreeNode(MemoryEntryInfo info) {
		TreeNode node = new TreeNode(info);
		Map<String, Object> props = info.getProperties();
		List<TreeNode> kids = new ArrayList<TreeNode>();
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getKey().equals(MemoryEntryInfo.CLASS))
				continue;
			if (entry.getKey().equals(MemoryEntryInfo.ADDRESS) 
					|| entry.getKey().equals(MemoryEntryInfo.OFFSET)
					|| entry.getKey().equals(MemoryEntryInfo.OFFSET2)
					)
				kids.add(new TreeNode(new Pair<String, String>(entry.getKey(), 
						">" + HexUtils.toHex4(((Number) entry.getValue()).intValue()))));
			else if (entry.getKey().equals(MemoryEntryInfo.SIZE)) {
				int size = ((Number) entry.getValue()).intValue();
				kids.add(new TreeNode(new Pair<String, String>(entry.getKey(),
							size == 0 ? "any size" : 
								(size < 0 ? "at most " : "") + ">" + HexUtils.toHex4(size))  ));
			}
			else
				kids.add(new TreeNode(entry));
		}
		node.setChildren(kids.toArray(new TreeNode[kids.size()]));
		return node;
	}

	private TreeNode makeTreeNode(IProperty pathProperty) {
		TreeNode node = new TreeNode(pathProperty);
		List<TreeNode> kids = new ArrayList<TreeNode>();
		if (pathProperty.getValue() instanceof List) {
			if (!pathProperty.getList().isEmpty()) {
				for (Object path : pathProperty.getList()) {
					kids.add(createPathNode(path));
				}
			} else {
				kids.add(new InfoTreeNode(new Pair<String, String>("Empty", "")));				
			}
		} else {
			kids.add(createPathNode(pathProperty.getValue()));
		}
		node.setChildren((TreeNode[]) kids.toArray(new TreeNode[kids.size()]));
		return node;
	}

	/**
	 * @param kids
	 * @param idx
	 * @param path
	 * @return
	 */
	protected TreeNode createPathNode(Object path) {
		try {
			URI uri = pathFileLocator.createURI(path.toString());
			return pathFileLocator.exists(uri) ? new TreeNode(uri) : new ErrorTreeNode(uri);
		} catch (URISyntaxException e) {
			return new ErrorTreeNode(new Pair<String, String>(path.toString(), e.getMessage()));
		}
	}
}