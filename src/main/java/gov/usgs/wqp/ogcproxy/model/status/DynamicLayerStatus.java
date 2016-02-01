package gov.usgs.wqp.ogcproxy.model.status;

/**
 * DynamicLayerStatus
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known states related to building
 *	a layer.
 */
public enum DynamicLayerStatus {
	AVAILABLE, BUILDING, INITIATED, EMPTY, ERROR;

}
