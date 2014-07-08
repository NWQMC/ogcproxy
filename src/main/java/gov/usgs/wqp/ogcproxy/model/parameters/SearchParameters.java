package gov.usgs.wqp.ogcproxy.model.parameters;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("serial")
public class SearchParameters<T, K> extends ConcurrentHashMap<String, List<String>> {
	private final int prime = 31;
	private int currentHash = 1;
	
	@Override
	public List<String> put(String key, List<String> value) {
		/**
		 * We need to keep track of these hash keys.  If we replace an existing 
		 * one we have to make sure we remove it before placing it.
		 */
		if(super.containsKey(key)) {
			this.remove(key);
		}
		
		int newHash = this.prime + key.hashCode();
		
		for(String s : value) {
			newHash = newHash * this.prime + s.hashCode();
		}
		
		this.currentHash += newHash;
		
		return super.put(key, value);
	}
	
	@Override
	public List<String> remove(Object key) {
		List<String> oldValue = super.remove(key);
		
		if(oldValue == null) {
			return null;
		}
		
		int oldHash = this.prime + ((String)key).hashCode();
		
		for(String s : oldValue) {
			oldHash = oldHash * this.prime + s.hashCode();
		}
		
		this.currentHash -= oldHash;
		
		return oldValue;
	}
	
	@Override
	public int hashCode() {
		return this.currentHash;
	}
	
	public long unsignedHashCode() {
		/**
		 * Since we want to use the hash as a descriptor for a layer, we want
		 * to use unsigned values for the hash.
		 */
		if(this.currentHash > 0) {
			return (long)this.currentHash;
		}
		
		long unsignedHash = this.currentHash & (-1L >>> 32);
		
		return unsignedHash;
	}
}
