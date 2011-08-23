/*
 * Copyright (C) 2007 by Quentin Anciaux
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *	@author Quentin Anciaux
 */



//import java.awt.Font;
//import java.awt.GraphicsEnvironment;
package org.allcolor.yahp.cl.converter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.allcolor.xml.parser.CShaniDomParser;
import org.allcolor.xml.parser.CXmlParser;
import org.allcolor.xml.parser.dom.ADocument;
import org.allcolor.yahp.cl.converter.CDocumentCut.DocumentAndSize;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.log4j.Logger;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ExtendedITextReplacedElementFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.lowagie.text.pdf.BaseFont;

/**
 * This class transform an html document in a PDF.
 * 
 * @author Quentin Anciaux
 * @version 0.02
 */
public final class CHtmlToPdfFlyingSaucerTransformer implements
		IHtmlToPdfTransformer {
	private static class _ITextRenderer extends ITextRenderer {
		private final Map knownFont = new HashMap();
		
		
		/**
		 * Initializes a new renderer with extended capabilities.
		 */
		public _ITextRenderer() {
			super();
			
			ITextOutputDevice outputDevice = getOutputDevice();
			
			ReplacedElementFactory replacedElementFactory = new ExtendedITextReplacedElementFactory(outputDevice);
			
			SharedContext sharedContext = getSharedContext();
			
			sharedContext.setReplacedElementFactory(replacedElementFactory);
		}

		private void addKnown(final String path) {
			this.knownFont.put(path, path);
		}

		private boolean isKnown(final String path) {
			return this.knownFont.get(path) != null;
		}
	}

	private static final Logger log = Logger
			.getLogger(CHtmlToPdfFlyingSaucerTransformer.class);

	private static boolean accept(final File dir, final String name) {
		return name.toLowerCase().endsWith(".ttf");
	}

	private static void registerTTF(final File f, final _ITextRenderer renderer) {
		if (f.isDirectory()) {
			final File[] list = f.listFiles();
			if (list != null) {
				for (int i = 0; i < list.length; i++) {
					CHtmlToPdfFlyingSaucerTransformer.registerTTF(list[i],
							renderer);
				}
			}
		} else if (CHtmlToPdfFlyingSaucerTransformer.accept(f.getParentFile(),
				f.getName())) {
			if (!renderer.isKnown(f.getAbsolutePath())) {
				/*InputStream in = null;
				try {
					in = f.toURI().toURL().openStream();
					GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
				} catch (final Throwable ignore) {
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (final Exception ignore) {
					}
				}*/
				try {
					renderer.getFontResolver().addFont(f.getAbsolutePath(),
							BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
					renderer.addKnown(f.getAbsolutePath());
				} catch (final Throwable ignore) {
				}
			}
		}
	}

	private static String removeScript(String a) {
		final List toRemove = new ArrayList();
		final Pattern p = Pattern.compile("(<script\\s*)");
		final Matcher m = p.matcher(a);
		int start = 0;
		while (m.find(start)) {
			final int is = m.start();
			int ie = m.start();
			while (ie < a.length()) {
				if (a.substring(ie).startsWith("</script>")) {
					ie = ie + 9;
					break;
				} else {
					ie++;
				}
			}
			start = ie + 1;
			toRemove.add(a.substring(is, ie));
			if (start >= a.length()) {
				break;
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			final String rem = (String) toRemove.get(i);
			final int index = a.indexOf(rem);
			a = a.substring(0, index) + a.substring(index + rem.length());
		}
		return a;
	}

	private final ThreadLocal tlparser = new ThreadLocal();

	private final ThreadLocal tlrenderer = new ThreadLocal();

	private final ThreadLocal tltidy = new ThreadLocal();

	/**
	 * Creates a new CHtmlToPdfFlyingSaucerTransformer object.
	 */
	public CHtmlToPdfFlyingSaucerTransformer() {
	}

	private void convertComboboxToVisibleHTML(final Document doc) {
		final NodeList nl = doc.getElementsByTagName("select");
		while (nl.getLength() > 0) {
			final Element select = (Element) nl.item(0);
			final String ssize = select.getAttribute("size");
			int size = 1;
			if (!"".equals(ssize.trim())) {
				try {
					size = Integer.parseInt(ssize);
				} catch (final Exception ignore) {
				}
			}
			final Node node = select.getParentNode();
			final NodeList options = select.getElementsByTagName("option");
			final String style = select.getAttribute("style");
			String width = null;
			String height = null;
			if (style.indexOf("width") != -1) {
				width = style.substring(style.indexOf("width") + 5);
				if (width.indexOf(':') != -1) {
					width = width.substring(width.indexOf(':'));
				}
				if (width.indexOf(';') != -1) {
					width = width.substring(0, width.indexOf(';'));
				}
			}
			if (width == null) {
				width = "50px";
			}
			if (style.indexOf("height") != -1) {
				height = style.substring(style.indexOf("height") + 6);
				if (height.indexOf(':') != -1) {
					height = height.substring(height.indexOf(':'));
				}
				if (height.indexOf(';') != -1) {
					height = height.substring(0, height.indexOf(';'));
				}
			}
			if (size > 1) {
				final Element span = doc.createElement("span");
				span
						.setAttribute("style",
								"display: inline-block;border: 1px solid black;"
										+ (width != null ? "width: " + width
												+ ";" : "")
										+ (height != null ? "height: " + height
												+ ";" : ""));
				for (int i = 0; (i < options.getLength()) && (i < size); i++) {
					final Element option = (Element) options.item(i);
					final Element content = doc.createElement("span");
					content.setTextContent(option.getTextContent());
					span.appendChild(content);
					if (i < options.getLength() - 1) {
						final Element br = doc.createElement("br");
						span.appendChild(br);
					}
				}
				node.insertBefore(span, select);
			} else {
				for (int i = 0; i < options.getLength(); i++) {
					final Element option = (Element) options.item(i);
					if ("selected".equalsIgnoreCase(option
							.getAttribute("selected"))) {
						final Element span = doc.createElement("span");
						span.setTextContent(option.getTextContent().trim());
						span.setAttribute("style",
								"display: inline-block;border: 1px solid black;"
										+ (width != null ? "width: " + width
												+ ";" : "")
										+ (height != null ? "height: " + height
												+ ";" : ""));
						node.insertBefore(span, select);
						break;
					}
				}
			}
			node.removeChild(select);
		}
	}

	private void convertInputToVisibleHTML(final Document doc) {
		final NodeList nl = doc.getElementsByTagName("input");
		while (nl.getLength() > 0) {
			final Element input = (Element) nl.item(0);
			final String type = input.getAttribute("type");
			final Node node = input.getParentNode();
			if ("image".equalsIgnoreCase(type)) {
				final Element img = doc.createElement("img");
				img.setAttribute("src", input.getAttribute("src"));
				img.setAttribute("alt", input.getAttribute("alt"));
				node.insertBefore(img, input);
			} else if ("text".equalsIgnoreCase(type)
					|| "password".equalsIgnoreCase(type)
					|| "button".equalsIgnoreCase(type)) {
				final String style = input.getAttribute("style");
				String width = null;
				String height = null;
				if (style.indexOf("width") != -1) {
					width = style.substring(style.indexOf("width") + 5);
					if (width.indexOf(':') != -1) {
						width = width.substring(width.indexOf(':'));
					}
					if (width.indexOf(';') != -1) {
						width = width.substring(0, width.indexOf(';'));
					}
				}
				if (width == null) {
					width = "100px";
				}
				if (style.indexOf("height") != -1) {
					height = style.substring(style.indexOf("height") + 6);
					if (height.indexOf(':') != -1) {
						height = height.substring(height.indexOf(':'));
					}
					if (height.indexOf(';') != -1) {
						height = height.substring(0, height.indexOf(';'));
					}
				}
				if (height == null) {
					height = "16px";
				}
				final Element span = doc.createElement("span");
				span.setTextContent(input.getAttribute("value"));
				span
						.setAttribute(
								"style",
								"display: inline-block;border: 1px solid black;"
										+ (width != null ? "width: " + width
												+ ";" : "")
										+ (height != null ? "height: " + height
												+ ";" : "")
										+ ("button".equalsIgnoreCase(type) ? "background-color: #DDDDDD;"
												: ""));
				node.insertBefore(span, input);
			} else if ("radio".equalsIgnoreCase(type)) {
				final Element span = doc.createElement("span");
				span.setTextContent("\u00A0");
				if ("checked".equalsIgnoreCase(input.getAttribute("checked"))) {
					span
							.setAttribute(
									"style",
									"display: inline-block;width: 10px;background-color: black;height: 10px;border: 1px solid black;");
				} else {
					span
							.setAttribute("style",
									"display: inline-block;width: 10px;height: 10px;border: 1px solid black;");
				}
				node.insertBefore(span, input);
			} else if ("checkbox".equalsIgnoreCase(type)) {
				final Element span = doc.createElement("span");
				span.setTextContent("\u00A0");
				if ("checked".equalsIgnoreCase(input.getAttribute("checked"))) {
					span
							.setAttribute(
									"style",
									"display: inline-block;width: 10px;background-color: black;height: 10px;border: 1px solid black;");
				} else {
					span
							.setAttribute("style",
									"display: inline-block;width: 10px;height: 10px;border: 1px solid black;");
				}
				node.insertBefore(span, input);
			}
			node.removeChild(input);
		}
	}

	private void convertTextAreaToVisibleHTML(final Document doc) {
		final NodeList nl = doc.getElementsByTagName("textarea");
		while (nl.getLength() > 0) {
			final Element textarea = (Element) nl.item(0);
			final Node node = textarea.getParentNode();
			final String style = textarea.getAttribute("style");
			String width = null;
			String height = null;
			if ((style.indexOf("display:none") != -1)
					|| (style.indexOf("display: none") != -1)
					|| (style.indexOf("visibility:hidden") != -1)
					|| (style.indexOf("visibility: hidden") != -1)) {
				node.removeChild(textarea);
				continue;
			}
			if (style.indexOf("width") != -1) {
				width = style.substring(style.indexOf("width") + 5);
				if (width.indexOf(':') != -1) {
					width = width.substring(width.indexOf(':'));
				}
				if (width.indexOf(';') != -1) {
					width = width.substring(0, width.indexOf(';'));
				}
			}
			if (width == null) {
				width = "100px";
			}
			if (style.indexOf("height") != -1) {
				height = style.substring(style.indexOf("height") + 6);
				if (height.indexOf(':') != -1) {
					height = height.substring(height.indexOf(':'));
				}
				if (height.indexOf(';') != -1) {
					height = height.substring(0, height.indexOf(';'));
				}
			}
			if (height == null) {
				height = "50px";
			}
			final Element span = doc.createElement("span");
			span
					.setAttribute("style",
							"display: inline-block;border: 1px solid black;"
									+ (width != null ? "width: " + width + ";"
											: "")
									+ (height != null ? "height: " + height
											+ ";" : ""));
			node.insertBefore(span, textarea);
			final String content = textarea.getTextContent();
			final BufferedReader reader = new BufferedReader(new StringReader(
					content));
			node.removeChild(textarea);
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					final Element econtent = doc.createElement("span");
					econtent.setTextContent(line);
					final Element br = doc.createElement("br");
					span.appendChild(econtent);
					span.appendChild(br);
				}
			} catch (final IOException ignore) {
			} finally {
				try {
					reader.close();
				} catch (final Exception ignore) {
				}
			}
		}

	}

	private CShaniDomParser getCShaniDomParser() {
		final Reference ref = (Reference) this.tlparser.get();
		final CShaniDomParser parser = (CShaniDomParser) (ref == null ? null
				: ref.get());
		if (parser != null) {
			return parser;
		}
		final CShaniDomParser ret = new CShaniDomParser(true, false);
		ret.setAutodoctype(false);
		ret.setIgnoreDTD(true);
		this.tlparser.set(new SoftReference(ret));
		return ret;
	}

	private _ITextRenderer getITextRenderer() {
		final Reference ref = (Reference) this.tlrenderer.get();
		final _ITextRenderer renderer = (_ITextRenderer) (ref == null ? null
				: ref.get());
		if (renderer != null) {
			return renderer;
		}
		final _ITextRenderer ret = new _ITextRenderer();
		this.tlrenderer.set(new SoftReference(ret));
		return ret;
	}

	private Tidy getTidy() {
		final Reference ref = (Reference) this.tltidy.get();
		final Tidy tidy = (Tidy) (ref == null ? null : ref.get());
		if (tidy != null) {
			return tidy;
		}
		final Tidy ret = new Tidy();
		ret.setInputEncoding("utf-8");
		ret.setXHTML(true);
		ret.setQuiet(true);
		ret.setShowWarnings(false);
		ret.setXmlOut(true);
		ret.setFixComments(true);
		ret.setEscapeCdata(true);
		ret.setMakeBare(true);
		ret.setMakeClean(true);
		ret.setFixBackslash(true);
		ret.setIndentContent(true);
		ret.setLogicalEmphasis(true);
		ret.setJoinClasses(true);
		ret.setJoinStyles(true);
		ret.setDocType("strict");
		ret.setErrout(new PrintWriter(new Writer() {
			public void close() throws IOException {
			}

			public void flush() throws IOException {
			}

			public void write(final char[] cbuf, final int off, final int len)
					throws IOException {
			}
		}));
		this.tltidy.set(new SoftReference(ret));
		return ret;
	}
	
	private String normalizeLink(String base,String href) {
		if (href.indexOf("//") == -1) {
			if (base != null) {
				if (href.startsWith("/")) {
					String scheme = base.substring(0, base
							.indexOf("://"));
					base = base.substring(base.indexOf("://") + 3);

					if (base.indexOf('/') != -1) {
						String host = base.substring(0, base
								.indexOf('/'));
						href = scheme + "://" + host + href;
					} // end if
					else {
						href = scheme + "://" + base + href;
					} // end else
				} // end if
				else {
					if (href.startsWith("../")) {
						String scheme = base.substring(0, base
								.indexOf("://"));
						String host = base.substring(base
								.indexOf("://") + 3);

						if (host.indexOf('/') != -1) {
							host = host.substring(0, host.indexOf('/'));
						}

						if (base.indexOf('/') != -1) {
							base = base.substring(0, base
									.lastIndexOf('/'));
						}

						while (href.startsWith("../")) {
							href = href.substring(3);

							if (base.lastIndexOf('/') != -1) {
								base = base.substring(0, base
										.lastIndexOf('/'));
							}
						} // end while

						if (base.indexOf(host) == -1) {
							base = scheme + "://" + host + "/";
						} else {
							base = base + "/";
						}

						href = base + href;
					} // end if
					else {
						base = base.substring(0,
								base.lastIndexOf('/') + 1);
						href = base + href;
					} // end else
				} // end else
			} // end if
		} else if (href.startsWith("//")) {
			href = "http:" + href;
		}
		return href;
	}

	/**
	 * Transform the html document in the inputstream to a pdf in the
	 * outputstream
	 * 
	 * @param in
	 *            html document stream
	 * @param urlForBase
	 *            base url of the document
	 * @param size
	 *            pdf document page size
	 * @param hf
	 *            header-footer list
	 * @param properties
	 *            transform properties
	 * @param out
	 *            out stream to the pdf file
	 */
	public final void transform(final InputStream in, String urlForBase,
			final PageSize size, final List hf, final Map properties,
			final OutputStream out) throws CConvertException {
		final List files = new ArrayList();
		try {
			final Tidy tidy = this.getTidy();
			final CShaniDomParser parser = this.getCShaniDomParser();
			final _ITextRenderer renderer = this.getITextRenderer();
			final Reader r = CXmlParser.getReader(in);
			final StringBuffer s = new StringBuffer();
			final char[] buffer = new char[2048];
			int inb = -1;
			while ((inb = r.read(buffer)) != -1) {
				s.append(buffer, 0, inb);
			}
			r.close();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			tidy.parse(
					new ByteArrayInputStream(s.toString().getBytes("utf-8")),
					bout);
			final String result = CHtmlToPdfFlyingSaucerTransformer
					.removeScript(new String(bout.toByteArray(), "utf-8"));
			Document theDoc = parser
					.parse(new InputStreamReader(new ByteArrayInputStream(
							result.getBytes("utf-8")), "utf-8"));
			if (theDoc.toString().length() == 0) {
				theDoc = parser.parse(new StringReader(
						CHtmlToPdfFlyingSaucerTransformer.removeScript(s
								.toString())));
			}
			this.convertInputToVisibleHTML(theDoc);
			this.convertComboboxToVisibleHTML(theDoc);
			this.convertTextAreaToVisibleHTML(theDoc);
			final NodeList styles = theDoc.getElementsByTagName("style");
			for (int i = 0; i < styles.getLength(); i++) {
				final Node n = styles.item(i);
				final StringBuffer style = new StringBuffer();
				while (n.getChildNodes().getLength() > 0) {
					final Node child = n.getChildNodes().item(0);
					if (child.getNodeType() == Node.COMMENT_NODE) {
						final Comment c = (Comment) child;
						style.append(c.getData());
					} else if (child.getNodeType() == Node.TEXT_NODE) {
						final Text c = (Text) child;
						style.append(c.getData());
					} else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
						final CDATASection c = (CDATASection) child;
						style.append(c.getData());
					}
					n.removeChild(child);
				}

				final String content = style.toString().trim();
				final Text start = theDoc.createTextNode("/*");
				final CDATASection cd = theDoc.createCDATASection("*/\n"
						+ content + "\n/*");
				final Text end = theDoc.createTextNode("*/\n");
				n.appendChild(start);
				n.appendChild(cd);
				n.appendChild(end);
			}
			final List toRemove = new ArrayList();
			final NodeList tnl = theDoc.getChildNodes();
			for (int i = 0; i < tnl.getLength(); i++) {
				final Node n = tnl.item(i);
				if (n != theDoc.getDocumentElement()) {
					toRemove.add(n);
				}
			}
			final Node title = theDoc.getDocumentElement()
					.getElementsByTagName("title").item(0);
			if ((title != null)
					&& (properties.get(IHtmlToPdfTransformer.PDF_TITLE) == null)) {
				properties.put(IHtmlToPdfTransformer.PDF_TITLE, title
						.getTextContent());
			}

			Node body = theDoc.getDocumentElement()
					.getElementsByTagName("body").item(0);
			Node head = theDoc.getDocumentElement()
					.getElementsByTagName("head").item(0);
			for (int i = 0; i < toRemove.size(); i++) {
				final Node n = (Node) toRemove.get(i);
				n.getParentNode().removeChild(n);
				if (n.getNodeType() == Node.TEXT_NODE) {
					final Text t = (Text) n;
					if (t.getData().trim().length() == 0) {
						continue;
					}
				}
				if ("link".equals(n.getNodeName())
						|| "style".equals(n.getNodeName())) {
					head.appendChild(n);
				} else {
					body.appendChild(n);
				}
			}
			final DocumentAndSize docs[] = CDocumentCut.cut(theDoc, size);
			for (int jj = 0; jj < docs.length; jj++) {
				Document mydoc = docs[jj].doc;
				body = mydoc.getDocumentElement().getElementsByTagName("body")
						.item(0);
				head = mydoc.getDocumentElement().getElementsByTagName("head")
						.item(0);
				try {
					String surlForBase = ((Element) mydoc.getElementsByTagName(
							"base").item(0)).getAttribute("href");
					if ((surlForBase == null) || "".equals(surlForBase)) {
						surlForBase = null;
					}
					if (surlForBase != null) {
						urlForBase = surlForBase;
					}
				} catch (final Exception ignore) {
				}
				if (urlForBase != null) {
					((ADocument) mydoc).setDocumentURI(urlForBase);
				}
				final NodeList nl = mydoc.getElementsByTagName("base");

				if (nl.getLength() == 0) {
					final ADocument doc = (ADocument) mydoc;
					final Element base = doc.createElement("base");
					base.setAttribute("href", urlForBase);

					if (head.getFirstChild() != null) {
						head.insertBefore(base, head.getFirstChild());
					} // end if
					else {
						head.appendChild(base);
					} // end else
				} else {
					final Element base = (Element) nl.item(0);
					base.setAttribute("href", urlForBase);
				}
				/* STEF: remove this, because it works well without this code, and when enabled, CSS selectors
				 * separated by commas are ignored for some reason.
				final NodeList linknl = mydoc.getElementsByTagName("link");
				for (int zi=0;zi<linknl.getLength();zi++) {
					try {
						Element e = (Element)linknl.item(zi);
						if ("stylesheet".equals(e.getAttribute("rel"))) {
							String url =  normalizeLink(urlForBase, e.getAttribute("href"));
							InputStream iin = null;
							try {
								iin = new URL(url).openStream();
								ByteArrayOutputStream bOut = new ByteArrayOutputStream();
								int iNbByteRead = -1;
								byte bbuffer[] = new byte[16384];

								while ((iNbByteRead = iin.read(bbuffer)) != -1) {
									bOut.write(bbuffer, 0, iNbByteRead);
								} // end while
								String document = new String(bOut.toByteArray(), "utf-8");
								CSSStyleSheet sheet = CCSSParser.parse(document, null,
										null, url);
								if (sheet != null) {
									Element style = mydoc.createElement("style");
									style.setAttribute("type", "text/css");
									style.setTextContent(sheet.toString());
									e.getParentNode().insertBefore(style, e);
									e.getParentNode().removeChild(e);
								}
							} // end try
							catch (final Throwable ignore) {
							} // end catch
							finally {
								try{iin.close();}catch(Throwable ignore){}
							}
						}
					}
					catch (Throwable ignore){}
				}
				*/
				final NumberFormat nf = NumberFormat.getInstance(Locale.US);
				nf.setMaximumFractionDigits(2);
				nf.setMinimumFractionDigits(0);
				final Element style = mydoc.createElement("style");
				style.setAttribute("type", "text/css");
				final double[] dsize = docs[jj].size.getCMSize();
				final double[] dmargin = docs[jj].size.getCMMargin();
				style.setTextContent("\n@page {\n" + "size: "
						+ nf.format(dsize[0] / 2.54) + "in "
						+ nf.format(dsize[1] / 2.54) + "in;\n"
						+ "margin-left: " + nf.format(dmargin[0] / 2.54)
						+ "in;\n" + "margin-right: "
						+ nf.format(dmargin[1] / 2.54) + "in;\n"
						+ "margin-bottom: " + nf.format(dmargin[2] / 2.54)
						+ "in;\n" + "margin-top: "
						+ nf.format(dmargin[3] / 2.54) + "in;\npadding: 0in;\n"
						+ "}\n"

				);
				head.appendChild(style);
				if (properties.get(IHtmlToPdfTransformer.FOP_TTF_FONT_PATH) != null) {
					final File dir = new File((String) properties
							.get(IHtmlToPdfTransformer.FOP_TTF_FONT_PATH));
					if (dir.isDirectory()) {
						CHtmlToPdfFlyingSaucerTransformer.registerTTF(dir,
								renderer);
					}
				}
				((ADocument) mydoc).setInputEncoding("utf-8");
				((ADocument) mydoc).setXmlEncoding("utf-8");
				renderer.getSharedContext().setBaseURL(urlForBase);
				mydoc = parser.parse(new StringReader(mydoc.toString()));
				mydoc.getDomConfig().setParameter("entities", Boolean.FALSE );
				mydoc.normalizeDocument();
				renderer.setDocument(mydoc, urlForBase);
				renderer.layout();
				final java.io.File f = java.io.File.createTempFile("pdf",
						"yahp");
				files.add(f);
				final OutputStream fout = new BufferedOutputStream(
						new FileOutputStream(f));
				renderer.createPDF(fout, true);
				fout.flush();
				fout.close();
			}
			final PageSize[] sizes = new PageSize[docs.length];
			for (int i = 0; i < docs.length; i++) {
				sizes[i] = docs[i].size;

			}
			CDocumentReconstructor
					.reconstruct(
							files,
							properties,
							out,
							urlForBase,
							"Flying Saucer Renderer (https://xhtmlrenderer.dev.java.net/)",
							sizes, hf);
		} catch (final Throwable e) {
			CHtmlToPdfFlyingSaucerTransformer.log.error(e);
			throw new CConvertException(
					"ERROR: An unhandled exception occured: " + e.getMessage(),
					e);
		} finally {
			try {
				out.flush();
			} catch (final Exception ignore) {
			}
			for (final Iterator it = files.iterator(); it.hasNext();) {
				final java.io.File f = (File) it.next();

				try {
					f.delete();
				} // end try
				catch (final Exception ignore) {
				}
			} // end for
		}
	}

}
