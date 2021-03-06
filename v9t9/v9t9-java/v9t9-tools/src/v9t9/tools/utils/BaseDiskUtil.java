package v9t9.tools.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import v9t9.common.files.Catalog;
import v9t9.common.files.CatalogEntry;
import v9t9.common.files.IEmulatedDisk;
import v9t9.common.files.IEmulatedFile;
import v9t9.common.files.IEmulatedFileHandler;
import v9t9.common.files.NativeFileFactory;
import v9t9.common.machine.IMachine;
import ejs.base.utils.Pair;

/**
 * @author ejs
 *
 */
public class BaseDiskUtil {
	protected IMachine machine;
	protected PrintStream out;
	
	public BaseDiskUtil(IMachine machine, PrintStream out) {
		this.machine = machine;
		this.out = out;
	}

	protected IEmulatedDisk resolveDisk(String path, boolean shouldBeFormatted) throws IOException {
		File target = new File(path);
		IEmulatedFileHandler handler = machine.getEmulatedFileHandler();
		IEmulatedDisk image;
		if (target.isDirectory())
			image = handler.getFilesInDirectoryMapper().createDiskDirectory(target);
		else
			image = handler.getDiskImageMapper().createDiskImage(target);
		if (!image.isFormatted())
			throw new IOException("disk image not formatted: " + path);
		return image;
	}
	
	
	protected IEmulatedDisk resolveDisk(String path) throws IOException {
		return resolveDisk(path, true);
	}
	
	protected Pair<IEmulatedDisk, String> decode(String arg) throws IOException {
		IEmulatedDisk disk;
		String pattern = null;
		
		int colIdx = arg.lastIndexOf(':');
		String path;
		if (colIdx > 1) {
			path = arg.substring(0, colIdx);
			arg = arg.substring(colIdx+1);
			pattern = "(?i)" + arg.replace("*", ".*").replace("?", ".");
		} else {
			path = arg;
		}
		
		try {
			disk = resolveDisk(path);
			return new Pair<IEmulatedDisk, String>(disk, pattern);
		} catch (IOException e) {
			// see if the file is a FIAD entry
			File fiadFile = new File(arg);
			/*NativeFile nativeFile =*/ NativeFileFactory.INSTANCE.createNativeFile(fiadFile);
			return new Pair<IEmulatedDisk, String>(resolveDisk(fiadFile.getParent()), fiadFile.getName());
		}
	}

	protected void getSrcFiles(List<IEmulatedFile> srcFiles, IEmulatedDisk disk,
			String pattern) throws IOException {
		if (!disk.isFormatted())
			return;
		
		Catalog catalog = disk.readCatalog();
		for (CatalogEntry entry : catalog.getEntries()) {
			if (pattern == null 
					|| entry.getFile().getFileName().matches("(?i)" + pattern)) {
				
				srcFiles.add(entry.getFile());
			}
		}
	}

}
