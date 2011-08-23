package org.xhtmlrenderer.pdf;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.codec.Base64;



/**
 * Refines rendering of XML elements, for example to allow the processing of data uri images.
 * 
 * @author Stéphane Thomas
 */
public class ExtendedITextReplacedElementFactory extends ITextReplacedElementFactory {
	private static final Logger log = Logger.getLogger(ExtendedITextReplacedElementFactory.class);
	
	
	/**
	 * Creates a new factory.
	 * 
	 * @param outputDevice output device
	 */
	public ExtendedITextReplacedElementFactory(ITextOutputDevice outputDevice) {
		super(outputDevice);
	}
	
	/**
	 * Renders an XML element (i.e. build its visual representation).
	 * 
	 * @see org.xhtmlrenderer.pdf.ITextReplacedElementFactory#createReplacedElement(org.xhtmlrenderer.layout.LayoutContext, org.xhtmlrenderer.render.BlockBox, org.xhtmlrenderer.extend.UserAgentCallback, int, int) for more information
	 */
	public ReplacedElement createReplacedElement(LayoutContext layoutContext, BlockBox blockBox, UserAgentCallback userAgentCallback, int cssWidth, int cssHeight) {
		Element element = blockBox.getElement();
		
		// Handles any images with a data uri, e.g.:
		// 
		//   <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUg ... 9TXL0Y4OHwAAAABJRU5ErkJggg==" alt="Red dot" />
		// 
		if (isDataUriImage(element)) {
			String src = element.getAttribute("src");
			
			try {
				String encoded = StringUtils.substringAfter(src, ";base64,");
				
				if (!encoded.isEmpty()) {
					byte[] decoded = Base64.decode(encoded);
					
					FSImage image = new ITextFSImage(Image.getInstance(decoded));
					
					if ((cssWidth != -1) || (cssHeight != -1)) {
						image.scale(cssWidth, cssHeight);
					}
					
					log.trace("Replaced data uri image successfully");
					
					return new ITextImageElement(image);
				} else {
					log.warn("Unable to replace data uri image because of empty data");
				}
			} catch (Exception exception) {
				log.warn("Unable to replace data uri image", exception);
			}
			
			return null;
		} else {
			return super.createReplacedElement(layoutContext, blockBox, userAgentCallback, cssWidth, cssHeight);
		}
	}
	
	/**
	 * Determines whether the specified XML element is an image with a data uri or not.
	 * 
	 * @param element element to analyze
	 * @return true if the specified element is an image with a data uri, false otherwise
	 * @see http://en.wikipedia.org/wiki/Data_URI_scheme for more information
	 */
	protected boolean isDataUriImage(Element element) {
		if (element != null) {
			String tag = element.getNodeName();
			
			if (tag.equalsIgnoreCase("img")) {
				String src = element.getAttribute("src");
				
				if (src != null) {
					// Prepares the source value to make sure the next comparison will be case insensitive
					src = src.trim().toLowerCase();
					
					return src.startsWith("data:image/");
				}
			}
		}
		
		return false;
	}
}