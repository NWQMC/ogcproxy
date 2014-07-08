<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
  <head><title>WMS PROXY - ${site} Cache Status</title></head>
  <body>
    <h1>${site} - Cache Status</h1>
    <h3>Total Layers Cached: ${cache.size()}</h3>
    <table style="border-collapse:collapse;">
    	<tr>
    		<td style="border: 2px solid black;font-size: 20px;font-weight: bold;text-align: center;padding-left: 5px;padding-right: 5px;">
    			Layer Name
    		</td>
    		<td style="border: 2px solid black;font-size: 20px;font-weight: bold;text-align: center;padding-left: 5px;padding-right: 5px;">
    			Originating Service
    		</td>
    		<td style="border: 2px solid black;font-size: 20px;font-weight: bold;text-align: center;padding-left: 5px;padding-right: 5px;">
    			Date Created
    		</td>
    		<td style="border: 2px solid black;font-size: 20px;font-weight: bold;text-align: center;padding-left: 5px;padding-right: 5px;">
    			Layer Status
    		</td>
    		<td style="border: 2px solid black;font-size: 20px;font-weight: bold;text-align: center;padding-left: 5px;padding-right: 5px;">
    			Layer Search Params
    		</td>
    	</tr>
    	<c:forEach var="cacheItem" items="${cache}" varStatus="status">
    		<tr>
    			<td style="border: 1px solid black;padding-left: 5px;padding-right: 5px;">
    				${cacheItem.layerName}
	    		</td>
	    		<td style="border: 1px solid black;padding-left: 5px;padding-right: 5px;">
    				${cacheItem.originatingService}
	    		</td>
	    		<td style="border: 1px solid black;padding-left: 5px;padding-right: 5px;">
	    			<fmt:formatDate type="both" dateStyle="long" timeStyle="long" value="${cacheItem.dateCreated}" />
	    		</td>
	    		<td style="border: 1px solid black;padding-left: 5px;padding-right: 5px;">
	    			${cacheItem.currentStatus}
	    		</td>
	    		<td style="border: 1px solid black;padding-left: 5px;padding-right: 5px;">
	    			<table>
	    				<c:forEach var="searchItem" items="${cacheItem.searchParameters}" varStatus="status">
	    					<tr>
	    						<td style="padding-right: 10px;">
	    							${searchItem.key}:
	    						</td>
	    						<td>
	    							${searchItem.value}
	    						</td>
	    					</tr>
	    				</c:forEach>
	    			</table>
	    		</td>
    		</tr>
    	</c:forEach>
    </table>
  </body>
</html>