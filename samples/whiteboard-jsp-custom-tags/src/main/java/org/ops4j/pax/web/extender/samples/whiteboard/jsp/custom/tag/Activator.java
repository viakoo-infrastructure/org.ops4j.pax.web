/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.samples.whiteboard.jsp.custom.tag;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultJspMapping;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Activator implements BundleActivator {

	private static final String CONTEXT_PATH = "sample";
	private static final String SAMPLE_CONTEXT = "sampleContext";
	private static final String CONTEXT_SELECTOR = "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + SAMPLE_CONTEXT + ")";

	private ServiceRegistration<Servlet> servletReg;
	private ServiceRegistration<HttpContext> httpContextReg;
	private ServiceRegistration<JspMapping> jspMappingRegistration;
	private ServiceRegistration<ResourceMapping> resourcesReg;

	class HttpContextWrapper implements HttpContext {

		private final HttpContext wrapped;

		public HttpContextWrapper(HttpContext httpContext) {
			this.wrapped = httpContext;
		}

		@Override
		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			// implement security
			return true;
		}

		@Override
		public URL getResource(String name) {
			return wrapped.getResource(name);
		}

		@Override
		public String getMimeType(String name) {
			return wrapped.getMimeType(name);
		}
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Dictionary<String, String> props;

		// I know, should be a tracker on this, but we know it's up for testing
		final ServiceReference<WebContainer> wcServiceRef = bundleContext.getServiceReference(WebContainer.class);
		final WebContainer webContainer = bundleContext.getService(wcServiceRef);

		// fWrappingEnabled=false: custom tag works, but can't customize handleSecurity
		// fWrappingEnabled=true: failure of custom tag, but handleSecurity in wrapper called
		final boolean fWrappingEnabled = false;

		// if fWhiteboard is true, fail with error 404
		final boolean fWhiteboard = false;

		props = new Hashtable<>();
		if(fWhiteboard) {
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, SAMPLE_CONTEXT);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, CONTEXT_PATH);
		} else {
			props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, SAMPLE_CONTEXT);
			props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_PATH, CONTEXT_PATH);
		}
		final HttpContext httpContext = webContainer.createDefaultHttpContext(SAMPLE_CONTEXT);
		if(!fWrappingEnabled) {
			httpContextReg = bundleContext.registerService(HttpContext.class, httpContext, props);
		} else {
			final HttpContext wrappedHttpContext = new HttpContextWrapper(httpContext);
			httpContextReg = bundleContext.registerService(HttpContext.class, wrappedHttpContext, props);
		}

		props = new Hashtable<>();
		final DefaultJspMapping jspMapping = new DefaultJspMapping();
		if(fWhiteboard) {
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, CONTEXT_SELECTOR);
		} else {
			jspMapping.setHttpContextId(SAMPLE_CONTEXT);
		}
		jspMappingRegistration = bundleContext.registerService(JspMapping.class, jspMapping, props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/index");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Index");
		if(fWhiteboard) {
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, CONTEXT_SELECTOR);
		} else {
			props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, SAMPLE_CONTEXT);
		}
		servletReg = bundleContext.registerService(Servlet.class, new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				req.getRequestDispatcher("index.jsp").forward(req, resp);
			}
		}, props);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(jspMappingRegistration != null) {
			jspMappingRegistration.unregister();
			jspMappingRegistration = null;
		}
		if(servletReg != null) {
			servletReg.unregister();
			servletReg = null;
		}
		if(resourcesReg != null) {
			resourcesReg.unregister();
			resourcesReg = null;
		}
		if(httpContextReg != null) {
			httpContextReg.unregister();
			httpContextReg = null;
		}
	}

}
