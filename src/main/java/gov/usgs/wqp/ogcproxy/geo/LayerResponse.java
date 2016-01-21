package gov.usgs.wqp.ogcproxy.geo;

import java.util.List;

import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;

public class LayerResponse {
	public class Layers {
		public class Layer {
			String name;
			String href;
			public String getName() {
				return name;
			}
			public void setName(String name) {
				this.name = name;
			}
			public String getHref() {
				return href;
			}
			public void setHref(String href) {
				this.href = href;
			}
		}
		private List<Layer> layer;
		public List<Layer> getLayer() {
			return layer;
		}
		public void setLayer(List<Layer> layer) {
			this.layer = layer;
		}
	}
	private Layers layers;
	public Layers getLayers() {
		return layers;
	}
	public void setLayers(Layers layers) {
		this.layers = layers;
	}
	
}
