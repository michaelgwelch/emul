package v9t9.engine.memory;


/**
 * @author ejs
 *
 */
public class ByteMemoryAccess {
	public byte[] memory;
	public int offset;

	public ByteMemoryAccess(byte[] memory, int offset) {
		this.memory = memory;
		this.offset = offset;
	}

	public ByteMemoryAccess(ByteMemoryAccess pattern) {
		this.memory = pattern.memory;
		this.offset = pattern.offset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + memory.hashCode();
		result = prime * result + offset;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ByteMemoryAccess other = (ByteMemoryAccess) obj;
		if (memory != other.memory) {
			return false;
		}
		if (offset != other.offset) {
			return false;
		}
		return true;
	}
	
	
}
